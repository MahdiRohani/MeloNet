package com.melonet.app.core.network

import com.google.gson.Gson
import com.melonet.app.core.common.AppError
import com.melonet.app.core.common.AppErrorException
import com.melonet.app.core.common.Result
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private val errorGson = Gson()

suspend fun <T> safeApiCall(block: suspend () -> ApiResponse<T>): Result<T> {
    return try {
        val response = block()
        when {
            response.error != null -> Result.Error(
                mapServerError(
                    code = response.error.code,
                    message = response.error.message,
                    httpStatus = 0,
                ),
            )
            response.data != null -> Result.Success(response.data)
            else -> Result.Error(AppError.Unknown("Empty response"))
        }
    } catch (e: HttpException) {
        Result.Error(e.toAppError())
    } catch (e: IOException) {
        Result.Error(e.toAppError())
    } catch (e: AppErrorException) {
        Result.Error(e.error)
    } catch (e: Exception) {
        Result.Error(AppError.Unknown(e.message ?: "Unknown error"))
    }
}

fun Throwable.toAppError(): AppError = when (this) {
    is AppErrorException -> error
    is HttpException -> mapHttpException(this)
    is IOException -> mapIoException(this)
    else -> AppError.Unknown(message ?: "Unknown error")
}

internal fun mapHttpException(exception: HttpException): AppError {
    val apiError = parseApiErrorBody(exception)
    return mapServerError(
        code = apiError?.code,
        message = apiError?.message,
        httpStatus = exception.code(),
    )
}

internal fun mapIoException(exception: IOException): AppError = when (exception) {
    is SocketTimeoutException,
    is InterruptedIOException,
    -> AppError.Timeout
    is UnknownHostException,
    is ConnectException,
    is NoRouteToHostException,
    -> AppError.NoConnection
    else -> AppError.NoConnection
}

internal fun mapServerError(
    code: String?,
    message: String?,
    httpStatus: Int,
): AppError {
    when (code) {
        "invalid_credentials" -> return AppError.Http(code, httpStatus.takeIf { it > 0 } ?: 401, message)
        "user_exists" -> return AppError.Http(code, httpStatus.takeIf { it > 0 } ?: 409, message)
        "request_timeout" -> return AppError.Timeout
        "rate_limited" -> return AppError.Http(code, httpStatus.takeIf { it > 0 } ?: 429, message)
        "not_found" -> return AppError.NotFound
        "forbidden" -> return AppError.Forbidden
        "unauthorized" -> return AppError.Unauthorized
        "internal_error" -> return AppError.Server
        "invalid_request",
        "invalid_email",
        "invalid_username",
        "invalid_password",
        -> return AppError.Validation(field = null, code = code)
    }

    return when (httpStatus) {
        401 -> AppError.Unauthorized
        403 -> AppError.Forbidden
        404 -> AppError.NotFound
        408, 504 -> AppError.Timeout
        in 500..599 -> AppError.Server
        else -> {
            if (!code.isNullOrBlank()) {
                AppError.Http(code, httpStatus, message)
            } else if (httpStatus > 0) {
                AppError.Http("http_$httpStatus", httpStatus, message)
            } else {
                AppError.Unknown(message ?: "Unknown error")
            }
        }
    }
}

private fun parseApiErrorBody(exception: HttpException): ApiErrorDto? {
    val raw = exception.response()?.errorBody()?.string() ?: return null
    return runCatching {
        errorGson.fromJson(raw, ApiErrorEnvelope::class.java)?.error
    }.getOrNull()
}

private data class ApiErrorEnvelope(
    val error: ApiErrorDto?,
)
