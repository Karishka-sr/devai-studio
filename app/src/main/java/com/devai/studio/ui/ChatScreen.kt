package com.devai.studio.ui

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devai.studio.R
import com.devai.studio.ai.AgentTool
import com.devai.studio.ai.AiClientFactory
import com.devai.studio.ai.AiFormat
import com.devai.studio.ai.AiMessage
import com.devai.studio.ai.McpClient
import com.devai.studio.ai.ToolAgent
import com.devai.studio.data.ApiKeyStore
import com.devai.studio.data.AppPrefs
import com.devai.studio.data.Attachment
import com.devai.studio.data.CharacterStore
import com.devai.studio.data.ChatMode
import com.devai.studio.data.ChatSession
import com.devai.studio.data.ChatSessionsStore
import com.devai.studio.data.LocalModelStore
import com.devai.studio.data.McpStore
import com.devai.studio.data.Plugin
import com.devai.studio.data.PluginStore
import com.devai.studio.data.PremiumFeature
import com.devai.studio.data.PremiumStore
import com.devai.studio.data.SearchClient
import com.devai.studio.util.AiSupport
import com.devai.studio.util.PremiumDialogs
import com.devai.studio.util.TtsManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(mode: ChatMode = ChatMode.CHAT) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val handler = remember { Handler(Looper.getMainLooper()) }
    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val historyLimit = AppPrefs.historyLimit(context)
    val chatFontSize = AppPrefs.chatFontSize(context).sp
    val codeFontSize = AppPrefs.codeFontSize(context).sp
    val codeWrap = AppPrefs.codeWrap(context)
    val sendOnEnter = AppPrefs.sendOnEnter(context)
    val autoScroll = AppPrefs.autoScroll(context)
    val chatFontFamily = when (AppPrefs.chatFontFamily(context)) {
        "serif" -> FontFamily.Serif
        "mono" -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }
    val bubbleWidth = AppPrefs.bubbleWidth(context).dp
    val showAvatars = AppPrefs.showAvatars(context)
    val compactChat = AppPrefs.compactChat(context)
    val hapticEnabled = AppPrefs.hapticFeedback(context)
    val haptic = LocalHapticFeedback.current

    var sessionId by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<AiMessage>>(emptyList()) }
    var input by rememberSaveable { mutableStateOf("") }
    var streaming by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var modelLabel by remember { mutableStateOf("") }
    val cancelled = remember { AtomicBoolean(false) }

    var showSessions by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var sessions by remember { mutableStateOf<List<ChatSession>>(emptyList()) }
    var searchEnabled by remember { mutableStateOf(AppPrefs.searchEnabled(context)) }
    var mcpEnabled by remember { mutableStateOf(AppPrefs.mcpEnabled(context)) }
    var attachments by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var showPlugins by remember { mutableStateOf(false) }
    var plugins by remember { mutableStateOf<List<Plugin>>(emptyList()) }
    var characterName by remember {
        mutableStateOf(CharacterStore.active(context)?.name ?: "Без персонажа")
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { withContext(Dispatchers.IO) { readAttachment(context, uri) } }
                    .onSuccess { attachment ->
                        if (attachments.size >= 3) {
                            toast("Можно прикрепить не больше 3 файлов")
                        } else {
                            attachments = attachments + attachment
                        }
                    }
                    .onFailure { toast("Файл: ${it.message ?: "не удалось прочитать"}") }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                characterName = CharacterStore.active(context)?.name ?: "Без персонажа"
                searchEnabled = AppPrefs.searchEnabled(context)
                mcpEnabled = AppPrefs.mcpEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun currentModelLabel(): String {
        val p = AiSupport.provider(context)
        return when {
            p == null -> "Провайдер не выбран"
            p.format == AiFormat.LOCAL ->
                p.name + " · " + LocalModelStore.activeName(context).ifBlank { "модель не выбрана" }
            else -> p.name + " · " + AiSupport.model(context, p)
        }
    }

    fun persist() {
        if (sessionId.isNotBlank()) {
            ChatSessionsStore.updateMessages(context, sessionId, messages)
        }
    }

    fun loadSession(session: ChatSession) {
        sessionId = session.id
        ChatSessionsStore.setCurrent(context, session.id)
        messages = session.messages
        modelLabel = currentModelLabel()
    }

    fun newChat() {
        loadSession(ChatSessionsStore.create(context))
        val greeting = CharacterStore.active(context)?.greeting.orEmpty()
        if (greeting.isNotBlank()) {
            messages = listOf(AiMessage("assistant", greeting))
            persist()
        }
    }

    fun openSessions() {
        persist()
        sessions = ChatSessionsStore.list(context)
        showSessions = true
    }

    fun deleteCurrentSession() {
        ChatSessionsStore.delete(context, sessionId)
        loadSession(ChatSessionsStore.getOrCreateCurrent(context))
        showSessions = false
        showDeleteConfirm = false
    }

    fun stop() {
        cancelled.set(true)
        job?.cancel()
        streaming = false
    }

    fun openPlugins() {
        if (!PremiumStore.hasFeature(context, PremiumFeature.PLUGINS)) {
            PremiumDialogs.show(context, "Плагины", PremiumFeature.PLUGINS.minPlan)
            return
        }
        plugins = PluginStore.list(context)
        showPlugins = true
    }

    fun requireFeature(feature: PremiumFeature, action: () -> Unit) {
        if (PremiumStore.hasFeature(context, feature)) action()
        else PremiumDialogs.show(context, feature.title, feature.minPlan)
    }

    fun send(textOverride: String? = null) {
        val typed = (textOverride ?: input).trim()
        val files = attachments
        if (typed.isEmpty() && files.isEmpty()) return
        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val cfg = try {
            AiSupport.config(context)
        } catch (e: IllegalStateException) {
            toast(e.message ?: context.getString(R.string.chat_no_key))
            return
        }

        val userContent = buildString {
            if (typed.isNotBlank()) append(typed)
            if (files.isNotEmpty()) {
                if (typed.isNotBlank()) append("\n\n")
                files.forEachIndexed { index, file ->
                    append("Файл «").append(file.name).append("»:\n```\n")
                    append(file.content)
                    append("\n```")
                    if (index != files.lastIndex) append("\n\n")
                }
            }
        }.trim()
        if (userContent.isEmpty()) return

        val searchOn = searchEnabled
        val useMcp = mcpEnabled
        input = ""
        attachments = emptyList()
        val snapshot = messages + AiMessage("user", userContent) + AiMessage("assistant", "")
        messages = snapshot
        val assistantIndex = snapshot.lastIndex
        val finished = AtomicBoolean(false)
        cancelled.set(false)
        streaming = true

        job = scope.launch {
            val sb = StringBuilder()
            var lastUi = 0L
            try {
                val client = AiClientFactory.create(cfg, context)

                var searchBlock = ""
                if (searchOn && typed.isNotBlank()) {
                    runCatching { SearchClient.search(context, typed, 5) }
                        .onSuccess { searchBlock = it.take(6000) }
                        .onFailure { toast("Поиск: ${it.message ?: "ошибка"}") }
                }

                val apiMessages = ArrayList<AiMessage>()
                val system = buildString {
                    CharacterStore.active(context)?.let {
                        append(it.systemText()).append("\n\n")
                    }
                    append(AppPrefs.systemPromptForMode(context, mode))
                    if (searchBlock.isNotBlank()) {
                        append("\n\nАктуальные результаты поиска (веб, GitHub, Stack Overflow, Wikipedia, DuckDuckGo, Hacker News; используй, если это полезно):\n")
                        append(searchBlock)
                    }
                }
                apiMessages.add(AiMessage("system", system))
                val history = snapshot.take(assistantIndex)
                apiMessages.addAll(
                    if (historyLimit > 0) history.takeLast(historyLimit) else history
                )

                val onDelta: (String) -> Unit = { delta ->
                    synchronized(sb) { sb.append(delta) }
                    if (!cancelled.get() && !finished.get()) {
                        val now = System.currentTimeMillis()
                        if (now - lastUi > 70) {
                            lastUi = now
                            val partial = synchronized(sb) { sb.toString() }
                            handler.post {
                                if (!cancelled.get() && !finished.get()) {
                                    messages = messages.toMutableList().also {
                                        if (assistantIndex in it.indices) {
                                            it[assistantIndex] = AiMessage("assistant", partial)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val agentTools = buildList {
                    if (useMcp) {
                        McpStore.tools(context).forEach { tool ->
                            add(
                                AgentTool(
                                    functionName = tool.functionName,
                                    description = tool.description.ifBlank { tool.name },
                                    inputSchema = tool.inputSchema,
                                ) { args ->
                                    val server = McpStore.findServer(context, tool.serverId)
                                    if (server == null) {
                                        "MCP-сервер не найден: ${tool.serverName}"
                                    } else {
                                        McpClient(
                                            server,
                                            ApiKeyStore.get(
                                                context,
                                                McpStore.tokenKey(server.id),
                                            ),
                                        ).callTool(tool.name, args)
                                    }
                                }
                            )
                        }
                    }
                    PluginStore.toolBindings(context).forEach { binding ->
                        add(
                            AgentTool(
                                functionName = binding.functionName,
                                description = binding.tool.description
                                    .ifBlank { binding.plugin.name + ": " + binding.tool.name },
                                inputSchema = binding.tool.parameters,
                            ) { args ->
                                withContext(Dispatchers.IO) {
                                    PluginStore.callTool(binding, args)
                                }
                            }
                        )
                    }
                }
                val full = if (agentTools.isNotEmpty() && cfg.format == AiFormat.OPENAI_COMPAT) {
                    ToolAgent.chat(cfg, apiMessages, agentTools, onDelta)
                } else {
                    client.chat(apiMessages, onDelta)
                }

                if (!cancelled.get()) {
                    finished.set(true)
                    val finalText = full.ifBlank { synchronized(sb) { sb.toString() } }
                    messages = messages.toMutableList().also {
                        if (assistantIndex in it.indices) {
                            it[assistantIndex] = AiMessage("assistant", finalText)
                        }
                    }
                    persist()
                    if (AppPrefs.ttsEnabled(context) && finalText.isNotBlank()) {
                        TtsManager.speak(context, finalText)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!cancelled.get()) {
                    finished.set(true)
                    messages = messages.toMutableList().also {
                        if (assistantIndex in it.indices) {
                            it[assistantIndex] = AiMessage(
                                "assistant",
                                context.getString(R.string.chat_error, e.message ?: e.javaClass.simpleName),
                            )
                        }
                    }
                    persist()
                }
            } finally {
                streaming = false
                job = null
            }
        }
    }

    fun regenerate() {
        if (streaming) return
        val lastUser = messages.indexOfLast { it.role == "user" }
        if (lastUser < 0) return
        val text = messages[lastUser].content
        messages = messages.take(lastUser)
        persist()
        input = text
        send()
    }

    fun exportChat() {
        if (!PremiumStore.hasFeature(context, PremiumFeature.CHAT_EXPORT)) {
            PremiumDialogs.show(
                context,
                context.getString(R.string.premium_feature_export),
                PremiumFeature.CHAT_EXPORT.minPlan,
            )
            return
        }
        val visible = messages.filter { it.content.isNotBlank() }
        if (visible.isEmpty()) {
            toast(context.getString(R.string.premium_export_empty))
            return
        }
        val sb = StringBuilder("# DevAI Studio — чат\n\n")
        visible.forEach { m ->
            sb.append(if (m.role == "user") "## Пользователь\n\n" else "## DevAI\n\n")
            sb.append(m.content).append("\n\n")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/markdown"
            putExtra(Intent.EXTRA_SUBJECT, "DevAI Studio — чат")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.premium_export_chooser)))
    }

    LaunchedEffect(Unit) {
        val session = ChatSessionsStore.getOrCreateCurrent(context)
        sessionId = session.id
        messages = session.messages
        modelLabel = currentModelLabel()
        val activeCharacter = CharacterStore.active(context)
        characterName = activeCharacter?.name ?: "Без персонажа"
        val greeting = activeCharacter?.greeting.orEmpty()
        if (messages.isEmpty() && greeting.isNotBlank()) {
            messages = listOf(AiMessage("assistant", greeting))
            persist()
        }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (autoScroll && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(Modifier.fillMaxSize()) {
        DevAITopBar(
            title = "DevAI",
            subtitle = "Studio · " + mode.title + " · " + modelLabel,
            actions = {
                DevAIActionIcon(Icons.AutoMirrored.Filled.List, "Сессии") { openSessions() }
                DevAIActionIcon(Icons.Default.Add, "Новый чат") { newChat() }
                DevAIActionIcon(Icons.Default.Share, "Экспорт") { exportChat() }
                DevAIActionIcon(Icons.Default.Refresh, "Заново", enabled = !streaming) { regenerate() }
                DevAIActionIcon(Icons.Default.Person, "Персонажи") {
                    requireFeature(PremiumFeature.CHARACTERS) {
                        context.startActivity(Intent(context, CharactersActivity::class.java))
                    }
                }
            },
        )

        if (messages.isEmpty() && !streaming) {
            EmptyChat(
                modifier = Modifier.weight(1f),
                mode = mode,
                onSuggestion = { input = it },
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (compactChat) 4.dp else 10.dp),
                contentPadding = PaddingValues(
                    horizontal = if (compactChat) 4.dp else 8.dp,
                    vertical = if (compactChat) 4.dp else 8.dp,
                ),
            ) {
                items(messages.size) { index ->
                    val message = messages[index]
                    if (!(streaming && index == messages.lastIndex && message.content.isBlank())) {
                        MessageBubble(
                            message = message,
                            fontSize = chatFontSize,
                            codeFontSize = codeFontSize,
                            codeWrap = codeWrap,
                            bubbleWidth = bubbleWidth,
                            showAvatars = showAvatars,
                            fontFamily = chatFontFamily,
                            onSpeak = { TtsManager.speak(context, it) },
                        )
                    }
                }
                if (streaming) {
                    item { TypingIndicator() }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = searchEnabled,
                onClick = {
                    requireFeature(PremiumFeature.SEARCH) {
                        searchEnabled = !searchEnabled
                        AppPrefs.setSearchEnabled(context, searchEnabled)
                    }
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("Поиск") },
            )
            FilterChip(
                selected = mcpEnabled,
                onClick = {
                    requireFeature(PremiumFeature.MCP) {
                        mcpEnabled = !mcpEnabled
                        AppPrefs.setMcpEnabled(context, mcpEnabled)
                        if (mcpEnabled && McpStore.tools(context).isEmpty()) {
                            toast("MCP: добавьте сервер в «Инструменты».")
                        }
                    }
                },
                leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) },
                label = { Text("MCP") },
            )
            AssistChip(
                onClick = { openPlugins() },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                label = { Text("Плагины") },
            )
            AssistChip(
                onClick = {
                    requireFeature(PremiumFeature.CHARACTERS) {
                        context.startActivity(Intent(context, CharactersActivity::class.java))
                    }
                },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                label = { Text(characterName) },
            )
            attachments.forEach { attachment ->
                AssistChip(
                    onClick = { attachments = attachments - attachment },
                    leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                    label = { Text(attachment.name) },
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
            ),
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.padding(5.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                IconButton(
                    onClick = {
                        pickFile.launch(
                            arrayOf(
                                "text/*",
                                "application/json",
                                "application/xml",
                                "application/octet-stream",
                            )
                        )
                    },
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Прикрепить файл",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Сообщение…") },
                    maxLines = 4,
                    shape = RoundedCornerShape(22.dp),
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (sendOnEnter) ImeAction.Send else ImeAction.Default
                    ),
                    keyboardActions = if (sendOnEnter) {
                        KeyboardActions(onSend = { if (!streaming) send() })
                    } else {
                        KeyboardActions()
                    },
                )
                Spacer(Modifier.width(6.dp))
                val canSend = streaming || input.isNotBlank() || attachments.isNotEmpty()
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend) {
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary,
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                )
                            }
                        )
                        .clickable(enabled = canSend) { if (streaming) stop() else send() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (streaming) Icons.Default.Close else Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (streaming) "Стоп" else "Отправить",
                        tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
        }
    }

    if (showSessions) {
        ModalBottomSheet(
            onDismissRequest = { showSessions = false },
            sheetState = sheetState,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    "Сессии",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(sessions) { session ->
                        ListItem(
                            headlineContent = { Text(session.title, maxLines = 1) },
                            leadingContent = { Icon(Icons.Default.Email, contentDescription = null) },
                            modifier = Modifier.clickable {
                                loadSession(session)
                                showSessions = false
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        exportChat()
                        showSessions = false
                    },
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Экспорт текущего чата")
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            newChat()
                            showSessions = false
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Новая")
                    }
                    OutlinedButton(
                        onClick = {
                            if (AppPrefs.confirmDelete(context)) {
                                showDeleteConfirm = true
                            } else {
                                deleteCurrentSession()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Удалить")
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Удалить чат?") },
            text = { Text("Текущая сессия и все её сообщения будут удалены.") },
            confirmButton = {
                TextButton(onClick = { deleteCurrentSession() }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена") }
            },
        )
    }

    if (showPlugins) {
        ModalBottomSheet(
            onDismissRequest = { showPlugins = false },
            sheetState = sheetState,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    "Плагины .devai",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Выберите плагин — его промпт подставится в сообщение.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                if (plugins.isEmpty()) {
                    Text(
                        "Плагинов нет. Добавьте .devai в «Инструменты → Плагины».",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val actions = plugins.flatMap { plugin ->
                        if (plugin.commands.isNotEmpty()) {
                            plugin.commands.map { command -> plugin to command }
                        } else {
                            listOf(plugin to null)
                        }
                    }
                    LazyColumn(Modifier.heightIn(max = 420.dp)) {
                        items(actions) { (plugin, command) ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        plugin.name + if (command != null) " " + command.name else "",
                                        maxLines = 1,
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        command?.description?.ifBlank { plugin.description }
                                            ?: plugin.description.ifBlank { plugin.command },
                                        maxLines = 2,
                                    )
                                },
                                leadingContent = {
                                    Icon(Icons.Default.Build, contentDescription = null)
                                },
                                modifier = Modifier.clickable {
                                    val base = input.trim()
                                    val rendered = PluginStore.runCommand(plugin, command, base)
                                    showPlugins = false
                                    if (base.isNotBlank()) {
                                        input = rendered
                                        send(rendered)
                                    } else {
                                        input = rendered
                                    }
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showPlugins = false
                            context.startActivity(Intent(context, PluginsActivity::class.java))
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Открыть плагины") }
                    OutlinedButton(
                        onClick = {
                            showPlugins = false
                            context.startActivity(Intent(context, PluginDocsActivity::class.java))
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Документация") }
                }
            }
        }
    }
}

@Composable
private fun EmptyChat(
    modifier: Modifier = Modifier,
    mode: ChatMode,
    onSuggestion: (String) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Face,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "Чем помочь сегодня?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (mode == ChatMode.DEV) {
                "Спросите про код, ошибки, Android, Kotlin или Compose."
            } else {
                "Спросите что угодно — объяснения, тексты, идеи."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (if (mode == ChatMode.DEV) {
                listOf(
                    "Напиши код на Kotlin",
                    "Объясни ошибку",
                    "Сделай UI на Compose",
                )
            } else {
                listOf(
                    "Объясни простыми словами",
                    "Помоги с текстом",
                    "Придумай идею",
                )
            }).forEach { text ->
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    ),
                    modifier = Modifier.clickable { onSuggestion(text) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Create,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(text, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            "DevAI печатает…",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MessageBubble(
    message: AiMessage,
    fontSize: TextUnit,
    codeFontSize: TextUnit,
    codeWrap: Boolean,
    bubbleWidth: Dp,
    showAvatars: Boolean,
    fontFamily: FontFamily,
    onSpeak: (String) -> Unit,
) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!isUser && showAvatars) {
            Avatar(isUser = false)
            Spacer(Modifier.width(8.dp))
        }
        val bubbleShape = RoundedCornerShape(
            topStart = if (isUser) 22.dp else 6.dp,
            topEnd = if (isUser) 6.dp else 22.dp,
            bottomStart = 22.dp,
            bottomEnd = 22.dp,
        )
        Surface(
            shape = bubbleShape,
            color = if (isUser) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            },
            contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            border = if (isUser) null else androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
            ),
            modifier = Modifier
                .widthIn(max = bubbleWidth)
                .then(
                    if (isUser) {
                        Modifier.background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                                )
                            ),
                            bubbleShape,
                        )
                    } else {
                        Modifier
                    }
                ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (isUser) {
                    SelectionContainer {
                        Text(
                            text = message.content,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = fontSize,
                            fontFamily = fontFamily,
                        )
                    }
                } else if (message.content.isBlank()) {
                    Text("…", color = MaterialTheme.colorScheme.onSurface)
                } else {
                    val segments = remember(message.content) { parseSegments(message.content) }
                    if (segments.isEmpty()) {
                        Text(
                            message.content,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = fontSize,
                            fontFamily = fontFamily,
                        )
                    } else {
                        segments.forEach { segment ->
                            if (segment.code) {
                                CodeBlock(
                                    segment.lang,
                                    segment.text,
                                    fontSize = codeFontSize,
                                    wrap = codeWrap,
                                )
                            } else {
                                SelectionContainer {
                                    Text(
                                        text = segment.text,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = fontSize,
                                        fontFamily = fontFamily,
                                    )
                                }
                            }
                        }
                    }
                }
                if (!isUser && message.content.isNotBlank()) {
                    TextButton(onClick = { onSpeak(message.content) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Озвучить")
                    }
                }
            }
        }
        if (isUser && showAvatars) {
            Spacer(Modifier.width(8.dp))
            Avatar(isUser = true)
        }
    }
}

@Composable
private fun Avatar(isUser: Boolean) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    if (isUser) {
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.tertiary,
                        )
                    }
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isUser) Icons.Default.Person else Icons.Default.Face,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun CodeBlock(lang: String, code: String, fontSize: TextUnit, wrap: Boolean) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    Surface(
        color = Color(0xFF1E1E1E),
        contentColor = Color(0xFFE6E6E6),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (lang.isBlank()) "code" else lang,
                    color = Color(0xFF9CDCFE),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    },
                ) {
                    if (copied) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF7FD1FF),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        if (copied) "Скопировано" else "Копировать",
                        color = Color(0xFF7FD1FF),
                        fontSize = 11.sp,
                    )
                }
            }
            Text(
                text = code,
                color = Color(0xFFE6E6E6),
                fontSize = fontSize,
                fontFamily = FontFamily.Monospace,
                softWrap = wrap,
                modifier = if (wrap) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.horizontalScroll(rememberScrollState())
                },
            )
        }
    }
}

private fun readAttachment(context: android.content.Context, uri: android.net.Uri): Attachment {
    val name = context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    } ?: uri.lastPathSegment ?: "file.txt"

    val extension = name.substringAfterLast('.', "").lowercase()
    val allowed = setOf(
        "txt", "md", "json", "kt", "kts", "java", "xml", "yaml", "yml",
        "csv", "log", "html", "css", "js", "ts", "py", "gradle",
        "properties", "sql", "sh", "toml", "ini", "devai",
    )
    if (extension !in allowed) {
        error("формат .$extension не поддержан")
    }

    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: error("не удалось открыть файл")
    if (bytes.size > 64 * 1024) error("файл больше 64 КБ")
    if (bytes.any { it == 0.toByte() }) error("это бинарный файл")

    val text = bytes.toString(Charsets.UTF_8)
    if (text.count { it == '\uFFFD' } > 5) error("файл не похож на текст")
    if (text.isBlank()) error("файл пустой")
    return Attachment(name, text)
}

private data class Segment(val code: Boolean, val lang: String, val text: String)

private fun parseSegments(content: String): List<Segment> {
    val out = mutableListOf<Segment>()
    content.split("```").forEachIndexed { index, part ->
        if (index % 2 == 0) {
            if (part.isNotBlank()) out.add(Segment(false, "", part.trim('\n')))
        } else {
            val nl = part.indexOf('\n')
            val lang = if (nl >= 0) part.substring(0, nl).trim() else ""
            val code = if (nl >= 0) part.substring(nl + 1) else part
            out.add(Segment(true, lang, code.trimEnd('\n')))
        }
    }
    return out
}
