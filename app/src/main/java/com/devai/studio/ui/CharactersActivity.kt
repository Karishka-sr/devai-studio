package com.devai.studio.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devai.studio.data.CharacterCard
import com.devai.studio.data.CharacterStore
import com.devai.studio.util.ThemeManager

class CharactersActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        ThemeManager.applyDynamicIfAvailable(this)
        setContent {
            DevAITheme {
                CharactersScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharactersScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var cards by remember { mutableStateOf(CharacterStore.list(context)) }
    var activeId by remember { mutableStateOf(CharacterStore.activeId(context)) }
    var editing by remember { mutableStateOf<CharacterCard?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<CharacterCard?>(null) }

    fun refresh() {
        cards = CharacterStore.list(context)
        activeId = CharacterStore.activeId(context)
    }

    fun select(card: CharacterCard?) {
        CharacterStore.setActive(context, card?.id)
        activeId = card?.id
        Toast.makeText(
            context,
            if (card == null) "Персонаж отключён" else "Персонаж: ${card.name}",
            Toast.LENGTH_SHORT,
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Карточки персонажей") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Персонаж добавляет свой характер и системный промпт в чат. " +
                        "Свой системный промпт из Настроек остаётся главным.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            editing = CharacterCard("", "", "")
                            showEditor = true
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Добавить")
                    }
                    OutlinedButton(
                        onClick = { select(null) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Без персонажа")
                    }
                }
            }
            items(cards) { card ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { select(card) },
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = if (activeId == card.id) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                card.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            if (activeId == card.id) {
                                Text(
                                    "Активен",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        if (card.description.isNotBlank()) {
                            Text(
                                card.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (card.builtIn) {
                            Text(
                                "Встроенный персонаж",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Row {
                            if (!card.builtIn) {
                                TextButton(onClick = {
                                    editing = card
                                    showEditor = true
                                }) { Text("Изменить") }
                                TextButton(onClick = { deleting = card }) {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Удалить")
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showEditor) {
        val initial = editing ?: CharacterCard("", "", "")
        CharacterEditorDialog(
            initial = initial,
            onDismiss = { showEditor = false },
            onSave = { card ->
                CharacterStore.save(context, card)
                showEditor = false
                refresh()
            },
        )
    }

    deleting?.let { card ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Удалить персонажа?") },
            text = { Text("«${card.name}» будет удалён.") },
            confirmButton = {
                TextButton(onClick = {
                    CharacterStore.delete(context, card.id)
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
private fun CharacterEditorDialog(
    initial: CharacterCard,
    onDismiss: () -> Unit,
    onSave: (CharacterCard) -> Unit,
) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var description by remember(initial.id) { mutableStateOf(initial.description) }
    var personality by remember(initial.id) { mutableStateOf(initial.personality) }
    var scenario by remember(initial.id) { mutableStateOf(initial.scenario) }
    var greeting by remember(initial.id) { mutableStateOf(initial.greeting) }
    var prompt by remember(initial.id) { mutableStateOf(initial.prompt) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.id.isBlank()) "Новый персонаж" else "Персонаж") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = personality,
                    onValueChange = { personality = it },
                    label = { Text("Характер") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = scenario,
                    onValueChange = { scenario = it },
                    label = { Text("Сценарий") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = greeting,
                    onValueChange = { greeting = it },
                    label = { Text("Приветствие в новом чате") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Свой системный промпт (необязательно)") },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            initial.copy(
                                name = name.trim(),
                                description = description.trim(),
                                personality = personality.trim(),
                                scenario = scenario.trim(),
                                greeting = greeting.trim(),
                                prompt = prompt.trim(),
                            )
                        )
                    }
                },
                enabled = name.isNotBlank(),
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}
