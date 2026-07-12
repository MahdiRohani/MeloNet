package com.melonet.app.data.local

import com.melonet.app.data.model.Song

fun Song.toLikedEntity(cachedAt: Long = System.currentTimeMillis()): LikedSongEntity =
    LikedSongEntity(
        songId = id,
        title = title,
        artistName = artistName,
        coverUrl = coverUrl,
        audioUrl = audioUrl,
        durationSec = durationSec,
        cachedAt = cachedAt,
    )

fun Song.toPlayHistoryEntity(playedAt: Long = System.currentTimeMillis()): PlayHistoryEntity =
    PlayHistoryEntity(
        songId = id,
        title = title,
        artistName = artistName,
        coverUrl = coverUrl,
        audioUrl = audioUrl,
        durationSec = durationSec,
        playedAt = playedAt,
    )

fun LikedSongEntity.toSong(): Song = Song(
    id = songId,
    title = title,
    artistName = artistName,
    coverUrl = coverUrl,
    audioUrl = audioUrl,
    category = "",
    lyrics = "",
    durationSec = durationSec,
)

fun PlayHistoryEntity.toSong(): Song = Song(
    id = songId,
    title = title,
    artistName = artistName,
    coverUrl = coverUrl,
    audioUrl = audioUrl,
    category = "",
    lyrics = "",
    durationSec = durationSec,
)
