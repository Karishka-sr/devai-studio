package com.devai.studio.data

object PhoneNumbers {
    /** Returns E.164 or null. Accepts +7, 8 and 10-digit Russian numbers. */
    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        val digits = trimmed.filter { it.isDigit() }
        if (digits.length !in 10..15) return null
        return when {
            trimmed.startsWith("+") -> "+$digits"
            digits.length == 11 && digits.startsWith("8") -> "+7" + digits.drop(1)
            digits.length == 11 && digits.startsWith("7") -> "+$digits"
            digits.length == 10 && digits.startsWith("9") -> "+7$digits"
            else -> "+$digits"
        }
    }

    fun pretty(e164: String): String {
        if (e164.startsWith("+7") && e164.length == 12) {
            return "+7 ${e164.substring(2, 5)} ${e164.substring(5, 8)}-${e164.substring(8, 10)}-${e164.substring(10)}"
        }
        return e164
    }
}
