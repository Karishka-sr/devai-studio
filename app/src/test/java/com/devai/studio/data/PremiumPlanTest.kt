package com.devai.studio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumPlanTest {

    @Test
    fun lifetimeContainsMoreThanThirtyFeatures() {
        assertTrue(PremiumFeature.entries.size >= 30)
        assertEquals(PremiumFeature.entries.size, PremiumFeature.forPlan(PremiumPlan.LIFETIME).size)
    }

    @Test
    fun everyPaidPlanAddsFeatures() {
        assertTrue(PremiumFeature.newInPlan(PremiumPlan.WEEK).isNotEmpty())
        assertTrue(PremiumFeature.newInPlan(PremiumPlan.MONTH).isNotEmpty())
        assertTrue(PremiumFeature.newInPlan(PremiumPlan.YEAR).isNotEmpty())
        assertTrue(PremiumFeature.newInPlan(PremiumPlan.LIFETIME).isNotEmpty())
    }

    @Test
    fun shorterPlansDoNotIncludeLongerFeatures() {
        val week = PremiumFeature.forPlan(PremiumPlan.WEEK).toSet()
        val month = PremiumFeature.forPlan(PremiumPlan.MONTH).toSet()
        val year = PremiumFeature.forPlan(PremiumPlan.YEAR).toSet()
        assertTrue(week.all { it.minPlan == PremiumPlan.WEEK })
        assertTrue(month.containsAll(week))
        assertTrue(year.containsAll(month))
        assertTrue(PremiumFeature.forPlan(PremiumPlan.LIFETIME).containsAll(year))
    }
}
