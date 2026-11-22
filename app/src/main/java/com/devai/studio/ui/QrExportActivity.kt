package com.devai.studio.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devai.studio.ai.AiFormat
import com.devai.studio.data.ApiKeyStore
import com.devai.studio.data.AppPrefs
import com.devai.studio.util.AiSupport
import com.devai.studio.util.QrUtils
import com.devai.studio.util.ThemeManager
import org.json.JSONObject

class QrExportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        ThemeManager.applyDynamicIfAvailable(this)
        setContent { DevAITheme { QrExportScreen(onBack = { finish() }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrExportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val provider = AiSupport.provider(context)
    var includeKey by remember { mutableStateOf(false) }

    val payload = remember(provider, includeKey) {
        if (provider == null) ""
        else {
            val base = if (provider.id == "custom") AppPrefs.customBase(context) else provider.baseUrl
            val model = AiSupport.model(context, provider)
            JSONObject().apply {
                put("app", "DevAI Studio")
                put("type", "provider")
                put("version", 1)
                put("id", provider.id)
                put("name", provider.name)
                put("format", provider.format.name)
                put("baseUrl", base)
                put("model", model)
                if (includeKey && provider.format != AiFormat.LOCAL) {
                    ApiKeyStore.get(context, provider.id)?.takeIf { it.isNotBlank() }?.let {
                        put("apiKey", it)
                    }
                }
            }.toString(2)
        }
    }
    val bitmap = remember(payload) {
        if (payload.isBlank()) null else QrUtils.make(payload, 720)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR-экспорт провайдера") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (provider == null) {
                Text("Сначала выберите провайдера во вкладке «ИИ».")
                return@Column
            }
            Text(
                "Активный провайдер: ${provider.name}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "QR содержит base URL, модель и формат. API-ключ по умолчанию не включается.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Включить API-ключ")
                    Text(
                        "Небезопасно: ключ будет виден на экране и в QR.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Switch(checked = includeKey, onCheckedChange = { includeKey = it })
            }
            Card(Modifier.fillMaxWidth()) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QR-код провайдера",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(12.dp),
                    )
                } else {
                    Text(
                        "Не удалось создать QR: слишком много данных.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            Button(
                onClick = { clipboard.setText(AnnotatedString(payload)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Скопировать JSON")
            }
            Text(
                payload,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
