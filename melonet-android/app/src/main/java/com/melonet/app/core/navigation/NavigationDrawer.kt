package com.melonet.app.core.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.melonet.app.R
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class DrawerMenuItem<T : Any>(
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val route: T,
)

val drawerMenuItems = listOf(
    DrawerMenuItem(R.string.nav_profile, Icons.Default.Person, ProfileRoute),
    DrawerMenuItem(R.string.nav_playlists, Icons.AutoMirrored.Filled.QueueMusic, PlaylistsRoute),
    DrawerMenuItem(R.string.nav_local_music, Icons.Default.LibraryMusic, LocalMusicRoute),
    DrawerMenuItem(R.string.chat_conversations_title, Icons.AutoMirrored.Filled.Chat, ConversationsRoute),
    DrawerMenuItem(R.string.nav_downloads, Icons.Default.Download, DownloadsRoute),
    DrawerMenuItem(R.string.settings_title, Icons.Default.Settings, SettingsRoute),
)

private val DrawerWidth = 288.dp

@Composable
fun MelonetNavigationDrawer(
    drawerState: DrawerState,
    scope: CoroutineScope,
    onNavigate: (Any) -> Unit,
    content: @Composable () -> Unit,
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(DrawerWidth)
                    .fillMaxHeight(),
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
            ) {
                val spacing = MeloNetTheme.spacing
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(
                        start = spacing.lg,
                        end = spacing.lg,
                        top = spacing.lg,
                        bottom = spacing.md,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(spacing.sm))
                    Text(
                        text = stringResource(R.string.menu_header),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = spacing.md))
                Spacer(modifier = Modifier.height(spacing.sm))

                drawerMenuItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = stringResource(item.titleRes),
                            )
                        },
                        label = { Text(stringResource(item.titleRes)) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onNavigate(item.route)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }
        },
        content = content,
    )
}
