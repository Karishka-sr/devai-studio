package com.devai.studio.ui

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
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
import com.devai.studio.ai.DevAICore
import com.devai.studio.ai.AiQuality
import com.devai.studio.ai.McpClient
import com.devai.studio.ai.ToolAgent
import com.devai.studio.data.ApiKeyStore
import com.devai.studio.data.AppPrefs
import com.devai.studio.data.Attachment
import com.devai.studio.data.CharacterStore
import com.devai.studio.data.ChatMode
import com.devai.studio.data.ChatSession
import com.devai.studio.data.ChatSessionsStore
import com.devai.studio.data.CodeLanguage
import com.devai.studio.data.CodeLanguages
import com.devai.studio.data.CodeSyntax
import com.devai.studio.data.LocalModelStore
import com.devai.studio.data.McpStore
import com.devai.studio.data.MemoryStore
import com.devai.studio.data.Plugin
import com.devai.studio.data.PluginStore
import com.devai.studio.data.PremiumFeature
import com.devai.studio.data.PremiumStore
import com.devai.studio.data.SearchClient
import com.devai.studio.data.StyleLayout
import com.devai.studio.util.AiSupport
import com.devai.studio.util.PremiumDialogs
import com.devai.studio.util.TtsManager
import com.qwenz.qwel.MainActivity as QwelActivity
import dev.chrisbanes.haze.hazeSource
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
    val uiStyle = LocalUiStyle.current
    val layout = uiStyle.layout
    val haze = LocalHazeState.current
    val centered = layout == StyleLayout.CHATGPT || layout == StyleLayout.CLAUDE
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
    val autoWeb = mode == ChatMode.CORE || AiSupport.provider(context)?.id == "devai-core"
    var mcpEnabled by remember { mutableStateOf(AppPrefs.mcpEnabled(context)) }
    var attachments by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var showPlugins by remember { mutableStateOf(false) }
    var showAiActions by remember { mutableStateOf(false) }
    var showPromptLibrary by remember { mutableStateOf(false) }
    var showChatSearch by remember { mutableStateOf(false) }
    var showMoreActions by remember { mutableStateOf(false) }
    var incognito by rememberSaveable { mutableStateOf(false) }
    var wallpaperId by remember { mutableStateOf(AppPrefs.chatWallpaper(context)) }
    var wallpaperDim by remember { mutableFloatStateOf(AppPrefs.chatWallpaperDim(context)) }
    var codeLanguageId by remember { mutableStateOf(AppPrefs.codeLanguage(context)) }
    val codeLanguage = CodeLanguages.byId(codeLanguageId) ?: CodeLanguages.default
    var plugins by remember { mutableStateOf<List<Plugin>>(emptyList()) }
    val clipboard = LocalClipboardManager.current
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

    val voiceInput = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                input = (input.trim() + " " + spoken).trim()
            }
        }
    }

    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажите сообщение")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        runCatching { voiceInput.launch(intent) }
            .onFailure { toast("Голосовой ввод недоступен на этом устройстве") }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                characterName = CharacterStore.active(context)?.name ?: "Без персонажа"
                searchEnabled = AppPrefs.searchEnabled(context)
                mcpEnabled = AppPrefs.mcpEnabled(context)
                wallpaperId = AppPrefs.chatWallpaper(context)
                wallpaperDim = AppPrefs.chatWallpaperDim(context)
                codeLanguageId = AppPrefs.codeLanguage(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun currentModelLabel(): String {
        if (mode == ChatMode.CORE) return "Core v${DevAICore.VERSION}"
        val p = AiSupport.provider(context)
        return when {
            p == null -> "Провайдер не выбран"
            p.format == AiFormat.LOCAL ->
                p.name + " · " + LocalModelStore.activeName(context).ifBlank { "модель не выбрана" }
            else -> p.name + " · " + AiSupport.model(context, p)
        }
    }

    fun persist() {
        if (!incognito && sessionId.isNotBlank()) {
            ChatSessionsStore.updateMessages(context, sessionId, messages)
        }
    }

    fun loadSession(session: ChatSession) {
        incognito = false
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

    fun newTempChat() {
        cancelled.set(true)
        job?.cancel()
        streaming = false
        incognito = true
        sessionId = ""
        messages = emptyList()
        modelLabel = currentModelLabel()
        val greeting = CharacterStore.active(context)?.greeting.orEmpty()
        if (greeting.isNotBlank()) {
            messages = listOf(AiMessage("assistant", greeting))
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

        if (mode == ChatMode.CORE) {
            val local = DevAICore.localReply(context, userContent)
            if (local != null) {
                input = ""
                attachments = emptyList()
                messages = messages + AiMessage("user", userContent) + AiMessage("assistant", local)
                persist()
                if (AppPrefs.ttsEnabled(context)) TtsManager.speak(context, local)
                return
            }
        }

        val cfg = try {
            AiSupport.config(context)
        } catch (e: IllegalStateException) {
            if (mode == ChatMode.CORE) {
                input = ""
                attachments = emptyList()
                val reply = DevAICore.offlineReply(context, userContent)
                messages = messages + AiMessage("user", userContent) + AiMessage("assistant", reply)
                persist()
                if (AppPrefs.ttsEnabled(context)) TtsManager.speak(context, reply)
                return
            }
            toast(e.message ?: context.getString(R.string.chat_no_key))
            return
        }

        val autoWeb = mode == ChatMode.CORE || AiSupport.provider(context)?.id == "devai-core"
        val searchOn = (searchEnabled || autoWeb) && SearchClient.isSearchWorthy(typed)
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
                    if (autoWeb) {
                        messages = messages.toMutableList().also {
                            if (assistantIndex in it.indices) {
                                it[assistantIndex] = AiMessage("assistant", "Ищу в интернете…")
                            }
                        }
                    }
                    runCatching { SearchClient.search(context, typed.take(300), 6) }
                        .onSuccess { searchBlock = it.take(8000) }
                        .onFailure { toast("Поиск: ${it.message ?: "ошибка"}") }
                }

                val apiMessages = ArrayList<AiMessage>()
                val system = buildString {
                    CharacterStore.active(context)?.let {
                        append(it.systemText()).append("\n\n")
                    }
                    append(AppPrefs.systemPromptForMode(context, mode))
                    append("\n\n").append(AiQuality.ANSWER_RULES)
                    val style = AppPrefs.responseStyle(context)
                    append(
                        "\n\nСтиль ответа: " + when (style) {
                            "concise" -> "максимально кратко, только суть и код."
                            "detailed" -> "подробно, с примерами, объяснениями и чек-листом."
                            "friendly" -> "дружелюбно и просто, без канцелярита."
                            "formal" -> "официально и структурированно."
                            "creative" -> "смело и нестандартно, предлагай несколько идей."
                            else -> "баланс краткости и полезных деталей."
                        }
                    )
                    append(
                        "\n\nЯзык ответа: " + when (AppPrefs.responseLanguage(context)) {
                            "en" -> "отвечай на английском языке."
                            "auto" -> "отвечай на языке пользователя."
                            else -> "отвечай на русском языке."
                        }
                    )
                    val now = java.text.SimpleDateFormat(
                        "d MMMM yyyy, HH:mm",
                        java.util.Locale("ru", "RU"),
                    ).format(java.util.Date())
                    append("\n\nСейчас: ").append(now)
                    append(" (").append(java.util.TimeZone.getDefault().id).append(").")
                    if (mode == ChatMode.DEV) {
                        append("\n\n").append(CodeLanguages.instruction(codeLanguage))
                    }
                    val memory = MemoryStore.promptText(context)
                    if (memory.isNotBlank()) {
                        append("\n\nЧто помнить о пользователе:\n").append(memory)
                    }
                    if (searchBlock.isNotBlank()) {
                        append("\n\nАктуальные результаты поиска (Bing, Wikipedia, GitHub, Stack Overflow, Hacker News и другие):\n")
                        append(searchBlock)
                        append("\n\n").append(AiQuality.SEARCH_RULES)
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
                    val answer = full.ifBlank { synchronized(sb) { sb.toString() } }
                    val finalText = withSearchSources(answer, searchBlock)
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
        if (incognito) return@LaunchedEffect
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

    Box(Modifier.fillMaxSize()) {
        ChatWallpaperLayer(wallpaperId, wallpaperDim)
        Column(Modifier.fillMaxSize()) {
        DevAITopBar(
            title = "DevAI",
            subtitle = "Studio · " + mode.title + (if (incognito) " · временный" else "") + " · " + modelLabel,
            actions = {
                DevAIActionIcon(Icons.AutoMirrored.Filled.List, "Сессии") { openSessions() }
                DevAIActionIcon(Icons.Default.Add, "Новый чат") { newChat() }
                DevAIActionIcon(Icons.Default.Share, "Экспорт") { exportChat() }
                Box {
                    DevAIActionIcon(Icons.Default.MoreVert, "Ещё") { showMoreActions = true }
                    DropdownMenu(
                        expanded = showMoreActions,
                        onDismissRequest = { showMoreActions = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Заново") },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                            enabled = !streaming,
                            onClick = {
                                showMoreActions = false
                                regenerate()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Персонажи") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            onClick = {
                                showMoreActions = false
                                requireFeature(PremiumFeature.CHARACTERS) {
                                    context.startActivity(Intent(context, CharactersActivity::class.java))
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Vexel") },
                            leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) },
                            onClick = {
                                showMoreActions = false
                                context.startActivity(Intent(context, QwelActivity::class.java))
                            },
                        )
                    }
                }
            },
        )

        if (messages.isEmpty() && !streaming) {
            EmptyChat(
                modifier = Modifier.weight(1f),
                mode = mode,
                language = codeLanguage,
                onSuggestion = { input = it },
            )
        } else {
            val listHorizontal = when (layout) {
                StyleLayout.CHATGPT, StyleLayout.CLAUDE -> 20.dp
                StyleLayout.TERMINAL -> 8.dp
                StyleLayout.MATERIAL_EXPRESSIVE -> 16.dp
                else -> if (compactChat) 4.dp else 8.dp
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxHeight()
                        .then(if (centered) Modifier.widthIn(max = 820.dp) else Modifier)
                        .fillMaxWidth()
                        .then(
                            if (uiStyle.isGlass && haze != null) {
                                Modifier.hazeSource(haze, zIndex = 1f)
                            } else {
                                Modifier
                            }
                        ),
                    verticalArrangement = Arrangement.spacedBy(
                        when (layout) {
                            StyleLayout.CLAUDE -> 18.dp
                            StyleLayout.CHATGPT -> 14.dp
                            StyleLayout.TERMINAL -> 2.dp
                            StyleLayout.MATERIAL_EXPRESSIVE -> 14.dp
                            else -> if (compactChat) 4.dp else 10.dp
                        }
                    ),
                    contentPadding = PaddingValues(
                        horizontal = listHorizontal,
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
                                onCopy = {
                                    clipboard.setText(AnnotatedString(it))
                                    toast("Скопировано")
                                },
                                onShare = { text ->
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, text)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(intent, "Поделиться сообщением")
                                    )
                                },
                            )
                        }
                    }
                    if (streaming) {
                        item { TypingIndicator() }
                    }
                }
            }
        }

        DevAIBlurPanel(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            shape = CircleShape,
            tint = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
            FilterChip(
                selected = searchEnabled || autoWeb,
                onClick = {
                    if (autoWeb) {
                        toast("DevAI Core ищет в интернете автоматически")
                    } else {
                        requireFeature(PremiumFeature.SEARCH) {
                            searchEnabled = !searchEnabled
                            AppPrefs.setSearchEnabled(context, searchEnabled)
                        }
                    }
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text(if (autoWeb) "Интернет · авто" else "Поиск") },
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
                onClick = {
                    context.startActivity(Intent(context, CodeLanguagesActivity::class.java))
                },
                leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) },
                label = { Text(codeLanguage.short) },
            )
            AssistChip(
                onClick = { showAiActions = true },
                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                label = { Text("AI-действия") },
            )
            AssistChip(
                onClick = { showPromptLibrary = true },
                leadingIcon = { Icon(Icons.Default.Create, contentDescription = null) },
                label = { Text("Промпты") },
            )
            AssistChip(
                onClick = { showChatSearch = true },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("Поиск") },
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
        }

        val dark = uiStyle.effectiveDark(MaterialTheme.colorScheme.background.luminance() < 0.35f)
        val glass = uiStyle.isGlass
        val composerShape = when (layout) {
            StyleLayout.CHATGPT -> RoundedCornerShape(28.dp)
            StyleLayout.CLAUDE -> RoundedCornerShape(18.dp)
            StyleLayout.MATERIAL_DESIGN -> RoundedCornerShape(4.dp)
            StyleLayout.MATERIAL_YOU -> RoundedCornerShape(28.dp)
            StyleLayout.MATERIAL_EXPRESSIVE -> RoundedCornerShape(32.dp)
            StyleLayout.GLASS -> RoundedCornerShape(26.dp)
            StyleLayout.LIQUID_GLASS -> RoundedCornerShape(32.dp)
            StyleLayout.TERMINAL -> RoundedCornerShape(0.dp)
            else -> MaterialTheme.shapes.extraLarge
        }
        val composerColor = when {
            glass && haze != null -> Color.Transparent
            layout == StyleLayout.TERMINAL -> Color.Black.copy(alpha = 0.88f)
            layout == StyleLayout.CHATGPT -> MaterialTheme.colorScheme.surface
            layout == StyleLayout.CLAUDE -> MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
            layout == StyleLayout.MATERIAL_DESIGN -> MaterialTheme.colorScheme.surface
            layout == StyleLayout.MATERIAL_YOU -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            layout == StyleLayout.MATERIAL_EXPRESSIVE -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        }
        val composerBorder = when {
            glass -> androidx.compose.foundation.BorderStroke(1.dp, uiStyle.glassBorder(dark))
            layout == StyleLayout.CLAUDE -> androidx.compose.foundation.BorderStroke(
                1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
            )
            layout == StyleLayout.MATERIAL_YOU -> androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
            layout == StyleLayout.TERMINAL -> androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            layout == StyleLayout.CHATGPT || layout == StyleLayout.MATERIAL_DESIGN ||
                layout == StyleLayout.MATERIAL_EXPRESSIVE -> null
            else -> androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            )
        }
        val composerFrosted = haze != null &&
            (glass || layout == StyleLayout.MATERIAL_EXPRESSIVE)
        Surface(
            shape = composerShape,
            color = if (composerFrosted) Color.Transparent else composerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = composerBorder,
            shadowElevation = when (layout) {
                StyleLayout.CHATGPT -> 8.dp
                StyleLayout.CLAUDE -> 0.dp
                StyleLayout.MATERIAL_DESIGN -> 4.dp
                StyleLayout.MATERIAL_EXPRESSIVE -> 0.dp
                StyleLayout.TERMINAL -> 0.dp
                else -> 8.dp
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = when (layout) {
                        StyleLayout.CHATGPT -> 16.dp
                        StyleLayout.CLAUDE -> 24.dp
                        StyleLayout.TERMINAL -> 8.dp
                        else -> 12.dp
                    },
                    vertical = 8.dp,
                )
                .then(
                    if (haze != null && (glass || layout == StyleLayout.MATERIAL_EXPRESSIVE)) {
                        Modifier.devaiFrosted(
                            haze,
                            composerShape,
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                    } else if (glass) {
                        Modifier.glassPanel(haze, uiStyle, composerShape, dark)
                    } else {
                        Modifier
                    }
                ),
        ) {
            Box {
            Row(
                modifier = Modifier.padding(if (layout == StyleLayout.CLAUDE) 8.dp else 5.dp),
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
                IconButton(onClick = { startVoiceInput() }) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Голосовой ввод",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Сообщение…") },
                    maxLines = 4,
                    shape = when (layout) {
                        StyleLayout.TERMINAL -> RoundedCornerShape(0.dp)
                        StyleLayout.MATERIAL_DESIGN -> RoundedCornerShape(8.dp)
                        else -> MaterialTheme.shapes.large
                    },
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
                val sendShape = if (layout == StyleLayout.TERMINAL) {
                    RoundedCornerShape(0.dp)
                } else {
                    CircleShape
                }
                val sendSize = if (layout == StyleLayout.MATERIAL_EXPRESSIVE) 52.dp else 48.dp
                FilledIconButton(
                    onClick = { if (streaming) stop() else send() },
                    enabled = canSend,
                    modifier = Modifier.size(sendSize),
                    shape = sendShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (streaming) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primary,
                        contentColor = if (streaming) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    ),
                ) {
                    Icon(
                        imageVector = if (streaming) Icons.Default.Close else Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (streaming) "Стоп" else "Отправить",
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
            if (uiStyle.isLiquidGlass) {
                LiquidSheen(Modifier.matchParentSize(), composerShape, 0.10f)
            }
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
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        newTempChat()
                        showSessions = false
                        toast("Временный чат: сообщения не сохраняются")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Временный чат (не сохранять)")
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

    if (showAiActions) {
        ChatAiActionsSheet(
            chatText = messages.filter { it.content.isNotBlank() }
                .joinToString("\n\n") { message ->
                    (if (message.role == "user") "Пользователь" else "DevAI") + ": " + message.content
                }
                .takeLast(12000),
            input = input,
            lastAnswer = messages.lastOrNull { it.role == "assistant" && it.content.isNotBlank() }
                ?.content.orEmpty(),
            onDismiss = { showAiActions = false },
            onRun = { prompt ->
                showAiActions = false
                send(prompt)
            },
        )
    }

    if (showPromptLibrary) {
        ChatPromptSheet(
            onDismiss = { showPromptLibrary = false },
            onUse = { prompt ->
                input = prompt
                showPromptLibrary = false
            },
        )
    }

    if (showChatSearch) {
        ChatSearchSheet(
            messages = messages,
            onDismiss = { showChatSearch = false },
            onJump = { index ->
                showChatSearch = false
                scope.launch { listState.animateScrollToItem(index) }
            },
        )
    }
}

@Composable
private fun ChatWallpaperLayer(id: String, dim: Float) {
    val wallpaper = ChatWallpapers.byId(id) ?: return
    val haze = LocalHazeState.current
    Box(
        Modifier
            .fillMaxSize()
            .then(if (haze != null) Modifier.hazeSource(haze, zIndex = 0.5f) else Modifier)
    ) {
        Image(
            painter = painterResource(wallpaper.full),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dim.coerceIn(0f, 0.85f))),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
                        )
                    )
                ),
        )
    }
}

@Composable
private fun EmptyChat(
    modifier: Modifier = Modifier,
    mode: ChatMode,
    language: CodeLanguage,
    onSuggestion: (String) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Face,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "Чем помочь сегодня?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            when (mode) {
                ChatMode.DEV ->
                    "Спросите про код, ошибки и ${language.name}. Основной язык: ${language.name}."
                ChatMode.CORE ->
                    "Свой ИИ: память, промпты и локальные команды. Напишите «что ты умеешь»."
                ChatMode.CHAT ->
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
            (when (mode) {
                ChatMode.DEV -> listOf(
                    "Напиши код на ${language.name}",
                    "Объясни ошибку",
                    "Сделай пример на ${language.name}",
                )
                ChatMode.CORE -> listOf(
                    "Что ты умеешь?",
                    "Промпт для Kotlin API",
                    "Запомни: я делаю DevAI Studio",
                    "Проверь систему",
                )
                ChatMode.CHAT -> listOf(
                    "Объясни простыми словами",
                    "Помоги с текстом",
                    "Придумай идею",
                )
            }).forEach { text ->
                AssistChip(
                    onClick = { onSuggestion(text) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Create,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                    },
                    label = { Text(text, maxLines = 1) },
                )
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
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
) {
    val isUser = message.role == "user"
    val style = LocalUiStyle.current
    val dark = style.effectiveDark(MaterialTheme.colorScheme.background.luminance() < 0.35f)
    val layout = style.layout
    when (layout) {
        StyleLayout.CHATGPT -> {
            ChatGptMessage(
                message, isUser, style, dark, fontSize, codeFontSize, codeWrap, bubbleWidth,
                fontFamily, onSpeak, onCopy, onShare,
            )
            return
        }
        StyleLayout.CLAUDE -> {
            ClaudeMessage(
                message, isUser, style, dark, fontSize, codeFontSize, codeWrap, bubbleWidth,
                fontFamily, onSpeak, onCopy, onShare,
            )
            return
        }
        StyleLayout.TERMINAL -> {
            TerminalMessage(
                message, isUser, style, dark, fontSize, codeFontSize, codeWrap,
                onSpeak, onCopy, onShare,
            )
            return
        }
        StyleLayout.MATERIAL_DESIGN,
        StyleLayout.MATERIAL_YOU,
        StyleLayout.MATERIAL_EXPRESSIVE -> {
            MaterialMessage(
                message, isUser, style, dark, fontSize, codeFontSize, codeWrap, bubbleWidth,
                showAvatars, fontFamily, onSpeak, onCopy, onShare,
            )
            return
        }
        else -> Unit
    }
    val userColor = style.userBubbleColor(dark)
    val onUser = style.onUserBubbleColor(dark)
    val aiColor = style.aiBubbleColor(dark)
    val onAi = style.onAiBubbleColor(dark)
    val transparentAi = style.bubble == com.devai.studio.data.StyleBubble.MINIMAL ||
        style.bubble == com.devai.studio.data.StyleBubble.FLAT
    val bubbleShape = when (style.bubble) {
        com.devai.studio.data.StyleBubble.ROUNDED -> RoundedCornerShape(
            topStart = if (isUser) 22.dp else 6.dp,
            topEnd = if (isUser) 6.dp else 22.dp,
            bottomStart = 22.dp,
            bottomEnd = 22.dp,
        )
        com.devai.studio.data.StyleBubble.BUBBLE -> RoundedCornerShape(
            topStart = if (isUser) 20.dp else 4.dp,
            topEnd = if (isUser) 4.dp else 20.dp,
            bottomStart = 20.dp,
            bottomEnd = 20.dp,
        )
        com.devai.studio.data.StyleBubble.SOFT -> RoundedCornerShape(style.corner.dp)
        com.devai.studio.data.StyleBubble.SHARP -> RoundedCornerShape(4.dp)
        com.devai.studio.data.StyleBubble.TERMINAL,
        com.devai.studio.data.StyleBubble.BORDER,
        com.devai.studio.data.StyleBubble.FLAT -> RoundedCornerShape(0.dp)
        com.devai.studio.data.StyleBubble.MINIMAL -> RoundedCornerShape(12.dp)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!isUser && showAvatars) {
            Avatar(isUser = false)
            Spacer(Modifier.width(8.dp))
        }
        val glass = style.isGlass
        Surface(
            shape = bubbleShape,
            color = when {
                glass -> if (isUser) userColor.copy(alpha = 0.62f) else aiColor.copy(alpha = 0.46f)
                isUser || transparentAi -> Color.Transparent
                else -> aiColor
            },
            contentColor = if (isUser) onUser else onAi,
            border = when {
                glass -> androidx.compose.foundation.BorderStroke(1.dp, style.glassBorder(dark))
                isUser -> null
                style.bubble == com.devai.studio.data.StyleBubble.MINIMAL ||
                    style.bubble == com.devai.studio.data.StyleBubble.FLAT -> null
                style.bubble == com.devai.studio.data.StyleBubble.BORDER ||
                    style.bubble == com.devai.studio.data.StyleBubble.TERMINAL -> androidx.compose.foundation.BorderStroke(
                    if (style.bubble == com.devai.studio.data.StyleBubble.BORDER) 2.dp else 1.dp,
                    if (style.bubble == com.devai.studio.data.StyleBubble.TERMINAL)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.outline,
                )
                else -> androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                )
            },
            modifier = Modifier
                .widthIn(max = bubbleWidth)
                .then(
                    if (isUser) {
                        if (style.id == com.devai.studio.data.UiStyleCatalog.CLASSIC_ID) {
                            Modifier.background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                                    )
                                ),
                                bubbleShape,
                            )
                        } else if (!glass) {
                            Modifier.background(userColor, bubbleShape)
                        } else {
                            Modifier
                        }
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
                            color = onUser,
                            fontSize = fontSize,
                            fontFamily = fontFamily,
                        )
                    }
                } else if (message.content.isBlank()) {
                    Text("…", color = onAi)
                } else {
                    val segments = remember(message.content) { parseSegments(message.content) }
                    if (segments.isEmpty()) {
                        Text(
                            message.content,
                            color = onAi,
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
                                        color = onAi,
                                        fontSize = fontSize,
                                        fontFamily = fontFamily,
                                    )
                                }
                            }
                        }
                    }
                }
                if (message.content.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = { onCopy(message.content) },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                Icons.Default.Create,
                                contentDescription = "Копировать",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        IconButton(
                            onClick = { onShare(message.content) },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Поделиться",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        if (!isUser) {
                            IconButton(
                                onClick = { onSpeak(message.content) },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Озвучить",
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
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
private fun MessageBody(
    message: AiMessage,
    color: Color,
    fontSize: TextUnit,
    codeFontSize: TextUnit,
    codeWrap: Boolean,
    fontFamily: FontFamily,
) {
    if (message.content.isBlank()) {
        Text("…", color = color)
        return
    }
    val segments = remember(message.content) { parseSegments(message.content) }
    if (segments.isEmpty()) {
        SelectionContainer {
            Text(message.content, color = color, fontSize = fontSize, fontFamily = fontFamily)
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            segments.forEach { segment ->
                if (segment.code) {
                    CodeBlock(segment.lang, segment.text, fontSize = codeFontSize, wrap = codeWrap)
                } else {
                    SelectionContainer {
                        Text(segment.text, color = color, fontSize = fontSize, fontFamily = fontFamily)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageActions(
    content: String,
    isUser: Boolean,
    tint: Color,
    onSpeak: (String) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
) {
    if (content.isBlank()) return
    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        IconButton(onClick = { onCopy(content) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Create, contentDescription = "Копировать", tint = tint, modifier = Modifier.size(15.dp))
        }
        IconButton(onClick = { onShare(content) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Share, contentDescription = "Поделиться", tint = tint, modifier = Modifier.size(15.dp))
        }
        if (!isUser) {
            IconButton(onClick = { onSpeak(content) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Озвучить", tint = tint, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun ChatGptMessage(
    message: AiMessage,
    isUser: Boolean,
    style: com.devai.studio.data.UiStyle,
    dark: Boolean,
    fontSize: TextUnit,
    codeFontSize: TextUnit,
    codeWrap: Boolean,
    bubbleWidth: Dp,
    fontFamily: FontFamily,
    onSpeak: (String) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
) {
    val userColor = style.userBubbleColor(dark)
    val onUser = style.onUserBubbleColor(dark)
    val onAi = style.onAiBubbleColor(dark)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (isUser) {
            Surface(
                modifier = Modifier.widthIn(max = bubbleWidth),
                shape = RoundedCornerShape(24.dp),
                color = userColor,
                contentColor = onUser,
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    MessageBody(message, onUser, fontSize, codeFontSize, codeWrap, fontFamily)
                    MessageActions(message.content, true, onUser.copy(alpha = 0.6f), onSpeak, onCopy, onShare)
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                MessageBody(message, onAi, fontSize, codeFontSize, codeWrap, fontFamily)
                MessageActions(message.content, false, onAi.copy(alpha = 0.55f), onSpeak, onCopy, onShare)
            }
        }
    }
}

@Composable
private fun ClaudeMessage(
    message: AiMessage,
    isUser: Boolean,
    style: com.devai.studio.data.UiStyle,
    dark: Boolean,
    fontSize: TextUnit,
    codeFontSize: TextUnit,
    codeWrap: Boolean,
    bubbleWidth: Dp,
    fontFamily: FontFamily,
    onSpeak: (String) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
) {
    val userColor = style.userBubbleColor(dark)
    val onUser = style.onUserBubbleColor(dark)
    val onAi = style.onAiBubbleColor(dark)
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        if (isUser) {
            Surface(
                modifier = Modifier.widthIn(max = bubbleWidth),
                shape = RoundedCornerShape(14.dp),
                color = userColor.copy(alpha = 0.52f),
                contentColor = onUser,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                ),
            ) {
                Column(Modifier.padding(14.dp)) {
                    MessageBody(message, onUser, fontSize, codeFontSize, codeWrap, fontFamily)
                    MessageActions(message.content, true, onUser.copy(alpha = 0.6f), onSpeak, onCopy, onShare)
                }
            }
        } else {
            MessageBody(
                message, onAi, fontSize, codeFontSize, codeWrap,
                FontFamily.Serif,
            )
            MessageActions(message.content, false, onAi.copy(alpha = 0.55f), onSpeak, onCopy, onShare)
        }
    }
}

@Composable
private fun TerminalMessage(
    message: AiMessage,
    isUser: Boolean,
    style: com.devai.studio.data.UiStyle,
    dark: Boolean,
    fontSize: TextUnit,
    codeFontSize: TextUnit,
    codeWrap: Boolean,
    onSpeak: (String) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
) {
    val prompt = if (isUser) ">" else "$"
    val color = if (isUser) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onBackground
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Text(
            prompt,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(16.dp),
        )
        Column(Modifier.weight(1f)) {
            MessageBody(message, color, fontSize, codeFontSize, codeWrap, FontFamily.Monospace)
            MessageActions(
                message.content, isUser, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                onSpeak, onCopy, onShare,
            )
        }
    }
}

@Composable
private fun MaterialMessage(
    message: AiMessage,
    isUser: Boolean,
    style: com.devai.studio.data.UiStyle,
    dark: Boolean,
    fontSize: TextUnit,
    codeFontSize: TextUnit,
    codeWrap: Boolean,
    bubbleWidth: Dp,
    showAvatars: Boolean,
    fontFamily: FontFamily,
    onSpeak: (String) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
) {
    val layout = style.layout
    val shape = when (layout) {
        StyleLayout.MATERIAL_DESIGN -> RoundedCornerShape(4.dp)
        StyleLayout.MATERIAL_YOU -> RoundedCornerShape(24.dp)
        else -> MaterialTheme.shapes.extraLarge
    }
    val container = when {
        isUser && layout == StyleLayout.MATERIAL_DESIGN -> MaterialTheme.colorScheme.primary
        isUser && layout == StyleLayout.MATERIAL_YOU -> MaterialTheme.colorScheme.primaryContainer
        isUser -> MaterialTheme.colorScheme.primaryContainer
        layout == StyleLayout.MATERIAL_DESIGN -> MaterialTheme.colorScheme.surface
        layout == StyleLayout.MATERIAL_YOU -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when {
        isUser && layout == StyleLayout.MATERIAL_DESIGN -> MaterialTheme.colorScheme.onPrimary
        isUser && layout == StyleLayout.MATERIAL_YOU -> MaterialTheme.colorScheme.onPrimaryContainer
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!isUser && showAvatars) {
            Avatar(false)
            Spacer(Modifier.width(8.dp))
        }
        Surface(
            modifier = Modifier.widthIn(max = bubbleWidth),
            shape = shape,
            color = container,
            contentColor = contentColor,
            shadowElevation = if (layout == StyleLayout.MATERIAL_DESIGN) 2.dp else 0.dp,
        ) {
            Column(Modifier.padding(if (layout == StyleLayout.MATERIAL_EXPRESSIVE) 16.dp else 12.dp)) {
                MessageBody(message, contentColor, fontSize, codeFontSize, codeWrap, fontFamily)
                MessageActions(
                    message.content, isUser, contentColor.copy(alpha = 0.58f),
                    onSpeak, onCopy, onShare,
                )
            }
        }
        if (isUser && showAvatars) {
            Spacer(Modifier.width(8.dp))
            Avatar(true)
        }
    }
}

@Composable
private fun Avatar(isUser: Boolean) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(LocalUiStyle.current.avatarShape())
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
    val highlighted = highlightedCode(lang, code)
    val languageName = remember(lang, code) { CodeSyntax.displayName(lang, code) }
    val lineCount = remember(code) { code.count { it == '\n' } + 1 }
    Surface(
        color = Color(0xFF1E1E1E),
        contentColor = Color(0xFFE6E6E6),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$languageName · $lineCount стр.",
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
            SelectionContainer {
                Text(
                    text = highlighted,
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
}


private fun withSearchSources(answer: String, searchBlock: String): String {
    if (searchBlock.isBlank()) return answer
    val existing = Regex("""https?://[^\s)>\]]+""")
        .findAll(answer)
        .map { it.value.trimEnd('.', ',', ';', ')', ']') }
        .toSet()
    val links = Regex("""https?://[^\s)>\]]+""")
        .findAll(searchBlock)
        .map { it.value.trimEnd('.', ',', ';', ')', ']') }
        .filter { !it.contains("bing.com/ck") && it !in existing }
        .distinct()
        .take(5)
        .toList()
    if (links.isEmpty()) return answer
    return answer.trimEnd() + "\n\nИсточники:\n" + links.joinToString("\n") { "- $it" }
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
    val allowed = CodeLanguages.supportedExtensions + setOf(
        "txt", "md", "csv", "log", "gradle", "properties", "toml", "ini", "devai",
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
