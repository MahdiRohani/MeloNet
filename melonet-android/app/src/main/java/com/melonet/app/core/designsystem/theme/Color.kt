package com.melonet.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Brand
val Gold = Color(0xFFFFD700)
val GoldDark = Color(0xFFB8860B)
val Amber = Color(0xFFF59E0B)

// Dark palette
val DarkPrimary = Gold
val DarkOnPrimary = Color(0xFF000000)
val DarkPrimaryContainer = Color(0xFF4A3F00)
val DarkOnPrimaryContainer = Color(0xFFFFF0A0)
val DarkSecondary = Color(0xFFD4AF37)
val DarkOnSecondary = Color(0xFF1A1400)
val DarkSecondaryContainer = Color(0xFF3D3200)
val DarkOnSecondaryContainer = Color(0xFFF5E6A0)
val DarkTertiary = Color(0xFFC4B5FD)
val DarkOnTertiary = Color(0xFF2E1065)
val DarkTertiaryContainer = Color(0xFF4C1D95)
val DarkOnTertiaryContainer = Color(0xFFEDE9FE)
val DarkError = Color(0xFFF87171)
val DarkOnError = Color(0xFF450A0A)
val DarkErrorContainer = Color(0xFF7F1D1D)
val DarkOnErrorContainer = Color(0xFFFECACA)
val DarkBackground = Color(0xFF000000)
val DarkOnBackground = Color(0xFFF5F5F5)
val DarkSurface = Color(0xFF1A1A1A)
val DarkOnSurface = Color(0xFFF5F5F5)
val DarkSurfaceVariant = Color(0xFF2A2A2A)
val DarkOnSurfaceVariant = Color(0xFFB0B0B0)
val DarkOutline = Color(0xFF5C5C5C)
val DarkOutlineVariant = Color(0xFF3D3D3D)
val DarkInverseSurface = Color(0xFFF5F5F5)
val DarkInverseOnSurface = Color(0xFF1A1A1A)
val DarkInversePrimary = Color(0xFF4A3F00)
val DarkScrim = Color(0x99000000)

// Light palette
val LightPrimary = Amber
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFFEF3C7)
val LightOnPrimaryContainer = Color(0xFF451A03)
val LightSecondary = Color(0xFFD97706)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFFFEDD5)
val LightOnSecondaryContainer = Color(0xFF431407)
val LightTertiary = Color(0xFF7C3AED)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFEDE9FE)
val LightOnTertiaryContainer = Color(0xFF2E1065)
val LightError = Color(0xFFDC2626)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFEE2E2)
val LightOnErrorContainer = Color(0xFF7F1D1D)
val LightBackground = Color(0xFFF1F5F9)
val LightOnBackground = Color(0xFF1E293B)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF1E293B)
val LightSurfaceVariant = Color(0xFFE2E8F0)
val LightOnSurfaceVariant = Color(0xFF64748B)
val LightOutline = Color(0xFF94A3B8)
val LightOutlineVariant = Color(0xFFCBD5E1)
val LightInverseSurface = Color(0xFF1E293B)
val LightInverseOnSurface = Color(0xFFF1F5F9)
val LightInversePrimary = Color(0xFFFEF3C7)
val LightScrim = Color(0x99000000)

// Semantic extension colors
data class MeloNetColors(
    val premium: Color,
    val onPremium: Color,
    val premiumContainer: Color,
    val onPremiumContainer: Color,
    val placeholder: Color,
    val disabled: Color,
    val shimmerBase: Float,
    val shimmerHighlight: Float,
)

val DarkMeloNetColors = MeloNetColors(
    premium = Gold,
    onPremium = Color(0xFF000000),
    premiumContainer = Gold.copy(alpha = 0.1f),
    onPremiumContainer = GoldDark,
    placeholder = DarkSurfaceVariant,
    disabled = Color(0xFF6B7280),
    shimmerBase = 0.05f,
    shimmerHighlight = 0.15f,
)

val LightMeloNetColors = MeloNetColors(
    premium = Gold,
    onPremium = Color(0xFF000000),
    premiumContainer = Gold.copy(alpha = 0.1f),
    onPremiumContainer = GoldDark,
    placeholder = LightSurfaceVariant,
    disabled = Color(0xFF9CA3AF),
    shimmerBase = 0.05f,
    shimmerHighlight = 0.15f,
)
