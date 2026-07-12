package com.melonet.app.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class Song(
    val id: String,
    val title: String,
    val artistName: String,
    val coverUrl: String,
    val audioUrl: String,
    val category: String,
    val lyrics: String,
    val durationSec: Int,
    val genre: String? = null,
    val albumTitle: String? = null
)
