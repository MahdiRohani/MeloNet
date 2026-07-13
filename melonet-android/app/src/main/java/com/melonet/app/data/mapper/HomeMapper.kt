package com.melonet.app.data.mapper

import com.melonet.app.data.model.HomeArtistRow
import com.melonet.app.data.model.HomeFeed
import com.melonet.app.data.model.HomeRow
import com.melonet.app.data.model.QuickAction
import com.melonet.app.data.model.Song
import com.melonet.app.data.remote.dto.HomeArtistRowDto
import com.melonet.app.data.remote.dto.HomeFeedDto
import com.melonet.app.data.remote.dto.HomeRowDto
import com.melonet.app.data.remote.dto.QuickActionDto
object HomeMapper {

    fun toModel(dto: HomeFeedDto): HomeFeed = HomeFeed(
        carousel = dto.carousel.map(SongMapper::toModel),
        quickActions = dto.quickActions.map(::quickActionToModel),
        rows = dto.rows.map(::rowToModel),
        artistRows = dto.artistRows.orEmpty().map(::artistRowToModel),
    )

    private fun artistRowToModel(dto: HomeArtistRowDto): HomeArtistRow = HomeArtistRow(
        id = dto.id,
        title = dto.title,
        seeAllPath = dto.seeAllPath,
        items = dto.items.orEmpty().map(ArtistMapper::toModel),
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
        items = dto.items.map(SongMapper::toModel)
    )
}
