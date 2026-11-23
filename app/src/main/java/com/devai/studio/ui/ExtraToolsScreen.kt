package com.devai.studio.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devai.studio.data.QuickTool
import com.devai.studio.data.QuickTools

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraToolsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Все") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selected = selectedId?.let { QuickTools.byId(it) }
    val filtered = remember(query, category) {
        QuickTools.all.filter { tool ->
            (category == "Все" || tool.category == category) &&
                (query.isBlank() ||
                    tool.title.contains(query, ignoreCase = true) ||
                    tool.description.contains(query, ignoreCase = true) ||
                    tool.category.contains(query, ignoreCase = true))
        }
    }

    DevAIBackground {
        Column(Modifier.fillMaxSize()) {
            DevAITopBar(
                title = "Лаборатория",
                subtitle = "${QuickTools.all.size} инструментов в одном месте",
                onBack = onBack,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Найти инструмент…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        TextButton(onClick = { query = "" }) { Text("Сброс") }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                QuickTools.categories.forEach { item ->
                    FilterChip(
                        selected = category == item,
                        onClick = { category = item },
                        label = { Text(item) },
                    )
                }
            }
            Text(
                "Найдено: ${filtered.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
            )
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { tool ->
                    ToolRow(tool = tool, onClick = { selectedId = tool.id })
                }
            }
        }
    }

    if (selected != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedId = null },
            sheetState = sheetState,
        ) {
            ToolWorkbench(tool = selected, onClose = { selectedId = null })
        }
    }
}

@Composable
private fun ToolRow(tool: QuickTool, onClick: () -> Unit) {
    val accent = when (tool.category) {
        "Кодирование" -> MaterialTheme.colorScheme.primary
        "JSON/CSV" -> MaterialTheme.colorScheme.secondary
        "Хеши" -> MaterialTheme.colorScheme.tertiary
        "Текст" -> MaterialTheme.colorScheme.primary
        "Числа" -> MaterialTheme.colorScheme.secondary
        "Время" -> MaterialTheme.colorScheme.tertiary
        "Цвет" -> MaterialTheme.colorScheme.primary
        "Генераторы" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }
    DevAICard(
        onClick = onClick,
        accent = accent,
        contentPadding = PaddingValues(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DevAIIconTile(Icons.Default.Build, tint = accent, size = 38.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    tool.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                tool.category,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolWorkbench(tool: QuickTool, onClose: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var input by remember(tool.id) { mutableStateOf("") }
    var second by remember(tool.id) { mutableStateOf("") }
    var output by remember(tool.id) { mutableStateOf<String?>(null) }
    var error by remember(tool.id) { mutableStateOf<String?>(null) }
    var copied by remember(tool.id) { mutableStateOf(false) }

    fun run() {
        runCatching { tool.transform(input, second) }
            .onSuccess {
                output = it
                error = null
                copied = false
            }
            .onFailure {
                output = null
                error = it.message ?: it.javaClass.simpleName
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    tool.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Закрыть")
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(tool.inputLabel) },
            placeholder = {
                Text(tool.inputPlaceholder.ifBlank { "Введите текст…" })
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 8,
            shape = RoundedCornerShape(16.dp),
        )

        if (tool.secondLabel != null) {
            OutlinedTextField(
                value = second,
                onValueChange = { second = it },
                label = { Text(tool.secondLabel) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { run() }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Выполнить")
            }
            OutlinedButton(
                onClick = {
                    input = ""
                    second = ""
                    output = null
                    error = null
                    copied = false
                },
                modifier = Modifier.weight(1f),
            ) { Text("Очистить") }
        }

        error?.let {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(it, modifier = Modifier.padding(12.dp))
            }
        }

        output?.let { text ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.padding(14.dp)) {
                    SelectionContainer {
                        Text(
                            text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(text))
                        copied = true
                        Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        if (copied) Icons.Default.Check else Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (copied) "Скопировано" else "Копировать")
                }
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Поделиться"))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Поделиться")
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "Инструмент работает локально: текст никуда не отправляется.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
