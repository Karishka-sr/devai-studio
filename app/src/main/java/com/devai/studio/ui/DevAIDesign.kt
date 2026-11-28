package com.devai.studio.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devai.studio.R
import dev.chrisbanes.haze.hazeSource

/** Material 3 tokens used by decorative effects and the legacy style previews. */
object DevAIPalette {
    val Primary = Color(0xFF6750A4)
    val PrimaryContainer = Color(0xFFEADDFF)
    val Secondary = Color(0xFF006A6A)
    val SecondaryContainer = Color(0xFFCCE8E8)
    val Tertiary = Color(0xFF7D5260)
    val TertiaryContainer = Color(0xFFFFD8E4)
    val Ink = Color(0xFF1D1B20)
    val Paper = Color(0xFFFEF7FF)
    val Card = Color(0xFFFFFBFE)
    val Line = Color(0xFFCAC4D0)
}

@Composable
fun DevAIBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val haze = LocalHazeState.current
    CompositionLocalProvider(LocalContentColor provides scheme.onBackground) {
        Box(
            modifier
                .fillMaxSize()
                .background(scheme.background)
                .then(if (haze != null) Modifier.hazeSource(haze, zIndex = 0f) else Modifier),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(scheme.primary.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width * 0.92f, size.height * 0.04f),
                        radius = size.width * 0.78f,
                    ),
                    radius = size.width * 0.78f,
                    center = Offset(size.width * 0.92f, size.height * 0.04f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(scheme.tertiary.copy(alpha = 0.09f), Color.Transparent),
                        center = Offset(size.width * 0.04f, size.height * 0.92f),
                        radius = size.width * 0.72f,
                    ),
                    radius = size.width * 0.72f,
                    center = Offset(size.width * 0.04f, size.height * 0.92f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(scheme.secondary.copy(alpha = 0.07f), Color.Transparent),
                        center = Offset(size.width * 0.1f, size.height * 0.18f),
                        radius = size.width * 0.52f,
                    ),
                    radius = size.width * 0.52f,
                    center = Offset(size.width * 0.1f, size.height * 0.18f),
                )
            }
            content()
        }
    }
}

@Composable
fun DevAILogo(size: Dp = 42.dp) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(size * 0.3f),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(size * 0.76f),
            )
        }
    }
}

@Composable
fun DevAITopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val haze = LocalHazeState.current
    val shape = RoundedCornerShape(0.dp)
    val surface = MaterialTheme.colorScheme.surface
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (haze != null) Modifier.devaiFrosted(haze, shape, surface)
                else Modifier.background(surface)
            ),
        shape = shape,
        color = if (haze != null) Color.Transparent else surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                FilledTonalIconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
            } else {
                DevAILogo(38.dp)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            actions()
        }
    }
}

@Composable
fun DevAIActionIcon(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
        ),
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(20.dp))
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
    val shape = MaterialTheme.shapes.large
    val body: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (accent != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.72f)),
                )
            }
            content()
        }
    }
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
    val elevation = CardDefaults.cardElevation(defaultElevation = if (accent != null) 2.dp else 0.dp)
    val border = BorderStroke(
        1.dp,
        (accent ?: MaterialTheme.colorScheme.outline).copy(alpha = if (accent != null) 0.24f else 0.14f),
    )
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            content = body,
        )
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            content = body,
        )
    }
}

@Composable
fun DevAIIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 44.dp,
) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(size * 0.3f),
        color = tint.copy(alpha = 0.14f),
        contentColor = tint,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(size * 0.48f))
        }
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
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
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
            DevAIIconTile(icon, size = 36.dp)
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
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
        color = accent.copy(alpha = 0.13f),
        contentColor = accent,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
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

@Composable
fun DevAIHairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    )
}

@Composable
fun DevAIEmptyState(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}
