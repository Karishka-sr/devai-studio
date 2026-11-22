package com.devai.studio.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.devai.studio.R
import com.devai.studio.ai.LocalLlmEngine
import com.devai.studio.data.ApiKeyStore
import com.devai.studio.data.AppPrefs
import com.devai.studio.data.ChatSessionsStore
import com.devai.studio.data.PremiumStore
import com.devai.studio.util.PremiumDialogs
import com.devai.studio.util.ThemeManager
import kotlin.math.roundToInt

private enum class SettingsCategory(val title: String, val icon: ImageVector) {
    GENERATION("Генерация", Icons.Default.Settings),
    MODELS("Модели", Icons.Default.PlayArrow),
    CHAT("Чат", Icons.AutoMirrored.Filled.Send),
    LOOK("Вид", Icons.Default.Star),
    PREMIUM("Premium", Icons.Default.Favorite),
    DATA("Данные", Icons.Default.Lock),
    ABOUT("О приложении", Icons.Default.Info),
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var temperature by remember { mutableFloatStateOf(AppPrefs.temperature(context)) }
    var topP by remember { mutableFloatStateOf(AppPrefs.topP(context)) }
    var maxTokens by remember { mutableStateOf(AppPrefs.maxTokens(context).toString()) }
    var historyLimit by remember { mutableStateOf(AppPrefs.historyLimit(context).toString()) }
    var timeout by remember { mutableStateOf(AppPrefs.timeoutSeconds(context).toString()) }
    var localContext by remember { mutableFloatStateOf(AppPrefs.localContextTokens(context).toFloat()) }
    var localTopK by remember { mutableStateOf(AppPrefs.localTopK(context).toString()) }
    var chatFont by remember { mutableFloatStateOf(AppPrefs.chatFontSize(context)) }
    var codeFont by remember { mutableFloatStateOf(AppPrefs.codeFontSize(context)) }
    var codeWrap by remember { mutableStateOf(AppPrefs.codeWrap(context)) }
    var sendOnEnter by remember { mutableStateOf(AppPrefs.sendOnEnter(context)) }
    var autoScroll by remember { mutableStateOf(AppPrefs.autoScroll(context)) }
    var dynamicColor by remember { mutableStateOf(AppPrefs.dynamicColor(context)) }
    var accent by remember { mutableStateOf(AppPrefs.accent(context)) }
    var chatFontFamily by remember { mutableStateOf(AppPrefs.chatFontFamily(context)) }
    var bubbleWidth by remember { mutableFloatStateOf(AppPrefs.bubbleWidth(context).toFloat()) }
    var showAvatars by remember { mutableStateOf(AppPrefs.showAvatars(context)) }
    var compactChat by remember { mutableStateOf(AppPrefs.compactChat(context)) }
    var hapticFeedback by remember { mutableStateOf(AppPrefs.hapticFeedback(context)) }
    var confirmDelete by remember { mutableStateOf(AppPrefs.confirmDelete(context)) }
    var customBase by remember { mutableStateOf(AppPrefs.customBase(context)) }
    var customModel by remember { mutableStateOf(AppPrefs.customModel(context)) }
    var systemPrompt by remember { mutableStateOf(AppPrefs.systemPrompt(context)) }
    var themeId by remember { mutableStateOf(AppPrefs.theme(context)) }
    var premium by remember { mutableStateOf(PremiumStore.isPremium(context)) }
    var category by remember { mutableStateOf(SettingsCategory.GENERATION) }
    var showTheme by remember { mutableStateOf(false) }
    var showClearKeys by remember { mutableStateOf(false) }
    var showClearHistory by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun appVersion(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            toast(context.getString(R.string.settings_author_link_error))
        }
    }

    fun chooseTheme(id: String) {
        if (ThemeManager.isPremium(id) && !PremiumStore.isPremium(context)) {
            showTheme = false
            PremiumDialogs.show(context, context.getString(R.string.premium_feature_themes))
            return
        }
        AppPrefs.setTheme(context, id)
        themeId = id
        showTheme = false
        activity?.recreate()
    }

    fun shareSettings() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_SUBJECT, "DevAI Studio — настройки")
            putExtra(Intent.EXTRA_TEXT, AppPrefs.exportJson(context))
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться настройками"))
    }

    val promptPresets = listOf(
        "Разработчик" to (AppPrefs.DEFAULT_SYSTEM_PROMPT +
            " Сначала уточняй требования, потом предлагай решение."),
        "Кратко" to "Отвечай максимально кратко, без вступлений и повторов. Только суть.",
        "Код" to "Давай готовый рабочий код. Комментарии — только по делу. Указывай язык и зависимости.",
        "Ревью" to "Ищи баги, риски и проблемы безопасности. Предлагай конкретные исправления с кодом.",
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                premium = PremiumStore.isPremium(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(category) { scrollState.scrollTo(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Настройки",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                "v" + appVersion(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SettingsCategory.entries.forEach { item ->
                FilterChip(
                    selected = category == item,
                    onClick = { category = item },
                    label = { Text(item.title, style = MaterialTheme.typography.labelMedium) },
                    leadingIcon = {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }
        }

        when (category) {
            SettingsCategory.GENERATION -> {
                SettingsSection("Параметры запроса") {
                    SliderSetting(
                        title = "Температура",
                        valueLabel = "%.2f".format(temperature),
                        value = temperature,
                        range = 0f..1f,
                        steps = 19,
                    ) {
                        temperature = it
                        AppPrefs.setTemperature(context, it)
                    }

                    SliderSetting(
                        title = "Top-P",
                        valueLabel = "%.2f".format(topP),
                        value = topP,
                        range = 0.1f..1f,
                        steps = 17,
                    ) {
                        topP = it
                        AppPrefs.setTopP(context, it)
                    }

                    CompactNumberSetting(
                        label = "Макс. токенов",
                        value = maxTokens,
                        supporting = "1–100000",
                        onChange = {
                            maxTokens = it
                            it.toIntOrNull()?.let { value ->
                                if (value in 1..100000) AppPrefs.setMaxTokens(context, value)
                            }
                        },
                    )

                    CompactNumberSetting(
                        label = "Лимит истории",
                        value = historyLimit,
                        supporting = "0 — вся история",
                        onChange = {
                            historyLimit = it
                            it.toIntOrNull()?.let { value ->
                                if (value in 0..500) AppPrefs.setHistoryLimit(context, value)
                            }
                        },
                    )

                    CompactNumberSetting(
                        label = "Таймаут, сек",
                        value = timeout,
                        supporting = "15–600",
                        onChange = {
                            timeout = it
                            it.toIntOrNull()?.let { value ->
                                if (value in 15..600) AppPrefs.setTimeoutSeconds(context, value)
                            }
                        },
                    )
                }

                SettingsSection("Свой провайдер") {
                    OutlinedTextField(
                        value = customBase,
                        onValueChange = {
                            customBase = it
                            AppPrefs.setCustomBase(context, it)
                        },
                        label = { Text("Base URL") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = customModel,
                        onValueChange = {
                            customModel = it
                            AppPrefs.setCustomModel(context, it)
                        },
                        label = { Text("Модель") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                SettingsSection("Системный промпт") {
                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = {
                            systemPrompt = it
                            AppPrefs.setSystemPrompt(context, it)
                        },
                        label = { Text("Промпт") },
                        minLines = 3,
                        maxLines = 5,
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        promptPresets.forEach { (label, prompt) ->
                            AssistChip(
                                onClick = {
                                    systemPrompt = prompt
                                    AppPrefs.setSystemPrompt(context, prompt)
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }

            SettingsCategory.MODELS -> {
                SettingsSection("Локальные модели") {
                    SliderSetting(
                        title = "Контекст, токенов",
                        valueLabel = localContext.roundToInt().toString(),
                        value = localContext,
                        range = 512f..1280f,
                        steps = 2,
                    ) {
                        val rounded = (it / 256f).roundToInt() * 256
                        localContext = rounded.toFloat()
                        AppPrefs.setLocalContextTokens(context, rounded)
                    }

                    CompactNumberSetting(
                        label = "Top-K",
                        value = localTopK,
                        supporting = "1–100. Меньше — стабильнее",
                        onChange = {
                            localTopK = it
                            it.toIntOrNull()?.let { value ->
                                if (value in 1..100) AppPrefs.setLocalTopK(context, value)
                            }
                        },
                    )

                    Text(
                        "Изменения применятся при следующем запросе.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(context, LocalModelsActivity::class.java)
                                )
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Модели")
                        }
                        OutlinedButton(
                            onClick = {
                                LocalLlmEngine.close()
                                toast("Модель выгружена")
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Выгрузить")
                        }
                    }
                }
            }

            SettingsCategory.CHAT -> {
                SettingsSection("Чат") {
                    SliderSetting(
                        title = "Размер текста",
                        valueLabel = "%.0f sp".format(chatFont),
                        value = chatFont,
                        range = 12f..22f,
                        steps = 9,
                    ) {
                        chatFont = it
                        AppPrefs.setChatFontSize(context, it)
                    }

                    SwitchSetting(
                        title = "Перенос длинных строк кода",
                        subtitle = "Иначе код прокручивается по горизонтали",
                        checked = codeWrap,
                        onCheckedChange = {
                            codeWrap = it
                            AppPrefs.setCodeWrap(context, it)
                        },
                    )

                    SwitchSetting(
                        title = "Отправлять по Enter",
                        subtitle = "Иначе Enter переносит строку",
                        checked = sendOnEnter,
                        onCheckedChange = {
                            sendOnEnter = it
                            AppPrefs.setSendOnEnter(context, it)
                        },
                    )

                    SwitchSetting(
                        title = "Автопрокрутка",
                        subtitle = "К последнему сообщению",
                        checked = autoScroll,
                        onCheckedChange = {
                            autoScroll = it
                            AppPrefs.setAutoScroll(context, it)
                        },
                    )

                    SliderSetting(
                        title = "Размер кода",
                        valueLabel = "%.0f sp".format(codeFont),
                        value = codeFont,
                        range = 10f..18f,
                        steps = 7,
                    ) {
                        codeFont = it
                        AppPrefs.setCodeFontSize(context, it)
                    }

                    SwitchSetting(
                        title = "Вибрация при отправке",
                        subtitle = "Короткий отклик на кнопку отправки",
                        checked = hapticFeedback,
                        onCheckedChange = {
                            hapticFeedback = it
                            AppPrefs.setHapticFeedback(context, it)
                        },
                    )

                    SwitchSetting(
                        title = "Подтверждать удаление чата",
                        subtitle = "Спрашивать перед удалением сессии",
                        checked = confirmDelete,
                        onCheckedChange = {
                            confirmDelete = it
                            AppPrefs.setConfirmDelete(context, it)
                        },
                    )
                }
            }

            SettingsCategory.LOOK -> {
                SettingsSection("Оформление") {
                    ActionRow(
                        icon = Icons.Default.Star,
                        title = "Тема",
                        subtitle = stringResource(ThemeManager.labelRes(themeId)),
                        onClick = { showTheme = true },
                    )

                    SwitchSetting(
                        title = "Dynamic color",
                        subtitle = "Цвета системы на Android 12+",
                        checked = dynamicColor,
                        onCheckedChange = {
                            dynamicColor = it
                            AppPrefs.setDynamicColor(context, it)
                            activity?.recreate()
                        },
                    )
                }

                SettingsSection("Кастомизация") {
                    Text("Акцентный цвет", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ThemeManager.accents.forEach { option ->
                            FilterChip(
                                selected = accent == option.id,
                                onClick = {
                                    accent = option.id
                                    AppPrefs.setAccent(context, option.id)
                                    activity?.recreate()
                                },
                                label = { Text(option.label, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }

                    Text("Шрифт чата", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(
                            "sans" to "Системный",
                            "serif" to "С засечками",
                            "mono" to "Моноширинный",
                        ).forEach { (id, label) ->
                            FilterChip(
                                selected = chatFontFamily == id,
                                onClick = {
                                    chatFontFamily = id
                                    AppPrefs.setChatFontFamily(context, id)
                                    activity?.recreate()
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }

                    SliderSetting(
                        title = "Ширина сообщений",
                        valueLabel = "${bubbleWidth.roundToInt()} dp",
                        value = bubbleWidth,
                        range = 240f..420f,
                        steps = 5,
                    ) {
                        bubbleWidth = it
                        AppPrefs.setBubbleWidth(context, it.roundToInt())
                    }

                    SwitchSetting(
                        title = "Показывать аватары",
                        subtitle = "Иконки пользователя и DevAI",
                        checked = showAvatars,
                        onCheckedChange = {
                            showAvatars = it
                            AppPrefs.setShowAvatars(context, it)
                        },
                    )

                    SwitchSetting(
                        title = "Компактный чат",
                        subtitle = "Меньше отступов между сообщениями",
                        checked = compactChat,
                        onCheckedChange = {
                            compactChat = it
                            AppPrefs.setCompactChat(context, it)
                        },
                    )
                }
            }

            SettingsCategory.PREMIUM -> {
                SettingsSection("Premium") {
                    Button(
                        onClick = {
                            context.startActivity(Intent(context, PremiumActivity::class.java))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (premium) "Premium активен" else "Открыть Premium")
                    }
                    Text(
                        if (premium) "Премиум-темы, большие офлайн-модели и экспорт чата доступны."
                        else "Премиум-темы, большие офлайн-модели и экспорт чата в Markdown.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SettingsCategory.DATA -> {
                SettingsSection("Данные") {
                    ActionRow(
                        icon = Icons.Default.Delete,
                        title = "Очистить историю чатов",
                        onClick = { showClearHistory = true },
                    )
                    ActionRow(
                        icon = Icons.Default.Share,
                        title = "Поделиться настройками",
                        onClick = { shareSettings() },
                    )
                    ActionRow(
                        icon = Icons.Default.Lock,
                        title = "Сбросить API-ключи",
                        onClick = { showClearKeys = true },
                    )
                    ActionRow(
                        icon = Icons.Default.Refresh,
                        title = "Сбросить настройки",
                        onClick = { showReset = true },
                    )
                }
            }

            SettingsCategory.ABOUT -> {
                SettingsSection("О приложении") {
                    Text(
                        "DevAI Studio " + appVersion(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    ActionRow(
                        icon = Icons.Default.Info,
                        title = "Обновления",
                        subtitle = "GitHub Releases",
                        onClick = {
                            openUrl("https://github.com/Karishka-sr/devai-studio/releases")
                        },
                    )
                    ActionRow(
                        icon = Icons.Default.Email,
                        title = "Telegram",
                        subtitle = "@NeonBeatt",
                        onClick = { openUrl(context.getString(R.string.author_telegram_url)) },
                    )
                    ActionRow(
                        icon = Icons.Default.Share,
                        title = "Сайт",
                        subtitle = "rytmix-live",
                        onClick = { openUrl(context.getString(R.string.author_site_url)) },
                    )
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "Автор",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Miko",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Vexel / DevAI Studio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.settings_author_credit),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                openUrl(context.getString(R.string.author_vexel_url))
                            },
                        )
                        Text(
                            "Конструктор Vexel не изменялся. Оригинал: t.me/cheburek_dev",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Text(
            text = "DevAI Studio " + appVersion(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showTheme) {
        AlertDialog(
            onDismissRequest = { showTheme = false },
            title = { Text("Оформление") },
            text = {
                Column {
                    ThemeManager.all.forEach { id ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { chooseTheme(id) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = themeId == id,
                                onClick = { chooseTheme(id) },
                            )
                            Text(stringResource(ThemeManager.labelRes(id)))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTheme = false }) { Text("Закрыть") }
            },
        )
    }

    if (showClearKeys) {
        AlertDialog(
            onDismissRequest = { showClearKeys = false },
            title = { Text("Сбросить все ключи?") },
            text = { Text("API-ключи будут удалены с устройства.") },
            confirmButton = {
                TextButton(onClick = {
                    ApiKeyStore.clearAll(context)
                    showClearKeys = false
                    toast(context.getString(R.string.settings_clear_confirmed))
                }) { Text("Да") }
            },
            dismissButton = {
                TextButton(onClick = { showClearKeys = false }) { Text("Отмена") }
            },
        )
    }

    if (showClearHistory) {
        AlertDialog(
            onDismissRequest = { showClearHistory = false },
            title = { Text("Очистить историю чатов?") },
            text = { Text("Все сессии и сообщения будут удалены с устройства.") },
            confirmButton = {
                TextButton(onClick = {
                    ChatSessionsStore.clearAll(context)
                    showClearHistory = false
                    toast("История чатов очищена")
                }) { Text("Очистить") }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistory = false }) { Text("Отмена") }
            },
        )
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Сбросить настройки?") },
            text = { Text("Параметры генерации, чата и оформления вернутся к значениям по умолчанию. API-ключи и чаты останутся.") },
            confirmButton = {
                TextButton(onClick = {
                    AppPrefs.reset(context)
                    showReset = false
                    activity?.recreate()
                }) { Text("Сбросить") }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            content()
        }
    }
}

@Composable
private fun SliderSetting(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                valueLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
        )
    }
}

@Composable
private fun CompactNumberSetting(
    label: String,
    value: String,
    supporting: String,
    onChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(108.dp),
        )
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
