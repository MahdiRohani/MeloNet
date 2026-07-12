package com.melonet.app.data.mapper

import com.melonet.app.data.model.Song
import com.melonet.app.data.remote.dto.SongDto

object SongMapper {
    fun toModel(dto: SongDto): Song = Song(
        id = dto.id,
        title = dto.title,
        artistName = dto.artistName ?: dto.artist.orEmpty(),
        coverUrl = dto.coverImageUrl ?: dto.coverUrl.orEmpty(),
        audioUrl = dto.audioUrl.orEmpty(),
        category = dto.category.orEmpty(),
        lyrics = dto.lyrics.orEmpty(),
        durationSec = dto.durationSec ?: 0,
        genre = dto.genre,
        albumTitle = dto.albumTitle,
    )
}
