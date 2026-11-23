package com.devai.studio.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devai.studio.R

object DevAIPalette {
    val Indigo = Color(0xFF5B4BE8)
    val IndigoDeep = Color(0xFF35258F)
    val Mint = Color(0xFF0B7A6B)
    val Coral = Color(0xFFC2415A)
    val Amber = Color(0xFFF59E0B)
    val Ink = Color(0xFF141322)
    val Paper = Color(0xFFF8F7FF)
    val Card = Color(0xFFFFFFFF)
    val Line = Color(0xFFE3DFF5)
}

@Composable
fun DevAIBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val style = LocalUiStyle.current
    val dark = style.effectiveDark(scheme.background.luminance() < 0.35f)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val step = with(density) { 28.dp.toPx() }
    CompositionLocalProvider(LocalContentColor provides scheme.onBackground) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    when (style.background) {
                        com.devai.studio.data.StyleBackground.AURORA ->
                            Brush.verticalGradient(listOf(scheme.background, if (dark) scheme.surface else scheme.background, scheme.background))
                        com.devai.studio.data.StyleBackground.MESH ->
                            Brush.linearGradient(listOf(scheme.background, scheme.surfaceVariant, scheme.background))
                        com.devai.studio.data.StyleBackground.GLASS ->
                            Brush.verticalGradient(listOf(scheme.background, scheme.surface.copy(alpha = 0.92f), scheme.background))
                        else -> Brush.verticalGradient(listOf(scheme.background, scheme.background))
                    }
                ),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                when (style.background) {
                    com.devai.studio.data.StyleBackground.AURORA -> {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(scheme.primary.copy(alpha = if (dark) 0.18f else 0.12f), Color.Transparent),
                                center = Offset(size.width * 0.92f, size.height * 0.03f),
                                radius = size.width * 0.75f,
                            ),
                            radius = size.width * 0.75f,
                            center = Offset(size.width * 0.92f, size.height * 0.03f),
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(scheme.tertiary.copy(alpha = if (dark) 0.12f else 0.08f), Color.Transparent),
                                center = Offset(size.width * 0.05f, size.height * 0.85f),
                                radius = size.width * 0.7f,
                            ),
                            radius = size.width * 0.7f,
                            center = Offset(size.width * 0.05f, size.height * 0.85f),
                        )
                    }
                    com.devai.studio.data.StyleBackground.GRID -> {
                        var x = 0f
                        while (x < size.width) {
                            drawLine(scheme.outline.copy(alpha = 0.22f), Offset(x, 0f), Offset(x, size.height), 1f)
                            x += step
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(scheme.outline.copy(alpha = 0.22f), Offset(0f, y), Offset(size.width, y), 1f)
                            y += step
                        }
                    }
                    com.devai.studio.data.StyleBackground.DOTS -> {
                        val dot = with(density) { 1.4.dp.toPx() }
                        var y = step / 2
                        while (y < size.height) {
                            var x = step / 2
                            while (x < size.width) {
                                drawCircle(scheme.outline.copy(alpha = 0.35f), dot, Offset(x, y))
                                x += step
                            }
                            y += step
                        }
                    }
                    com.devai.studio.data.StyleBackground.PAPER -> {
                        var y = step
                        while (y < size.height) {
                            drawLine(scheme.outline.copy(alpha = 0.18f), Offset(0f, y), Offset(size.width, y), 1f)
                            y += step
                        }
                    }
                    com.devai.studio.data.StyleBackground.TERMINAL -> {
                        var y = 0f
                        while (y < size.height) {
                            drawRect(Color.Black.copy(alpha = 0.16f), Offset(0f, y), androidx.compose.ui.geometry.Size(size.width, 1f))
                            y += with(density) { 5.dp.toPx() }
                        }
                    }
                    com.devai.studio.data.StyleBackground.MESH, com.devai.studio.data.StyleBackground.GLASS -> {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(scheme.primary.copy(alpha = if (dark) 0.16f else 0.10f), Color.Transparent),
                                center = Offset(size.width * 0.15f, size.height * 0.12f),
                                radius = size.width * 0.8f,
                            ),
                            radius = size.width * 0.8f,
                            center = Offset(size.width * 0.15f, size.height * 0.12f),
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(scheme.tertiary.copy(alpha = if (dark) 0.12f else 0.08f), Color.Transparent),
                                center = Offset(size.width * 0.9f, size.height * 0.82f),
                                radius = size.width * 0.75f,
                            ),
                            radius = size.width * 0.75f,
                            center = Offset(size.width * 0.9f, size.height * 0.82f),
                        )
                    }
                    com.devai.studio.data.StyleBackground.NEON -> {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(scheme.secondary.copy(alpha = 0.18f), Color.Transparent),
                                center = Offset(size.width * 0.2f, size.height * 0.25f),
                                radius = size.width * 0.9f,
                            ),
                            radius = size.width * 0.9f,
                            center = Offset(size.width * 0.2f, size.height * 0.25f),
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(scheme.tertiary.copy(alpha = 0.12f), Color.Transparent),
                                center = Offset(size.width * 0.85f, size.height * 0.7f),
                                radius = size.width * 0.8f,
                            ),
                            radius = size.width * 0.8f,
                            center = Offset(size.width * 0.85f, size.height * 0.7f),
                        )
                    }
                    com.devai.studio.data.StyleBackground.SOLID -> Unit
                }
            }
            content()
        }
    }
}

@Composable
fun DevAILogo(size: Dp = 42.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.34f))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(size * 0.78f),
        )
    }
}

@Composable
fun DevAITopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        } else {
            DevAILogo(42.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        actions()
    }
}

@Composable
fun DevAIActionIcon(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
    ) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp)) {
            Icon(
                icon,
                contentDescription = description,
                modifier = Modifier.size(19.dp),
                tint = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
fun DevAICard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = LocalUiStyle.current.cardShape()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(
            alpha = if (LocalUiStyle.current.background == com.devai.studio.data.StyleBackground.GLASS) 0.66f else 0.9f,
        ),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            1.dp,
            (accent ?: MaterialTheme.colorScheme.outline).copy(alpha = if (accent != null) 0.35f else 0.16f),
        ),
        shadowElevation = 0.dp,
    ) {
        Box {
            if (accent != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.15f))))
                )
            }
            Column(
                modifier = Modifier.padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}

@Composable
fun DevAIIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 44.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(if (LocalUiStyle.current.corner == 0) RoundedCornerShape(0.dp) else RoundedCornerShape(size * 0.32f))
            .background(
                Brush.linearGradient(
                    listOf(tint.copy(alpha = 0.18f), tint.copy(alpha = 0.06f))
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun DevAIGradientButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = LocalUiStyle.current.pillShape()
    val brush = Brush.horizontalGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
    )
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .clip(shape)
            .background(if (enabled) brush else Brush.horizontalGradient(listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceVariant,
            ))),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DevAISectionTitle(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            DevAIIconTile(icon, size = 34.dp)
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun DevAIStatusPill(text: String, accent: Color = MaterialTheme.colorScheme.primary) {
    Surface(
        shape = CircleShape,
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = accent,
        )
    }
}
