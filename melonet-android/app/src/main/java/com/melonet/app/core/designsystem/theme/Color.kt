package com.melonet.app.core.designsystem.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand — warm gold (no soft-purple)
val Gold = Color(0xFFE8C547)
val GoldBright = Color(0xFFFFD56A)
val GoldDark = Color(0xFFB8860B)
val Amber = Color(0xFFF59E0B)
val Copper = Color(0xFFB87333)
val Champagne = Color(0xFFF5E6C8)

// Dark palette — deepened Dark Gold
val DarkPrimary = Gold
val DarkOnPrimary = Color(0xFF1A1400)
val DarkPrimaryContainer = Color(0xFF3D3208)
val DarkOnPrimaryContainer = Champagne
val DarkSecondary = Color(0xFFD4AF37)
val DarkOnSecondary = Color(0xFF1A1400)
val DarkSecondaryContainer = Color(0xFF2E2808)
val DarkOnSecondaryContainer = Color(0xFFF0E0A8)
val DarkTertiary = Copper
val DarkOnTertiary = Color(0xFF1A0C00)
val DarkTertiaryContainer = Color(0xFF4A2C12)
val DarkOnTertiaryContainer = Color(0xFFFFE0C2)
val DarkError = Color(0xFFF87171)
val DarkOnError = Color(0xFF450A0A)
val DarkErrorContainer = Color(0xFF7F1D1D)
val DarkOnErrorContainer = Color(0xFFFECACA)
val DarkBackground = Color(0xFF0B0A07)
val DarkOnBackground = Color(0xFFF7F3EA)
val DarkSurface = Color(0xFF16140F)
val DarkOnSurface = Color(0xFFF7F3EA)
val DarkSurfaceVariant = Color(0xFF242018)
val DarkOnSurfaceVariant = Color(0xFFC4BBA8)
val DarkOutline = Color(0xFF6B6352)
val DarkOutlineVariant = Color(0xFF3D382C)
val DarkInverseSurface = Color(0xFFF7F3EA)
val DarkInverseOnSurface = Color(0xFF1A160E)
val DarkInversePrimary = Color(0xFF4A3F00)
val DarkScrim = Color(0x99000000)

// Light palette — clean cool neutrals + restrained gold accent
val LightPrimary = Color(0xFFA67C00)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFF3E8C8)
val LightOnPrimaryContainer = Color(0xFF3A2E00)
val LightSecondary = Color(0xFF5B6670)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFE4E8EC)
val LightOnSecondaryContainer = Color(0xFF1F282F)
val LightTertiary = Color(0xFF6B7C6E)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFDCE6DF)
val LightOnTertiaryContainer = Color(0xFF1C2A20)
val LightError = Color(0xFFC62828)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFEBEE)
val LightOnErrorContainer = Color(0xFF7F1D1D)
val LightBackground = Color(0xFFF3F4F6)
val LightOnBackground = Color(0xFF171A1F)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF171A1F)
val LightSurfaceVariant = Color(0xFFE6E8EC)
val LightOnSurfaceVariant = Color(0xFF4A5058)
val LightOutline = Color(0xFF8B929A)
val LightOutlineVariant = Color(0xFFCED3D8)
val LightInverseSurface = Color(0xFF23272E)
val LightInverseOnSurface = Color(0xFFF3F4F6)
val LightInversePrimary = Color(0xFFF3E8C8)
val LightScrim = Color(0x99000000)

/** Semantic extension colors + atmospheric brushes (keep feature code off raw Color(0x…)). */
data class MeloNetColors(
    val premium: Color,
    val onPremium: Color,
    val premiumContainer: Color,
    val onPremiumContainer: Color,
    val placeholder: Color,
    val disabled: Color,
    val shimmerBase: Float,
    val shimmerHighlight: Float,
    val surfaceGradientTop: Color,
    val surfaceGradientBottom: Color,
    val illustrationWell: Color,
    val brandSoft: Color,
    val vinylInner: Color,
    val vinylOuter: Color,
    val vinylSpindle: Color,
    val vinylRim: Color,
) {
    val surfaceBrush: Brush
        get() = Brush.verticalGradient(
            colors = listOf(surfaceGradientTop, surfaceGradientBottom),
        )
}

val DarkMeloNetColors = MeloNetColors(
    premium = GoldBright,
    onPremium = Color(0xFF1A1400),
    premiumContainer = Gold.copy(alpha = 0.14f),
    onPremiumContainer = GoldBright,
    placeholder = DarkSurfaceVariant,
    disabled = Color(0xFF6B7280),
    shimmerBase = 0.06f,
    shimmerHighlight = 0.22f,
    surfaceGradientTop = Color(0xFF14110C),
    surfaceGradientBottom = DarkBackground,
    illustrationWell = Color(0xFF1F1B14),
    brandSoft = Gold.copy(alpha = 0.18f),
    vinylInner = Color(0xFF2A2A2A),
    vinylOuter = Color(0xFF0D0D0D),
    vinylSpindle = Color(0xFF111111),
    vinylRim = Color(0x14FFFFFF),
)

val LightMeloNetColors = MeloNetColors(
    premium = GoldDark,
    onPremium = Color(0xFFFFFFFF),
    premiumContainer = Gold.copy(alpha = 0.14f),
    onPremiumContainer = GoldDark,
    placeholder = LightSurfaceVariant,
    disabled = Color(0xFF9CA3AF),
    shimmerBase = 0.06f,
    shimmerHighlight = 0.22f,
    surfaceGradientTop = Color(0xFFFFFFFF),
    surfaceGradientBottom = LightBackground,
    illustrationWell = Color(0xFFE8EBEE),
    brandSoft = Gold.copy(alpha = 0.14f),
    vinylInner = Color(0xFF2A2A2A),
    vinylOuter = Color(0xFF0D0D0D),
    vinylSpindle = Color(0xFF111111),
    vinylRim = Color(0x14FFFFFF),
)
