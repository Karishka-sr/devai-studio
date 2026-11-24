package com.devai.studio.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.devai.studio.data.StyleAvatar
import com.devai.studio.data.StyleBackground
import com.devai.studio.data.StyleFont
import com.devai.studio.data.StyleLayout
import com.devai.studio.data.StylePalette
import com.devai.studio.data.UiStyle
import com.devai.studio.data.UiStyleCatalog
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

val LocalUiStyle = staticCompositionLocalOf { UiStyleCatalog.classic }
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

val UiStyle.isGlass: Boolean
    get() = layout == StyleLayout.GLASS || layout == StyleLayout.LIQUID_GLASS ||
        background == StyleBackground.GLASS

val UiStyle.isLiquidGlass: Boolean
    get() = layout == StyleLayout.LIQUID_GLASS

val UiStyle.isMaterial: Boolean
    get() = layout == StyleLayout.MATERIAL_YOU ||
        layout == StyleLayout.MATERIAL_DESIGN ||
        layout == StyleLayout.MATERIAL_EXPRESSIVE

fun UiStyle.effectiveDark(systemDark: Boolean): Boolean =
    forceDark || (!forceLight && systemDark)

fun UiStyle.palette(dark: Boolean): StylePalette =
    if (effectiveDark(dark)) this.dark else light

fun UiStyle.colorScheme(systemDark: Boolean, accent: Color? = null): ColorScheme {
    val dark = effectiveDark(systemDark)
    val p = palette(dark)
    val scheme = if (dark) {
        darkColorScheme(
            primary = Color(p.primary),
            onPrimary = Color(p.onPrimary),
            primaryContainer = Color(p.primary).copy(alpha = 0.28f),
            onPrimaryContainer = Color(p.onBackground),
            secondary = Color(p.secondary),
            onSecondary = Color(p.onSecondary),
            secondaryContainer = Color(p.secondary).copy(alpha = 0.24f),
            onSecondaryContainer = Color(p.onBackground),
            tertiary = Color(p.tertiary),
            onTertiary = Color(p.onTertiary),
            tertiaryContainer = Color(p.tertiary).copy(alpha = 0.24f),
            onTertiaryContainer = Color(p.onBackground),
            background = Color(p.background),
            onBackground = Color(p.onBackground),
            surface = Color(p.surface),
            onSurface = Color(p.onSurface),
            surfaceVariant = Color(p.surfaceVariant),
            onSurfaceVariant = Color(p.onSurfaceVariant),
            outline = Color(p.outline),
            outlineVariant = Color(p.outline).copy(alpha = 0.55f),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
        )
    } else {
        lightColorScheme(
            primary = Color(p.primary),
            onPrimary = Color(p.onPrimary),
            primaryContainer = Color(p.primary).copy(alpha = 0.16f),
            onPrimaryContainer = Color(p.onBackground),
            secondary = Color(p.secondary),
            onSecondary = Color(p.onSecondary),
            secondaryContainer = Color(p.secondary).copy(alpha = 0.14f),
            onSecondaryContainer = Color(p.onBackground),
            tertiary = Color(p.tertiary),
            onTertiary = Color(p.onTertiary),
            tertiaryContainer = Color(p.tertiary).copy(alpha = 0.14f),
            onTertiaryContainer = Color(p.onBackground),
            background = Color(p.background),
            onBackground = Color(p.onBackground),
            surface = Color(p.surface),
            onSurface = Color(p.onSurface),
            surfaceVariant = Color(p.surfaceVariant),
            onSurfaceVariant = Color(p.onSurfaceVariant),
            outline = Color(p.outline),
            outlineVariant = Color(p.outline).copy(alpha = 0.55f),
            error = Color(0xFFB3261E),
            onError = Color.White,
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B),
        )
    }
    if (accent == null) return scheme
    return scheme.copy(
        primary = accent,
        onPrimary = if (accent.luminance() > 0.55f) Color(0xFF111827) else Color.White,
        primaryContainer = accent.copy(alpha = if (dark) 0.28f else 0.16f),
        onPrimaryContainer = if (dark) Color(0xFFE5E7EB) else Color(0xFF111827),
    )
}

fun UiStyle.shapes(): Shapes {
    val r = corner.coerceIn(0, 40)
    fun radius(factor: Float) = (r * factor).dp
    return Shapes(
        extraSmall = RoundedCornerShape(radius(0.3f)),
        small = RoundedCornerShape(radius(0.55f)),
        medium = RoundedCornerShape(radius(0.8f)),
        large = RoundedCornerShape(radius(1f)),
        extraLarge = RoundedCornerShape(radius(1.3f)),
    )
}

fun UiStyle.typography(base: Typography): Typography {
    val family = when (font) {
        StyleFont.SANS -> FontFamily.SansSerif
        StyleFont.SERIF -> FontFamily.Serif
        StyleFont.MONO -> FontFamily.Monospace
    }
    val bodyFamily = if (font == StyleFont.MONO) FontFamily.Monospace else FontFamily.SansSerif
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = bodyFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = bodyFamily),
        bodySmall = base.bodySmall.copy(fontFamily = bodyFamily),
        labelLarge = base.labelLarge.copy(fontFamily = bodyFamily),
        labelMedium = base.labelMedium.copy(fontFamily = bodyFamily),
        labelSmall = base.labelSmall.copy(fontFamily = bodyFamily),
    )
}

fun UiStyle.avatarShape(): Shape = when (avatar) {
    StyleAvatar.CIRCLE -> CircleShape
    StyleAvatar.ROUNDED -> RoundedCornerShape((corner * 0.7f).dp)
    StyleAvatar.SQUARE -> RoundedCornerShape(2.dp)
}

fun UiStyle.cardShape(): Shape = RoundedCornerShape(corner.dp)
fun UiStyle.pillShape(): Shape = RoundedCornerShape((corner + 12).dp)
fun UiStyle.userBubbleColor(dark: Boolean): Color = Color(palette(dark).userBubble)
fun UiStyle.onUserBubbleColor(dark: Boolean): Color = Color(palette(dark).onUserBubble)
fun UiStyle.aiBubbleColor(dark: Boolean): Color = Color(palette(dark).aiBubble)
fun UiStyle.onAiBubbleColor(dark: Boolean): Color = Color(palette(dark).onAiBubble)


/**
 * Real backdrop blur for glass styles. Haze captures the background and message list
 * and redraws them behind this panel; the Surface itself must stay translucent.
 */
fun Modifier.glassPanel(
    haze: HazeState?,
    style: UiStyle,
    shape: Shape,
    dark: Boolean,
): Modifier {
    if (!style.isGlass || haze == null) return this
    val palette = style.palette(dark)
    val surface = Color(palette.surface)
    val primary = Color(palette.primary)
    val liquid = style.isLiquidGlass
    return clip(shape).hazeEffect(state = haze) {
        backgroundColor = surface.copy(alpha = if (liquid) 0.34f else 0.52f)
        tints = listOf(
            HazeTint(Color.White.copy(alpha = if (dark) 0.05f else 0.18f)),
            HazeTint(primary.copy(alpha = if (liquid) 0.10f else 0.04f)),
        )
        blurRadius = if (liquid) 30.dp else 22.dp
        noiseFactor = 0.05f
    }
}

fun UiStyle.glassBorder(dark: Boolean): Brush {
    val p = palette(dark)
    return Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = if (dark) 0.22f else 0.72f),
            Color(p.primary).copy(alpha = 0.20f),
            Color.White.copy(alpha = if (dark) 0.10f else 0.38f),
        )
    )
}

@Composable
fun LiquidSheen(
    modifier: Modifier = Modifier,
    shape: Shape,
    strength: Float = 0.20f,
) {
    val transition = rememberInfiniteTransition(label = "liquid-glass")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sheen",
    )
    Box(
        modifier
            .clip(shape)
            .drawBehind {
                val start = Offset(size.width * (shift - 0.55f), -size.height * 0.35f)
                val end = Offset(size.width * (shift + 0.55f), size.height * 1.35f)
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = strength), Color.Transparent),
                        start = start,
                        end = end,
                    )
                )
            }
    )
}
