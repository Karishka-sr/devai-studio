package com.devai.studio.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devai.studio.ai.AiFormat
import com.devai.studio.ai.ModelFetcher
import com.devai.studio.data.ApiKeyStore
import com.devai.studio.data.AppPrefs
import com.devai.studio.data.Category
import com.devai.studio.data.LocalModelStore
import com.devai.studio.data.ProviderCatalog
import com.devai.studio.data.ProviderInfo
import com.devai.studio.util.AndroidUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProvidersScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf<Category?>(null) }
    var rev by remember { mutableStateOf(0) }
    val activeId = remember(rev) { AppPrefs.activeProvider(context) }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun buildRows(): List<ProviderRow> = buildList {
        add(ProviderRow.Header)
        val categories = filter?.let { listOf(it) }
            ?: listOf(Category.FULLY_FREE, Category.FREE, Category.PAID)
        categories.forEach { category ->
            val list = ProviderCatalog.byCategory(category)
            if (list.isNotEmpty()) {
                add(ProviderRow.CategoryHeader(category))
                list.forEach { add(ProviderRow.Provider(it)) }
            }
        }
    }

    val rows = remember(filter, rev) { buildRows() }

    fun openLocalModels() {
        context.startActivity(Intent(context, LocalModelsActivity::class.java))
    }

    fun openKey(p: ProviderInfo) {
        if (p.format == AiFormat.LOCAL) {
            openLocalModels()
            return
        }
        CommonDialogs.keyDialog(context, p) { rev++ }
    }

    fun fetchModels(p: ProviderInfo) {
        scope.launch {
            try {
                val key = ApiKeyStore.get(context, p.id).orEmpty()
                val base = if (p.id == "custom") AppPrefs.customBase(context) else p.baseUrl
                if (base.isBlank()) {
                    toast("base URL пуст")
                    return@launch
                }
                val models = withContext(Dispatchers.IO) { ModelFetcher.fetch(p, key, base) }
                if (models.isEmpty()) {
                    toast("Модели не найдены")
                } else {
                    AppPrefs.setModelsFor(context, p.id, models)
                    toast("Загружено моделей: ${models.size}")
                    rev++
                }
            } catch (e: Exception) {
                toast("Ошибка: ${e.message ?: "network"}")
            }
        }
    }

    fun openModel(p: ProviderInfo) {
        if (p.format == AiFormat.LOCAL) {
            openLocalModels()
            return
        }
        val current = AppPrefs.modelFor(context, p.id) ?: p.models.firstOrNull().orEmpty()
        CommonDialogs.modelDialog(
            context,
            p,
            current,
            AppPrefs.modelsFor(context, p.id),
            onFetch = { fetchModels(p) },
        ) { model ->
            AppPrefs.setModelFor(context, p.id, model)
            toast("Модель: $model")
            rev++
        }
    }

    fun use(p: ProviderInfo) {
        AppPrefs.setActiveProvider(context, p.id)
        rev++
        toast("Активен: ${p.name}")
        if (p.format == AiFormat.LOCAL) {
            if (LocalModelStore.active(context) == null) openLocalModels()
            return
        }
        val needsKey = p.id != "custom" &&
            (p.category != Category.FULLY_FREE || p.id == "openrouter-free" || p.id == "devai-core") &&
            !ApiKeyStore.has(context, p.id)
        if (needsKey) {
            CommonDialogs.keyDialog(context, p) { rev++ }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(rows) { row ->
            when (row) {
                ProviderRow.Header -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DevAITopBar(
                        title = "ИИ",
                        subtitle = "Провайдеры, ключи и модели",
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DevAICard(
                            modifier = Modifier.weight(1f),
                            onClick = { openLocalModels() },
                            contentPadding = PaddingValues(14.dp),
                        ) {
                            DevAIIconTile(
                                Icons.AutoMirrored.Filled.List,
                                size = 36.dp,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                "Локальные модели",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "Работают без сети",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DevAICard(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                context.startActivity(Intent(context, QrExportActivity::class.java))
                            },
                            contentPadding = PaddingValues(14.dp),
                        ) {
                            DevAIIconTile(
                                Icons.Default.Share,
                                size = 36.dp,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                            Text(
                                "QR-перенос",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "На другой телефон",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = filter == null,
                            onClick = { filter = null },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                            label = { Text("Все") },
                        )
                        Category.entries.forEach { category ->
                            FilterChip(
                                selected = filter == category,
                                onClick = { filter = category },
                                leadingIcon = {
                                    Icon(categoryIcon(category), contentDescription = null)
                                },
                                label = { Text(categoryLabel(category)) },
                            )
                        }
                    }
                }

                is ProviderRow.CategoryHeader -> DevAISectionTitle(
                    title = categoryLabel(row.category),
                    icon = categoryIcon(row.category),
                )

                is ProviderRow.Provider -> ProviderCard(
                    p = row.info,
                    active = row.info.id == activeId,
                    model = AppPrefs.modelFor(context, row.info.id)
                        ?: row.info.models.firstOrNull().orEmpty(),
                    onKey = { openKey(row.info) },
                    onModel = { openModel(row.info) },
                    onSite = {
                        if (row.info.website.isBlank()) toast("Ссылка не указана")
                        else AndroidUtils.openUrl(context, row.info.website)
                    },
                    onUse = { use(row.info) },
                )
            }
        }
    }
}

@Composable
private fun ProviderCard(
    p: ProviderInfo,
    active: Boolean,
    model: String,
    onKey: () -> Unit,
    onModel: () -> Unit,
    onSite: () -> Unit,
    onUse: () -> Unit,
) {
    DevAICard(
        accent = if (active) MaterialTheme.colorScheme.primary else null,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DevAIIconTile(
                categoryIcon(p.category),
                size = 44.dp,
                tint = when (p.category) {
                    Category.FULLY_FREE -> MaterialTheme.colorScheme.secondary
                    Category.FREE -> MaterialTheme.colorScheme.primary
                    Category.PAID -> MaterialTheme.colorScheme.tertiary
                },
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = p.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = categoryLabel(p.category) + " · " + formatLabel(p.format),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (active) {
                DevAIStatusPill("Активен")
            }
        }
        Text(
            text = p.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (model.isNotBlank()) {
            Text(
                text = "Модель: $model",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (p.freeNote.isNotBlank()) {
            Text(
                text = p.freeNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val hasKey = ApiKeyStore.has(LocalContext.current, p.id)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TextButton(onClick = onKey) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (p.id == "devai-core" && !hasKey) "Вставить ключ" else "Ключ")
            }
            TextButton(onClick = onModel) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(4.dp))
                Text("Модель")
            }
            TextButton(onClick = onSite) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(4.dp))
                Text("Сайт")
            }
            TextButton(onClick = onUse) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (active) "Выбран" else "Использовать")
            }
        }
    }
}

private sealed interface ProviderRow {
    data object Header : ProviderRow
    data class CategoryHeader(val category: Category) : ProviderRow
    data class Provider(val info: ProviderInfo) : ProviderRow
}

private fun categoryIcon(category: Category): ImageVector = when (category) {
    Category.FULLY_FREE -> Icons.Default.Star
    Category.FREE -> Icons.Default.CheckCircle
    Category.PAID -> Icons.Default.ShoppingCart
}

private fun categoryLabel(category: Category): String = when (category) {
    Category.FULLY_FREE -> "Полностью бесплатные"
    Category.FREE -> "Бесплатные"
    Category.PAID -> "Платные"
}

private fun formatLabel(format: AiFormat): String = when (format) {
    AiFormat.LOCAL -> "Локальная"
    AiFormat.OPENAI_COMPAT -> "OpenAI-совместимый"
    AiFormat.ANTHROPIC -> "Anthropic"
    AiFormat.GEMINI -> "Gemini"
}
