package com.melonet.app.domain.model

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

data class HomeFeed(
    val carousel: List<Song>,
    val quickActions: List<QuickAction>,
    val rows: List<HomeRow>
)
