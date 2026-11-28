package com.zenkaigym.domain

import com.zenkaigym.util.dayOfWeekFromEpochDay

/** Um dia da faixa da semana do Dojo. [dayOfWeek]: 1 = segunda ... 7 = domingo. */
data class WeekStripDay(val dayOfWeek: Int, val done: Boolean, val today: Boolean, val missed: Boolean)

object WeekStrip {
    /**
     * Os 7 dias da semana de [today], na ordem de [start].
     * [weekDots] é o `ProgressStats.weekDots` (índice i = início da semana + i); vazio conta como sem treino.
     * [plannedDays] são os dias com horário planejado; perdido = planejado, antes de hoje e sem treino.
     */
    fun days(start: WeekStart, today: Long, weekDots: List<Boolean>, plannedDays: Set<Int>): List<WeekStripDay> {
        val todayPosition = start.position(dayOfWeekFromEpochDay(today))
        return start.days.mapIndexed { index, dayOfWeek ->
            val done = weekDots.getOrElse(index) { false }
            WeekStripDay(
                dayOfWeek = dayOfWeek,
                done = done,
                today = index == todayPosition,
                missed = dayOfWeek in plannedDays && index < todayPosition && !done,
            )
        }
    }
}
