package com.qwenz.qwel.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Заглушка источника фона: живое стекло требует GraphicsLayer из Compose 1.12. */
class BackdropSource

val LocalBackdrop = compositionLocalOf<BackdropSource?> { null }

@Composable
fun glassColor(base: Color, alpha: Float = 0.78f): Color = base

@Composable
fun glassElevation(normal: Dp): Dp = normal

@Composable
fun glassBorder(): BorderStroke? = null

@Composable
fun Modifier.frostedGlass(shape: Shape, radius: Dp = 22.dp): Modifier = this
