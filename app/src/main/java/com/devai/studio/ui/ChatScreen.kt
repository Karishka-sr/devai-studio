package com.devai.studio.ui

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val handler = remember { Handler(Looper.getMainLooper()) }
    val listState = rememberLazyListState()

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

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = modelLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {
                persist()
                sessions = ChatSessionsStore.list(context)
                showSessions = true
            }) { Text("Сессии") }
            TextButton(onClick = { regenerate() }, enabled = !streaming) { Text("Заново") }
            TextButton(onClick = { exportChat() }) { Text("Экспорт") }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(messages.size) { index ->
                MessageBubble(messages[index])
            }
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Сообщение") },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (!streaming) send() }),
            )
            Button(
                onClick = { if (streaming) stop() else send() },
                enabled = streaming || input.isNotBlank(),
            ) {
                Text(if (streaming) "Стоп" else "Отправить")
            }
        }
    }

    if (showSessions) {
        AlertDialog(
            onDismissRequest = { showSessions = false },
            title = { Text("Сессии") },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    sessions.forEach { session ->
                        TextButton(
                            onClick = {
                                loadSession(session)
                                showSessions = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(session.title, maxLines = 1)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    loadSession(ChatSessionsStore.create(context))
                    showSessions = false
                }) { Text("Новая") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        ChatSessionsStore.delete(context, sessionId)
                        loadSession(ChatSessionsStore.getOrCreateCurrent(context))
                        showSessions = false
                    }) { Text("Удалить") }
                    TextButton(onClick = { showSessions = false }) { Text("Закрыть") }
                }
            },
        )
    }
}

@Composable
private fun MessageBubble(message: AiMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(max = 340.dp),
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
                        Text(message.content, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
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
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF7FD1FF)),
                ) {
                    Text(if (copied) "Скопировано" else "Копировать", fontSize = 11.sp)
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
