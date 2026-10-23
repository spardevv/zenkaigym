package com.zenkaigym.domain

import com.zenkaigym.data.entity.SetLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseProgressTest {
    private fun log(day: Long, index: Int, reps: Int, kg: Double) =
        SetLog(epochDay = day, exerciseId = 1, setIndex = index, reps = reps, weightKg = kg)

    @Test
    fun agrupaPorDiaEOrdenaPorSerie() {
        val progress = ExerciseProgress.from(listOf(log(20, 2, 8, 60.0), log(10, 1, 10, 50.0), log(20, 1, 10, 55.0)))
        assertEquals(listOf(10L, 20L), progress.sessions.map { it.epochDay })
        assertEquals(listOf(DoneSet(10, 55.0), DoneSet(8, 60.0)), progress.sessions[1].sets)
    }

    @Test
    fun tendenciaUsaCargaMaximaDeCadaSessao() {
        val progress = ExerciseProgress.from(listOf(log(10, 1, 10, 50.0), log(10, 2, 8, 52.5), log(20, 1, 10, 55.0)))
        assertTrue(progress.byLoad)
        assertEquals(listOf(52.5, 55.0), progress.trend)
        assertEquals(55.0, progress.best, 0.0)
        assertEquals(2.5, progress.change!!, 0.0)
        assertEquals(20L, progress.bestDay)
    }

    @Test
    fun semCargaAcompanhaRepeticoes() {
        val progress = ExerciseProgress.from(listOf(log(10, 1, 30, 0.0), log(10, 2, 30, 0.0), log(20, 1, 45, 0.0)))
        assertFalse(progress.byLoad)
        assertEquals(listOf(60.0, 45.0), progress.trend)
        assertEquals(10L, progress.bestDay)
        assertEquals(-15.0, progress.change!!, 0.0)
    }

    @Test
    fun empateNoRecordeFicaComODiaMaisAntigo() {
        val progress = ExerciseProgress.from(listOf(log(10, 1, 5, 80.0), log(20, 1, 5, 80.0)))
        assertEquals(10L, progress.bestDay)
    }

    @Test
    fun umaSessaoNaoTemVariacaoEVazioNaoQuebra() {
        assertNull(ExerciseProgress.from(listOf(log(10, 1, 5, 80.0))).change)
        val empty = ExerciseProgress.from(emptyList())
        assertEquals(0.0, empty.best, 0.0)
        assertNull(empty.bestDay)
    }
}
