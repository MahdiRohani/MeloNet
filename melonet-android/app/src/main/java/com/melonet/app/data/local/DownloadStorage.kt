package com.melonet.app.data.local

import android.content.Context
import java.io.File

class DownloadStorage(context: Context) {
    private val directory: File = File(context.filesDir, "downloads").apply { mkdirs() }

    fun fileFor(songId: String): File = File(directory, "$songId.mp3")

    fun deleteFile(songId: String) {
        fileFor(songId).delete()
    }
}
