package com.devai.studio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeSyntaxTest {
    private fun kinds(tag: String, code: String): List<SyntaxKind> =
        CodeSyntax.highlight(tag, code).map { it.kind }

    @Test
    fun highlightsKotlinKeywordsStringsCommentsAndNumbers() {
        val code = "// note\nfun main() {\n    val name = \"DevAI\"\n    val count = 42\n}"
        val spans = CodeSyntax.highlight("kotlin", code)
        assertTrue(spans.any { it.kind == SyntaxKind.COMMENT && code.substring(it.start, it.end).contains("note") })
        assertTrue(spans.any { it.kind == SyntaxKind.KEYWORD && code.substring(it.start, it.end) == "fun" })
        assertTrue(spans.any { it.kind == SyntaxKind.KEYWORD && code.substring(it.start, it.end) == "val" })
        assertTrue(spans.any { it.kind == SyntaxKind.STRING && code.substring(it.start, it.end) == "\"DevAI\"" })
        assertTrue(spans.any { it.kind == SyntaxKind.NUMBER && code.substring(it.start, it.end) == "42" })
        assertTrue(spans.any { it.kind == SyntaxKind.FUNCTION && code.substring(it.start, it.end) == "main" })
    }

    @Test
    fun highlightsPythonAndSql() {
        val python = "# comment\nprint('hello')"
        assertTrue(kinds("python", python).contains(SyntaxKind.COMMENT))
        assertTrue(kinds("python", python).contains(SyntaxKind.STRING))

        val sql = "SELECT id FROM users WHERE active = 1"
        val sqlSpans = CodeSyntax.highlight("sql", sql)
        assertTrue(sqlSpans.any { sql.substring(it.start, it.end).equals("SELECT", ignoreCase = true) })
        assertTrue(sqlSpans.any { sql.substring(it.start, it.end).equals("FROM", ignoreCase = true) })
    }

    @Test
    fun highlightsMarkupTagsAndAttributes() {
        val html = "<div class=\"card\">Hello</div>"
        val spans = CodeSyntax.highlight("html", html)
        assertTrue(spans.any { it.kind == SyntaxKind.KEYWORD && html.substring(it.start, it.end) == "div" })
        assertTrue(spans.any { it.kind == SyntaxKind.TYPE && html.substring(it.start, it.end) == "class" })
        assertTrue(spans.any { it.kind == SyntaxKind.STRING && html.substring(it.start, it.end) == "\"card\"" })
    }

    @Test
    fun resolvesAliasesAndFileExtensions() {
        assertEquals("javascript", CodeSyntax.languageId("js", ""))
        assertEquals("typescript", CodeSyntax.languageId("ts", ""))
        assertEquals("cpp", CodeSyntax.languageId("c++", ""))
        assertEquals("csharp", CodeSyntax.languageId("c#", ""))
        assertEquals("objectivec", CodeSyntax.languageId("objective-c", ""))
        assertEquals("kotlin", CodeLanguages.fromFileName("Main.kt")?.id)
        assertEquals("python", CodeLanguages.fromFileName("script.py")?.id)
        assertNotNull(CodeLanguages.fromFileName("Program.cs"))
        assertFalse("rust" in CodeLanguages.all.map { it.id })
    }

    @Test
    fun detectsUntaggedKotlinAndHtml() {
        assertEquals("kotlin", CodeSyntax.languageId("", "package com.devai\n\nfun main() = println(42)"))
        assertEquals("html", CodeSyntax.languageId("", "<div class=\"card\">Hello</div>"))
        assertNotNull(CodeSyntax.highlight("", "package com.devai\n\nfun main() = println(42)").firstOrNull())
    }

    @Test
    fun everyLanguageHighlightsItsOwnExample() {
        CodeLanguages.all.forEach { language ->
            val spans = CodeSyntax.highlight(language.id, language.example)
            assertTrue("${language.id} did not produce syntax spans", spans.isNotEmpty())
        }
    }

    @Test
    fun spansStayInsideCodeAndDoNotOverlap() {
        val code = "/* block */\nclass Main { fun run() = 7 }"
        val spans = CodeSyntax.highlight("kotlin", code)
        var previousEnd = 0
        spans.forEach { span ->
            assertTrue(span.start >= previousEnd)
            assertTrue(span.end <= code.length)
            assertTrue(span.end > span.start)
            previousEnd = span.end
        }
    }
}
