package com.melonet.app.core.designsystem.component

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    when (variant) {
        MeloButtonVariant.Primary -> {
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
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
                modifier = modifier,
                enabled = enabled,
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
                modifier = modifier,
                enabled = enabled,
            ) {
                Text(text = text)
            }
        }
        MeloButtonVariant.Text -> {
            TextButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
            ) {
                Text(text = text)
            }
        }
    }
}
