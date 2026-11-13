package com.zenkaigym.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WeekStartTest {
    @Test
    fun ordemDosDias() {
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), WeekStart.MONDAY.days)
        assertEquals(listOf(7, 1, 2, 3, 4, 5, 6), WeekStart.SUNDAY.days)
        assertEquals(0, WeekStart.SUNDAY.position(7))
        assertEquals(6, WeekStart.MONDAY.position(7))
    }

    @Test
    fun inicioDaSemana() {
        // 20.000 desde 1970-01-01 é uma sexta.
        assertEquals(19_996L, WeekStart.MONDAY.weekStartOf(20_000))
        assertEquals(19_995L, WeekStart.SUNDAY.weekStartOf(20_000))
        // No próprio domingo, a semana de domingo começa nele; a de segunda começou seis dias antes.
        assertEquals(19_995L, WeekStart.SUNDAY.weekStartOf(19_995))
        assertEquals(19_989L, WeekStart.MONDAY.weekStartOf(19_995))
    }

    @Test
    fun fromName_padraoESegunda() {
        assertEquals(WeekStart.SUNDAY, WeekStart.fromName("SUNDAY"))
        assertEquals(WeekStart.MONDAY, WeekStart.fromName(null))
    }

    @Test
    fun fichaSegueAOrdemDaSemana() {
        val catalog = com.zenkaigym.data.ExerciseCatalog.all().mapIndexed { i, e -> e.copy(id = i + 1L) }
        val plan = WorkoutPlanner.plan(listOf(1, 3, 7), com.zenkaigym.data.entity.Goal.MAINTAIN, catalog, WeekStart.SUNDAY)
        assertEquals(listOf(7, 1, 3), plan.days.map { it.dayOfWeek })
    }
}
