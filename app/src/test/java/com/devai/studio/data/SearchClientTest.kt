package com.devai.studio.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchClientTest {
    @Test
    fun parsesDesktopAndMobileBingResults() {
        val desktop = """
            <li class="b_algo">
              <h2><a href="https://www.bing.com/ck/a?!&amp;&amp;u=a1aHR0cHM6Ly9leGFtcGxlLmNvbS9wYWdl&amp;ntb=1">
                Example <strong>Title</strong>
              </a></h2>
              <p class="b_lineclamp">Example snippet</p>
            </li>
        """.trimIndent()
        val mobile = """
            <li class="b_algo">
              <a href="https://mobile.example.org/x"><h2>Mobile title</h2></a>
              <div><p>Mobile snippet</p></div>
            </li>
        """.trimIndent()

        val text = SearchClient.parseBing(desktop + mobile, 5).orEmpty()

        assertTrue(text.contains("Example Title"))
        assertTrue(text.contains("https://example.com/page"))
        assertTrue(text.contains("Mobile title"))
        assertTrue(text.contains("https://mobile.example.org/x"))
        assertTrue(text.contains("Example snippet"))
    }

    @Test
    fun ignoresPagesWithoutResults() {
        assertNull(SearchClient.parseBing("<html><body>No results</body></html>", 5))
    }

    @Test
    fun greetingsDoNotTriggerWebSearch() {
        assertFalse(SearchClient.isSearchWorthy("Привет"))
        assertFalse(SearchClient.isSearchWorthy("Привет!"))
        assertFalse(SearchClient.isSearchWorthy("Здравствуйте, как дела?"))
        assertFalse(SearchClient.isSearchWorthy("Спасибо"))
        assertFalse(SearchClient.isSearchWorthy("Кто ты?"))
        assertFalse(SearchClient.isSearchWorthy("Что ты умеешь?"))
    }

    @Test
    fun factualQueriesTriggerWebSearch() {
        assertTrue(SearchClient.isSearchWorthy("Курс доллара"))
        assertTrue(SearchClient.isSearchWorthy("Погода в Москве"))
        assertTrue(SearchClient.isSearchWorthy("Последние новости технологий"))
        assertTrue(SearchClient.isSearchWorthy("Привет, что нового в Kotlin?"))
    }
}
