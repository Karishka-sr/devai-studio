package com.devai.studio.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devai.studio.ai.AiMessage
import com.devai.studio.data.PromptLibrary

private data class AiAction(
    val title: String,
    val subtitle: String,
    val prompt: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAiActionsSheet(
    chatText: String,
    input: String,
    lastAnswer: String,
    onDismiss: () -> Unit,
    onRun: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val source = input.ifBlank { lastAnswer }.trim()
    val actions = remember(source, lastAnswer, chatText) {
        listOf(
            AiAction(
                "Улучшить промпт",
                "Сделать запрос точнее и понятнее",
                "Улучши мой промпт: сделай его конкретным, добавь контекст и критерии результата. " +
                    "Верни только улучшенный промпт и коротко объясни, что изменил.\n\n$source",
            ),
            AiAction(
                "Сократить ответ",
                "Оставить только суть",
                "Сократи этот ответ до 5–7 пунктов, сохрани важные детали и код:\n\n$source",
            ),
            AiAction(
                "Подробнее",
                "Раскрыть тему глубже",
                "Объясни этот ответ подробнее, добавь примеры, подводные камни и итоговый чек-лист:\n\n$source",
            ),
            AiAction(
                "Перевести на русский",
                "Если ответ на другом языке",
                "Переведи на русский язык, сохрани код и форматирование:\n\n$source",
            ),
            AiAction(
                "Перевести на английский",
                "Для документации и коммитов",
                "Translate to natural English. Keep code and markdown formatting:\n\n$source",
            ),
            AiAction(
                "Объяснить код",
                "Разобрать по шагам",
                "Объясни этот код по шагам: что делает, какие входы и выходы, где риски. " +
                    "В конце дай короткий пример использования:\n\n$source",
            ),
            AiAction(
                "Найти баги",
                "Ревью кода и логики",
                "Проверь код и текст на баги, гонки, утечки, null-safety и ошибки обработки. " +
                    "Сначала список проблем по приоритету, затем исправленный код:\n\n$source",
            ),
            AiAction(
                "Сгенерировать тесты",
                "Unit-тесты на Kotlin",
                "Напиши unit-тесты на Kotlin/JUnit для этого кода. Покрой успех, границы и ошибки. " +
                    "Не используй mocks без необходимости:\n\n$source",
            ),
            AiAction(
                "Commit message",
                "Conventional Commits",
                "Составь commit message в Conventional Commits для этих изменений. " +
                    "Заголовок до 72 символов и тело с причиной:\n\n$source",
            ),
            AiAction(
                "Резюме чата",
                "Кратко о всём диалоге",
                "Сделай резюме диалога: цель, принятые решения, код, открытые вопросы и следующие шаги.\n\n$chatText",
            ),
            AiAction(
                "Заголовок чата",
                "Короткое имя сессии",
                "Придумай короткий заголовок для этого диалога, до 40 символов. " +
                    "Верни только заголовок без кавычек.\n\n$chatText",
            ),
            AiAction(
                "Сделать список",
                "Разбить на пункты",
                "Преобразуй ответ в структурированный список с короткими пунктами:\n\n$source",
            ),
            AiAction(
                "Сделать таблицу",
                "Сравнение в таблице",
                "Преобразуй информацию в Markdown-таблицу с понятными колонками:\n\n$source",
            ),
            AiAction(
                "Проверить факты",
                "Найти сомнительные утверждения",
                "Проверь ответ на фактические ошибки и сомнительные утверждения. " +
                    "Отметь, что нужно проверить в источниках:\n\n$source",
            ),
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                "AI-действия",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Быстрые операции над текущим сообщением или всем чатом.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.heightIn(max = 480.dp)) {
                items(actions) { action ->
                    ListItem(
                        headlineContent = { Text(action.title) },
                        supportingContent = { Text(action.subtitle) },
                        leadingContent = {
                            Icon(Icons.Default.Build, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            if (action.prompt.isNotBlank()) onRun(action.prompt)
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPromptSheet(
    onDismiss: () -> Unit,
    onUse: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                "Библиотека промптов",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${PromptLibrary.items.size} готовых промптов для кода, текста и анализа.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.heightIn(max = 480.dp)) {
                items(PromptLibrary.items) { item ->
                    ListItem(
                        headlineContent = { Text(item.title) },
                        supportingContent = {
                            Text(item.prompt, maxLines = 2)
                        },
                        leadingContent = {
                            Icon(Icons.Default.Create, contentDescription = null)
                        },
                        modifier = Modifier.clickable { onUse(item.prompt) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSearchSheet(
    messages: List<AiMessage>,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val results = remember(query, messages) {
        if (query.isBlank()) emptyList()
        else messages.withIndex().filter { (_, message) ->
            message.content.contains(query, ignoreCase = true)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                "Поиск по чату",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Слово или фраза…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            if (query.isNotBlank() && results.isEmpty()) {
                Text(
                    "Ничего не найдено",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(results) { (index, message) ->
                    ListItem(
                        headlineContent = {
                            Text(if (message.role == "user") "Вы" else "DevAI")
                        },
                        supportingContent = {
                            Text(message.content.take(180), maxLines = 3)
                        },
                        trailingContent = {
                            Text("${index + 1}")
                        },
                        modifier = Modifier.clickable {
                            onJump(index)
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Закрыть")
            }
        }
    }
}
