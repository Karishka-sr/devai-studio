package com.devai.studio.ui

import android.content.Context
import android.net.Uri
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import com.devai.studio.R
import com.devai.studio.data.ApiKeyStore
import com.devai.studio.data.ProviderInfo
import com.devai.studio.util.AndroidUtils

object CommonDialogs {

    fun keyDialog(ctx: Context, provider: ProviderInfo, onDone: () -> Unit) {
        val edit = EditText(ctx).apply {
            hint = "API-ключ"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(ApiKeyStore.get(ctx, provider.id).orEmpty())
            setSelection(text.length)
        }
        AlertDialog.Builder(ctx)
            .setTitle("Ключ — " + provider.name)
            .setView(padded(ctx, edit))
            .setPositiveButton("Сохранить") { _, _ ->
                val key = edit.text.toString().trim()
                if (key.isEmpty()) ApiKeyStore.clear(ctx, provider.id)
                else ApiKeyStore.save(ctx, provider.id, key)
                onDone()
            }
            .setNeutralButton("Получить ключ") { _, _ -> AndroidUtils.openUrl(ctx, provider.getKeyUrl) }
            .setNegativeButton("Отмена", null)
            .show()
    }

    fun modelDialog(
        ctx: Context,
        provider: ProviderInfo,
        current: String,
        extra: List<String> = emptyList(),
        onFetch: (() -> Unit)? = null,
        onChosen: (String) -> Unit
    ) {
        val models = (provider.models + extra).distinct()
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 20), dp(ctx, 8), dp(ctx, 20), dp(ctx, 8))
        }
        val group = RadioGroup(ctx).apply { orientation = RadioGroup.VERTICAL }
        val buttons = mutableListOf<RadioButton>()
        models.forEachIndexed { i, m ->
            val rb = RadioButton(ctx).apply {
                id = i + 1
                text = m
                isChecked = (m == current)
            }
            group.addView(rb)
            buttons.add(rb)
        }
        container.addView(group)

        val custom = EditText(ctx).apply {
            hint = "Своя модель"
            setText(if (models.contains(current)) "" else current)
        }
        container.addView(custom)

        val scroll = ScrollView(ctx).apply { addView(container) }

        val builder = AlertDialog.Builder(ctx)
            .setTitle("Модель — " + provider.name)
            .setView(scroll)
            .setPositiveButton("Выбрать") { _, _ ->
                val chosen = if (custom.text.isNotBlank()) custom.text.toString().trim()
                else buttons.firstOrNull { it.isChecked }?.text?.toString().orEmpty()
                if (chosen.isNotBlank()) onChosen(chosen)
            }
            .setNegativeButton("Отмена", null)
        if (onFetch != null) {
            builder.setNeutralButton(R.string.model_fetch) { _, _ -> onFetch() }
        }
        builder.show()
    }

    fun resultDialog(ctx: Context, title: String, message: String, uri: Uri?, mime: String) {
        val builder = AlertDialog.Builder(ctx)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Готово", null)
        if (uri != null) {
            builder.setNeutralButton("Поделиться") { _, _ -> AndroidUtils.share(ctx, uri, mime) }
        }
        builder.show()
    }

    private fun padded(ctx: Context, view: android.view.View): android.view.View {
        val wrap = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 20), dp(ctx, 8), dp(ctx, 20), dp(ctx, 8))
            addView(view)
        }
        return wrap
    }

    private fun dp(ctx: Context, v: Int) = (v * ctx.resources.displayMetrics.density).toInt()
}