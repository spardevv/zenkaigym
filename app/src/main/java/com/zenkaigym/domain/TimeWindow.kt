package com.zenkaigym.domain

private const val MINUTES_PER_DAY = 24 * 60
private const val LAST_MINUTE = MINUTES_PER_DAY - 1

data class TimeOfDay(val hour: Int, val minute: Int) : Comparable<TimeOfDay> {
    init {
        require(hour in 0..23) { "Hora inválida: $hour" }
        require(minute in 0..59) { "Minuto inválido: $minute" }
    }

    val totalMinutes get() = hour * 60 + minute

    fun format() = "%02d:%02d".format(hour, minute)

    fun plusMinutes(minutes: Int) = fromMinutes(totalMinutes + minutes)

    override fun compareTo(other: TimeOfDay) = totalMinutes.compareTo(other.totalMinutes)

    companion object {
        private val pattern = Regex("""^(\d{1,2}):(\d{2})$""")

        /** Aceita "H:mm" ou "HH:mm"; devolve null para qualquer outra coisa. */
        fun parse(text: String?): TimeOfDay? {
            val match = pattern.matchEntire(text?.trim() ?: return null) ?: return null
            val hour = match.groupValues[1].toInt()
            val minute = match.groupValues[2].toInt()
            return if (hour in 0..23 && minute in 0..59) TimeOfDay(hour, minute) else null
        }

        /** Limita ao intervalo 00:00–23:59. */
        fun fromMinutes(minutes: Int): TimeOfDay {
            val m = minutes.coerceIn(0, LAST_MINUTE)
            return TimeOfDay(m / 60, m % 60)
        }
    }
}

/** Janela de tempo dentro de um mesmo dia (início e fim), como num alarme. */
data class TimeWindow(val start: TimeOfDay, val end: TimeOfDay) {
    init {
        require(end.totalMinutes - start.totalMinutes >= MIN_DURATION_MINUTES) {
            tr("O fim deve ser ao menos {0} min depois do início.", MIN_DURATION_MINUTES)
        }
    }

    val durationMinutes get() = end.totalMinutes - start.totalMinutes

    fun format() = "${start.format()} – ${end.format()}"

    fun durationLabel() = formatDuration(durationMinutes)

    /** Muda o início mantendo a duração; no fim do dia o término é limitado a 23:59. */
    fun withStart(newStart: TimeOfDay): TimeWindow? =
        tryWindow(newStart, newStart.plusMinutes(durationMinutes))

    fun withEnd(newEnd: TimeOfDay): TimeWindow? = tryWindow(start, newEnd)

    companion object {
        const val MIN_DURATION_MINUTES = 15
        private const val LEGACY_DURATION_MINUTES = 60

        val DEFAULT = TimeWindow(TimeOfDay(18, 0), TimeOfDay(19, 0))

        fun tryWindow(start: TimeOfDay, end: TimeOfDay): TimeWindow? =
            if (end.totalMinutes - start.totalMinutes >= MIN_DURATION_MINUTES) TimeWindow(start, end) else null

        /** Lê o que está no banco; só o início (formato antigo) vira uma janela de 1 hora. */
        fun fromStored(start: String?, end: String?): TimeWindow? {
            val s = TimeOfDay.parse(start) ?: return null
            val e = if (end.isNullOrBlank()) s.plusMinutes(LEGACY_DURATION_MINUTES) else TimeOfDay.parse(end) ?: return null
            return tryWindow(s, e)
        }
    }
}

fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}min"
        m == 0 -> "${h}h"
        else -> "${h}h%02d".format(m)
    }
}

object WeeklySchedule {
    /** Aplica a janela de [fromDay] a todos os dias já presentes; sem efeito se o dia não existir. */
    fun copyToAll(windows: Map<Int, TimeWindow>, fromDay: Int): Map<Int, TimeWindow> {
        val source = windows[fromDay] ?: return windows
        return windows.mapValues { source }
    }

    fun totalMinutes(windows: Map<Int, TimeWindow>) = windows.values.sumOf { it.durationMinutes }

    fun totalLabel(windows: Map<Int, TimeWindow>) = formatDuration(totalMinutes(windows))
}
