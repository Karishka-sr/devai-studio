package com.devai.studio.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.devai.studio.data.CodeSyntax
import com.devai.studio.data.SyntaxKind

@Composable
fun highlightedCode(tag: String, code: String): AnnotatedString {
    val spans = remember(tag, code) { CodeSyntax.highlight(tag, code) }
    return remember(tag, code, spans) {
        buildAnnotatedString {
            append(code)
            spans.forEach { span ->
                addStyle(SpanStyle(color = syntaxColor(span.kind)), span.start, span.end)
            }
        }
    }
}

private fun syntaxColor(kind: SyntaxKind): Color = when (kind) {
    SyntaxKind.KEYWORD -> Color(0xFFC586C0)
    SyntaxKind.TYPE -> Color(0xFF4EC9B0)
    SyntaxKind.STRING -> Color(0xFFCE9178)
    SyntaxKind.COMMENT -> Color(0xFF6A9955)
    SyntaxKind.NUMBER -> Color(0xFFB5CEA8)
    SyntaxKind.ANNOTATION -> Color(0xFFDCDCAA)
    SyntaxKind.FUNCTION -> Color(0xFFDCDCAA)
}
