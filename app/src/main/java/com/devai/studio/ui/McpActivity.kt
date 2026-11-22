package com.devai.studio.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.devai.studio.ai.McpClient
import com.devai.studio.data.ApiKeyStore
import com.devai.studio.data.AppPrefs
import com.devai.studio.data.McpServer
import com.devai.studio.data.McpStore
import com.devai.studio.data.McpTool
import com.devai.studio.util.ThemeManager
import kotlinx.coroutines.launch

class McpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        ThemeManager.applyDynamicIfAvailable(this)
        setContent { DevAITheme { DevAIBackground { McpScreen(onBack = { finish() }) } } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var servers by remember { mutableStateOf(McpStore.listServers(context)) }
    var tools by remember { mutableStateOf(McpStore.tools(context)) }
    var enabled by remember { mutableStateOf(AppPrefs.mcpEnabled(context)) }
    var busy by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<McpServer?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<McpServer?>(null) }

    fun refresh() {
        servers = McpStore.listServers(context)
        tools = McpStore.tools(context)
    }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    fun refreshTools() {
        if (busy) return
        busy = true
        scope.launch {
            val all = mutableListOf<McpTool>()
            val errors = mutableListOf<String>()
            McpStore.listServers(context).filter { it.enabled }.forEach { server ->
                runCatching {
                    McpClient(server, ApiKeyStore.get(context, McpStore.tokenKey(server.id)))
                        .listTools()
                }.onSuccess { all.addAll(it) }
                    .onFailure { errors.add("${server.name}: ${it.message ?: "ошибка"}") }
            }
            McpStore.saveTools(context, all)
            tools = all
            busy = false
            toast(
                if (errors.isEmpty()) "Инструментов: ${all.size}"
                else "Инструментов: ${all.size}. Ошибки: ${errors.joinToString("; ").take(300)}"
            )
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            DevAITopBar(
                title = "MCP-инструменты",
                subtitle = "Внешние инструменты для модели",
                onBack = onBack,
                actions = {
                    DevAIActionIcon(Icons.Default.Refresh, "Обновить", enabled = !busy) {
                        refreshTools()
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                DevAICard(accent = MaterialTheme.colorScheme.primary) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Использовать MCP в чате", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Модель сможет вызывать инструменты MCP-серверов.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = enabled,
                                onCheckedChange = {
                                    enabled = it
                                    AppPrefs.setMcpEnabled(context, it)
                                },
                            )
                        }
                        Text(
                            "Работает с OpenAI-совместимыми провайдерами. Для Anthropic/Gemini/Local инструменты пропускаются.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            editing = McpServer("", "", "")
                            showEditor = true
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Сервер")
                    }
                    OutlinedButton(
                        onClick = { refreshTools() },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(Modifier.width(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Обновить инструменты")
                        }
                    }
                }
            }
            item {
                DevAISectionTitle(
                    title = "Серверы",
                    subtitle = "Подключения по Streamable HTTP",
                    icon = Icons.Default.Build,
                )
            }
            if (servers.isEmpty()) {
                item {
                    Text(
                        "Добавьте MCP-сервер: укажите URL Streamable HTTP endpoint, например https://example.com/mcp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(servers) { server ->
                DevAICard(
                    accent = if (server.enabled) MaterialTheme.colorScheme.secondary else null,
                    contentPadding = PaddingValues(14.dp),
                ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DevAIIconTile(
                                Icons.Default.Build,
                                size = 40.dp,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(server.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    server.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = server.enabled,
                                onCheckedChange = {
                                    McpStore.setEnabled(context, server.id, it)
                                    refresh()
                                },
                            )
                        }
                        Text(
                            "Инструментов: ${tools.count { it.serverId == server.id }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row {
                            TextButton(onClick = {
                                editing = server
                                showEditor = true
                            }) { Text("Изменить") }
                            TextButton(onClick = { deleting = server }) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Удалить")
                            }
                        }
                }
            }
            item {
                DevAISectionTitle(
                    title = "Инструменты",
                    subtitle = "Что доступно модели",
                    icon = Icons.AutoMirrored.Filled.List,
                )
            }
            if (tools.isEmpty()) {
                item {
                    Text(
                        "Пока нет инструментов. Нажмите «Обновить инструменты».",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(tools) { tool ->
                DevAICard(contentPadding = PaddingValues(12.dp)) {
                        Text(tool.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            tool.serverName + if (tool.description.isBlank()) "" else " · " + tool.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showEditor) {
        McpServerDialog(
            initial = editing ?: McpServer("", "", ""),
            onDismiss = { showEditor = false },
            onSave = { server, token ->
                val saved = McpStore.saveServer(context, server)
                if (token.isBlank()) ApiKeyStore.clear(context, McpStore.tokenKey(saved.id))
                else ApiKeyStore.save(context, McpStore.tokenKey(saved.id), token)
                showEditor = false
                refresh()
            },
        )
    }

    deleting?.let { server ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Удалить MCP-сервер?") },
            text = { Text("«${server.name}» и его токен будут удалены.") },
            confirmButton = {
                TextButton(onClick = {
                    McpStore.deleteServer(context, server.id)
                    ApiKeyStore.clear(context, McpStore.tokenKey(server.id))
                    deleting = null
                    refresh()
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun McpServerDialog(
    initial: McpServer,
    onDismiss: () -> Unit,
    onSave: (McpServer, String) -> Unit,
) {
    val context = LocalContext.current
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var url by remember(initial.id) { mutableStateOf(initial.url) }
    var token by remember(initial.id) {
        mutableStateOf(ApiKeyStore.get(context, McpStore.tokenKey(initial.id)).orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "Новый MCP-сервер" else "MCP-сервер") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL (Streamable HTTP)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Bearer-токен (если нужен)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        initial.copy(name = name.trim(), url = url.trim()),
                        token.trim(),
                    )
                },
                enabled = name.isNotBlank() && url.isNotBlank(),
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
