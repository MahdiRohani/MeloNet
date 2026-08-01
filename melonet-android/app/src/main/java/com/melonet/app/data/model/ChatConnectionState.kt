package com.melonet.app.data.model

/** Client-visible chat transport status. */
enum class ChatConnectionState {
    Connected,
    Reconnecting,
    Offline,
}
