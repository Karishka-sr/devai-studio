package com.devai.studio.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.devai.studio.data.StyleAvatar
import com.devai.studio.data.StyleFont
import com.devai.studio.data.StylePalette
import com.devai.studio.data.UiStyle
import com.devai.studio.data.UiStyleCatalog

val LocalUiStyle = staticCompositionLocalOf { UiStyleCatalog.classic }

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
