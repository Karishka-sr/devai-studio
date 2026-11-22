package com.devai.studio.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devai.studio.util.ThemeManager

class PluginDocsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        ThemeManager.applyDynamicIfAvailable(this)
        setContent { DevAITheme { PluginDocsScreen(onBack = { finish() }) } }
    }
}

private sealed interface DocBlock {
    data class Heading(val level: Int, val text: String) : DocBlock
    data class Code(val text: String) : DocBlock
    data class Paragraph(val text: String) : DocBlock
    data class Bullet(val text: String) : DocBlock
    data class Table(val lines: List<String>) : DocBlock
    data object Space : DocBlock
}

private fun parseDocs(markdown: String): List<DocBlock> {
    val blocks = mutableListOf<DocBlock>()
    val lines = markdown.lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        when {
            line.startsWith("```") -> {
                val code = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].startsWith("```")) {
                    code.add(lines[i])
                    i++
                }
                blocks.add(DocBlock.Code(code.joinToString("\n")))
            }
            line.startsWith("|") -> {
                val table = mutableListOf<String>()
                while (i < lines.size && lines[i].startsWith("|")) {
                    table.add(lines[i])
                    i++
                }
                blocks.add(DocBlock.Table(table))
                continue
            }
            line.startsWith("### ") -> blocks.add(DocBlock.Heading(3, line.removePrefix("### ")))
            line.startsWith("## ") -> blocks.add(DocBlock.Heading(2, line.removePrefix("## ")))
            line.startsWith("# ") -> blocks.add(DocBlock.Heading(1, line.removePrefix("# ")))
            line.startsWith("- ") -> blocks.add(DocBlock.Bullet(line.removePrefix("- ")))
            line.isBlank() -> blocks.add(DocBlock.Space)
            else -> blocks.add(DocBlock.Paragraph(line))
        }
        i++
    }
    return blocks
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginDocsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val docs = remember {
        runCatching {
            context.assets.open("devai-plugins.md").bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrElse { "Не удалось открыть документацию: ${it.message}" }
    }
    val blocks = remember(docs) { parseDocs(docs) }

    val saver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(docs.toByteArray(Charsets.UTF_8))
            } ?: error("Не удалось открыть файл")
        }.onSuccess {
            Toast.makeText(context, "Документация сохранена", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Ошибка: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Документация по плагинам") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text(
                    "Документация по плагинам DevAI Studio",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Lua 5.2 через LuaJ + старый DPL. Плагины выполняют код в песочнице и дают ИИ инструменты.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { saver.launch("devai-plugins.md") },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Скачать .md")
                    }
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/markdown"
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    "Документация по плагинам DevAI Studio",
                                )
                                putExtra(Intent.EXTRA_TEXT, docs)
                            }
                            context.startActivity(
                                Intent.createChooser(intent, "Поделиться документацией")
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Поделиться")
                    }
                }
            }
            items(blocks) { block ->
                when (block) {
                    is DocBlock.Heading -> Text(
                        block.text,
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.headlineSmall
                            2 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleSmall
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = if (block.level == 1) 14.dp else 8.dp),
                    )
                    is DocBlock.Code -> Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        SelectionContainer {
                            Text(
                                block.text,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                    }
                    is DocBlock.Bullet -> Row {
                        Text("•", color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(block.text, style = MaterialTheme.typography.bodyMedium)
                    }
                    is DocBlock.Table -> Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            block.lines.joinToString("\n"),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(8.dp),
                        )
                    }
                    is DocBlock.Paragraph -> Text(
                        block.text,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    DocBlock.Space -> Spacer(Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}
