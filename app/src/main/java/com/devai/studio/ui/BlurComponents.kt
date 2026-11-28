package com.devai.studio.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Frosted surface backed by Haze. The modifier is intentionally a no-op fallback
 * on devices without a Haze source so every caller can use it unconditionally.
 */
@Composable
fun Modifier.devaiFrosted(
    haze: HazeState?,
    shape: Shape,
    tint: Color = MaterialTheme.colorScheme.surface,
): Modifier {
    if (haze == null) return clip(shape).background(tint.copy(alpha = 0.96f))
    val primary = MaterialTheme.colorScheme.primary
    return clip(shape).hazeEffect(state = haze) {
        backgroundColor = tint.copy(alpha = 0.78f)
        tints = listOf(
            HazeTint(Color.White.copy(alpha = 0.10f)),
            HazeTint(primary.copy(alpha = 0.06f)),
        )
        blurRadius = 24.dp
        noiseFactor = 0.04f
    }
}

/** A card that reads as frosted glass over the app's animated background. */
@Composable
fun DevAIBlurCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val haze = LocalHazeState.current
    val shape = MaterialTheme.shapes.large
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Box(
        modifier
            .fillMaxWidth()
            .then(clickable)
            .devaiFrosted(haze, shape),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (accent != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(accent.copy(alpha = 0.72f))
                )
            }
            content()
        }
    }
}

@Composable
fun DevAIBlurPanel(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    tint: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    Box(
        modifier.devaiFrosted(LocalHazeState.current, shape, tint),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Decorative bloom: a blurred color blob used behind hero content. */
@Composable
fun DevAIBlurredGlow(
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    blur: Dp = 56.dp,
) {
    Box(
        modifier
            .size(size)
            .blur(blur)
            .background(color.copy(alpha = 0.18f), CircleShape),
    )
}

/** Compact frosted badge used on top of images and colored surfaces. */
@Composable
fun DevAIBlurBadge(
    text: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.34f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            }
            if (icon != null) {
                androidx.compose.foundation.layout.Spacer(Modifier.size(5.dp))
            }
            Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** A small frosted dialog shell for confirmations that sit over rich backgrounds. */
@Composable
fun DevAIBlurDialog(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

/** Search field with a real Haze backdrop and Material 3 shape. */
@Composable
fun DevAIBlurSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val shape = CircleShape
    Surface(
        modifier = modifier.fillMaxWidth().devaiFrosted(
            LocalHazeState.current,
            shape,
            MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = shape,
        color = Color.Transparent,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            singleLine = true,
            shape = shape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
            ),
        )
    }
}
