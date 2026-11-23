package com.devai.studio.ui

import android.content.Intent
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devai.studio.data.AppPrefs
import com.devai.studio.data.ChatMode
import com.devai.studio.data.ChatSessionsStore
import com.devai.studio.data.AppUpdate
import com.devai.studio.data.PremiumStore
import com.devai.studio.data.UpdateChecker
import com.devai.studio.util.AndroidUtils
import com.devai.studio.util.ThemeManager
import com.qwenz.qwel.MainActivity as QwelActivity

private val AuroraLight = lightColorScheme(
    primary = Color(0xFF5B4BE8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E3FF),
    onPrimaryContainer = Color(0xFF1B1450),
    secondary = Color(0xFF0B7A6B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC9F7EF),
    onSecondaryContainer = Color(0xFF00382F),
    tertiary = Color(0xFFC2415A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9DF),
    onTertiaryContainer = Color(0xFF4A0018),
    background = Color(0xFFF8F7FF),
    onBackground = Color(0xFF141322),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF141322),
    surfaceVariant = Color(0xFFEEEBFF),
    onSurfaceVariant = Color(0xFF5B5870),
    outline = Color(0xFFC9C4E4),
    outlineVariant = Color(0xFFE3DFF5),
)

private val AuroraDark = darkColorScheme(
    primary = Color(0xFFC9C1FF),
    onPrimary = Color(0xFF241A66),
    primaryContainer = Color(0xFF3B2FA0),
    onPrimaryContainer = Color(0xFFE7E3FF),
    secondary = Color(0xFF6FE7D4),
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF0B5F54),
    onSecondaryContainer = Color(0xFFC9F7EF),
    tertiary = Color(0xFFFFB3C0),
    onTertiary = Color(0xFF4A0018),
    tertiaryContainer = Color(0xFF7A2038),
    onTertiaryContainer = Color(0xFFFFD9DF),
    background = Color(0xFF0B0B14),
    onBackground = Color(0xFFF1EFFF),
    surface = Color(0xFF141422),
    onSurface = Color(0xFFF1EFFF),
    surfaceVariant = Color(0xFF1E1D30),
    onSurfaceVariant = Color(0xFFB9B4D0),
    outline = Color(0xFF4A4763),
    outlineVariant = Color(0xFF2A2840),
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
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

object DevAIThemeState {
    var revision by mutableIntStateOf(0)
        private set

    fun refresh() {
        revision++
    }
}

@Composable
fun DevAITheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val revision = DevAIThemeState.revision
    val dark = isSystemInDarkTheme()
    val selected = AppPrefs.theme(context).let { id ->
        val feature = ThemeManager.featureFor(id)
        if (feature != null && !PremiumStore.hasFeature(context, feature)) ThemeManager.M3 else id
    }
    val base = when {
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
    val scheme = if (accent == null || accent.id == "default") base else base.copy(
        primary = Color(if (dark) accent.dark else accent.light),
        onPrimary = if (dark) Color(0xFF111827) else Color.White,
        primaryContainer = Color(if (dark) accent.dark else accent.light).copy(alpha = 0.18f),
        onPrimaryContainer = if (dark) Color(0xFFE5E7EB) else Color(0xFF111827),
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = DevAITypography,
        shapes = DevAIShapes,
        content = content,
    )
}

@Composable
fun DevAIApp() {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf("chat") }
    var modeId by rememberSaveable { mutableStateOf(AppPrefs.chatMode(context)) }
    var update by remember { mutableStateOf<AppUpdate?>(null) }
    var updateDismissed by rememberSaveable { mutableStateOf(false) }
    val mode = ChatMode.fromId(modeId)

    LaunchedEffect(Unit) {
        update = UpdateChecker.cached(context) ?: UpdateChecker.check(context)
    }

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
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
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

    val available = update
    if (available != null && !updateDismissed) {
        UpdateOverlay(
            onDownload = {
                AndroidUtils.openUrl(
                    context,
                    available.apkUrl.ifBlank { available.pageUrl },
                )
                updateDismissed = true
            },
            onDismiss = { updateDismissed = true },
        )
    }
}

@Composable
private fun DevAIBottomBar(
    tab: String,
    onTab: (String) -> Unit,
    onBuilder: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
            ),
            shadowElevation = 18.dp,
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
private fun RowScope.DevAINavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary,
                            )
                        )
                    )
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
            tint = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
