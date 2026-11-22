package com.devai.studio.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devai.studio.data.Plugin
import com.devai.studio.data.PluginStore
import com.devai.studio.util.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PluginsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        ThemeManager.applyDynamicIfAvailable(this)
        setContent { DevAITheme { DevAIBackground { PluginsScreen(onBack = { finish() }) } } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var plugins by remember { mutableStateOf(PluginStore.list(context)) }
    var busy by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Plugin?>(null) }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { PluginStore.import(context, uri) } }
                .onSuccess {
                    Toast.makeText(context, "Плагин добавлен: ${it.name}", Toast.LENGTH_SHORT).show()
                    plugins = PluginStore.list(context)
                }
                .onFailure {
                    Toast.makeText(
                        context,
                        "Ошибка плагина: ${it.message ?: "неизвестно"}",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            busy = false
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            DevAITopBar(
                title = "Плагины .devai",
                subtitle = "Команды и промпты без выполнения кода",
                onBack = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                DevAISectionTitle(
                    title = "Плагины",
                    subtitle = "DPL-промпты и Lua-команды для чата.",
                    icon = Icons.Default.Info,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { importer.launch(arrayOf("*/*")) },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(Modifier.width(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Импорт .devai")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(context, PluginDocsActivity::class.java))
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Документация")
                    }
                }
            }
            if (plugins.isEmpty()) {
                item {
                    Text(
                        "Плагинов нет. Импортируйте .devai файл.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(plugins) { plugin ->
                DevAICard(
                    accent = MaterialTheme.colorScheme.secondary,
                    contentPadding = PaddingValues(14.dp),
                ) {
                        Text(plugin.name, fontWeight = FontWeight.SemiBold)
                        if (plugin.description.isNotBlank()) {
                            Text(
                                plugin.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            buildString {
                                if (plugin.lua) {
                                    append("Lua · команд: ").append(plugin.commands.size)
                                    append(" · инструментов ИИ: ").append(plugin.tools.size)
                                } else {
                                    append(plugin.command).append(" · DPL ").append(plugin.version)
                                }
                                if (plugin.author.isNotBlank()) append(" · ").append(plugin.author)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (plugin.commands.isNotEmpty()) {
                            Text(
                                plugin.commands.joinToString("  ") { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else if (plugin.prompt.isNotBlank()) {
                            Text(
                                plugin.prompt.take(240),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { deleting = plugin }) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Удалить")
                        }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    deleting?.let { plugin ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Удалить плагин?") },
            text = { Text("«${plugin.name}» будет удалён из приложения.") },
            confirmButton = {
                TextButton(onClick = {
                    PluginStore.delete(context, plugin)
                    deleting = null
                    plugins = PluginStore.list(context)
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Отмена") } },
        )
    }
}
