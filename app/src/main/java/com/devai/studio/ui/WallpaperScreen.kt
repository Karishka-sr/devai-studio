package com.devai.studio.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devai.studio.data.AppPrefs
import kotlin.math.roundToInt

@Composable
fun WallpaperScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var selected by rememberSaveable { mutableStateOf(AppPrefs.chatWallpaper(context)) }
    var dim by remember { mutableFloatStateOf(AppPrefs.chatWallpaperDim(context)) }

    fun choose(id: String) {
        selected = id
        AppPrefs.setChatWallpaper(context, id)
        Toast.makeText(context, ChatWallpapers.title(id), Toast.LENGTH_SHORT).show()
    }

    DevAIBackground {
        Column(Modifier.fillMaxSize()) {
            DevAITopBar(
                title = "Обои чата",
                subtitle = "${ChatWallpapers.all.size} вариантов · ${ChatWallpapers.title(selected)}",
                onBack = onBack,
                actions = {
                    DevAIActionIcon(Icons.Default.Refresh, "Случайные обои") {
                        choose(ChatWallpapers.all.random().id)
                    }
                },
            )

            DevAICard(
                accent = MaterialTheme.colorScheme.primary,
                contentPadding = PaddingValues(14.dp),
            ) {
                Text(
                    "Затемнение: ${(dim * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Больше затемнения — лучше читается текст на ярких обоях.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = dim,
                    onValueChange = {
                        dim = it
                        AppPrefs.setChatWallpaperDim(context, it)
                    },
                    valueRange = 0f..0.85f,
                    steps = 16,
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "none") {
                    WallpaperTile(
                        wallpaper = ChatWallpapers.none,
                        selected = selected == ChatWallpapers.none.id,
                        onClick = { choose(ChatWallpapers.none.id) },
                    )
                }
                items(ChatWallpapers.all, key = { it.id }) { wallpaper ->
                    WallpaperTile(
                        wallpaper = wallpaper,
                        selected = selected == wallpaper.id,
                        onClick = { choose(wallpaper.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WallpaperTile(
    wallpaper: ChatWallpaper,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
        ),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .background(
                    if (wallpaper.id == ChatWallpapers.none.id) {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.background,
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                            )
                        )
                    }
                ),
        ) {
            if (wallpaper.id == ChatWallpapers.none.id) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center).size(26.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Image(
                    painter = painterResource(wallpaper.thumb),
                    contentDescription = wallpaper.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            if (selected) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Выбрано",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 5.dp),
            ) {
                Text(
                    wallpaper.title,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}
