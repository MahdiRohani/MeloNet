package com.melonet.app.data.model

data class QuickAction(
    val id: String,
    val title: String,
    val target: String,
    val icon: String?
)

data class HomeRow(
    val id: String,
    val title: String,
    val rowType: String,
    val seeAllPath: String?,
    val items: List<Song>
)

data class HomeArtistRow(
    val id: String,
    val title: String,
    val seeAllPath: String?,
    val items: List<Artist>
)

data class HomeFeed(
    val carousel: List<Song>,
    val quickActions: List<QuickAction>,
    val rows: List<HomeRow>,
    val artistRows: List<HomeArtistRow> = emptyList(),
) {
    val isEmpty: Boolean
        get() = carousel.isEmpty() && quickActions.isEmpty() && rows.isEmpty() && artistRows.isEmpty()
}
