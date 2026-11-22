package com.devai.studio.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.devai.studio.R
import com.devai.studio.data.ApiKeyStore
import com.devai.studio.data.AppPrefs
import com.devai.studio.data.PremiumStore
import com.devai.studio.util.PremiumDialogs
import com.devai.studio.util.ThemeManager

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var temperature by remember { mutableFloatStateOf(AppPrefs.temperature(context)) }
    var maxTokens by remember { mutableStateOf(AppPrefs.maxTokens(context).toString()) }
    var customBase by remember { mutableStateOf(AppPrefs.customBase(context)) }
    var customModel by remember { mutableStateOf(AppPrefs.customModel(context)) }
    var systemPrompt by remember { mutableStateOf(AppPrefs.systemPrompt(context)) }
    var themeId by remember { mutableStateOf(AppPrefs.theme(context)) }
    var premium by remember { mutableStateOf(PremiumStore.isPremium(context)) }
    var showTheme by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

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

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                premium = PremiumStore.isPremium(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Настройки", style = MaterialTheme.typography.headlineMedium)

        Text(
            text = "Температура: " + "%.2f".format(temperature),
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = temperature,
            onValueChange = {
                temperature = it
                AppPrefs.setTemperature(context, it)
            },
            valueRange = 0f..1f,
            steps = 19,
        )

        OutlinedTextField(
            value = maxTokens,
            onValueChange = {
                maxTokens = it
                it.toIntOrNull()?.let { value ->
                    if (value in 1..100000) AppPrefs.setMaxTokens(context, value)
                }
            },
            label = { Text("Макс. токенов") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = customBase,
            onValueChange = {
                customBase = it
                AppPrefs.setCustomBase(context, it)
            },
            label = { Text("Свой base URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = customModel,
            onValueChange = {
                customModel = it
                AppPrefs.setCustomModel(context, it)
            },
            label = { Text("Своя модель") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = systemPrompt,
            onValueChange = {
                systemPrompt = it
                AppPrefs.setSystemPrompt(context, it)
            },
            label = { Text("Системный промпт") },
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedButton(
            onClick = { showTheme = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Оформление: " + stringResource(ThemeManager.labelRes(themeId)))
        }

        Button(
            onClick = {
                context.startActivity(Intent(context, PremiumActivity::class.java))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (premium) "Premium активен" else "Premium")
        }

        Card(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Автор", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Miko",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Vexel / DevAI Studio",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row {
                    TextButton(onClick = { openUrl(context.getString(R.string.author_telegram_url)) }) {
                        Text("Telegram")
                    }
                    TextButton(onClick = { openUrl(context.getString(R.string.author_site_url)) }) {
                        Text("Сайт")
                    }
                }
                Text(
                    text = stringResource(R.string.settings_author_credit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        openUrl(context.getString(R.string.author_vexel_url))
                    },
                )
            }
        }

        AuthorArtBanner()

        OutlinedButton(
            onClick = { showClear = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сбросить все ключи")
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
                                .padding(vertical = 8.dp),
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

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("Сбросить все ключи?") },
            text = { Text("API-ключи будут удалены с устройства.") },
            confirmButton = {
                TextButton(onClick = {
                    ApiKeyStore.clearAll(context)
                    showClear = false
                    toast(context.getString(R.string.settings_clear_confirmed))
                }) { Text("Да") }
            },
            dismissButton = {
                TextButton(onClick = { showClear = false }) { Text("Отмена") }
            },
        )
    }
}
