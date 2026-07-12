package com.melonet.app.data.mapper

import com.melonet.app.data.model.Playlist
import com.melonet.app.data.remote.dto.PlaylistDto

object PlaylistMapper {
    fun toModel(dto: PlaylistDto): Playlist = Playlist(
        id = dto.id,
        ownerId = dto.ownerId,
        ownerName = dto.ownerName.orEmpty(),
        title = dto.title,
        description = dto.description.orEmpty(),
        visibility = dto.visibility,
        coverUrl = dto.coverUrl.orEmpty(),
        isSystem = dto.isSystem,
        isOwner = dto.isOwner,
        songCount = dto.songCount,
    )
}
