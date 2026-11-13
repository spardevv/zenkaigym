package com.zenkaigym.util

import com.zenkaigym.domain.tr
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private const val DAY_MS = 86_400_000L

val WEEK_DAYS_SHORT: List<String> get() = listOf(tr("Seg"), tr("Ter"), tr("Qua"), tr("Qui"), tr("Sex"), tr("Sáb"), tr("Dom"))

/** Dia local atual como número de dias desde 1970-01-01. */
fun todayEpochDay(): Long {
    val now = System.currentTimeMillis()
    return (now + TimeZone.getDefault().getOffset(now)) / DAY_MS
}

/** Converte Calendar.DAY_OF_WEEK (domingo = 1) para 1 = segunda ... 7 = domingo. */
fun dayOfWeekFromCalendar(calendarDay: Int): Int = (calendarDay + 5) % 7 + 1

/** 1 = segunda ... 7 = domingo para um dia contado desde 1970-01-01 (uma quinta-feira). */
fun dayOfWeekFromEpochDay(epochDay: Long): Int = Math.floorMod(epochDay + 3, 7L).toInt() + 1

fun todayDayOfWeek(): Int = dayOfWeekFromCalendar(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))

fun formatEpochDay(epochDay: Long): String {
    val format = SimpleDateFormat("dd/MM", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
    return format.format(epochDay * DAY_MS)
}

/** Converte "72,5" ou "72.5" em Double; null se inválido. */
fun String.toDecimalOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

/** Instante atual reduzido ao que a interface precisa: o dia e os minutos desde 00:00. */
data class Clock(val epochDay: Long, val minutes: Int) {
    companion object {
        fun now(): Clock {
            val cal = Calendar.getInstance()
            return Clock(todayEpochDay(), cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE))
        }
    }
}
