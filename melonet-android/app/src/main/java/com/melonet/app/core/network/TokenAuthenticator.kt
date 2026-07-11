package com.melonet.app.core.network

import com.melonet.app.data.local.TokenManager
import com.melonet.app.data.remote.AuthApi
import com.melonet.app.data.remote.dto.RefreshTokenRequestDto
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val refreshAuthApi: AuthApi,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        val refreshToken = runBlocking { tokenManager.getRefreshToken() } ?: return null

        val tokenResponse = runBlocking {
            try {
                refreshAuthApi.refresh(RefreshTokenRequestDto(refreshToken)).data
            } catch (_: Exception) {
                null
            }
        } ?: run {
            runBlocking { tokenManager.clearTokens() }
            return null
        }

        runBlocking {
            tokenManager.saveTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${tokenResponse.accessToken}")
            .build()
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
}
