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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.melonet.app.R
import com.melonet.app.core.designsystem.theme.Amber
import com.melonet.app.core.designsystem.theme.Champagne
import com.melonet.app.core.designsystem.theme.DarkBackground
import com.melonet.app.core.designsystem.theme.Gold
import com.melonet.app.core.designsystem.theme.GoldBright
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
    val logoScale = remember { Animatable(0.78f) }
    val logoAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffset = remember { Animatable(18f) }
    val ringProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }
        launch {
            delay(220)
            titleAlpha.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
            titleOffset.animateTo(0f, tween(650, easing = FastOutSlowInEasing))
        }
        launch {
            ringProgress.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Loading) return@LaunchedEffect
        delay(1400L)
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
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )
    val wavePhase by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A160E),
                        DarkBackground,
                        Color(0xFF0A0906),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Soft ambient glow behind the whole brand block.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f - 36.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Gold.copy(alpha = 0.2f * glow),
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
            // Logo + rings share one box so everything stays concentric.
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val iconRadius = 56.dp.toPx()

                    // Orbiting equalizer ticks around the icon.
                    val bars = 28
                    val tickBase = iconRadius + 14.dp.toPx()
                    for (i in 0 until bars) {
                        val angle = Math.toRadians((i * (360.0 / bars) + wavePhase).toDouble())
                        val len = 8.dp.toPx() + 14.dp.toPx() * ((sin(angle * 3) + 1) / 2).toFloat()
                        val x0 = cx + (tickBase * cos(angle)).toFloat()
                        val y0 = cy + (tickBase * sin(angle)).toFloat()
                        val x1 = cx + ((tickBase + len) * cos(angle)).toFloat()
                        val y1 = cy + ((tickBase + len) * sin(angle)).toFloat()
                        drawLine(
                            color = Amber.copy(alpha = 0.2f + 0.22f * glow),
                            start = Offset(x0, y0),
                            end = Offset(x1, y1),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round,
                        )
                    }

                    // Animated gold ring hugging the icon edge.
                    drawCircle(
                        color = GoldBright.copy(alpha = 0.45f * ringProgress.value),
                        radius = iconRadius + 4.dp.toPx(),
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.5.dp.toPx() * ringProgress.value),
                    )
                }

                // Circular badge: background plate + scaled foreground so the mark fills the disc.
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF16120A))
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    GoldBright.copy(alpha = 0.55f),
                                    Gold.copy(alpha = 0.2f),
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
                    // Adaptive foregrounds leave a safe-zone margin; scale up so the
                    // glyph fills the visible disc (parent Box already clips to circle).
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = stringResource(R.string.app_name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1.36f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Champagne,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .graphicsLayer { translationY = titleOffset.value },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = Gold.copy(alpha = 0.85f),
                modifier = Modifier.alpha(titleAlpha.value * 0.95f),
            )
        }
    }
}
