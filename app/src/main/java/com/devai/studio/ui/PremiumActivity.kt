package com.devai.studio.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.devai.studio.data.PremiumFeature
import com.devai.studio.data.PremiumPlan
import com.devai.studio.data.PremiumStore
import com.devai.studio.data.PromoClient
import com.devai.studio.data.TelegramStars
import com.devai.studio.util.ThemeManager
import kotlinx.coroutines.launch

class PremiumActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        ThemeManager.applyDynamicIfAvailable(this)
        setContent { DevAITheme { PremiumScreen(onBack = { finish() }) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf(PremiumStore.plan(context)) }
    var selected by remember {
        mutableStateOf(if (current.isPaid) current else PremiumPlan.LIFETIME)
    }
    var code by remember { mutableStateOf("") }
    var activating by remember { mutableStateOf(false) }
    val orders = remember { mutableStateMapOf<String, String>() }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    fun orderFor(plan: PremiumPlan): String =
        orders.getOrPut(plan.id) { TelegramStars.orderId(plan) }

    fun refresh() {
        current = PremiumStore.plan(context)
    }

    DevAIBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                DevAITopBar(
                    title = "Premium",
                    subtitle = "Больше возможностей для DevAI",
                    onBack = onBack,
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
            StatusCard(current)

            Text(
                "Тарифы",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            PremiumPlan.entries.filter { it.isPaid }.forEach { plan ->
                PlanCard(
                    plan = plan,
                    current = current,
                    onPay = {
                        val order = orderFor(plan)
                        if (!TelegramStars.open(context, plan, order)) {
                            TelegramStars.copy(context, plan, order)
                            toast("Telegram не найден. Заказ скопирован.")
                        }
                    },
                    onCopy = {
                        TelegramStars.copy(context, plan, orderFor(plan))
                        toast("Заказ скопирован")
                    },
                )
            }

            PaymentHelpCard()

            DevAICard(
                accent = MaterialTheme.colorScheme.primary,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
            ) {
                    Text(
                        "Активация по коду",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Код приходит в Telegram после оплаты звёздами.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("DVS-… или DEVAI-…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = !activating,
                            onClick = {
                                if (code.isBlank()) {
                                    toast("Введите код")
                                } else {
                                    scope.launch {
                                        activating = true
                                        val result = PromoClient.activate(context, code)
                                        val serverPlan = result.plan
                                        val activated = if (serverPlan != null) {
                                            PremiumStore.activatePlan(
                                                context,
                                                serverPlan,
                                                result.code.ifBlank { code },
                                                result.days,
                                                result.title,
                                            )
                                            serverPlan
                                        } else {
                                            PremiumStore.activate(context, code)
                                        }
                                        activating = false
                                        if (activated == null) {
                                            toast(result.error ?: "Неверный код")
                                        } else {
                                            code = ""
                                            refresh()
                                            selected = activated
                                            toast(
                                                if (result.days > 0 && result.days < activated.days) {
                                                    "Пробный Premium на ${result.days} дн."
                                                } else {
                                                    "Активирован тариф «${activated.title}»"
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (activating) "Проверяем…" else "Активировать")
                        }
                        if (current.isPaid) {
                            OutlinedButton(
                                onClick = {
                                    PremiumStore.deactivate(context)
                                    refresh()
                                    toast("Premium отключён")
                                },
                            ) { Text("Сбросить") }
                        }
                    }
            }

            Text(
                "Функции тарифа",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PremiumPlan.entries.filter { it.isPaid }.forEach { plan ->
                    FilterChip(
                        selected = selected == plan,
                        onClick = { selected = plan },
                        label = { Text(plan.title) },
                    )
                }
            }
            val features = PremiumFeature.forPlan(selected)
            Text(
                "${features.size} функций · тариф «${selected.title}»",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            features.forEach { feature ->
                FeatureRow(feature, selected)
            }

            Text(
                "Оплата звёздами Telegram",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Нажмите «Оплатить», Telegram откроет чат @${TelegramStars.USERNAME} " +
                    "с готовым заказом. После оплаты пришлите заказ — вам выдадут код активации.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = {
                    val order = TelegramStars.orderId(PremiumPlan.LIFETIME)
                    TelegramStars.copy(context, PremiumPlan.LIFETIME, order)
                    toast("Заказ скопирован")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Star, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Скопировать заказ «Навсегда»")
            }
            Spacer(Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun StatusCard(plan: PremiumPlan) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val until = PremiumStore.expiresText(context)
    val left = PremiumStore.daysLeft(context)
    DevAICard(
        accent = if (plan.isPaid) MaterialTheme.colorScheme.primary else null,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DevAIIconTile(
                if (plan.isPaid) Icons.Default.CheckCircle else Icons.Default.Lock,
                size = 48.dp,
                tint = if (plan.isPaid) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (plan.isPaid) "Premium ${PremiumStore.planLabel(context)}" else "Premium не активен",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    when {
                        !plan.isPaid -> "Доступны базовые функции. Выберите тариф ниже."
                        until.isNotBlank() -> "Действует до $until · осталось $left дн."
                        else -> "Доступ навсегда · все функции и будущие обновления."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (PremiumStore.code(context).isNotBlank()) {
            DevAIStatusPill("Код: ${PremiumStore.code(context)}")
        }
    }
}

@Composable
private fun PlanCard(
    plan: PremiumPlan,
    current: PremiumPlan,
    onPay: () -> Unit,
    onCopy: () -> Unit,
) {
    val active = current.rank >= plan.rank
    val accent = if (plan == PremiumPlan.LIFETIME) MaterialTheme.colorScheme.tertiary
    else MaterialTheme.colorScheme.primary
    DevAICard(
        accent = if (plan == PremiumPlan.LIFETIME || active) accent else null,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        plan.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (plan == PremiumPlan.LIFETIME) {
                        Spacer(Modifier.width(8.dp))
                        DevAIStatusPill("Выгодно", MaterialTheme.colorScheme.tertiary)
                    }
                }
                Text(
                    "${plan.duration} · ${PremiumFeature.forPlan(plan).size} функций",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${plan.stars} ⭐",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }
        Text(plan.tagline, style = MaterialTheme.typography.bodySmall)
        if (active) {
            DevAIStatusPill("Уже доступно", accent)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DevAIGradientButton(
                    text = "Оплатить ${plan.stars} ⭐",
                    icon = Icons.Default.Star,
                    modifier = Modifier.weight(1f),
                    onClick = onPay,
                )
                OutlinedButton(onClick = onCopy) { Text("Копировать") }
            }
        }
    }
}

@Composable
private fun PaymentHelpCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    DevAICard(
        accent = MaterialTheme.colorScheme.secondary,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
    ) {
            Text(
                "Как оплатить",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "1. Нажмите «Оплатить» у нужного тарифа.\n" +
                    "2. Telegram откроет чат @${TelegramStars.USERNAME} с заказом.\n" +
                    "3. Отправьте заказ и оплатите звёздами.\n" +
                    "4. Получите код вида DVS-XXXX-XXXX или DEVAI-MONTH.\n" +
                    "5. Введите код в поле активации.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(TelegramStars.URL),
                            )
                        )
                    }
                },
            ) { Text("Открыть @${TelegramStars.USERNAME}") }
    }
}

@Composable
private fun FeatureRow(feature: PremiumFeature, selected: PremiumPlan) {
    val included = selected.rank >= feature.minPlan.rank
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (included) Color.Transparent
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            if (included) Icons.Default.CheckCircle else Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp),
            tint = if (included) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                feature.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                feature.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            if (!feature.available) "скоро" else feature.minPlan.title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
