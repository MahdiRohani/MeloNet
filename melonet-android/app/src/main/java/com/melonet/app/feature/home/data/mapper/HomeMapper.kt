package com.melonet.app.feature.home.data.mapper

import com.melonet.app.domain.model.HomeFeed
import com.melonet.app.domain.model.HomeRow
import com.melonet.app.domain.model.QuickAction
import com.melonet.app.domain.model.Song
import com.melonet.app.feature.home.data.dto.HomeFeedDto
import com.melonet.app.feature.home.data.dto.HomeRowDto
import com.melonet.app.feature.home.data.dto.QuickActionDto
import com.melonet.app.feature.home.data.dto.SongDto

object HomeMapper {

    fun toDomain(dto: HomeFeedDto): HomeFeed = HomeFeed(
        carousel = dto.carousel.map(::songToDomain),
        quickActions = dto.quickActions.map(::quickActionToDomain),
        rows = dto.rows.map(::rowToDomain)
    )

    private fun songToDomain(dto: SongDto): Song = Song(
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

    private fun quickActionToDomain(dto: QuickActionDto): QuickAction = QuickAction(
        id = dto.id,
        title = dto.title,
        target = dto.target,
        icon = dto.icon
    )

    private fun rowToDomain(dto: HomeRowDto): HomeRow = HomeRow(
        id = dto.id,
        title = dto.title,
        rowType = dto.rowType,
        seeAllPath = dto.seeAllPath,
        items = dto.items.map(::songToDomain)
    )
}
