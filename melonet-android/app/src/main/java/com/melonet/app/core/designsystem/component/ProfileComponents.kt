package com.melonet.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.melonet.app.R
import com.melonet.app.core.designsystem.theme.MeloNetTheme

@Composable
fun ProfileAvatar(
    avatarUrl: String?,
    isPremium: Boolean,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MeloNetTheme.spacing
    val dimensions = MeloNetTheme.dimensions
    val colors = MeloNetTheme.colors

    Box(
        contentAlignment = Alignment.BottomEnd,
        modifier = modifier.size(dimensions.avatarRing)
    ) {
        MeloImage(
            imageUrl = avatarUrl?.ifBlank { null },
            contentDescription = stringResource(R.string.cd_user_avatar),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(dimensions.avatarLg)
                .clip(CircleShape)
                .border(
                    width = dimensions.avatarBorder,
                    color = if (isPremium) colors.premium else MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                )
        )

        if (isPremium) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(dimensions.iconSm)
                    .clip(CircleShape)
                    .background(colors.premium)
                    .padding(spacing.xs),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = stringResource(R.string.cd_premium_user),
                    tint = colors.onPremium,
                    modifier = Modifier.size(dimensions.iconSm - spacing.sm)
                )
            }
        }

        IconButton(
            onClick = onEditClick,
            modifier = Modifier
                .size(dimensions.iconLg)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.cd_edit_profile),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun PremiumSubscriptionCard(
    isPremium: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MeloNetTheme.spacing
    val colors = MeloNetTheme.colors

    MeloCard(
        onClick = null,
        modifier = modifier.fillMaxWidth(),
        containerColor = if (isPremium) colors.premiumContainer else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (isPremium) R.string.premium_active_title else R.string.premium_upgrade_title
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isPremium) colors.onPremiumContainer else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = stringResource(
                        if (isPremium) R.string.premium_active_description else R.string.premium_upgrade_description
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(spacing.md))

            MeloButton(
                text = stringResource(
                    if (isPremium) R.string.premium_active_button else R.string.premium_upgrade_button
                ),
                onClick = onActionClick,
                enabled = !isPremium,
                containerColor = if (isPremium) colors.disabled else null,
            )
        }
    }
}

@Composable
fun ProfileQuickActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MeloNetTheme.spacing
    val dimensions = MeloNetTheme.dimensions

    MeloCard(
        onClick = onClick,
        modifier = modifier.height(dimensions.quickActionCardHeight),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(dimensions.iconMd)
            )
            Spacer(modifier = Modifier.height(spacing.sm))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
