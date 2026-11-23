package com.devai.studio.data

import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale
import java.util.UUID
import kotlin.random.Random
import org.json.JSONArray
import org.json.JSONObject

data class QuickTool(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val inputLabel: String = "Текст",
    val secondLabel: String? = null,
    val inputPlaceholder: String = "",
    val transform: (String, String) -> String,
)

object QuickTools {
    val categories = listOf(
        "Все",
        "Кодирование",
        "JSON/CSV",
        "Хеши",
        "Текст",
        "Числа",
        "Время",
        "Цвет",
        "Генераторы",
        "Справка",
    )

    val all: List<QuickTool> = listOf(
        tool("b64_encode", "Base64: кодировать", "Текст в Base64", "Кодирование") { s, _ ->
            Base64.getEncoder().encodeToString(s.toByteArray(Charsets.UTF_8))
        },
        tool("b64_decode", "Base64: декодировать", "Base64 обратно в текст", "Кодирование") { s, _ ->
            String(Base64.getDecoder().decode(s.trim()), Charsets.UTF_8)
        },
        tool("url_encode", "URL: кодировать", "Percent-encoding для ссылок", "Кодирование") { s, _ ->
            URLEncoder.encode(s, "UTF-8")
        },
        tool("url_decode", "URL: декодировать", "Раскодировать ссылку", "Кодирование") { s, _ ->
            URLDecoder.decode(s, "UTF-8")
        },
        tool("json_pretty", "JSON: форматировать", "Красивый JSON с отступами", "JSON/CSV") { s, _ ->
            prettyJson(s)
        },
        tool("json_minify", "JSON: минифицировать", "Убрать пробелы из JSON", "JSON/CSV") { s, _ ->
            val t = s.trim()
            if (t.startsWith("[")) JSONArray(t).toString() else JSONObject(t).toString()
        },
        tool("json_keys", "JSON: все ключи", "Список ключей с путями", "JSON/CSV") { s, _ ->
            val out = mutableListOf<String>()
            collectKeys(parseJson(s), "", out)
            out.joinToString("\n")
        },
        tool("csv_to_json", "CSV → JSON", "Первая строка — заголовки", "JSON/CSV") { s, _ ->
            csvToJson(s)
        },
        tool("json_to_csv", "JSON → CSV", "Массив объектов в таблицу", "JSON/CSV") { s, _ ->
            jsonToCsv(s)
        },
        tool("md5", "MD5", "Хеш MD5 (не для паролей)", "Хеши") { s, _ -> digest("MD5", s) },
        tool("sha1", "SHA-1", "Хеш SHA-1", "Хеши") { s, _ -> digest("SHA-1", s) },
        tool("sha256", "SHA-256", "Хеш SHA-256", "Хеши") { s, _ -> digest("SHA-256", s) },
        tool("jwt_decode", "JWT: разобрать", "Header и payload без проверки подписи", "Кодирование") { s, _ ->
            decodeJwt(s)
        },
        tool("html_escape", "HTML: экранировать", "Безопасный HTML-текст", "Кодирование") { s, _ ->
            s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;")
        },
        tool("html_unescape", "HTML: раскодировать", "Вернуть символы из entities", "Кодирование") { s, _ ->
            s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&amp;", "&")
        },
        tool("unicode_escape", "Unicode: escape", "Не-ASCII символы в \\uXXXX", "Кодирование") { s, _ ->
            s.map { ch ->
                if (ch.code in 32..126 || ch == '\n' || ch == '\t') ch.toString()
                else String.format(Locale.ROOT, "\\u%04x", ch.code)
            }.joinToString("")
        },
        tool("unicode_unescape", "Unicode: unescape", "\\uXXXX обратно в символы", "Кодирование") { s, _ ->
            Regex("""\\u([0-9a-fA-F]{4})""").replace(s) {
                it.groupValues[1].toInt(16).toChar().toString()
            }
        },
        tool("upper", "ВЕРХНИЙ РЕГИСТР", "Все буквы заглавные", "Текст") { s, _ -> s.uppercase() },
        tool("lower", "нижний регистр", "Все буквы строчные", "Текст") { s, _ -> s.lowercase() },
        tool("title", "Заголовочный регистр", "Каждое слово с большой буквы", "Текст") { s, _ ->
            words(s).joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
        },
        tool("snake", "snake_case", "Слова через подчёркивание", "Текст") { s, _ ->
            words(s).joinToString("_") { it.lowercase() }
        },
        tool("camel", "camelCase", "Первое слово строчное, остальные с заглавной", "Текст") { s, _ ->
            val w = words(s)
            w.mapIndexed { index, word ->
                if (index == 0) word.lowercase()
                else word.replaceFirstChar(Char::uppercaseChar)
            }.joinToString("")
        },
        tool("kebab", "kebab-case", "Слова через дефис", "Текст") { s, _ ->
            words(s).joinToString("-") { it.lowercase() }
        },
        tool("slug", "Slug для URL", "Транслит, нижний регистр и дефисы", "Текст") { s, _ ->
            slug(s)
        },
        tool("reverse", "Реверс текста", "Символы в обратном порядке", "Текст") { s, _ -> s.reversed() },
        tool("sort_lines", "Сортировать строки", "По алфавиту", "Текст") { s, _ ->
            s.lines().sorted().joinToString("\n")
        },
        tool("dedupe_lines", "Удалить дубли строк", "Оставить уникальные строки", "Текст") { s, _ ->
            s.lines().distinct().joinToString("\n")
        },
        tool("trim_lines", "Обрезать пробелы", "Trim каждой строки", "Текст") { s, _ ->
            s.lines().joinToString("\n") { it.trim() }
        },
        tool("number_lines", "Нумеровать строки", "1., 2., 3. …", "Текст") { s, _ ->
            s.lines().mapIndexed { index, line -> "${index + 1}. $line" }.joinToString("\n")
        },
        tool("word_freq", "Частота слов", "Топ-15 слов в тексте", "Текст") { s, _ ->
            val counts = Regex("[\\p{L}\\p{N}]+").findAll(s.lowercase())
                .groupingBy { it.value }.eachCount()
            counts.entries.sortedByDescending { it.value }.take(15)
                .joinToString("\n") { "${it.key}: ${it.value}" }
        },
        tool("text_stats", "Статистика текста", "Символы, слова, строки, предложения", "Текст") { s, _ ->
            val words = Regex("[\\p{L}\\p{N}]+").findAll(s).count()
            val sentences = Regex("[.!?]+").findAll(s).count().coerceAtLeast(if (s.isBlank()) 0 else 1)
            buildString {
                appendLine("Символов: ${s.length}")
                appendLine("Без пробелов: ${s.count { !it.isWhitespace() }}")
                appendLine("Слов: $words")
                appendLine("Строк: ${s.lines().size}")
                append("Предложений: $sentences")
            }
        },
        tool("palindrome", "Палиндром?", "Проверка без регистра и знаков", "Текст") { s, _ ->
            val clean = s.filter { it.isLetterOrDigit() }.lowercase()
            if (clean.isNotEmpty() && clean == clean.reversed()) "Да, это палиндром" else "Нет, не палиндром"
        },
        tool("find_replace", "Найти и заменить", "Формат: что=>на что", "Текст", secondLabel = "что=>на что") { s, rule ->
            val (from, to) = splitRule(rule)
            s.replace(from, to)
        },
        tool("regex_matches", "Regex: совпадения", "Найти все совпадения", "Текст", secondLabel = "Регулярное выражение") { s, pattern ->
            Regex(pattern).findAll(s).joinToString("\n") { match ->
                if (match.groups.size > 1) {
                    match.value + "  →  " + match.groupValues.drop(1).joinToString(" | ")
                } else match.value
            }.ifBlank { "Совпадений нет" }
        },
        tool("regex_replace", "Regex: замена", "Формат: regex=>замена", "Текст", secondLabel = "regex=>замена") { s, rule ->
            val (pattern, replacement) = splitRule(rule)
            Regex(pattern).replace(s, replacement)
        },
        tool("diff_lines", "Сравнить два текста", "Построчный diff", "Текст", secondLabel = "Второй текст") { a, b ->
            lineDiff(a, b)
        },
        tool("markdown_table", "Markdown-таблица", "Строки через | или запятую", "Текст") { s, _ ->
            markdownTable(s)
        },
        tool("line_wrap", "Перенос строк", "Ширина в символах", "Текст", secondLabel = "Ширина, символов") { s, width ->
            wrapText(s, width.toIntOrNull() ?: 80)
        },
        tool("char_codes", "Коды символов", "Unicode-коды каждого символа", "Текст") { s, _ ->
            s.map { ch -> String.format(Locale.ROOT, "%s  U+%04X", ch, ch.code) }.joinToString("\n")
        },
        tool("byte_size", "Размер в байтах", "Число → КБ, МБ, ГБ", "Числа") { s, _ ->
            humanBytes(s.trim().toDoubleOrNull() ?: error("Введите число байт"))
        },
        tool("number_base", "Системы счисления", "DEC → BIN, OCT, HEX", "Числа") { s, _ ->
            val value = s.trim().toLongOrNull() ?: error("Введите целое число")
            "DEC: $value\nBIN: ${value.toString(2)}\nOCT: ${value.toString(8)}\nHEX: ${value.toString(16).uppercase()}"
        },
        tool("bin_to_dec", "BIN → DEC", "Двоичное число в десятичное", "Числа") { s, _ ->
            (s.trim().toLongOrNull(2) ?: error("Нужно двоичное число")).toString()
        },
        tool("hex_to_dec", "HEX → DEC", "Шестнадцатеричное в десятичное", "Числа") { s, _ ->
            (s.trim().removePrefix("0x").toLongOrNull(16) ?: error("Нужно HEX-число")).toString()
        },
        tool("dec_to_hex", "DEC → HEX", "Десятичное в шестнадцатеричное", "Числа") { s, _ ->
            "0x" + (s.trim().toLongOrNull() ?: error("Нужно целое число")).toString(16).uppercase()
        },
        tool("timestamp_to_date", "Timestamp → дата", "Секунды или миллисекунды", "Время") { s, _ ->
            timestampToDate(s.trim().toLongOrNull() ?: error("Введите timestamp"))
        },
        tool("date_to_timestamp", "Дата → timestamp", "ISO или 2026-11-21 18:30", "Время") { s, _ ->
            dateToTimestamp(s.trim())
        },
        tool("color_hex_to_rgb", "HEX → RGB", "Цвет в формате #RRGGBB", "Цвет") { s, _ ->
            hexToRgb(s.trim())
        },
        tool("color_rgb_to_hex", "RGB → HEX", "Например: 91, 75, 232", "Цвет") { s, _ ->
            rgbToHex(s)
        },
        tool("uuid", "UUID v4", "Случайный UUID", "Генераторы", inputLabel = "Необязательно") { _, _ ->
            UUID.randomUUID().toString()
        },
        tool("password", "Пароль", "Случайный пароль, длина во втором поле", "Генераторы", inputLabel = "Необязательно", secondLabel = "Длина, по умолчанию 20") { _, length ->
            password(length.toIntOrNull() ?: 20)
        },
        tool("lorem", "Lorem ipsum", "Рыба-текст для макета", "Генераторы", inputLabel = "Необязательно") { _, _ ->
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor " +
                "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis " +
                "nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."
        },
        tool("random_number", "Случайное число", "Диапазон: 1..100", "Генераторы", inputLabel = "Необязательно", secondLabel = "Диапазон min..max") { _, range ->
            val (min, max) = parseRange(range, 1, 100)
            Random.nextInt(min, max + 1).toString()
        },
        tool("dice", "Кубики", "Формат 2d6, по умолчанию 1d6", "Генераторы", inputLabel = "Необязательно", secondLabel = "Формат NdM") { _, spec ->
            rollDice(spec.ifBlank { "1d6" })
        },
        tool("coin", "Монетка", "Орёл или решка", "Генераторы", inputLabel = "Необязательно") { _, _ ->
            if (Random.nextBoolean()) "Орёл" else "Решка"
        },
        tool("mime_type", "MIME по расширению", "png, jpg, pdf, json…", "Справка") { s, _ ->
            mimeType(s.trim())
        },
        tool("http_status", "HTTP-статус", "Код и его значение", "Справка") { s, _ ->
            httpStatus(s.trim().toIntOrNull() ?: error("Введите HTTP-код"))
        },
    )

    fun byId(id: String): QuickTool? = all.firstOrNull { it.id == id }

    private fun tool(
        id: String,
        title: String,
        description: String,
        category: String,
        inputLabel: String = "Текст",
        secondLabel: String? = null,
        inputPlaceholder: String = "",
        transform: (String, String) -> String,
    ) = QuickTool(id, title, description, category, inputLabel, secondLabel, inputPlaceholder, transform)

    private fun prettyJson(raw: String): String {
        val t = raw.trim()
        return if (t.startsWith("[")) JSONArray(t).toString(2) else JSONObject(t).toString(2)
    }

    private fun parseJson(raw: String): Any {
        val t = raw.trim()
        return if (t.startsWith("[")) JSONArray(t) else JSONObject(t)
    }

    private fun collectKeys(value: Any?, prefix: String, out: MutableList<String>) {
        when (value) {
            is JSONObject -> value.keys().forEach { key ->
                val path = if (prefix.isBlank()) key else "$prefix.$key"
                out.add(path)
                collectKeys(value.opt(key), path, out)
            }
            is JSONArray -> for (i in 0 until value.length()) {
                collectKeys(value.opt(i), "$prefix[$i]", out)
            }
        }
    }

    private fun csvToJson(raw: String): String {
        val rows = parseCsv(raw)
        if (rows.isEmpty()) error("CSV пустой")
        val header = rows.first()
        val array = JSONArray()
        rows.drop(1).forEach { row ->
            val obj = JSONObject()
            header.forEachIndexed { index, name ->
                obj.put(name.ifBlank { "column${index + 1}" }, row.getOrElse(index) { "" })
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    private fun jsonToCsv(raw: String): String {
        val array = JSONArray(raw.trim())
        if (array.length() == 0) return ""
        val keys = linkedSetOf<String>()
        for (i in 0 until array.length()) {
            array.optJSONObject(i)?.keys()?.forEach { keys.add(it) }
        }
        val out = StringBuilder()
        out.append(keys.joinToString(",") { csvEscape(it) }).append('\n')
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            out.append(keys.joinToString(",") { csvEscape(obj.optString(it)) }).append('\n')
        }
        return out.toString().trimEnd()
    }

    private fun parseCsv(raw: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < raw.length) {
            val ch = raw[i]
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < raw.length && raw[i + 1] == '"') {
                        cell.append('"'); i++
                    } else quoted = false
                } else cell.append(ch)
            } else when (ch) {
                '"' -> quoted = true
                ',' -> { row.add(cell.toString()); cell.clear() }
                '\n' -> {
                    row.add(cell.toString()); cell.clear()
                    rows.add(row); row = mutableListOf()
                }
                '\r' -> Unit
                else -> cell.append(ch)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row.add(cell.toString()); rows.add(row)
        }
        return rows
    }

    private fun csvEscape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' }) "\"${value.replace("\"", "\"\"")}\"" else value

    private fun digest(algorithm: String, text: String): String =
        MessageDigest.getInstance(algorithm).digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { String.format(Locale.ROOT, "%02x", it.toInt() and 0xff) }

    private fun decodeJwt(raw: String): String {
        val parts = raw.trim().split('.')
        if (parts.size < 2) error("JWT должен содержать минимум header.payload")
        fun decode(part: String): String =
            String(Base64.getUrlDecoder().decode(part), Charsets.UTF_8)
        return buildString {
            appendLine("HEADER")
            appendLine(prettyJson(decode(parts[0])))
            appendLine()
            appendLine("PAYLOAD")
            appendLine(prettyJson(decode(parts[1])))
            if (parts.size > 2) {
                appendLine()
                append("SIGNATURE: ${parts[2]}")
            }
        }.trimEnd()
    }

    private fun words(text: String): List<String> =
        text.replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
            .split(Regex("\\s+")).filter { it.isNotBlank() }

    private val translit = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e",
        'ё' to "e", 'ж' to "zh", 'з' to "z", 'и' to "i", 'й' to "y", 'к' to "k",
        'л' to "l", 'м' to "m", 'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r",
        'с' to "s", 'т' to "t", 'у' to "u", 'ф' to "f", 'х' to "h", 'ц' to "ts",
        'ч' to "ch", 'ш' to "sh", 'щ' to "sch", 'ъ' to "", 'ы' to "y", 'ь' to "",
        'э' to "e", 'ю' to "yu", 'я' to "ya",
    )

    private fun slug(text: String): String = text.lowercase().map { ch ->
        translit[ch] ?: ch.toString()
    }.joinToString("").replace(Regex("[^a-z0-9]+"), "-").trim('-')

    private fun splitRule(rule: String): Pair<String, String> {
        val index = rule.indexOf("=>")
        if (index < 0) error("Формат: что=>на что")
        return rule.substring(0, index) to rule.substring(index + 2)
    }

    private fun lineDiff(a: String, b: String): String {
        val left = a.lines()
        val right = b.lines()
        val dp = Array(left.size + 1) { IntArray(right.size + 1) }
        for (i in left.indices.reversed()) {
            for (j in right.indices.reversed()) {
                dp[i][j] = if (left[i] == right[j]) dp[i + 1][j + 1] + 1
                else maxOf(dp[i + 1][j], dp[i][j + 1])
            }
        }
        val out = mutableListOf<String>()
        var i = 0
        var j = 0
        while (i < left.size && j < right.size) {
            when {
                left[i] == right[j] -> { out.add("  ${left[i]}"); i++; j++ }
                dp[i + 1][j] >= dp[i][j + 1] -> { out.add("- ${left[i]}"); i++ }
                else -> { out.add("+ ${right[j]}"); j++ }
            }
        }
        while (i < left.size) out.add("- ${left[i++]}")
        while (j < right.size) out.add("+ ${right[j++]}")
        return out.joinToString("\n").ifBlank { "Тексты совпадают" }
    }

    private fun markdownTable(raw: String): String {
        val rows = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (rows.isEmpty()) error("Добавьте строки таблицы")
        val cells = rows.map { line ->
            line.removePrefix("|").removeSuffix("|")
                .split('|', ';', ',').map { it.trim() }
        }
        val width = cells.maxOf { it.size }
        val normalized = cells.map { row -> row + List(width - row.size) { "" } }
        val header = normalized.first()
        return buildString {
            append("| ").append(header.joinToString(" | ")).append(" |\n")
            append("| ").append(header.joinToString(" | ") { "---" }).append(" |\n")
            normalized.drop(1).forEach { row ->
                append("| ").append(row.joinToString(" | ")).append(" |\n")
            }
        }.trimEnd()
    }

    private fun wrapText(raw: String, width: Int): String {
        if (width < 10) error("Ширина должна быть не меньше 10")
        return raw.lines().joinToString("\n") { line ->
            if (line.length <= width) line
            else line.split(' ').fold(mutableListOf<String>()) { acc, word ->
                if (acc.isEmpty() || acc.last().length + word.length + 1 > width) acc.add(word)
                else acc[acc.lastIndex] = acc.last() + " " + word
                acc
            }.joinToString("\n")
        }
    }

    private fun humanBytes(bytes: Double): String {
        val units = listOf("Б", "КБ", "МБ", "ГБ", "ТБ")
        var value = bytes
        var index = 0
        while (value >= 1024 && index < units.lastIndex) {
            value /= 1024; index++
        }
        return String.format(Locale.ROOT, "%.2f %s", value, units[index])
    }

    private fun timestampToDate(value: Long): String {
        val millis = if (value > 10_000_000_000L) value else value * 1000
        val instant = Instant.ofEpochMilli(millis)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
            .withZone(ZoneId.systemDefault())
        return "Локально: ${formatter.format(instant)}\nISO: ${instant}"
    }

    private fun dateToTimestamp(raw: String): String {
        val zone = ZoneId.systemDefault()
        val instant = runCatching { Instant.parse(raw) }.getOrNull() ?: run {
            val patterns = listOf(
                "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
                "dd.MM.yyyy HH:mm:ss", "dd.MM.yyyy HH:mm", "dd.MM.yyyy",
            )
            var parsed: LocalDateTime? = null
            for (pattern in patterns) {
                parsed = runCatching {
                    if (pattern.contains("H")) LocalDateTime.parse(raw, DateTimeFormatter.ofPattern(pattern))
                    else LocalDate.parse(raw, DateTimeFormatter.ofPattern(pattern)).atStartOfDay()
                }.getOrNull()
                if (parsed != null) break
            }
            (parsed ?: error("Не удалось разобрать дату")).atZone(zone).toInstant()
        }
        return "Секунды: ${instant.epochSecond}\nМиллисекунды: ${instant.toEpochMilli()}"
    }

    private fun hexToRgb(raw: String): String {
        val hex = raw.removePrefix("#")
        if (hex.length != 3 && hex.length != 6) error("Нужен HEX вида #RGB или #RRGGBB")
        val full = if (hex.length == 3) hex.map { "$it$it" }.joinToString("") else hex
        val value = full.toIntOrNull(16) ?: error("Некорректный HEX")
        val r = (value shr 16) and 0xff
        val g = (value shr 8) and 0xff
        val b = value and 0xff
        return "rgb($r, $g, $b)\nHEX: #${full.uppercase()}\nInt: 0xFF${full.uppercase()}"
    }

    private fun rgbToHex(raw: String): String {
        val nums = Regex("\\d+").findAll(raw).map { it.value.toInt() }.toList()
        if (nums.size < 3) error("Введите три числа: R, G, B")
        val (r, g, b) = nums.take(3).map { it.coerceIn(0, 255) }
        return String.format(Locale.ROOT, "#%02X%02X%02X", r, g, b)
    }

    private fun password(length: Int): String {
        val size = length.coerceIn(8, 128)
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*"
        val random = SecureRandom()
        return buildString {
            repeat(size) { append(chars[random.nextInt(chars.length)]) }
        }
    }

    private fun parseRange(raw: String, defaultMin: Int, defaultMax: Int): Pair<Int, Int> {
        if (raw.isBlank()) return defaultMin to defaultMax
        val parts = raw.replace(" ", "").split("..", "-")
        if (parts.size != 2) error("Формат: min..max")
        val min = parts[0].toIntOrNull() ?: error("Некорректный min")
        val max = parts[1].toIntOrNull() ?: error("Некорректный max")
        if (min > max) error("min больше max")
        return min to max
    }

    private fun rollDice(spec: String): String {
        val match = Regex("(\\d*)d(\\d+)", RegexOption.IGNORE_CASE).matchEntire(spec.trim())
            ?: error("Формат: NdM, например 2d6")
        val count = match.groupValues[1].ifBlank { "1" }.toInt().coerceIn(1, 50)
        val sides = match.groupValues[2].toInt().coerceIn(2, 1000)
        val rolls = List(count) { Random.nextInt(1, sides + 1) }
        return "Броски: ${rolls.joinToString(", ")}\nСумма: ${rolls.sum()}"
    }

    private fun mimeType(raw: String): String {
        val ext = raw.substringAfterLast('.', raw).lowercase()
        return when (ext) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "pdf" -> "application/pdf"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "text/javascript"
            "txt", "md", "log" -> "text/plain"
            "csv" -> "text/csv"
            "zip" -> "application/zip"
            "apk" -> "application/vnd.android.package-archive"
            "mp3" -> "audio/mpeg"
            "mp4" -> "video/mp4"
            "woff2" -> "font/woff2"
            else -> "application/octet-stream"
        }
    }

    private fun httpStatus(code: Int): String {
        val text = when (code) {
            200 -> "OK"
            201 -> "Created"
            204 -> "No Content"
            301 -> "Moved Permanently"
            302 -> "Found"
            304 -> "Not Modified"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            409 -> "Conflict"
            418 -> "I'm a teapot"
            422 -> "Unprocessable Entity"
            429 -> "Too Many Requests"
            500 -> "Internal Server Error"
            502 -> "Bad Gateway"
            503 -> "Service Unavailable"
            504 -> "Gateway Timeout"
            else -> "Неизвестный код"
        }
        return "$code $text"
    }
}
