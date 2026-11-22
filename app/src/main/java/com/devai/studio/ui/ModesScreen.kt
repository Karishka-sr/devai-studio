package com.devai.studio.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devai.studio.data.ChatMode

@Composable
fun ModesScreen(
    current: ChatMode,
    onSelect: (ChatMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        DevAITopBar(
            title = "Режимы",
            subtitle = "Выбери, как работать с DevAI",
        )
        DevAISectionTitle(
            title = "Стиль общения",
            subtitle = "Режим меняет системный промпт и подсказки в чате.",
            icon = Icons.AutoMirrored.Filled.List,
        )

        ModeCard(
            mode = ChatMode.CHAT,
            icon = Icons.Default.Face,
            selected = current == ChatMode.CHAT,
            accent = MaterialTheme.colorScheme.primary,
            onSelect = onSelect,
        )
        ModeCard(
            mode = ChatMode.DEV,
            icon = Icons.Default.Build,
            selected = current == ChatMode.DEV,
            accent = MaterialTheme.colorScheme.secondary,
            onSelect = onSelect,
        )

        DevAICard(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
            accent = MaterialTheme.colorScheme.tertiary,
        ) {
            Row(verticalAlignment = Alignment.Top) {
                DevAIIconTile(Icons.Default.Info, size = 34.dp, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Свой промпт важнее режима",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Если в Настройках задан свой системный промпт, он имеет приоритет над выбранным режимом.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeCard(
    mode: ChatMode,
    icon: ImageVector,
    selected: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onSelect: (ChatMode) -> Unit,
) {
    DevAICard(
        onClick = { onSelect(mode) },
        accent = if (selected) accent else null,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DevAIIconTile(icon, size = 52.dp, tint = accent)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    mode.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    mode.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                DevAIStatusPill("Выбран", accent)
            } else {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                )
            }
        }
    }
}
