package com.qwenz.qwel.render

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.qwenz.qwel.data.Node

const val MAX_BLUR_LAYERS = 8

interface Backdrop {
    fun drawBlurred(scope: DrawScope, index: Int, node: Node, outline: Path)
}

/** Живое размытие отключено: публичный Compose 1.10.6 не содержит GraphicsLayer. */
class LayerBackdrop : Backdrop {
    override fun drawBlurred(scope: DrawScope, index: Int, node: Node, outline: Path) = Unit
}
