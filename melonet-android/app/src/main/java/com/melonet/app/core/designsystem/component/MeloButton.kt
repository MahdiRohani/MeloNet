package com.melonet.app.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import com.melonet.app.core.designsystem.theme.MeloMotion
import com.melonet.app.core.designsystem.theme.MeloNetTheme

enum class MeloButtonVariant {
    Primary,
    Secondary,
    Outlined,
    Text,
}

@Composable
fun MeloButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MeloButtonVariant = MeloButtonVariant.Primary,
    enabled: Boolean = true,
    containerColor: Color? = null,
) {
    val colors = MeloNetTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = MeloMotion.pressSpring,
        label = "melo_button_press",
    )
    val scaledModifier = modifier.scale(scale)

    when (variant) {
        MeloButtonVariant.Primary -> {
            Button(
                onClick = onClick,
                modifier = scaledModifier,
                enabled = enabled,
                interactionSource = interaction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = containerColor ?: MaterialTheme.colorScheme.primary,
                    disabledContainerColor = colors.disabled,
                ),
            ) {
                Text(text = text)
            }
        }
        MeloButtonVariant.Secondary -> {
            Button(
                onClick = onClick,
                modifier = scaledModifier,
                enabled = enabled,
                interactionSource = interaction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = containerColor ?: MaterialTheme.colorScheme.secondary,
                ),
            ) {
                Text(text = text)
            }
        }
        MeloButtonVariant.Outlined -> {
            OutlinedButton(
                onClick = onClick,
                modifier = scaledModifier,
                enabled = enabled,
                interactionSource = interaction,
            ) {
                Text(text = text)
            }
        }
        MeloButtonVariant.Text -> {
            TextButton(
                onClick = onClick,
                modifier = scaledModifier,
                enabled = enabled,
                interactionSource = interaction,
            ) {
                Text(text = text)
            }
        }
    }
}
