package com.melonet.app.core.designsystem.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.melonet.app.core.designsystem.component.MeloButton
import com.melonet.app.core.designsystem.component.MeloButtonVariant
import com.melonet.app.core.designsystem.component.QuickActionChip
import com.melonet.app.core.designsystem.component.SectionHeader
import com.melonet.app.core.designsystem.component.SongCard
import com.melonet.app.core.designsystem.theme.MeloNetTheme

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "RTL", locale = "fa", showBackground = true)
@Composable
private fun ComponentPreview() {
    MeloNetTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader(title = "Popular Songs")
            SongCard(
                title = "Song Title",
                subtitle = "Artist Name",
                imageUrl = null,
                onClick = {}
            )
            QuickActionChip(
                title = "Liked",
                icon = Icons.Default.Favorite,
                onClick = {}
            )
            MeloButton(
                text = "Subscribe",
                onClick = {},
                variant = MeloButtonVariant.Primary
            )
        }
    }
}
