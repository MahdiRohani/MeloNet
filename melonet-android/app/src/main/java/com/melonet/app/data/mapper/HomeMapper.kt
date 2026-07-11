package com.melonet.app.data.mapper

import com.melonet.app.data.model.HomeFeed
import com.melonet.app.data.model.HomeRow
import com.melonet.app.data.model.QuickAction
import com.melonet.app.data.model.Song
import com.melonet.app.data.remote.dto.HomeFeedDto
import com.melonet.app.data.remote.dto.HomeRowDto
import com.melonet.app.data.remote.dto.QuickActionDto
import com.melonet.app.data.remote.dto.SongDto

object HomeMapper {

    fun toModel(dto: HomeFeedDto): HomeFeed = HomeFeed(
        carousel = dto.carousel.map(::songToModel),
        quickActions = dto.quickActions.map(::quickActionToModel),
        rows = dto.rows.map(::rowToModel)
    )

    private fun songToModel(dto: SongDto): Song = Song(
        id = dto.id,
        title = dto.title,
        artistName = dto.artistName ?: dto.artist.orEmpty(),
        coverUrl = dto.coverImageUrl ?: dto.coverUrl.orEmpty(),
        audioUrl = dto.audioUrl.orEmpty(),
        category = dto.category.orEmpty(),
        lyrics = dto.lyrics.orEmpty(),
        durationSec = dto.durationSec ?: 0,
        genre = dto.genre,
        albumTitle = dto.albumTitle
    )

    private fun quickActionToModel(dto: QuickActionDto): QuickAction = QuickAction(
        id = dto.id,
        title = dto.title,
        target = dto.target,
        icon = dto.icon
    )

    private fun rowToModel(dto: HomeRowDto): HomeRow = HomeRow(
        id = dto.id,
        title = dto.title,
        rowType = dto.rowType,
        seeAllPath = dto.seeAllPath,
        items = dto.items.map(::songToModel)
    )
}
