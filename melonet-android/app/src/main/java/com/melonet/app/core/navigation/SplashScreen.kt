package com.melonet.app.core.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.melonet.app.R
import com.melonet.app.core.designsystem.theme.MeloMotion
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.AuthState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    authState: AuthState,
    onNavigateToAuth: () -> Unit,
    onNavigateToMain: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val dimensions = MeloNetTheme.dimensions
    val colors = MeloNetTheme.colors
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val titleStartOffsetPx = with(density) { spacing.md.toPx() }

    val logoScale = remember { Animatable(MeloMotion.splashLogoStartScale) }
    val logoAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffset = remember { Animatable(titleStartOffsetPx) }
    val ringProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(
                1f,
                tween(MeloMotion.splashLogoFadeMs, easing = FastOutSlowInEasing),
            )
        }
        launch {
            logoScale.animateTo(
                1f,
                tween(MeloMotion.splashLogoScaleMs, easing = FastOutSlowInEasing),
            )
        }
        launch {
            delay(MeloMotion.splashTitleDelayMs.toLong())
            titleAlpha.animateTo(
                1f,
                tween(MeloMotion.splashTitleMs, easing = FastOutSlowInEasing),
            )
            titleOffset.animateTo(
                0f,
                tween(MeloMotion.splashTitleMs, easing = FastOutSlowInEasing),
            )
        }
        launch {
            ringProgress.animateTo(
                1f,
                tween(MeloMotion.splashRingMs, easing = FastOutSlowInEasing),
            )
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Loading) return@LaunchedEffect
        delay(MeloMotion.splashHoldMs.toLong())
        when (authState) {
            is AuthState.Authenticated -> onNavigateToMain()
            AuthState.Unauthenticated -> onNavigateToAuth()
            AuthState.Loading -> Unit
        }
    }

    val pulse = rememberInfiniteTransition(label = "splashPulse")
    val glow by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(MeloMotion.splashPulseMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )
    val wavePhase by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(MeloMotion.splashWaveMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.splashBrush),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f - dimensions.splashGlowLift.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        scheme.primary.copy(alpha = 0.22f * glow),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = size.minDimension * 0.38f,
                ),
                radius = size.minDimension * 0.38f,
                center = Offset(cx, cy),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(dimensions.splashStageSize)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val iconRadius = dimensions.splashBadgeSize.toPx() / 2f
                    val tickBase = iconRadius + dimensions.splashTickGap.toPx()
                    val tickMin = dimensions.splashTickMinLen.toPx()
                    val tickExtra = dimensions.splashTickExtraLen.toPx()
                    val tickStroke = dimensions.splashTickStroke.toPx()
                    val bars = 28

                    for (i in 0 until bars) {
                        val angle = Math.toRadians((i * (360.0 / bars) + wavePhase).toDouble())
                        val len = tickMin + tickExtra * ((sin(angle * 3) + 1) / 2).toFloat()
                        val x0 = cx + (tickBase * cos(angle)).toFloat()
                        val y0 = cy + (tickBase * sin(angle)).toFloat()
                        val x1 = cx + ((tickBase + len) * cos(angle)).toFloat()
                        val y1 = cy + ((tickBase + len) * sin(angle)).toFloat()
                        drawLine(
                            color = scheme.tertiary.copy(alpha = 0.2f + 0.22f * glow),
                            start = Offset(x0, y0),
                            end = Offset(x1, y1),
                            strokeWidth = tickStroke,
                            cap = StrokeCap.Round,
                        )
                    }

                    drawCircle(
                        color = colors.premium.copy(alpha = 0.45f * ringProgress.value),
                        radius = iconRadius + dimensions.splashRingPad.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(
                            width = dimensions.splashRingStroke.toPx() * ringProgress.value,
                        ),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(dimensions.splashBadgeSize)
                        .clip(CircleShape)
                        .background(colors.splashLogoPlate)
                        .border(
                            width = dimensions.borderThin,
                            brush = Brush.linearGradient(
                                listOf(
                                    colors.premium.copy(alpha = 0.55f),
                                    scheme.primary.copy(alpha = 0.2f),
                                ),
                            ),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_background),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = stringResource(R.string.app_name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(dimensions.splashForegroundScale),
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = scheme.onBackground,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .graphicsLayer { translationY = titleOffset.value },
            )
            Spacer(modifier = Modifier.height(spacing.sm))
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.primary.copy(alpha = 0.85f),
                modifier = Modifier.alpha(titleAlpha.value * 0.95f),
            )
        }
    }
}
