package com.devai.studio.ui

import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.qwenz.qwel.MainActivity as QwelActivity

@Composable
fun DevAITheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = if (isSystemInDarkTheme()) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun DevAIApp() {
    var tab by rememberSaveable { mutableStateOf("chat") }
    val context = LocalContext.current
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == "chat",
                    onClick = { tab = "chat" },
                    icon = { Text("💬") },
                    label = { Text("Чат") },
                )
                NavigationBarItem(
                    selected = tab == "providers",
                    onClick = { tab = "providers" },
                    icon = { Text("🤖") },
                    label = { Text("ИИ") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, QwelActivity::class.java))
                    },
                    icon = { Text("🧩") },
                    label = { Text("Конструктор") },
                )
                NavigationBarItem(
                    selected = tab == "settings",
                    onClick = { tab = "settings" },
                    icon = { Text("⚙️") },
                    label = { Text("Настройки") },
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                "providers" -> ProvidersScreen()
                "settings" -> SettingsScreen()
                else -> ChatScreen()
            }
        }
    }
}
