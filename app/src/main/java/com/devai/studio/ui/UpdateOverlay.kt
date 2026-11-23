package com.devai.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.min

@Composable
fun UpdateOverlay(
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val scale = min(
        configuration.screenWidthDp / 360f,
        configuration.screenHeightDp / 800f,
    ).coerceAtMost(1f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(360.dp, 800.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                UpdateWindow(
                    onDownload = onDownload,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateWindow(
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Box(
            Modifier
                .offset(12.38.dp, 185.63.dp)
                .size(335.25.dp, 428.73.dp)
                .background(
                    Brush.radialGradient(
                        listOf(MaterialTheme.colorScheme.primary, Color(0xFF012121)),
                    ),
                    RoundedCornerShape(17.31.dp),
                ),
        )
        Box(
            Modifier
                .offset(1.92.dp, 313.86.dp)
                .size(170.79.dp, 170.79.dp)
                .rotate(30f)
                .alpha(0.33f)
                .blur(2.6.dp, BlurredEdgeTreatment.Unbounded)
                .background(
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialShapes.Clover4Leaf.toShape(),
                ),
        )
        Box(
            Modifier
                .offset(162.17.dp, 432.56.dp)
                .size(170.79.dp, 170.79.dp)
                .alpha(0.33f)
                .blur(2.6.dp, BlurredEdgeTreatment.Unbounded)
                .background(
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialShapes.SoftBurst.toShape(),
                ),
        )
        Box(
            Modifier
                .offset(27.01.dp, 198.81.dp)
                .size(305.9.dp, 46.2.dp)
                .blur(2.69.dp, BlurredEdgeTreatment.Unbounded)
                .background(
                    Brush.radialGradient(
                        listOf(MaterialTheme.colorScheme.secondary, Color(0xFF012121)),
                    ),
                    RoundedCornerShape(23.1.dp),
                ),
        )
        Box(
            Modifier
                .offset(174.26.dp, 197.96.dp)
                .size(170.79.dp, 170.79.dp)
                .rotate(30f)
                .alpha(0.33f)
                .blur(2.6.dp, BlurredEdgeTreatment.Unbounded)
                .background(
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialShapes.Cookie12Sided.toShape(),
                ),
        )
        Text(
            text = "Вышло обновление DevAI Studio!",
            modifier = Modifier.offset(43.8.dp, 211.dp),
            color = Color(0xFFFF8739),
            fontSize = 17.41.sp,
            fontWeight = FontWeight.W700,
            softWrap = false,
        )
        OverlayButton(
            label = "Скачать",
            y = 423.12.dp,
            onClick = onDownload,
        )
        OverlayButton(
            label = "Позже",
            y = 493.51.dp,
            onClick = onDismiss,
        )
    }
}

@Composable
private fun OverlayButton(
    label: String,
    y: Dp,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.45.dp)
    Box(
        Modifier
            .offset(96.18.dp, y)
            .size(167.7.dp, 48.9.dp)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .alpha(0.64f)
                .blur(1.06.dp, BlurredEdgeTreatment.Unbounded)
                .background(
                    Brush.radialGradient(
                        listOf(MaterialTheme.colorScheme.primary, Color(0xFF012121)),
                    ),
                    shape,
                ),
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.W500,
            softWrap = false,
        )
    }
}
