package com.zenkaigym.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeOfDayTest {
    @Test
    fun formatsWithLeadingZeros() {
        assertEquals("07:05", TimeOfDay(7, 5).format())
        assertEquals("18:30", TimeOfDay(18, 30).format())
        assertEquals("00:00", TimeOfDay(0, 0).format())
    }

    @Test
    fun parsesValidTimes() {
        assertEquals(TimeOfDay(18, 30), TimeOfDay.parse("18:30"))
        assertEquals(TimeOfDay(7, 5), TimeOfDay.parse("7:05"))
        assertEquals(TimeOfDay(23, 59), TimeOfDay.parse(" 23:59 "))
        assertEquals(TimeOfDay(0, 0), TimeOfDay.parse("00:00"))
    }

    @Test
    fun rejectsInvalidTimes() {
        listOf("24:00", "12:60", "abc", "", "1830", "18:3", "-1:00", "18:30:00", "ex.: 18:30").forEach {
            assertNull("'$it' deveria ser inválido", TimeOfDay.parse(it))
        }
        assertNull(TimeOfDay.parse(null))
    }

    @Test
    fun convertsToAndFromMinutes() {
        assertEquals(1110, TimeOfDay(18, 30).totalMinutes)
        assertEquals(TimeOfDay(18, 30), TimeOfDay.fromMinutes(1110))
        assertEquals(TimeOfDay(23, 59), TimeOfDay.fromMinutes(5000))
        assertEquals(TimeOfDay(0, 0), TimeOfDay.fromMinutes(-30))
    }

    @Test
    fun plusMinutesStopsAtEndOfDay() {
        assertEquals(TimeOfDay(19, 30), TimeOfDay(18, 30).plusMinutes(60))
        assertEquals(TimeOfDay(23, 59), TimeOfDay(23, 30).plusMinutes(60))
    }

    @Test
    fun timesAreOrdered() {
        assertTrue(TimeOfDay(7, 0) < TimeOfDay(7, 1))
        assertTrue(TimeOfDay(18, 0) > TimeOfDay(9, 59))
    }

    @Test(expected = IllegalArgumentException::class)
    fun hourOutOfRangeIsInvalid() {
        TimeOfDay(24, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun minuteOutOfRangeIsInvalid() {
        TimeOfDay(10, 60)
    }
}

class TimeWindowTest {
    private val evening = TimeWindow(TimeOfDay(18, 0), TimeOfDay(19, 30))

    @Test
    fun hasDurationAndLabel() {
        assertEquals(90, evening.durationMinutes)
        assertEquals("18:00 – 19:30", evening.format())
        assertEquals("1h30", evening.durationLabel())
    }

    @Test
    fun durationLabelVariants() {
        fun label(minutes: Int) = TimeWindow(TimeOfDay(8, 0), TimeOfDay.fromMinutes(8 * 60 + minutes)).durationLabel()
        assertEquals("45min", label(45))
        assertEquals("1h", label(60))
        assertEquals("2h05", label(125))
    }

    @Test
    fun defaultIsOneHourInTheEvening() {
        assertEquals("18:00 – 19:00", TimeWindow.DEFAULT.format())
    }

    @Test(expected = IllegalArgumentException::class)
    fun endMustBeAfterStart() {
        TimeWindow(TimeOfDay(10, 0), TimeOfDay(9, 0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun windowShorterThanMinimumIsInvalid() {
        TimeWindow(TimeOfDay(10, 0), TimeOfDay(10, 14))
    }

    @Test
    fun minimumWindowIsAccepted() {
        assertEquals(15, TimeWindow(TimeOfDay(10, 0), TimeOfDay(10, 15)).durationMinutes)
    }

    @Test
    fun changingStartKeepsTheDuration() {
        val moved = evening.withStart(TimeOfDay(20, 0))!!
        assertEquals(TimeOfDay(20, 0), moved.start)
        assertEquals(TimeOfDay(21, 30), moved.end)
    }

    @Test
    fun changingStartNearMidnightClampsTheEnd() {
        val moved = evening.withStart(TimeOfDay(23, 0))!!
        assertEquals(TimeOfDay(23, 59), moved.end)
    }

    @Test
    fun changingStartTooCloseToMidnightIsRejected() {
        assertNull(evening.withStart(TimeOfDay(23, 50)))
    }

    @Test
    fun changingEndRequiresMinimumDuration() {
        assertEquals(TimeOfDay(20, 0), evening.withEnd(TimeOfDay(20, 0))!!.end)
        assertEquals(TimeOfDay(18, 15), evening.withEnd(TimeOfDay(18, 15))!!.end)
        assertNull(evening.withEnd(TimeOfDay(18, 14)))
        assertNull(evening.withEnd(TimeOfDay(17, 0)))
    }

    @Test
    fun readsStoredWindows() {
        assertEquals(evening, TimeWindow.fromStored("18:00", "19:30"))
    }

    @Test
    fun legacyStartOnlyGetsOneHour() {
        assertEquals(TimeWindow(TimeOfDay(18, 30), TimeOfDay(19, 30)), TimeWindow.fromStored("18:30", null))
        assertEquals(TimeOfDay(23, 59), TimeWindow.fromStored("23:30", null)!!.end)
    }

    @Test
    fun unusableStoredValuesGiveNull() {
        assertNull(TimeWindow.fromStored(null, null))
        assertNull(TimeWindow.fromStored("", ""))
        assertNull(TimeWindow.fromStored("ex.: 18:30", "19:00"))
        assertNull(TimeWindow.fromStored("19:00", "18:00"))
        assertNull(TimeWindow.fromStored("23:55", null))
        assertNull(TimeWindow.fromStored(null, "19:00"))
    }
}

class WeeklyScheduleTest {
    private val morning = TimeWindow(TimeOfDay(7, 0), TimeOfDay(8, 0))
    private val evening = TimeWindow(TimeOfDay(18, 0), TimeOfDay(19, 30))

    @Test
    fun copyToAllAppliesTheChosenDayToEveryDay() {
        val result = WeeklySchedule.copyToAll(mapOf(1 to morning, 3 to evening, 5 to morning), fromDay = 3)
        assertEquals(mapOf(1 to evening, 3 to evening, 5 to evening), result)
    }

    @Test
    fun copyFromUnknownDayChangesNothing() {
        val original = mapOf(1 to morning, 3 to evening)
        assertEquals(original, WeeklySchedule.copyToAll(original, fromDay = 6))
    }

    @Test
    fun totalWeeklyMinutesSumsAllWindows() {
        assertEquals(60 + 90, WeeklySchedule.totalMinutes(mapOf(1 to morning, 3 to evening)))
        assertEquals(0, WeeklySchedule.totalMinutes(emptyMap()))
    }

    @Test
    fun totalLabelIsReadable() {
        assertEquals("2h30", WeeklySchedule.totalLabel(mapOf(1 to morning, 3 to evening)))
        assertEquals("0min", WeeklySchedule.totalLabel(emptyMap()))
    }
}
