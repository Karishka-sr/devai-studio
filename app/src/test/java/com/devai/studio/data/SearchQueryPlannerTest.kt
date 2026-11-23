package com.devai.studio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQueryPlannerTest {

    @Test
    fun removesPoliteWrappingAndQuestionPrefix() {
        assertEquals("Kotlin", SearchQueryPlanner.normalize("Подскажи, пожалуйста, что такое Kotlin?"))
    }

    @Test
    fun removesGreetingBeforeSearch() {
        assertEquals("что нового в Kotlin", SearchQueryPlanner.normalize("Привет, что нового в Kotlin?"))
    }

    @Test
    fun keepsTechnicalErrorText() {
        val query = SearchQueryPlanner.normalize("Помоги исправить ошибку Gradle: Could not resolve com.android.tools.build")
        assertTrue(query.contains("Could not resolve"))
        assertTrue(query.contains("Gradle"))
    }

    @Test
    fun freshQueriesAddCurrentYearAlternate() {
        val plan = SearchQueryPlanner.plan("последние новости Android", year = 2026)
        assertTrue(plan.fresh)
        assertTrue(plan.alternates.contains("последние новости Android 2026"))
    }

    @Test
    fun codeQueriesPreferStackOverflowAlternate() {
        val plan = SearchQueryPlanner.plan("Kotlin coroutines exception")
        assertTrue(plan.code)
        assertTrue(plan.alternates.any { it.contains("stackoverflow") })
        assertEquals("en", plan.language)
    }

    @Test
    fun smallTalkIsNotTurnedIntoSearchNoise() {
        assertFalse(SearchQueryPlanner.plan("привет").fresh)
        assertFalse(SearchQueryPlanner.plan("привет").code)
    }
}
