package com.zenkaigym.domain

import com.zenkaigym.data.entity.Availability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AvailabilityMappingTest {
    @Test
    fun availableRowWithStartAndEnd_givesWindow() {
        val row = Availability(1, true, "18:00", "19:30")
        assertEquals(TimeWindow(TimeOfDay(18, 0), TimeOfDay(19, 30)), row.timeWindow())
    }

    @Test
    fun unavailableRowHasNoWindow_evenWithStoredTimes() {
        assertNull(Availability(1, false, "18:00", "19:30").timeWindow())
    }

    @Test
    fun legacyRowWithOnlyStart_getsOneHour() {
        assertEquals("18:30 – 19:30", Availability(2, true, "18:30", null).timeWindow()!!.format())
    }

    @Test
    fun availableRowWithoutTimes_hasNoWindow() {
        assertNull(Availability(3, true, null, null).timeWindow())
    }

    @Test
    fun windowsByDay_includesOnlyAvailableDaysWithTimes() {
        val rows = listOf(
            Availability(1, true, "07:00", "08:00"),
            Availability(2, false, "09:00", "10:00"),
            Availability(3, true, null, null),
            Availability(5, true, "18:00", "19:00")
        )
        assertEquals(setOf(1, 5), rows.windowsByDay().keys)
    }

    @Test
    fun rowFromWindow_storesFormattedTimes() {
        val row = availabilityOf(4, TimeWindow(TimeOfDay(6, 5), TimeOfDay(7, 0)))
        assertEquals(Availability(4, true, "06:05", "07:00"), row)
    }

    @Test
    fun rowWithoutWindow_isUnavailableAndClearsTimes() {
        assertEquals(Availability(6, false, null, null), availabilityOf(6, null))
    }
}
