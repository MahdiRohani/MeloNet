package com.melonet.app.data.mapper

import com.melonet.app.data.model.Artist
import com.melonet.app.data.remote.dto.ArtistDto

object ArtistMapper {
    fun toModel(dto: ArtistDto): Artist = Artist(
        id = dto.id,
        name = dto.name,
        imageUrl = dto.imageUrl.orEmpty(),
        bio = dto.bio,
        songCount = dto.songCount ?: 0,
        region = dto.region.orEmpty(),
        isFollowing = dto.isFollowing ?: false,
    )
}
