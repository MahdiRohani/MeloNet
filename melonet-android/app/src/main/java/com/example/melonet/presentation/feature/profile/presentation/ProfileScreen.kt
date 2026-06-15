package com.example.melonet.presentation.feature.profile.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    onEditProfileClick: () -> Unit = {},
    onLikedSongsClick: () -> Unit = {},
    onMyPlaylistsClick: () -> Unit = {},
    onUpgradePremiumClick: () -> Unit = {}
) {
    val state by profileViewModel.state.collectAsState()
    val userName = state.userName
    val isPremium = state.isPremium
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))


        ProfileAvatarSection(
            isPremium = isPremium,
            onEditClick = onEditProfileClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = userName,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QuickActionButton(
                modifier = Modifier.weight(1f),
                title = "آهنگ‌های لایک شده",
                icon = Icons.Default.Favorite,
                onClick = onLikedSongsClick
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                title = "پلی‌لیست‌های من",
                icon = Icons.Default.LibraryMusic,
                onClick = onMyPlaylistsClick
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        PremiumSubscriptionCard(
            isPremium = isPremium,
            onActionClick = onUpgradePremiumClick
        )
    }
}

@Composable
fun ProfileAvatarSection(isPremium: Boolean, onEditClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.BottomEnd,
        modifier = Modifier.size(140.dp)
    ) {

        Image(
            painter = painterResource(id = android.R.drawable.ic_menu_myplaces), // جایگزین با کاور پروفایل
            contentDescription = "آواتار کاربر",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .border(
                    width = 4.dp,
                    color = if (isPremium) Color(0xFFFFD700) else MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                )
        )


        if (isPremium) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color(0xFFFFD700), CircleShape)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "کاربر ویژه",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }


        IconButton(
            onClick = onEditClick,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .size(40.dp)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "ویرایش پروفایل",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun QuickActionButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PremiumSubscriptionCard(isPremium: Boolean, onActionClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPremium) Color(0xFFFFD700).copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPremium) "اشتراک شما فعال است" else "ارتقا به حساب ویژه",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isPremium) Color(0xFFB8860B) else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isPremium) "شما به تمامی امکانات و دانلود آفلاین دسترسی دارید."
                    else "برای پخش نامحدود و دانلود آهنگ‌ها، حساب خود را ارتقا دهید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPremium) Color.Gray else MaterialTheme.colorScheme.primary
                ),
                enabled = !isPremium
            ) {
                Text(text = if (isPremium) "فعال شده" else "خرید اشتراک")
            }
        }
    }
}