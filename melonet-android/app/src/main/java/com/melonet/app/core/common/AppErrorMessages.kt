package com.melonet.app.core.common

import android.content.Context
import com.melonet.app.R

fun AppError.displayMessage(context: Context): String = when (this) {
    AppError.Timeout -> context.getString(R.string.error_timeout)
    AppError.NoConnection -> context.getString(R.string.error_no_connection)
    AppError.Unauthorized -> context.getString(R.string.error_unauthorized)
    AppError.Forbidden -> context.getString(R.string.error_forbidden)
    AppError.NotFound -> context.getString(R.string.error_not_found)
    AppError.Server -> context.getString(R.string.error_server)
    is AppError.Validation -> validationMessage(context, code)
    is AppError.Http -> httpMessage(context, code)
    is AppError.Unknown -> context.getString(R.string.error_unknown)
}

private fun validationMessage(context: Context, code: String): String = when (code) {
    "required_fields" -> context.getString(R.string.auth_error_required_fields)
    "invalid_email" -> context.getString(R.string.auth_error_invalid_email)
    "invalid_username" -> context.getString(R.string.auth_error_invalid_username)
    "invalid_password" -> context.getString(R.string.auth_error_invalid_password)
    else -> context.getString(R.string.error_validation)
}

private fun httpMessage(context: Context, code: String): String = when (code) {
    "invalid_credentials" -> context.getString(R.string.error_invalid_credentials)
    "user_exists" -> context.getString(R.string.error_user_exists)
    "rate_limited" -> context.getString(R.string.error_rate_limited)
    "registration_failed" -> context.getString(R.string.auth_error_registration_failed)
    else -> context.getString(R.string.error_unknown)
}
