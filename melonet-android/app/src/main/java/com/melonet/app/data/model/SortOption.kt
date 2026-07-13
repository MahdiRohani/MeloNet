package com.melonet.app.data.model

import androidx.annotation.StringRes
import com.melonet.app.R

/** Sort options offered on catalog/artist song lists. */
enum class SortOption(val apiValue: String, @StringRes val labelRes: Int) {
    MOST_PLAYED("most_played", R.string.sort_most_played),
    MOST_LIKED("most_liked", R.string.sort_most_liked),
    NEWEST("newest", R.string.sort_newest),
    OLDEST("oldest", R.string.sort_oldest),
}
