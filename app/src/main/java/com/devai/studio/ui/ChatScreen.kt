package com.devai.studio.ui

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devai.studio.R
import com.devai.studio.ai.AiClientFactory
import com.devai.studio.ai.AiFormat
import com.devai.studio.ai.AiMessage
import com.devai.studio.data.AppPrefs
import com.devai.studio.data.ChatSession
import com.devai.studio.data.ChatSessionsStore
import com.devai.studio.data.LocalModelStore
import com.devai.studio.data.PremiumStore
import com.devai.studio.util.AiSupport
import com.devai.studio.util.PremiumDialogs
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val handler = remember { Handler(Looper.getMainLooper()) }
    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var sessionId by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<AiMessage>>(emptyList()) }
    var input by rememberSaveable { mutableStateOf("") }
    var streaming by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var modelLabel by remember { mutableStateOf("") }
    val cancelled = remember { AtomicBoolean(false) }

    var showSessions by remember { mutableStateOf(false) }
    var sessions by remember { mutableStateOf<List<ChatSession>>(emptyList()) }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
    }

    fun openSessions() {
        persist()
        sessions = ChatSessionsStore.list(context)
        showSessions = true
    }

    fun stop() {
        cancelled.set(true)
        job?.cancel()
        streaming = false
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty()) return
        val cfg = try {
            AiSupport.config(context)
        } catch (e: IllegalStateException) {
            toast(e.message ?: context.getString(R.string.chat_no_key))
            return
        }

        input = ""
        val snapshot = messages + AiMessage("user", text) + AiMessage("assistant", "")
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
                val apiMessages = ArrayList<AiMessage>()
                apiMessages.add(AiMessage("system", AppPrefs.systemPrompt(context)))
                apiMessages.addAll(snapshot.take(assistantIndex))

                val full = client.chat(apiMessages) { delta ->
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

                if (!cancelled.get()) {
                    finished.set(true)
                    val finalText = full.ifBlank { synchronized(sb) { sb.toString() } }
                    messages = messages.toMutableList().also {
                        if (assistantIndex in it.indices) {
                            it[assistantIndex] = AiMessage("assistant", finalText)
                        }
                    }
                    persist()
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
        if (!PremiumStore.isPremium(context)) {
            PremiumDialogs.show(context, context.getString(R.string.premium_feature_export))
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
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        "DevAI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        modelLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            },
            actions = {
                IconButton(onClick = { openSessions() }) {
                    Icon(Icons.Default.List, contentDescription = "Сессии")
                }
                IconButton(onClick = { newChat() }) {
                    Icon(Icons.Default.Add, contentDescription = "Новый чат")
                }
                IconButton(onClick = { exportChat() }) {
                    Icon(Icons.Default.Share, contentDescription = "Экспорт")
                }
                IconButton(onClick = { regenerate() }, enabled = !streaming) {
                    Icon(Icons.Default.Refresh, contentDescription = "Заново")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        )

        if (messages.isEmpty() && !streaming) {
            EmptyChat(
                modifier = Modifier.weight(1f),
                onSuggestion = { input = it },
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            ) {
                items(messages.size) { index ->
                    val message = messages[index]
                    if (!(streaming && index == messages.lastIndex && message.content.isBlank())) {
                        MessageBubble(message)
                    }
                }
                if (streaming) {
                    item { TypingIndicator() }
                }
            }
        }

        Surface(
            tonalElevation = 3.dp,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Сообщение…") },
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (!streaming) send() }),
                )
                Spacer(Modifier.width(4.dp))
                FilledIconButton(
                    onClick = { if (streaming) stop() else send() },
                    enabled = streaming || input.isNotBlank(),
                ) {
                    Icon(
                        imageVector = if (streaming) Icons.Default.Close else Icons.Default.Send,
                        contentDescription = if (streaming) "Стоп" else "Отправить",
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
                Spacer(Modifier.height(12.dp))
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
                            ChatSessionsStore.delete(context, sessionId)
                            loadSession(ChatSessionsStore.getOrCreateCurrent(context))
                            showSessions = false
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
}

@Composable
private fun EmptyChat(
    modifier: Modifier = Modifier,
    onSuggestion: (String) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(80.dp),
        ) {
            Icon(
                Icons.Default.Face,
                contentDescription = null,
                modifier = Modifier.padding(20.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Чем помочь?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Спросите что угодно — код, ошибки, интерфейс.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                "Напиши код на Kotlin",
                "Объясни ошибку",
                "Сделай UI на Compose",
            ).forEach { text ->
                AssistChip(
                    onClick = { onSuggestion(text) },
                    label = { Text(text) },
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
private fun MessageBubble(message: AiMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!isUser) {
            Avatar(isUser = false)
            Spacer(Modifier.width(8.dp))
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = if (isUser) 18.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp,
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(max = 320.dp),
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
                            fontSize = 15.sp,
                        )
                    }
                } else if (message.content.isBlank()) {
                    Text("…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val segments = remember(message.content) { parseSegments(message.content) }
                    if (segments.isEmpty()) {
                        Text(
                            message.content,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                        )
                    } else {
                        segments.forEach { segment ->
                            if (segment.code) {
                                CodeBlock(segment.lang, segment.text)
                            } else {
                                SelectionContainer {
                                    Text(
                                        text = segment.text,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 15.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Avatar(isUser = true)
        }
    }
}

@Composable
private fun Avatar(isUser: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (isUser) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        },
        modifier = Modifier.size(28.dp),
    ) {
        Icon(
            imageVector = if (isUser) Icons.Default.Person else Icons.Default.Face,
            contentDescription = null,
            modifier = Modifier.padding(5.dp),
            tint = if (isUser) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onTertiaryContainer
            },
        )
    }
}

@Composable
private fun CodeBlock(lang: String, code: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    Surface(color = Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp)) {
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
                fontSize = 12.5.sp,
                fontFamily = FontFamily.Monospace,
                softWrap = false,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            )
        }
    }
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
