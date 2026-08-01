package com.melonet.app.core.network

import com.melonet.app.data.local.TokenManager
import com.melonet.app.data.remote.AuthApi
import com.melonet.app.data.remote.dto.RefreshTokenRequestDto
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import java.io.IOException

class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val refreshAuthApi: AuthApi,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        val refreshToken = runBlocking { tokenManager.getRefreshToken() } ?: return null

        val tokens = runBlocking {
            try {
                val body = refreshAuthApi.refresh(RefreshTokenRequestDto(refreshToken))
                val data = body.data
                when {
                    data != null -> RefreshResult.Success(data.accessToken, data.refreshToken)
                    body.error?.code in AUTH_FAILURE_CODES -> RefreshResult.AuthFailed
                    else -> RefreshResult.AuthFailed
                }
            } catch (e: HttpException) {
                if (e.code() == 401 || e.code() == 403) {
                    RefreshResult.AuthFailed
                } else {
                    RefreshResult.Transient
                }
            } catch (_: IOException) {
                // Offline / timeout — keep tokens so offline session can continue.
                RefreshResult.Transient
            } catch (_: Exception) {
                RefreshResult.Transient
            }
        }

        return when (tokens) {
            is RefreshResult.Success -> {
                runBlocking {
                    tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken)
                }
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${tokens.accessToken}")
                    .build()
            }
            RefreshResult.AuthFailed -> {
                runBlocking { tokenManager.clearTokens() }
                null
            }
            RefreshResult.Transient -> null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private sealed interface RefreshResult {
        data class Success(val accessToken: String, val refreshToken: String) : RefreshResult
        data object AuthFailed : RefreshResult
        data object Transient : RefreshResult
    }

    private companion object {
        val AUTH_FAILURE_CODES = setOf("unauthorized", "invalid_credentials", "forbidden")
    }
}
