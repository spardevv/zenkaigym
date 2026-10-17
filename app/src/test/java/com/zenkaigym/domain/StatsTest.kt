package com.zenkaigym.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsTest {
    private val today = 20_000L

    @Test
    fun weeklyBucketsEndToday() {
        val weeks = Stats.weekly(mapOf(today to 10.0, today - 6 to 5.0, today - 7 to 3.0, today - 100 to 99.0), today, weeks = 3)
        assertEquals(listOf(0.0, 3.0, 15.0), weeks)
    }

    @Test
    fun futureDaysAreIgnored() {
        assertEquals(listOf(0.0, 0.0), Stats.weekly(mapOf(today + 1 to 5.0), today, weeks = 2))
    }

    @Test
    fun changeNeedsAPreviousWeek() {
        assertNull(Stats.changePercent(0.0, 100.0))
        assertEquals(25, Stats.changePercent(800.0, 1000.0))
        assertEquals(-50, Stats.changePercent(1000.0, 500.0))
    }

    @Test
    fun streakCountsBackFromTodayOrYesterday() {
        val hits = setOf(today - 1, today - 2, today - 3, today - 5)
        assertEquals(3, Stats.streak(today) { it in hits })
        assertEquals(4, Stats.streak(today) { it in hits + today })
        assertEquals(0, Stats.streak(today) { false })
    }

    @Test
    fun trainingSummary() {
        val progress = Stats.training(
            volumePerDay = mapOf(today to 1000.0, today - 7 to 800.0),
            sessionDays = setOf(today, today - 2, today - 7, today - 40),
            today = today
        )
        assertEquals(Stats.WEEKS, progress.weeklyVolume.size)
        assertEquals(2, progress.weeklySessions.last())
        assertEquals(25, progress.volumeChange)
        assertEquals(3, progress.sessionsLast30)
        assertTrue(progress.hasData)
    }

    @Test
    fun dietAdherence() {
        val progress = Stats.diet(mapOf(today to 4, today - 1 to 4, today - 2 to 2), mealsPerDay = 4, today = today, days = 4)
        assertEquals(listOf(0f, 0.5f, 1f, 1f), progress.daily)
        assertEquals(63, progress.averagePercent)
        assertEquals(2, progress.streak)
    }

    @Test
    fun dietWithoutMealsHasNoData() {
        assertFalse(Stats.diet(emptyMap(), mealsPerDay = 4, today = today).hasData)
        assertEquals(0, Stats.diet(mapOf(today to 3), mealsPerDay = 0, today = today).streak)
    }
}
