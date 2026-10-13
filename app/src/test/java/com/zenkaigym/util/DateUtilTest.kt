package com.zenkaigym.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class DateUtilTest {
    @Test
    fun calendarWeekdayMapsToMondayFirst() {
        assertEquals(1, dayOfWeekFromCalendar(Calendar.MONDAY))
        assertEquals(2, dayOfWeekFromCalendar(Calendar.TUESDAY))
        assertEquals(5, dayOfWeekFromCalendar(Calendar.FRIDAY))
        assertEquals(6, dayOfWeekFromCalendar(Calendar.SATURDAY))
        assertEquals(7, dayOfWeekFromCalendar(Calendar.SUNDAY))
    }

    @Test
    fun epochDayIsFormattedAsDayMonth() {
        assertEquals("01/01", formatEpochDay(0))
        assertEquals("02/01", formatEpochDay(1))
        assertEquals("01/02", formatEpochDay(31))
    }

    @Test
    fun decimalParsingAcceptsCommaAndDot() {
        assertEquals(72.5, "72,5".toDecimalOrNull()!!, 0.0)
        assertEquals(72.5, " 72.5 ".toDecimalOrNull()!!, 0.0)
        assertNull("".toDecimalOrNull())
        assertNull("abc".toDecimalOrNull())
    }

    @Test
    fun weekLabelsStartOnMonday() {
        assertEquals(7, WEEK_DAYS_SHORT.size)
        assertEquals("Seg", WEEK_DAYS_SHORT.first())
        assertEquals("Dom", WEEK_DAYS_SHORT.last())
    }
}
