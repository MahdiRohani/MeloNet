package com.melonet.app.core.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

fun audioReadPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

fun Context.hasAudioReadPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, audioReadPermission()) == PackageManager.PERMISSION_GRANTED

@Composable
fun rememberAudioPermissionRequester(
    onResult: (Boolean) -> Unit,
): () -> Unit {
    val permission = remember { audioReadPermission() }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onResult,
    )
    return remember(permission, launcher) {
        { launcher.launch(permission) }
    }
}

@Composable
fun rememberAudioPermissionRequesterWithCallback(): (onResult: (Boolean) -> Unit) -> Unit {
    var pendingCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val requestPermission = rememberAudioPermissionRequester { granted ->
        pendingCallback?.invoke(granted)
        pendingCallback = null
    }
    return remember(requestPermission) {
        { callback ->
            pendingCallback = callback
            requestPermission()
        }
    }
}
