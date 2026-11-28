package com.zenkaigym.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeekStripTest {
    // 2026-11-23 é segunda-feira
    private val monday = LocalDate.of(2026, 11, 23).toEpochDay()

    @Test fun ordemSegueOInicioDaSemana() {
        val days = WeekStrip.days(WeekStart.SUNDAY, monday + 2, List(7) { false }, emptySet())
        assertEquals(listOf(7, 1, 2, 3, 4, 5, 6), days.map { it.dayOfWeek })
        assertEquals(3, days.indexOfFirst { it.today })
    }

    @Test fun diaPlanejadoQuePassouSemTreinoFicaPerdido() {
        val days = WeekStrip.days(WeekStart.MONDAY, monday + 2, List(7) { false }, setOf(1, 3, 5))
        assertEquals(listOf(true, false, false, false, false, false, false), days.map { it.missed })
    }

    @Test fun diaComTreinoNaoContaComoPerdido() {
        val days = WeekStrip.days(WeekStart.MONDAY, monday + 2, listOf(true, false, true, false, false, false, false), setOf(1, 3))
        assertEquals(listOf(true, false, true, false, false, false, false), days.map { it.done })
        assertTrue(days.none { it.missed })
    }

    @Test fun semanaSemDadosNaoMarcaNada() {
        val days = WeekStrip.days(WeekStart.MONDAY, monday, emptyList(), emptySet())
        assertEquals(7, days.size)
        assertTrue(days.none { it.done || it.missed })
    }
}
