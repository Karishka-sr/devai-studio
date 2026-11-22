package com.devai.studio.ui

import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.devai.studio.data.AppPrefs
import com.devai.studio.data.ChatMode
import com.devai.studio.data.ChatSessionsStore
import com.devai.studio.data.PremiumStore
import com.devai.studio.util.ThemeManager
import com.qwenz.qwel.MainActivity as QwelActivity

private val ExpressiveLight = lightColorScheme(
    primary = Color(0xFF7C4DFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF22005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
)

private val ExpressiveDark = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
)

private val RetroLight = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = Color(0xFF0D47A1),
    secondary = Color(0xFF546E7A),
    onSecondary = Color.White,
    tertiary = Color(0xFF00897B),
    onTertiary = Color.White,
)

private val RetroDark = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFBBDEFB),
    secondary = Color(0xFFB0BEC5),
    onSecondary = Color(0xFF263238),
    tertiary = Color(0xFF4DB6AC),
    onTertiary = Color(0xFF00332E),
)

@Composable
fun DevAITheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val selected = AppPrefs.theme(context).let { id ->
        if (ThemeManager.isPremium(id) && !PremiumStore.isPremium(context)) ThemeManager.M3 else id
    }
    val base = when {
        selected == ThemeManager.EXPRESSIVE -> if (dark) ExpressiveDark else ExpressiveLight
        selected == ThemeManager.RETRO -> if (dark) RetroDark else RetroLight
        AppPrefs.dynamicColor(context) -> if (dark) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    val accent = ThemeManager.accents.firstOrNull { it.id == AppPrefs.accent(context) }
    val scheme = if (accent == null || accent.id == "default") base else base.copy(
        primary = Color(if (dark) accent.dark else accent.light),
        onPrimary = if (dark) Color(0xFF111827) else Color.White,
        primaryContainer = Color(if (dark) accent.dark else accent.light).copy(alpha = 0.18f),
        onPrimaryContainer = if (dark) Color(0xFFE5E7EB) else Color(0xFF111827),
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun DevAIApp() {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf("chat") }
    var modeId by rememberSaveable { mutableStateOf(AppPrefs.chatMode(context)) }
    val mode = ChatMode.fromId(modeId)
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == "chat" || tab == "modes",
                    onClick = { tab = "modes" },
                    icon = { Icon(Icons.Default.List, contentDescription = "Режимы") },
                    label = { Text("Режимы") },
                )
                NavigationBarItem(
                    selected = tab == "providers",
                    onClick = { tab = "providers" },
                    icon = { Icon(Icons.Default.Person, contentDescription = "ИИ") },
                    label = { Text("ИИ") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, QwelActivity::class.java))
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Конструктор") },
                    label = { Text("Конструктор") },
                )
                NavigationBarItem(
                    selected = tab == "tools",
                    onClick = { tab = "tools" },
                    icon = { Icon(Icons.Default.Build, contentDescription = "Инструменты") },
                    label = { Text("Ещё") },
                )
                NavigationBarItem(
                    selected = tab == "settings",
                    onClick = { tab = "settings" },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Настройки") },
                    label = { Text("Настройки") },
                )
            }
        }
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
