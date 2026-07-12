package com.melonet.app.core.common

import android.content.Context
import com.melonet.app.R

fun AppError.displayMessage(context: Context): String = when (this) {
    is AppError.Network -> message
    AppError.Unauthorized -> context.getString(R.string.error_unauthorized)
    is AppError.Unknown -> message
}
