package com.melonet.app.data.realtime

import com.melonet.app.BuildConfig
import com.melonet.app.core.common.DispatchersProvider
import com.melonet.app.data.local.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import kotlin.math.min

enum class WsSocketState {
    Connected,
    Reconnecting,
    Disconnected,
}

class ChatWebSocketClient(
    private val tokenManager: TokenManager,
    dispatchers: DispatchersProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private val _events = MutableSharedFlow<ChatWsEvent>(extraBufferCapacity = 128)
    val events: SharedFlow<ChatWsEvent> = _events.asSharedFlow()

    private val _socketState = MutableStateFlow(WsSocketState.Disconnected)
    val socketState: StateFlow<WsSocketState> = _socketState.asStateFlow()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var shouldConnect = false
    private var retryAttempt = 0

    fun connect() {
        shouldConnect = true
        retryAttempt = 0
        openConnection()
    }

    fun disconnect() {
        shouldConnect = false
        reconnectJob?.cancel()
        webSocket?.close(1000, "client disconnect")
        webSocket = null
        _socketState.value = WsSocketState.Disconnected
        scope.launch { _events.emit(ChatWsEvent.Disconnected) }
    }

    fun sendText(payload: WsMessageSendPayload): Boolean =
        send(ChatWsEvents.MESSAGE_SEND, payload)

    fun sendSongShare(payload: WsMessageSendPayload): Boolean =
        send(ChatWsEvents.SONG_SHARE, payload)

    fun sendRead(payload: WsMessageReadPayload): Boolean =
        send(ChatWsEvents.MESSAGE_READ, payload)

    fun sendTypingStart(conversationId: Int): Boolean =
        send(ChatWsEvents.TYPING_START, WsTypingPayload(conversation_id = conversationId))

    fun sendTypingStop(conversationId: Int): Boolean =
        send(ChatWsEvents.TYPING_STOP, WsTypingPayload(conversation_id = conversationId))

    private fun send(event: String, data: Any): Boolean {
        val socket = webSocket ?: return false
        val json = ChatWsParser.encode(event, data)
        return socket.send(json)
    }

    private fun openConnection() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val token = tokenManager.getAccessToken()
            if (token.isNullOrBlank()) {
                _socketState.value = WsSocketState.Disconnected
                return@launch
            }

            if (_socketState.value != WsSocketState.Connected) {
                _socketState.value = WsSocketState.Reconnecting
            }

            val wsBase = BuildConfig.API_BASE_URL
                .replace("https://", "wss://")
                .replace("http://", "ws://")
                .trimEnd('/')
            val url = "$wsBase/ws/chat?token=$token"
            val request = Request.Builder().url(url).build()

            webSocket?.cancel()
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    retryAttempt = 0
                    _socketState.value = WsSocketState.Connected
                    scope.launch { _events.emit(ChatWsEvent.Connected) }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val event = ChatWsParser.parse(text) ?: return
                    scope.launch { _events.emit(event) }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    scope.launch { _events.emit(ChatWsEvent.Disconnected) }
                    scheduleReconnect()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    scope.launch { _events.emit(ChatWsEvent.Disconnected) }
                    scheduleReconnect()
                }
            })
        }
    }

    private fun scheduleReconnect() {
        if (!shouldConnect) {
            _socketState.value = WsSocketState.Disconnected
            return
        }
        _socketState.value = WsSocketState.Reconnecting
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            retryAttempt++
            val delayMs = min(30_000L, 1_000L shl min(retryAttempt, 5))
            delay(delayMs)
            if (shouldConnect) openConnection()
        }
    }
}
