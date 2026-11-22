package com.devai.studio.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.devai.studio.data.ApiKeyStore
import com.devai.studio.data.AppPrefs
import com.devai.studio.data.CharacterStore
import com.devai.studio.data.McpStore
import com.devai.studio.data.PluginStore
import com.devai.studio.data.SearchClient
import com.devai.studio.util.AndroidUtils
import com.devai.studio.util.TtsManager
import kotlinx.coroutines.launch

@Composable
fun ToolsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchKey by remember {
        mutableStateOf(ApiKeyStore.get(context, SearchClient.KEY_ID).orEmpty())
    }
    var searchEnabled by remember { mutableStateOf(AppPrefs.searchEnabled(context)) }
    var searchResult by remember { mutableStateOf("") }
    var searchBusy by remember { mutableStateOf(false) }
    var ttsEnabled by remember { mutableStateOf(AppPrefs.ttsEnabled(context)) }
    var ttsRate by remember { mutableFloatStateOf(AppPrefs.ttsRate(context)) }
    var mcpEnabled by remember { mutableStateOf(AppPrefs.mcpEnabled(context)) }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Build,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Инструменты",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            "Поиск, MCP, озвучивание, персонажи, плагины и QR.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ToolCard("Веб-поиск", "Tavily. Результаты добавляются в контекст ответа.") {
            OutlinedTextField(
                value = searchKey,
                onValueChange = { searchKey = it },
                label = { Text("Ключ Tavily") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (searchKey.isBlank()) {
                            ApiKeyStore.clear(context, SearchClient.KEY_ID)
                        } else {
                            ApiKeyStore.save(context, SearchClient.KEY_ID, searchKey.trim())
                        }
                        toast("Ключ поиска сохранён")
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Сохранить") }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            searchBusy = true
                            searchResult = runCatching {
                                SearchClient.search(context, "Kotlin 2.2 release", 3)
                            }.getOrElse { "Ошибка: ${it.message}" }
                            searchBusy = false
                        }
                    },
                    enabled = !searchBusy,
                    modifier = Modifier.weight(1f),
                ) { Text(if (searchBusy) "Ищу…" else "Проверить") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Использовать поиск в чате")
                    Text(
                        "Кнопка «Поиск» в чате включает запрос к Tavily.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = searchEnabled,
                    onCheckedChange = {
                        searchEnabled = it
                        AppPrefs.setSearchEnabled(context, it)
                    },
                )
            }
            if (searchResult.isNotBlank()) {
                Text(
                    searchResult.take(1200),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { AndroidUtils.openUrl(context, "https://tavily.com") }) {
                Text("Получить ключ Tavily")
            }
        }

        ToolCard("Озвучивание (TTS)", "Системный голос Android. Можно слушать ответы.") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Озвучивать ответы автоматически")
                    Text(
                        "Кнопка 🔊 есть у каждого ответа.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = ttsEnabled,
                    onCheckedChange = {
                        ttsEnabled = it
                        AppPrefs.setTtsEnabled(context, it)
                    },
                )
            }
            Text("Скорость: %.1f×".format(ttsRate))
            Slider(
                value = ttsRate,
                onValueChange = {
                    ttsRate = it
                    AppPrefs.setTtsRate(context, it)
                },
                valueRange = 0.5f..2f,
                steps = 5,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { TtsManager.speak(context, "Привет! Это DevAI Studio.") },
                    modifier = Modifier.weight(1f),
                ) { Text("Проверить голос") }
                OutlinedButton(
                    onClick = { TtsManager.stop() },
                    modifier = Modifier.weight(1f),
                ) { Text("Стоп") }
            }
        }

        ToolCard("MCP-инструменты", "Подключение внешних инструментов по MCP.") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Использовать MCP в чате")
                    Text(
                        "Серверов: ${McpStore.listServers(context).size} · " +
                            "инструментов: ${McpStore.tools(context).size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = mcpEnabled,
                    onCheckedChange = {
                        mcpEnabled = it
                        AppPrefs.setMcpEnabled(context, it)
                    },
                )
            }
            Button(
                onClick = { context.startActivity(Intent(context, McpActivity::class.java)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Build, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Настроить MCP-серверы")
            }
        }

        ToolCard("Персонажи", "Карточки с характером и системным промптом.") {
            Text(
                "Активен: " + (CharacterStore.active(context)?.name ?: "без персонажа"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { context.startActivity(Intent(context, CharactersActivity::class.java)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Открыть персонажей")
            }
        }

        ToolCard(
            "Плагины .devai",
            "Lua 5.2 в песочнице: команды и инструменты, которые вызывает ИИ.",
        ) {
            val plugins = PluginStore.list(context)
            Text(
                "Плагинов: ${plugins.size} · инструментов ИИ: " +
                    plugins.sumOf { it.tools.size },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { context.startActivity(Intent(context, PluginsActivity::class.java)) },
                    modifier = Modifier.weight(1f),
                ) { Text("Плагины") }
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(context, PluginDocsActivity::class.java))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Документация")
                }
            }
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(context, PluginDocsActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Документация по плагинам DevAI Studio")
            }
        }

        ToolCard("QR-экспорт", "Перенос настроек провайдера на другой телефон.") {
            Button(
                onClick = { context.startActivity(Intent(context, QrExportActivity::class.java)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("QR активного провайдера")
            }
        }

        Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun ToolCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}
