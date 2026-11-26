package com.zenkaigym.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Desafios semanais, escudos de sequência e detecção de subida de nível. */
class GamificationTest {
    private val week = 20_769L // uma segunda-feira

    // ---- Desafios ----

    @Test
    fun desafios_tresPorSemanaComOsTreinosSempreNoTopo() {
        val list = Challenges.forWeek(week, plannedPerWeek = 4, mealsPerDay = 5, activity = WeekActivity())
        assertEquals(Challenges.PER_WEEK, list.size)
        assertEquals("sessions", list.first().id)
        assertEquals(4, list.first().target)
        assertEquals(list.size, list.map { it.id }.toSet().size)
    }

    @Test
    fun desafios_mesmaSemanaSempreIguaisESemanasDiferentesVariam() {
        val a = Challenges.forWeek(week, 3, 4, WeekActivity()).map { it.id }
        assertEquals(a, Challenges.forWeek(week, 3, 4, WeekActivity()).map { it.id })
        // Em oito semanas seguidas aparece mais de uma combinação.
        val combos = (0 until 8).map { Challenges.forWeek(week + it * 7, 3, 4, WeekActivity()).map { c -> c.id }.toSet() }.toSet()
        assertTrue(combos.size > 1)
    }

    @Test
    fun desafios_alvoMinimoDeTreinosEProgressoLimitadoAoAlvo() {
        val list = Challenges.forWeek(week, plannedPerWeek = 0, mealsPerDay = 3, activity = WeekActivity(sessions = 9))
        val sessions = list.first()
        assertEquals(2, sessions.target)
        assertEquals(2, sessions.progress)
        assertTrue(sessions.completed)
        assertEquals(1f, sessions.fraction, 0f)
    }

    @Test
    fun desafios_progressoVemDaAtividadeDaSemana() {
        val activity = WeekActivity(sets = 99, fullDietDays = 7, mealsDone = 99, weighIns = 1, waterGoalDays = 7, cardioMinutes = 60, records = 2)
        // Com tudo feito, qualquer combinação sorteada fica completa.
        (0 until 10).forEach { i ->
            val list = Challenges.forWeek(week + i * 7, plannedPerWeek = 3, mealsPerDay = 4, activity = activity.copy(sessions = 3))
            assertTrue(list.all { it.completed })
        }
        val none = Challenges.forWeek(week, 3, 4, WeekActivity())
        assertTrue(none.none { it.completed })
    }

    // ---- Escudos ----

    @Test
    fun escudo_semEscudoUmaFalhaZeraASequencia() {
        val r = Shields.streak(listOf(true, true, false, true, false), earnEvery = 4, maxShields = 1)
        assertEquals(1, r.current)
        assertEquals(2, r.best)
        assertEquals(0, r.shieldsUsed)
    }

    @Test
    fun escudo_ganhoDepoisDeQuatroPeriodosSalvaUmaFalha() {
        // 4 cumpridos (ganha escudo), 1 perdido (gasta), 2 cumpridos, atual em aberto.
        val r = Shields.streak(listOf(true, true, true, true, false, true, true, false), earnEvery = 4, maxShields = 1)
        assertEquals(6, r.current)
        assertEquals(6, r.best)
        assertEquals(0, r.shields)
        assertEquals(1, r.shieldsUsed)
    }

    @Test
    fun escudo_duasFalhasSeguidasComUmEscudoQuebram() {
        val r = Shields.streak(listOf(true, true, true, true, false, false, true, false), earnEvery = 4, maxShields = 1)
        assertEquals(1, r.current)
        assertEquals(4, r.best)
        assertEquals(1, r.shieldsUsed)
    }

    @Test
    fun escudo_periodoAtualEmAbertoNaoQuebraNemGastaEscudo() {
        val r = Shields.streak(listOf(true, true, true, true, false), earnEvery = 4, maxShields = 1)
        assertEquals(4, r.current)
        assertEquals(1, r.shields)
        assertEquals(0, r.shieldsUsed)
    }

    @Test
    fun escudo_respeitaOLimiteGuardado() {
        val r = Shields.streak(List(21) { true }, earnEvery = 7, maxShields = 2)
        assertEquals(21, r.current)
        assertEquals(2, r.shields)
    }

    @Test
    fun escudo_listaVaziaNaoQuebra() {
        assertEquals(StreakResult(0, 0, 0, 0), Shields.streak(emptyList(), 4, 1))
    }

    // ---- Subida de nível ----

    @Test
    fun subida_soDetectaQuandoHaReferenciaENivelMaior() {
        assertNull(LevelUp.detect(lastSeen = -1, current = 5)) // primeira vez: só vira referência
        assertNull(LevelUp.detect(lastSeen = 4, current = 4))
        assertNull(LevelUp.detect(lastSeen = 6, current = 4))
        assertEquals(LevelUp(4, 5), LevelUp.detect(lastSeen = 4, current = 5))
    }

    @Test
    fun subida_patenteMudaNasFaixasDeNivel() {
        assertFalse(LevelUp(3, 4).rankUp) // Aprendiz → Aprendiz
        val up = LevelUp(4, 5)
        assertTrue(up.rankUp)
        assertEquals(Rank.APPRENTICE, up.fromRank)
        assertEquals(Rank.WARRIOR, up.toRank)
        assertNotEquals(up.fromRank.kanji, up.toRank.kanji)
    }
}
