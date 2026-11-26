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
        assertEquals(0.0, empty.bestOneRepMax, 0.0)
    }

    @Test
    fun aquecimentoFicaDeForaDoHistorico() {
        val warm = SetLog(epochDay = 10, exerciseId = 1, setIndex = -1, reps = 10, weightKg = 100.0, warmup = true)
        val progress = ExerciseProgress.from(listOf(warm, log(10, 1, 8, 60.0)))
        assertEquals(1, progress.sessions.single().sets.size)
        assertEquals(60.0, progress.best, 0.0)
    }

    @Test
    fun umRmEstimadoPorSessaoEMelhorDeTodas() {
        val progress = ExerciseProgress.from(listOf(log(10, 1, 10, 60.0), log(10, 2, 5, 70.0), log(20, 1, 3, 80.0)))
        // Dia 10: 10 × 60 = 80 e 5 × 70 = 81,7 → 81,7. Dia 20: 3 × 80 = 88.
        assertEquals(81.667, progress.oneRepMaxTrend[0], 0.001)
        assertEquals(88.0, progress.oneRepMaxTrend[1], 0.001)
        assertEquals(88.0, progress.bestOneRepMax, 0.001)
    }

    @Test
    fun notasRirECardioSomadosNaSessao() {
        val logs = listOf(
            SetLog(epochDay = 10, exerciseId = 1, setIndex = 1, reps = 15, weightKg = 0.0, distanceKm = 2.5, kcal = 120, note = " ritmo leve "),
            SetLog(epochDay = 10, exerciseId = 1, setIndex = 2, reps = 15, weightKg = 0.0, distanceKm = 2.0, kcal = 110, rir = 2, note = "  ")
        )
        val session = ExerciseProgress.from(logs).sessions.single()
        assertEquals(listOf("ritmo leve"), session.notes)
        assertEquals(4.5, session.distanceKm!!, 0.001)
        assertEquals(230, session.kcal)
        assertEquals(2, session.sets[1].rir)
        assertNull(ExerciseProgress.from(listOf(log(10, 1, 8, 60.0))).sessions.single().kcal)
    }
}
