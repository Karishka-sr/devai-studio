package com.devai.studio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumbersTest {
    @Test
    fun normalizesRussianFormats() {
        assertEquals("+79991234567", PhoneNumbers.normalize("+7 999 123-45-67"))
        assertEquals("+79991234567", PhoneNumbers.normalize("8 (999) 123-45-67"))
        assertEquals("+79991234567", PhoneNumbers.normalize("9991234567"))
        assertEquals("+79991234567", PhoneNumbers.normalize("79991234567"))
    }

    @Test
    fun keepsOtherCountryCodes() {
        assertEquals("+14155550123", PhoneNumbers.normalize("+1 415 555 0123"))
    }

    @Test
    fun rejectsShortNumbers() {
        assertNull(PhoneNumbers.normalize("123"))
        assertNull(PhoneNumbers.normalize(""))
    }

    @Test
    fun prettyPrintsRussianNumber() {
        assertEquals("+7 999 123-45-67", PhoneNumbers.pretty("+79991234567"))
    }
}
