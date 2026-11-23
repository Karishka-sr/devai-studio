package com.devai.studio.data

import org.junit.Assert.assertTrue
import org.junit.Test

class QuickToolsTest {

    @Test
    fun catalogHasAtLeastFiftyTools() {
        assertTrue("Ожидалось 50+ инструментов, найдено ${QuickTools.all.size}", QuickTools.all.size >= 50)
        assertTrue(QuickTools.all.map { it.id }.distinct().size == QuickTools.all.size)
    }

    @Test
    fun everyToolWorksOnItsSample() {
        val samples = mapOf(
            "b64_decode" to ("SGVsbG8=" to ""),
            "json_pretty" to ("""{"a":1,"b":[2,3]}""" to ""),
            "json_minify" to ("""{"a": 1, "b": [2, 3]}""" to ""),
            "json_keys" to ("""{"a":1,"b":{"c":2}}""" to ""),
            "json_to_csv" to ("""[{"a":1,"b":2}]""" to ""),
            "jwt_decode" to ("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMifQ.signature" to ""),
            "csv_to_json" to ("name,age\nAnn,30" to ""),
            "find_replace" to ("hello world" to "world=>DevAI"),
            "regex_matches" to ("a1 b2" to "\\d"),
            "regex_replace" to ("a1 b2" to "\\d=>X"),
            "diff_lines" to ("one\ntwo" to "one\nthree"),
            "line_wrap" to ("one two three four five" to "10"),
            "password" to ("" to "16"),
            "random_number" to ("" to "1..6"),
            "dice" to ("" to "2d6"),
            "date_to_timestamp" to ("2026-11-21 18:30" to ""),
            "timestamp_to_date" to ("1700000000" to ""),
            "color_hex_to_rgb" to ("#AABBCC" to ""),
            "color_rgb_to_hex" to ("1, 2, 3" to ""),
            "bin_to_dec" to ("1010" to ""),
            "hex_to_dec" to ("FF" to ""),
            "dec_to_hex" to ("255" to ""),
            "number_base" to ("255" to ""),
            "byte_size" to ("1024" to ""),
            "mime_type" to ("photo.png" to ""),
            "http_status" to ("404" to ""),
            "unicode_unescape" to ("\\u041f\\u0440\\u0438\\u0432\\u0435\\u0442" to ""),
            "url_decode" to ("hello%20world" to ""),
        )
        QuickTools.all.forEach { tool ->
            val (input, second) = samples[tool.id] ?: ("Hello world\nHello" to "")
            val output = runCatching { tool.transform(input, second) }
                .getOrElse { error("${tool.id} упал: ${it.message}") }
            assertTrue("${tool.id} вернул пустой результат", output.isNotBlank())
        }
    }
}
