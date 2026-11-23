package com.devai.studio.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.devai.studio.R
import com.devai.studio.ai.LocalLlmEngine
import com.devai.studio.data.ApiKeyStore
import com.devai.studio.data.AppPrefs
import com.devai.studio.data.MemoryStore
import com.devai.studio.data.ChatSessionsStore
import com.devai.studio.data.CodeLanguages
import com.devai.studio.data.PremiumFeature
import com.devai.studio.data.PremiumPlan
import com.devai.studio.data.PhoneAuthManager
import com.devai.studio.data.PhoneNumbers
import com.devai.studio.data.PremiumStore
import com.devai.studio.data.ProfileStore
import com.devai.studio.data.PromptLibrary
import com.devai.studio.util.PremiumDialogs
import com.devai.studio.util.ThemeManager
import kotlin.math.roundToInt

private enum class SettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val group: String,
    val keywords: String,
) {
    GENERATION(
        "Генерация", "Температура, лимиты и промпт", Icons.Default.Settings, "Основное",
        "температура top-p top p токены лимит история таймаут провайдер base url модель промпт пресет",
    ),
    MODELS(
        "Модели", "Локальные модели и контекст", Icons.Default.PlayArrow, "Основное",
        "локальная модель контекст top-k top k память квантование скачать",
    ),
    CHAT(
        "Чат", "Шрифт, отправка и прокрутка", Icons.AutoMirrored.Filled.Send, "Основное",
        "шрифт размер код перенос отправка enter прокрутка аватары компактный вибрация",
    ),
    LOOK(
        "Вид", "Тема, акцент и аватары", Icons.Default.Star, "Интерфейс",
        "тема оформление акцент цвет динамический аватары пузыри ширина обои",
    ),
    PROFILE(
        "Профиль", "Ник и номер телефона", Icons.Default.Settings, "Аккаунт",
        "профиль ник имя телефон номер регистрация sms аккаунт войти",
    ),
    PREMIUM(
        "Premium", "Тариф и возможности", Icons.Default.Favorite, "Аккаунт",
        "premium тариф подписка оплата возможности месяц год",
    ),
    DATA(
        "Данные", "История, экспорт и сброс", Icons.Default.Lock, "Аккаунт",
        "история чаты экспорт импорт бэкап backup сброс удалить ключи память",
    ),
    ABOUT(
        "О приложении", "Версия, обновления и автор", Icons.Default.Info, "Приложение",
        "версия обновление автор лицензия ссылки приложение",
    ),
}

private enum class QuickToggle {
    DYNAMIC_COLOR,
    HAPTIC,
    CODE_WRAP,
    SEND_ON_ENTER,
    SHOW_AVATARS,
    COMPACT_CHAT,
    AUTO_SCROLL,
    CONFIRM_DELETE,
}

private data class QuickToggles(
    val dynamicColor: Boolean,
    val haptic: Boolean,
    val codeWrap: Boolean,
    val sendOnEnter: Boolean,
    val showAvatars: Boolean,
    val compactChat: Boolean,
    val autoScroll: Boolean,
    val confirmDelete: Boolean,
)

private data class SettingsSummary(
    val temperature: Float,
    val topP: Float,
    val maxTokens: Int,
    val language: String,
    val theme: String,
)

private data class SettingsPreset(
    val name: String,
    val temperature: Float,
    val topP: Float,
    val maxTokens: Int,
)

private val settingsPresets = listOf(
    SettingsPreset("Быстрый", 0.30f, 0.80f, 2048),
    SettingsPreset("Баланс", 0.70f, 0.90f, 4096),
    SettingsPreset("Творческий", 1.00f, 1.00f, 8192),
    SettingsPreset("Локальный", 0.80f, 0.90f, 1024),
    SettingsPreset("Код", 0.20f, 0.80f, 8192),
)

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
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
    var responseStyle by remember { mutableStateOf(AppPrefs.responseStyle(context)) }
    var responseLanguage by remember { mutableStateOf(AppPrefs.responseLanguage(context)) }
    var codeLanguageId by remember { mutableStateOf(AppPrefs.codeLanguage(context)) }
    var memoryText by remember { mutableStateOf("") }
    var memories by remember { mutableStateOf(MemoryStore.list(context)) }
    var nickname by remember { mutableStateOf(ProfileStore.nickname(context)) }
    var phone by remember { mutableStateOf(PhoneNumbers.pretty(ProfileStore.phone(context))) }
    var phoneCode by remember { mutableStateOf("") }
    var phoneRequested by remember { mutableStateOf(false) }
    var phoneBusy by remember { mutableStateOf(false) }
    var phoneStatus by remember {
        mutableStateOf(if (ProfileStore.isVerified(context)) "Номер подтверждён" else "")
    }
    var plan by remember { mutableStateOf(PremiumStore.plan(context)) }
    var categoryId by rememberSaveable { mutableStateOf(SettingsCategory.GENERATION.name) }
    val category = SettingsCategory.valueOf(categoryId)
    var home by rememberSaveable { mutableStateOf(true) }
    var query by rememberSaveable { mutableStateOf("") }
    var showTheme by remember { mutableStateOf(false) }
    var showPromptLibrary by remember { mutableStateOf(false) }
    var showClearKeys by remember { mutableStateOf(false) }
    var showClearHistory by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun requireFeature(feature: PremiumFeature, action: () -> Unit) {
        if (PremiumStore.hasFeature(context, feature)) action()
        else PremiumDialogs.show(context, feature.title, feature.minPlan)
    }

    val importChats = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: error("Пустой файл")
        }.onSuccess { raw ->
            val count = ChatSessionsStore.importJson(context, raw)
            toast(if (count > 0) "Импортировано чатов: $count" else "В файле нет чатов")
        }.onFailure {
            toast("Не удалось импортировать: ${it.message ?: "ошибка"}")
        }
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
        val feature = ThemeManager.featureFor(id)
        if (feature != null && !PremiumStore.hasFeature(context, feature)) {
            showTheme = false
            PremiumDialogs.show(context, feature.title, feature.minPlan)
            return
        }
        AppPrefs.setTheme(context, id)
        themeId = id
        showTheme = false
        DevAIThemeState.refresh()
    }

    fun reloadPrefs() {
        temperature = AppPrefs.temperature(context)
        topP = AppPrefs.topP(context)
        maxTokens = AppPrefs.maxTokens(context).toString()
        historyLimit = AppPrefs.historyLimit(context).toString()
        timeout = AppPrefs.timeoutSeconds(context).toString()
        localContext = AppPrefs.localContextTokens(context).toFloat()
        localTopK = AppPrefs.localTopK(context).toString()
        chatFont = AppPrefs.chatFontSize(context)
        codeFont = AppPrefs.codeFontSize(context)
        codeWrap = AppPrefs.codeWrap(context)
        sendOnEnter = AppPrefs.sendOnEnter(context)
        autoScroll = AppPrefs.autoScroll(context)
        dynamicColor = AppPrefs.dynamicColor(context)
        accent = AppPrefs.accent(context)
        chatFontFamily = AppPrefs.chatFontFamily(context)
        bubbleWidth = AppPrefs.bubbleWidth(context).toFloat()
        showAvatars = AppPrefs.showAvatars(context)
        compactChat = AppPrefs.compactChat(context)
        hapticFeedback = AppPrefs.hapticFeedback(context)
        confirmDelete = AppPrefs.confirmDelete(context)
        customBase = AppPrefs.customBase(context)
        customModel = AppPrefs.customModel(context)
        systemPrompt = AppPrefs.systemPrompt(context)
        themeId = AppPrefs.theme(context)
        responseStyle = AppPrefs.responseStyle(context)
        responseLanguage = AppPrefs.responseLanguage(context)
        codeLanguageId = AppPrefs.codeLanguage(context)
        memories = MemoryStore.list(context)
        plan = PremiumStore.plan(context)
    }

    fun applyQuickPreset(name: String, temp: Float, p: Float, tokens: Int) {
        temperature = temp
        topP = p
        maxTokens = tokens.toString()
        AppPrefs.setTemperature(context, temp)
        AppPrefs.setTopP(context, p)
        AppPrefs.setMaxTokens(context, tokens)
        toast("Профиль «$name»: T %.2f · Top-P %.2f · %d токенов".format(temp, p, tokens))
    }

    fun toggleQuick(key: QuickToggle, value: Boolean) {
        when (key) {
            QuickToggle.DYNAMIC_COLOR -> {
                dynamicColor = value
                AppPrefs.setDynamicColor(context, value)
            }
            QuickToggle.HAPTIC -> {
                hapticFeedback = value
                AppPrefs.setHapticFeedback(context, value)
            }
            QuickToggle.CODE_WRAP -> {
                codeWrap = value
                AppPrefs.setCodeWrap(context, value)
            }
            QuickToggle.SEND_ON_ENTER -> {
                sendOnEnter = value
                AppPrefs.setSendOnEnter(context, value)
            }
            QuickToggle.SHOW_AVATARS -> {
                showAvatars = value
                AppPrefs.setShowAvatars(context, value)
            }
            QuickToggle.COMPACT_CHAT -> {
                compactChat = value
                AppPrefs.setCompactChat(context, value)
            }
            QuickToggle.AUTO_SCROLL -> {
                autoScroll = value
                AppPrefs.setAutoScroll(context, value)
            }
            QuickToggle.CONFIRM_DELETE -> {
                confirmDelete = value
                AppPrefs.setConfirmDelete(context, value)
            }
        }
    }

    fun saveNickname() {
        val value = nickname.trim()
        if (!Regex("^[\\p{L}\\p{N} _.-]{2,32}$").matches(value)) {
            toast("Ник: 2–32 символа, буквы, цифры, пробел, точка, дефис или _")
            return
        }
        ProfileStore.setNickname(context, value)
        nickname = value
        toast("Ник сохранён")
    }

    fun requestPhoneCode() {
        val activity = context as? Activity
        if (activity == null) {
            toast("Не удалось открыть проверку номера")
            return
        }
        phoneBusy = true
        phoneStatus = "Отправляем SMS…"
        PhoneAuthManager.start(
            activity = activity,
            rawPhone = phone,
            onCodeSent = { pretty ->
                phoneBusy = false
                phoneRequested = true
                phone = pretty
                phoneStatus = "Код отправлен на $pretty"
            },
            onError = { message ->
                phoneBusy = false
                phoneStatus = message
                toast(message)
            },
            onSignedIn = { session ->
                phoneBusy = false
                phoneRequested = false
                phone = PhoneNumbers.pretty(session.phone)
                phoneStatus = "Номер подтверждён"
                toast("Регистрация завершена")
            },
        )
    }

    fun confirmPhoneCode() {
        val activity = context as? Activity
        if (activity == null) {
            toast("Не удалось открыть проверку номера")
            return
        }
        phoneBusy = true
        PhoneAuthManager.confirm(
            activity = activity,
            code = phoneCode,
            onSuccess = { session ->
                phoneBusy = false
                phoneRequested = false
                phoneCode = ""
                phone = PhoneNumbers.pretty(session.phone)
                phoneStatus = "Номер подтверждён"
                toast("Регистрация завершена")
            },
            onError = { message ->
                phoneBusy = false
                phoneStatus = message
                toast(message)
            },
        )
    }

    fun signOutPhone() {
        PhoneAuthManager.signOut(context)
        phone = ""
        phoneCode = ""
        phoneRequested = false
        phoneStatus = ""
        toast("Вы вышли из аккаунта")
    }

    fun shareSettings() {
        requireFeature(PremiumFeature.SETTINGS_BACKUP) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "DevAI Studio — настройки")
                putExtra(Intent.EXTRA_TEXT, AppPrefs.exportJson(context))
            }
            context.startActivity(Intent.createChooser(intent, "Поделиться настройками"))
        }
    }

    fun shareChats() {
        requireFeature(PremiumFeature.CHAT_BACKUP) {
            val sessions = ChatSessionsStore.list(context)
            if (sessions.isEmpty()) {
                toast("Чатов пока нет")
                return@requireFeature
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "DevAI Studio — все чаты")
                putExtra(Intent.EXTRA_TEXT, ChatSessionsStore.exportJson(context))
            }
            context.startActivity(Intent.createChooser(intent, "Экспорт всех чатов"))
        }
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
                plan = PremiumStore.plan(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = !home || query.isNotEmpty()) {
        if (!home) home = true else query = ""
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(home, category) { scrollState.scrollTo(0) }

    if (home) {
        SettingsHome(
            plan = plan,
            version = appVersion(),
            nickname = nickname,
            query = query,
            onQueryChange = { query = it },
            summary = SettingsSummary(
                temperature = temperature,
                topP = topP,
                maxTokens = maxTokens.toIntOrNull() ?: AppPrefs.maxTokens(context),
                language = (CodeLanguages.byId(codeLanguageId) ?: CodeLanguages.default).name,
                theme = context.getString(ThemeManager.labelRes(themeId)),
            ),
            quick = QuickToggles(
                dynamicColor = dynamicColor,
                haptic = hapticFeedback,
                codeWrap = codeWrap,
                sendOnEnter = sendOnEnter,
                showAvatars = showAvatars,
                compactChat = compactChat,
                autoScroll = autoScroll,
                confirmDelete = confirmDelete,
            ),
            onPreset = ::applyQuickPreset,
            onToggle = ::toggleQuick,
            onOpen = {
                categoryId = it.name
                home = false
            },
            onPremium = {
                context.startActivity(Intent(context, PremiumActivity::class.java))
            },
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DevAITopBar(
                title = category.title,
                subtitle = category.subtitle,
                onBack = { home = true },
            )

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
                        supporting = "0 — вся история (Premium Год)",
                        onChange = {
                            val value = it.toIntOrNull()
                            if (value == 0 && !PremiumStore.hasFeature(
                                    context,
                                    PremiumFeature.HISTORY_UNLIMITED,
                                )
                            ) {
                                PremiumDialogs.show(
                                    context,
                                    "Вся история",
                                    PremiumFeature.HISTORY_UNLIMITED.minPlan,
                                )
                                return@CompactNumberSetting
                            }
                            historyLimit = it
                            if (value != null && value in 0..500) {
                                AppPrefs.setHistoryLimit(context, value)
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
                    val providerUnlocked =
                        PremiumStore.hasFeature(context, PremiumFeature.CUSTOM_PROVIDER)
                    Box {
                        OutlinedTextField(
                            value = customBase,
                            onValueChange = {
                                customBase = it
                                AppPrefs.setCustomBase(context, it)
                            },
                            label = { Text("Base URL") },
                            singleLine = true,
                            enabled = providerUnlocked,
                            textStyle = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (!providerUnlocked) {
                            LockedFieldOverlay {
                                PremiumDialogs.show(
                                    context,
                                    "Свой провайдер",
                                    PremiumFeature.CUSTOM_PROVIDER.minPlan,
                                )
                            }
                        }
                    }

                    Box {
                        OutlinedTextField(
                            value = customModel,
                            onValueChange = {
                                customModel = it
                                AppPrefs.setCustomModel(context, it)
                            },
                            label = { Text("Модель") },
                            singleLine = true,
                            enabled = providerUnlocked,
                            textStyle = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (!providerUnlocked) {
                            LockedFieldOverlay {
                                PremiumDialogs.show(
                                    context,
                                    "Свой провайдер",
                                    PremiumFeature.CUSTOM_PROVIDER.minPlan,
                                )
                            }
                        }
                    }
                }

                SettingsSection("Системный промпт") {
                    val promptUnlocked =
                        PremiumStore.hasFeature(context, PremiumFeature.SYSTEM_PROMPT)
                    Box {
                        OutlinedTextField(
                            value = systemPrompt,
                            onValueChange = {
                                systemPrompt = it
                                AppPrefs.setSystemPrompt(context, it)
                            },
                            label = { Text("Промпт") },
                            minLines = 3,
                            maxLines = 5,
                            enabled = promptUnlocked,
                            textStyle = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (!promptUnlocked) {
                            LockedFieldOverlay {
                                PremiumDialogs.show(
                                    context,
                                    "Свой системный промпт",
                                    PremiumFeature.SYSTEM_PROMPT.minPlan,
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        promptPresets.forEach { (label, prompt) ->
                            AssistChip(
                                onClick = {
                                    requireFeature(PremiumFeature.PROMPT_PRESETS) {
                                        systemPrompt = prompt
                                        AppPrefs.setSystemPrompt(context, prompt)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                label = { Text(label) },
                            )
                        }
                    }

                    ActionRow(
                        icon = Icons.AutoMirrored.Filled.List,
                        title = "Библиотека промптов",
                        subtitle = "${PromptLibrary.items.size} готовых промптов",
                        onClick = {
                            requireFeature(PremiumFeature.PROMPT_LIBRARY) {
                                showPromptLibrary = true
                            }
                        },
                    )
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
                SettingsSection("Сообщения") {
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

                    SliderSetting(
                        title = "Размер кода",
                        valueLabel = "%.0f sp".format(codeFont),
                        value = codeFont,
                        range = 10f..18f,
                        steps = 7,
                    ) {
                        requireFeature(PremiumFeature.CODE_FONT) {
                            codeFont = it
                            AppPrefs.setCodeFontSize(context, it)
                        }
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
                }

                SettingsSection("Стиль ответа") {
                    val styles = listOf(
                        "balanced" to "Баланс",
                        "concise" to "Кратко",
                        "detailed" to "Подробно",
                        "friendly" to "Дружелюбно",
                        "formal" to "Официально",
                        "creative" to "Креативно",
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        styles.forEach { (id, label) ->
                            FilterChip(
                                selected = responseStyle == id,
                                onClick = {
                                    responseStyle = id
                                    AppPrefs.setResponseStyle(context, id)
                                },
                                label = { Text(label) },
                            )
                        }
                    }
                    Text(
                        "Стиль добавляется к системному промпту и влияет на длину и тон ответа.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SettingsSection("Язык ответа") {
                    val languages = listOf(
                        "ru" to "Русский",
                        "en" to "English",
                        "auto" to "Как в запросе",
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        languages.forEach { (id, label) ->
                            FilterChip(
                                selected = responseLanguage == id,
                                onClick = {
                                    responseLanguage = id
                                    AppPrefs.setResponseLanguage(context, id)
                                },
                                label = { Text(label) },
                            )
                        }
                    }
                }

                SettingsSection("Язык кода") {
                    ActionRow(
                        icon = Icons.Default.Build,
                        title = "Основной язык",
                        subtitle = (CodeLanguages.byId(codeLanguageId) ?: CodeLanguages.default).let {
                            "${it.name} · .${it.extension} · ${it.testFramework}"
                        },
                        onClick = {
                            context.startActivity(Intent(context, CodeLanguagesActivity::class.java))
                        },
                    )
                    Text(
                        "Язык подставляется в системный промпт режима «Разработка». " +
                            "Доступно ${CodeLanguages.all.size} языков.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SettingsSection("Память") {
                    Text(
                        "Факты о вас, которые DevAI будет учитывать в каждом чате. Хранятся только на телефоне.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = memoryText,
                        onValueChange = { memoryText = it },
                        label = { Text("Новый факт") },
                        placeholder = { Text("Например: пишу на Kotlin, проект DevAI Studio") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (MemoryStore.add(context, memoryText)) {
                                    memories = MemoryStore.list(context)
                                    memoryText = ""
                                    toast("Факт добавлен в память")
                                } else {
                                    toast("Пустой факт или уже есть в памяти")
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("Добавить") }
                        OutlinedButton(
                            onClick = {
                                MemoryStore.clear(context)
                                memories = emptyList()
                                toast("Память очищена")
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("Очистить") }
                    }
                    memories.forEach { fact ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                fact,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            IconButton(
                                onClick = {
                                    MemoryStore.remove(context, fact)
                                    memories = MemoryStore.list(context)
                                },
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить факт")
                            }
                        }
                    }
                }

                SettingsSection("Поведение") {
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

                SettingsSection("Отклик") {
                    SwitchSetting(
                        title = "Вибрация при отправке",
                        subtitle = "Короткий отклик на кнопку отправки",
                        checked = hapticFeedback,
                        onCheckedChange = {
                            requireFeature(PremiumFeature.HAPTIC) {
                                hapticFeedback = it
                                AppPrefs.setHapticFeedback(context, it)
                            }
                        },
                    )
                }
            }

            SettingsCategory.LOOK -> {
                SettingsSection("Оформление") {
                    ThemeChooserRow(
                        current = themeId,
                        onChoose = { chooseTheme(it) },
                    )

                    ActionRow(
                        icon = Icons.Default.Star,
                        title = "Все темы",
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
                            DevAIThemeState.refresh()
                        },
                    )

                    ActionRow(
                        icon = Icons.Default.Star,
                        title = "Обои чата",
                        subtitle = "${ChatWallpapers.all.size} вариантов · " +
                            ChatWallpapers.title(AppPrefs.chatWallpaper(context)),
                        onClick = {
                            context.startActivity(Intent(context, WallpaperActivity::class.java))
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
                                    requireFeature(PremiumFeature.ACCENT_COLORS) {
                                        accent = option.id
                                        AppPrefs.setAccent(context, option.id)
                                        DevAIThemeState.refresh()
                                    }
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
                                    requireFeature(PremiumFeature.CHAT_FONTS) {
                                        chatFontFamily = id
                                        AppPrefs.setChatFontFamily(context, id)
                                        DevAIThemeState.refresh()
                                    }
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
                        requireFeature(PremiumFeature.BUBBLE_WIDTH) {
                            bubbleWidth = it
                            AppPrefs.setBubbleWidth(context, it.roundToInt())
                        }
                    }

                    SwitchSetting(
                        title = "Показывать аватары",
                        subtitle = "Иконки пользователя и DevAI",
                        checked = showAvatars,
                        onCheckedChange = {
                            requireFeature(PremiumFeature.AVATARS) {
                                showAvatars = it
                                AppPrefs.setShowAvatars(context, it)
                            }
                        },
                    )

                    SwitchSetting(
                        title = "Компактный чат",
                        subtitle = "Меньше отступов между сообщениями",
                        checked = compactChat,
                        onCheckedChange = {
                            requireFeature(PremiumFeature.COMPACT_CHAT) {
                                compactChat = it
                                AppPrefs.setCompactChat(context, it)
                            }
                        },
                    )
                }
            }

            SettingsCategory.PROFILE -> {
                SettingsSection("Профиль") {
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it.take(32) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ник") },
                        supportingText = { Text("2–32 символа: буквы, цифры, пробел, точка, дефис или _") },
                        singleLine = true,
                    )
                    Button(
                        onClick = ::saveNickname,
                        enabled = nickname.trim().length >= 2,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Сохранить ник")
                    }
                    Text(
                        "Ник показывается в профиле и на главном экране настроек.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SettingsSection("Телефон") {
                    val verified = ProfileStore.isVerified(context)
                    if (verified) {
                        Text(
                            "Номер подтверждён",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            PhoneNumbers.pretty(ProfileStore.phone(context)),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "Аккаунт привязан к Firebase uid. Номер хранится в зашифрованных prefs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = ::signOutPhone,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Выйти из аккаунта")
                        }
                    } else {
                        if (!PhoneAuthManager.isConfigured()) {
                            Text(
                                "SMS-регистрация не настроена",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                "Добавьте FIREBASE_API_KEY, FIREBASE_APP_ID и FIREBASE_PROJECT_ID " +
                                    "в local.properties. Инструкция: docs/PHONE_AUTH.md",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it.take(24) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Номер телефона") },
                            placeholder = { Text("+7 999 123-45-67") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            enabled = !phoneBusy,
                        )
                        if (!phoneRequested) {
                            Button(
                                onClick = ::requestPhoneCode,
                                enabled = PhoneAuthManager.isConfigured() && !phoneBusy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (phoneBusy) "Отправляем…" else "Получить код по SMS")
                            }
                        } else {
                            OutlinedTextField(
                                value = phoneCode,
                                onValueChange = { phoneCode = it.filter(Char::isDigit).take(6) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Код из SMS") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                enabled = !phoneBusy,
                            )
                            Button(
                                onClick = ::confirmPhoneCode,
                                enabled = !phoneBusy && phoneCode.length == 6,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (phoneBusy) "Проверяем…" else "Подтвердить номер")
                            }
                            TextButton(
                                onClick = {
                                    phoneRequested = false
                                    phoneCode = ""
                                    phoneStatus = ""
                                },
                            ) {
                                Text("Изменить номер")
                            }
                        }
                    }
                    if (phoneStatus.isNotBlank()) {
                        Text(
                            phoneStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                        Text(
                            if (plan.isPaid) "Premium ${plan.title} активен"
                            else "Открыть Premium"
                        )
                    }
                    Text(
                        if (plan.isPaid) {
                            val until = PremiumStore.expiresText(context)
                            val left = PremiumStore.daysLeft(context)
                            "Открыто функций: ${PremiumFeature.forPlan(plan).size} из ${PremiumFeature.entries.size}." +
                                if (until.isNotBlank()) " Действует до $until ($left дн.)." else " Навсегда."
                        } else {
                            "4 тарифа: Неделя, Месяц, Год и Навсегда. " +
                                "На «Навсегда» — все ${PremiumFeature.entries.size} функций и будущие обновления."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SettingsCategory.DATA -> {
                SettingsSection("История") {
                    ActionRow(
                        icon = Icons.Default.Delete,
                        title = "Очистить историю чатов",
                        subtitle = "Удалить все сессии и сообщения",
                        onClick = { showClearHistory = true },
                    )
                }

                SettingsSection("Резервные копии") {
                    ActionRow(
                        icon = Icons.Default.Share,
                        title = "Поделиться настройками",
                        subtitle = "Premium Год",
                        onClick = { shareSettings() },
                    )
                    ActionRow(
                        icon = Icons.Default.Share,
                        title = "Экспорт всех чатов",
                        subtitle = "Premium Год · JSON",
                        onClick = { shareChats() },
                    )
                    ActionRow(
                        icon = Icons.AutoMirrored.Filled.List,
                        title = "Импорт чатов",
                        subtitle = "Premium Год · JSON",
                        onClick = {
                            requireFeature(PremiumFeature.CHAT_BACKUP) {
                                importChats.launch(arrayOf("application/json", "text/plain", "*/*"))
                            }
                        },
                    )
                    ActionRow(
                        icon = Icons.Default.Share,
                        title = "QR-перенос настроек",
                        subtitle = "Premium Год",
                        onClick = {
                            requireFeature(PremiumFeature.QR_EXPORT) {
                                context.startActivity(Intent(context, QrExportActivity::class.java))
                            }
                        },
                    )
                }

                SettingsSection("Сброс") {
                    ActionRow(
                        icon = Icons.Default.Lock,
                        title = "Сбросить API-ключи",
                        subtitle = "Удалить сохранённые ключи",
                        onClick = { showClearKeys = true },
                    )
                    ActionRow(
                        icon = Icons.Default.Refresh,
                        title = "Сбросить настройки",
                        subtitle = "Вернуть значения по умолчанию",
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

                DevAICard(
                    accent = MaterialTheme.colorScheme.tertiary,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
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

            Text(
                text = "DevAI Studio " + appVersion(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
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

    if (showPromptLibrary) {
        AlertDialog(
            onDismissRequest = { showPromptLibrary = false },
            title = { Text("Библиотека промптов") },
            text = {
                var promptQuery by remember { mutableStateOf("") }
                val promptItems = remember(promptQuery) { PromptLibrary.search(promptQuery) }
                Column(
                    modifier = Modifier
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    OutlinedTextField(
                        value = promptQuery,
                        onValueChange = { promptQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Поиск: язык или задача") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    )
                    Text(
                        "${promptItems.size} из ${PromptLibrary.items.size} промптов",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(promptItems) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        systemPrompt = item.prompt
                                        AppPrefs.setSystemPrompt(context, item.prompt)
                                        showPromptLibrary = false
                                        toast("Промпт «${item.title}» установлен")
                                    }
                                    .padding(vertical = 8.dp),
                            ) {
                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    item.prompt.take(90) + "…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPromptLibrary = false }) { Text("Закрыть") }
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
                    reloadPrefs()
                    showReset = false
                    DevAIThemeState.refresh()
                    toast("Настройки сброшены")
                }) { Text("Сбросить") }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun SettingsHome(
    plan: PremiumPlan,
    version: String,
    nickname: String,
    query: String,
    onQueryChange: (String) -> Unit,
    summary: SettingsSummary,
    quick: QuickToggles,
    onPreset: (String, Float, Float, Int) -> Unit,
    onToggle: (QuickToggle, Boolean) -> Unit,
    onOpen: (SettingsCategory) -> Unit,
    onPremium: () -> Unit,
) {
    val filtered = SettingsCategory.entries.filter { item ->
        query.isBlank() ||
            item.title.contains(query, ignoreCase = true) ||
            item.subtitle.contains(query, ignoreCase = true) ||
            item.keywords.contains(query, ignoreCase = true)
    }
    val groups = listOf("Основное", "Интерфейс", "Аккаунт", "Приложение")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DevAITopBar(
            title = "Настройки",
            subtitle = "DevAI Studio · v$version",
            actions = {
                DevAIStatusPill(
                    if (plan.isPaid) "Premium" else "Free",
                    accent = if (plan.isPaid) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary,
                )
            },
        )

        DevAICard(
            onClick = onPremium,
            accent = MaterialTheme.colorScheme.primary,
            contentPadding = PaddingValues(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DevAILogo(54.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        nickname,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "DevAI Studio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    DevAIStatusPill(
                        if (plan.isPaid) "Premium · ${plan.title}" else "Free",
                        accent = if (plan.isPaid) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.primary,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "Настройте генерацию, чат и оформление под себя.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DevAICard(contentPadding = PaddingValues(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Сейчас",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                DevAIStatusPill("Live", accent = MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "T %.2f · Top-P %.2f · %d токенов".format(
                    summary.temperature,
                    summary.topP,
                    summary.maxTokens,
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "Язык: ${summary.language} · Тема: ${summary.theme}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Поиск: температура, тема, модель…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Очистить")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
        )

        if (query.isBlank()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("температура", "тема", "модель", "язык", "данные").forEach { term ->
                    AssistChip(
                        onClick = { onQueryChange(term) },
                        label = { Text(term) },
                    )
                }
            }

            SettingsSection("Быстрые настройки") {
                Text(
                    "Один тап меняет профиль генерации.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    settingsPresets.forEach { preset ->
                        AssistChip(
                            onClick = {
                                onPreset(preset.name, preset.temperature, preset.topP, preset.maxTokens)
                            },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                            label = { Text(preset.name) },
                        )
                    }
                }
                QuickToggleRow("Динамические цвета", quick.dynamicColor) {
                    onToggle(QuickToggle.DYNAMIC_COLOR, it)
                }
                QuickToggleRow("Вибрация", quick.haptic) {
                    onToggle(QuickToggle.HAPTIC, it)
                }
                QuickToggleRow("Перенос кода", quick.codeWrap) {
                    onToggle(QuickToggle.CODE_WRAP, it)
                }
                QuickToggleRow("Enter отправляет", quick.sendOnEnter) {
                    onToggle(QuickToggle.SEND_ON_ENTER, it)
                }
                QuickToggleRow("Показывать аватары", quick.showAvatars) {
                    onToggle(QuickToggle.SHOW_AVATARS, it)
                }
                QuickToggleRow("Компактный чат", quick.compactChat) {
                    onToggle(QuickToggle.COMPACT_CHAT, it)
                }
                QuickToggleRow("Автопрокрутка", quick.autoScroll) {
                    onToggle(QuickToggle.AUTO_SCROLL, it)
                }
                QuickToggleRow("Подтверждать удаление", quick.confirmDelete) {
                    onToggle(QuickToggle.CONFIRM_DELETE, it)
                }
            }
        }

        if (filtered.isEmpty()) {
            DevAICard(contentPadding = PaddingValues(18.dp)) {
                Text(
                    "Ничего не найдено",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Попробуйте «температура», «тема», «модели» или «данные».",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (query.isNotBlank()) {
            SettingsGroup("Результаты · ${filtered.size}", filtered, plan, onOpen)
        } else {
            groups.forEach { group ->
                val items = filtered.filter { it.group == group }
                if (items.isNotEmpty()) SettingsGroup(group, items, plan, onOpen)
            }
        }

        Text(
            text = "DevAI Studio $version",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun QuickToggleRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    items: List<SettingsCategory>,
    plan: PremiumPlan,
    onOpen: (SettingsCategory) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )
        DevAICard(contentPadding = PaddingValues(0.dp)) {
            Column {
                items.forEachIndexed { index, item ->
                    SettingsNavRow(item, plan, onClick = { onOpen(item) })
                    if (index != items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 66.dp, end = 14.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsNavRow(
    item: SettingsCategory,
    plan: PremiumPlan,
    onClick: () -> Unit,
) {
    val tint = when (item) {
        SettingsCategory.GENERATION, SettingsCategory.ABOUT -> MaterialTheme.colorScheme.primary
        SettingsCategory.MODELS -> MaterialTheme.colorScheme.secondary
        SettingsCategory.CHAT -> MaterialTheme.colorScheme.primary
        SettingsCategory.LOOK, SettingsCategory.PROFILE, SettingsCategory.PREMIUM ->
            MaterialTheme.colorScheme.tertiary
        SettingsCategory.DATA -> MaterialTheme.colorScheme.secondary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DevAIIconTile(item.icon, size = 40.dp, tint = tint)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (item == SettingsCategory.PREMIUM) {
            Spacer(Modifier.width(8.dp))
            DevAIStatusPill(
                if (plan.isPaid) "Активен" else "Free",
                accent = if (plan.isPaid) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.width(6.dp))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ThemeChooserRow(current: String, onChoose: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeManager.all.forEach { id ->
            val selected = id == current
            val accent = when (id) {
                ThemeManager.EXPRESSIVE -> Color(0xFF7C4DFF)
                ThemeManager.RETRO -> Color(0xFF1565C0)
                else -> MaterialTheme.colorScheme.primary
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onChoose(id) },
                shape = RoundedCornerShape(16.dp),
                color = if (selected) accent.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(
                    if (selected) 1.5.dp else 1.dp,
                    if (selected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            Modifier.size(14.dp).clip(CircleShape).background(accent)
                        )
                        Box(
                            Modifier.size(14.dp).clip(CircleShape)
                                .background(accent.copy(alpha = 0.55f))
                        )
                        Box(
                            Modifier.size(14.dp).clip(CircleShape)
                                .background(accent.copy(alpha = 0.25f))
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when (id) {
                            ThemeManager.EXPRESSIVE -> "Expressive"
                            ThemeManager.RETRO -> "Классика"
                            else -> "Aurora"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val icon = when (title) {
        "Параметры запроса", "Быстрые настройки", "Профиль", "Телефон" -> Icons.Default.Settings
        "Свой провайдер" -> Icons.Default.Share
        "Системный промпт" -> Icons.Default.Edit
        "Локальные модели" -> Icons.Default.PlayArrow
        "Чат", "Сообщения" -> Icons.AutoMirrored.Filled.Send
        "Поведение" -> Icons.Default.Settings
        "Отклик" -> Icons.Default.Star
        "Оформление", "Кастомизация" -> Icons.Default.Star
        "Premium" -> Icons.Default.Favorite
        "Данные" -> Icons.Default.Lock
        "История" -> Icons.Default.Delete
        "Резервные копии" -> Icons.Default.Share
        "Сброс" -> Icons.Default.Refresh
        else -> Icons.Default.Info
    }
    val tint = when (title) {
        "Параметры запроса", "Быстрые настройки", "Профиль", "Телефон", "Системный промпт" ->
            MaterialTheme.colorScheme.primary
        "Локальные модели", "Чат", "Сообщения", "Поведение", "Резервные копии" ->
            MaterialTheme.colorScheme.secondary
        "Оформление", "Кастомизация", "Premium", "Отклик", "Сброс" ->
            MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    DevAICard(
        accent = tint,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
    ) {
        DevAISectionTitle(title = title, icon = icon)
        content()
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
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BoxScope.LockedFieldOverlay(onClick: () -> Unit) {
    Box(
        Modifier
            .matchParentSize()
            .clickable(onClick = onClick)
    )
}
