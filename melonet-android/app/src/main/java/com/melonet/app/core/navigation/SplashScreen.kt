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
    val logoScale = remember { Animatable(0.72f) }
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
            ringProgress.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
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
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * 0.42f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Gold.copy(alpha = 0.22f * glow),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = size.minDimension * 0.42f,
                ),
                radius = size.minDimension * 0.42f,
                center = Offset(cx, cy),
            )
            val bars = 28
            for (i in 0 until bars) {
                val angle = Math.toRadians((i * (360.0 / bars) + wavePhase).toDouble())
                val radius = size.minDimension * 0.28f
                val len = 18f + 26f * ((sin(angle * 3) + 1) / 2).toFloat()
                val x0 = cx + (radius * cos(angle)).toFloat()
                val y0 = cy + (radius * sin(angle)).toFloat()
                val x1 = cx + ((radius + len) * cos(angle)).toFloat()
                val y1 = cy + ((radius + len) * sin(angle)).toFloat()
                drawLine(
                    color = Amber.copy(alpha = 0.18f + 0.2f * glow),
                    start = Offset(x0, y0),
                    end = Offset(x1, y1),
                    strokeWidth = 3.5f,
                    cap = StrokeCap.Round,
                )
            }
            drawCircle(
                color = GoldBright.copy(alpha = 0.35f),
                radius = size.minDimension * 0.16f * ringProgress.value,
                center = Offset(cx, cy),
                style = Stroke(width = 3f * ringProgress.value),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Gold.copy(alpha = 0.35f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF16120A)),
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
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
