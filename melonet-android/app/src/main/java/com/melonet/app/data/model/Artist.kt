package com.melonet.app.data.model

data class Artist(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val bio: String?,
    val songCount: Int = 0,
)
