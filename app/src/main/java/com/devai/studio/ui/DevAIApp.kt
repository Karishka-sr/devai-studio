package com.devai.studio.ui

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.devai.studio.data.AppPrefs
import com.devai.studio.data.StyleLayout
import com.devai.studio.data.UiStyleCatalog
import com.devai.studio.data.ChatMode
import com.devai.studio.data.ChatSessionsStore
import com.devai.studio.data.AppUpdate
import com.devai.studio.data.PremiumStore
import com.devai.studio.data.UpdateChecker
import com.devai.studio.util.AndroidUtils
import com.devai.studio.util.ThemeManager
import com.qwenz.qwel.MainActivity as QwelActivity
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val AuroraLight = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF006A6A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE8E8),
    onSecondaryContainer = Color(0xFF002020),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    background = Color(0xFFFEF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9),
    inverseSurface = Color(0xFF322F35),
    inverseOnSurface = Color(0xFFF5EFF7),
    inversePrimary = Color(0xFFD0BCFF),
)

private val AuroraDark = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFF8FD8D8),
    onSecondary = Color(0xFF003737),
    secondaryContainer = Color(0xFF005050),
    onSecondaryContainer = Color(0xFFCCE8E8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF1D1B20),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B),
    inverseSurface = Color(0xFFE6E0E9),
    inverseOnSurface = Color(0xFF322F35),
    inversePrimary = Color(0xFF6750A4),
)

private val ExpressiveLight = lightColorScheme(
    primary = Color(0xFF7C4DFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF22005D),
    secondary = Color(0xFF0E9F8A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC9F7EF),
    onSecondaryContainer = Color(0xFF00382F),
    tertiary = Color(0xFFFF4081),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    background = Color(0xFFFFF7FF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFEEE7F7),
    onSurfaceVariant = Color(0xFF5B5367),
    outline = Color(0xFFC9BEE0),
    outlineVariant = Color(0xFFE8DEF8),
)

private val ExpressiveDark = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFF6FE7D4),
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF0B5F54),
    onSecondaryContainer = Color(0xFFC9F7EF),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    background = Color(0xFF0F0A1A),
    onBackground = Color(0xFFF1EFFF),
    surface = Color(0xFF1A1328),
    onSurface = Color(0xFFF1EFFF),
    surfaceVariant = Color(0xFF262033),
    onSurfaceVariant = Color(0xFFC9C0D8),
    outline = Color(0xFF514861),
    outlineVariant = Color(0xFF352E43),
)

private val RetroLight = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = Color(0xFF00897B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2DFDB),
    onSecondaryContainer = Color(0xFF00332E),
    tertiary = Color(0xFF455A64),
    onTertiary = Color.White,
    background = Color(0xFFECEFF1),
    onBackground = Color(0xFF212121),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFE3E8EB),
    onSurfaceVariant = Color(0xFF546E7A),
    outline = Color(0xFF90A4AE),
    outlineVariant = Color(0xFFCFD8DC),
)

private val RetroDark = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFBBDEFB),
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color(0xFF00332E),
    secondaryContainer = Color(0xFF00695C),
    onSecondaryContainer = Color(0xFFB2DFDB),
    tertiary = Color(0xFFB0BEC5),
    onTertiary = Color(0xFF263238),
    background = Color(0xFF10161A),
    onBackground = Color(0xFFECEFF1),
    surface = Color(0xFF1A2228),
    onSurface = Color(0xFFECEFF1),
    surfaceVariant = Color(0xFF263238),
    onSurfaceVariant = Color(0xFFB0BEC5),
    outline = Color(0xFF546E7A),
    outlineVariant = Color(0xFF37474F),
)

private val DevAITypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

private val DevAIShapes = androidx.compose.material3.Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

object DevAIThemeState {
    var revision by mutableIntStateOf(0)
        private set

    fun refresh() {
        revision++
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DevAITheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val revision = DevAIThemeState.revision
    val dark = isSystemInDarkTheme()
    val style = UiStyleCatalog.byId(AppPrefs.uiStyle(context))
    val classic = style.id == UiStyleCatalog.CLASSIC_ID
    val selected = AppPrefs.theme(context).let { id ->
        val feature = ThemeManager.featureFor(id)
        if (feature != null && !PremiumStore.hasFeature(context, feature)) ThemeManager.M3 else id
    }
    val legacyBase = when {
        selected == ThemeManager.EXPRESSIVE -> if (dark) ExpressiveDark else ExpressiveLight
        selected == ThemeManager.RETRO -> if (dark) RetroDark else RetroLight
        AppPrefs.dynamicColor(context) -> if (dark) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
        dark -> AuroraDark
        else -> AuroraLight
    }
    val accent = ThemeManager.accents.firstOrNull { it.id == AppPrefs.accent(context) }
    val accentColor = accent?.takeIf { it.id != "default" }?.let {
        Color(if (dark) it.dark else it.light)
    }
    val dynamicYou = !classic && style.layout == StyleLayout.MATERIAL_YOU &&
        AppPrefs.dynamicColor(context)
    val scheme = if (dynamicYou) {
        val base = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        if (accentColor != null) {
            base.copy(
                primary = accentColor,
                onPrimary = if (accentColor.luminance() > 0.55f) Color(0xFF111827) else Color.White,
                primaryContainer = accentColor.copy(alpha = if (dark) 0.28f else 0.16f),
                onPrimaryContainer = if (dark) Color(0xFFE5E7EB) else Color(0xFF111827),
            )
        } else {
            base
        }
    } else if (!classic) {
        style.colorScheme(dark, accentColor)
    } else if (accentColor != null) {
        legacyBase.copy(
            primary = accentColor,
            onPrimary = if (dark) Color(0xFF111827) else Color.White,
            primaryContainer = accentColor.copy(alpha = 0.18f),
            onPrimaryContainer = if (dark) Color(0xFFE5E7EB) else Color(0xFF111827),
        )
    } else {
        legacyBase
    }
    val shapes = if (classic) DevAIShapes else style.shapes()
    val typography = if (classic) DevAITypography else style.typography(DevAITypography)
    CompositionLocalProvider(LocalUiStyle provides style) {
        if (style.layout == StyleLayout.MATERIAL_EXPRESSIVE) {
            MaterialExpressiveTheme(
                colorScheme = scheme,
                typography = typography,
                shapes = shapes,
                content = content,
            )
        } else {
            MaterialTheme(
                colorScheme = scheme,
                typography = typography,
                shapes = shapes,
                content = content,
            )
        }
    }
}

@Composable
fun DevAIApp() {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf("chat") }
    var modeId by rememberSaveable { mutableStateOf(AppPrefs.chatMode(context)) }
    var update by remember { mutableStateOf(UpdateChecker.cached(context)) }
    var dismissedVersion by rememberSaveable {
        mutableStateOf(UpdateChecker.dismissedVersion(context))
    }
    val mode = ChatMode.fromId(modeId)
    val lifecycleOwner = LocalLifecycleOwner.current
    val hazeState = rememberHazeState(blurEnabled = true)

    // Poll while the app is actually visible: every 30 s the cheap website
    // JSON is checked, every 5 min (and on resume) GitHub Releases is checked.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var first = true
            while (isActive) {
                val next = runCatching { UpdateChecker.check(context, force = first) }.getOrNull()
                if (next != null) update = next
                first = false
                delay(30_000L)
            }
        }
    }

    CompositionLocalProvider(LocalHazeState provides hazeState) {
    DevAIBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                DevAIBottomBar(
                    tab = tab,
                    onTab = { tab = it },
                    onBuilder = {
                        context.startActivity(Intent(context, QwelActivity::class.java))
                    },
                )
            },
            floatingActionButton = {
                if (tab != "chat") {
                    ExtendedFloatingActionButton(
                        onClick = {
                            context.startActivity(Intent(context, QwelActivity::class.java))
                        },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Vexel", fontWeight = FontWeight.SemiBold) },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.End,
        ) { padding ->
            AnimatedContent(
                targetState = tab,
                modifier = Modifier.fillMaxSize().padding(padding),
                transitionSpec = {
                    fadeIn(tween(220)) togetherWith fadeOut(tween(140))
                },
                label = "main-tabs",
            ) { current ->
                when (current) {
                    "providers" -> ProvidersScreen()
                    "settings" -> SettingsScreen()
                    "tools" -> ToolsScreen()
                    "modes" -> ModesScreen(
                        current = mode,
                        onSelect = { selected ->
                            if (selected.id != modeId) {
                                ChatSessionsStore.create(context)
                            }
                            modeId = selected.id
                            AppPrefs.setChatMode(context, selected.id)
                            tab = "chat"
                        },
                    )
                    else -> ChatScreen(mode = mode)
                }
            }
        }
    }
    }

    val available = update?.takeIf { it.version != dismissedVersion }
    if (available != null) {
        UpdateOverlay(
            onDownload = {
                AndroidUtils.openUrl(
                    context,
                    available.apkUrl.ifBlank { available.pageUrl },
                )
                dismissedVersion = available.version
                UpdateChecker.dismiss(context, available.version)
            },
            onDismiss = {
                dismissedVersion = available.version
                UpdateChecker.dismiss(context, available.version)
            },
        )
    }
}

@Composable
private fun DevAIBottomBar(
    tab: String,
    onTab: (String) -> Unit,
    onBuilder: () -> Unit,
) {
    val style = LocalUiStyle.current
    if (style.isMaterial) {
        MaterialBottomBar(tab, onTab, onBuilder)
        return
    }
    val nav = style.nav
    val layout = style.layout
    val outerPadding = when (nav) {
        com.devai.studio.data.StyleNav.FLAT_BAR -> 8.dp
        com.devai.studio.data.StyleNav.MINIMAL -> 6.dp
        else -> 12.dp
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = outerPadding, vertical = 8.dp),
    ) {
        val glass = style.isGlass
        val haze = LocalHazeState.current
        val dark = style.effectiveDark(MaterialTheme.colorScheme.background.luminance() < 0.35f)
        val shape = when {
            layout == com.devai.studio.data.StyleLayout.CHATGPT ||
                layout == com.devai.studio.data.StyleLayout.CLAUDE -> RoundedCornerShape(0.dp)
            nav == com.devai.studio.data.StyleNav.FLAT_BAR -> RoundedCornerShape(0.dp)
            nav == com.devai.studio.data.StyleNav.MINIMAL -> RoundedCornerShape(0.dp)
            nav == com.devai.studio.data.StyleNav.TERMINAL -> RoundedCornerShape(0.dp)
            nav == com.devai.studio.data.StyleNav.SEGMENTED -> RoundedCornerShape(24.dp)
            else -> RoundedCornerShape(28.dp)
        }
        Surface(
            modifier = Modifier.then(
                if (glass) Modifier.glassPanel(haze, style, shape, dark) else Modifier
            ),
            shape = shape,
            color = when {
                glass && haze != null -> Color.Transparent
                layout == com.devai.studio.data.StyleLayout.CHATGPT -> MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                layout == com.devai.studio.data.StyleLayout.CLAUDE -> Color.Transparent
                nav == com.devai.studio.data.StyleNav.MINIMAL -> Color.Transparent
                nav == com.devai.studio.data.StyleNav.SEGMENTED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                else -> MaterialTheme.colorScheme.surface.copy(
                    alpha = if (glass) 0.72f else 0.94f,
                )
            },
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = when {
                glass -> BorderStroke(1.dp, style.glassBorder(dark))
                layout == com.devai.studio.data.StyleLayout.CLAUDE -> BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
                nav == com.devai.studio.data.StyleNav.MINIMAL -> null
                else -> BorderStroke(
                    1.dp,
                    if (nav == com.devai.studio.data.StyleNav.TERMINAL)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                )
            },
            shadowElevation = when {
                glass -> 8.dp
                layout == com.devai.studio.data.StyleLayout.CHATGPT -> 6.dp
                nav == com.devai.studio.data.StyleNav.MINIMAL -> 0.dp
                nav == com.devai.studio.data.StyleNav.TERMINAL -> 0.dp
                nav == com.devai.studio.data.StyleNav.FLAT_BAR -> 4.dp
                else -> 18.dp
            },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                DevAINavItem(
                    label = "Режимы",
                    icon = Icons.AutoMirrored.Filled.List,
                    selected = tab == "chat" || tab == "modes",
                    onClick = { onTab("modes") },
                )
                DevAINavItem(
                    label = "ИИ",
                    icon = Icons.Default.Person,
                    selected = tab == "providers",
                    onClick = { onTab("providers") },
                )
                DevAINavItem(
                    label = "Vexel",
                    icon = Icons.Default.Add,
                    selected = false,
                    onClick = onBuilder,
                )
                DevAINavItem(
                    label = "Ещё",
                    icon = Icons.Default.Build,
                    selected = tab == "tools",
                    onClick = { onTab("tools") },
                )
                DevAINavItem(
                    label = "Настройки",
                    icon = Icons.Default.Settings,
                    selected = tab == "settings",
                    onClick = { onTab("settings") },
                )
            }
        }
    }
}

@Composable
private fun MaterialBottomBar(
    tab: String,
    onTab: (String) -> Unit,
    onBuilder: () -> Unit,
) {
    val style = LocalUiStyle.current
    val haze = LocalHazeState.current
    val container = if (style.layout == StyleLayout.MATERIAL_EXPRESSIVE) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    NavigationBar(
        modifier = Modifier.fillMaxWidth().then(
            if (haze != null) Modifier.devaiFrosted(haze, RoundedCornerShape(0.dp), container) else Modifier
        ),
        containerColor = if (haze != null) Color.Transparent else container,
        tonalElevation = 0.dp,
    ) {
        MaterialNavItem("Чат", Icons.Default.Face, tab == "chat") { onTab("chat") }
        MaterialNavItem("Режимы", Icons.AutoMirrored.Filled.List, tab == "modes") { onTab("modes") }
        MaterialNavItem("ИИ", Icons.Default.Person, tab == "providers") { onTab("providers") }
        MaterialNavItem("Ещё", Icons.Default.Build, tab == "tools") { onTab("tools") }
        MaterialNavItem("Настройки", Icons.Default.Settings, tab == "settings") { onTab("settings") }
    }
}
@Composable
private fun androidx.compose.foundation.layout.RowScope.MaterialNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val design = LocalUiStyle.current.layout == StyleLayout.MATERIAL_DESIGN
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp)) },
        label = { Text(label, maxLines = 1, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = if (design) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSecondaryContainer,
            selectedTextColor = if (design) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            indicatorColor = if (design) Color.Transparent else MaterialTheme.colorScheme.secondaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun RowScope.DevAINavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val style = LocalUiStyle.current
    val sharp = style.nav == com.devai.studio.data.StyleNav.TERMINAL ||
        style.nav == com.devai.studio.data.StyleNav.MINIMAL ||
        style.layout == com.devai.studio.data.StyleLayout.CHATGPT ||
        style.layout == com.devai.studio.data.StyleLayout.CLAUDE ||
        style.layout == com.devai.studio.data.StyleLayout.GEMINI
    val shape = if (sharp) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(shape)
            .then(
                if (selected) {
                    if (sharp) {
                        Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                    } else {
                        Modifier.background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary,
                                )
                            )
                        )
                    }
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = if (selected) {
                if (sharp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
            } else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) {
                if (sharp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
            } else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
