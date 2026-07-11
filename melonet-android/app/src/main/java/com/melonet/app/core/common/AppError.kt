package com.melonet.app.core.common

sealed class AppError {
    data class Network(val message: String, val code: String? = null) : AppError()
    data class Unknown(val message: String) : AppError()
    data object Unauthorized : AppError()
}
