package com.melonet.app.core.common

sealed class AppError {
    data class Http(
        val code: String,
        val httpStatus: Int,
        val serverMessage: String? = null,
    ) : AppError()

    data object Timeout : AppError()
    data object NoConnection : AppError()
    data object Unauthorized : AppError()
    data object Forbidden : AppError()
    data object NotFound : AppError()
    data object Server : AppError()

    data class Validation(
        val field: String? = null,
        val code: String,
    ) : AppError()

    data class Unknown(val message: String) : AppError()
}

/** Carries a typed [AppError] through layers that only accept [Throwable] (e.g. Paging). */
class AppErrorException(val error: AppError) : Exception(error.toString())
