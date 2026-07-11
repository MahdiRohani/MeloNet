package com.melonet.app.core.network

import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.Result
import retrofit2.HttpException
import java.io.IOException

suspend fun <T> safeApiCall(block: suspend () -> ApiResponse<T>): Result<T> {
    return try {
        val response = block()
        when {
            response.error != null -> Result.Error(
                AppError.Network(
                    message = response.error.message,
                    code = response.error.code
                )
            )
            response.data != null -> Result.Success(response.data)
            else -> Result.Error(AppError.Unknown("Empty response"))
        }
    } catch (e: HttpException) {
        if (e.code() == 401) {
            Result.Error(AppError.Unauthorized)
        } else {
            Result.Error(AppError.Network(message = e.message ?: "HTTP ${e.code()}"))
        }
    } catch (e: IOException) {
        Result.Error(AppError.Network(message = e.message ?: "Network error"))
    } catch (e: Exception) {
        Result.Error(AppError.Unknown(e.message ?: "Unknown error"))
    }
}
