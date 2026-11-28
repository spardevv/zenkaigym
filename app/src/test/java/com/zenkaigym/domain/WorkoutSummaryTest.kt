package com.zenkaigym.domain

import com.zenkaigym.data.entity.SetLog
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutSummaryTest {
    private fun set(day: Long, ex: Long, i: Int, reps: Int, kg: Double, warmup: Boolean = false) =
        SetLog(epochDay = day, exerciseId = ex, setIndex = i, reps = reps, weightKg = kg, warmup = warmup)

    @Test fun resumoDoDiaComRecorde() {
        val sets = listOf(set(10, 1, 1, 10, 40.0), set(12, 1, 1, 10, 40.0), set(12, 1, 2, 8, 42.5),
            set(12, 1, -1, 10, 60.0, warmup = true), set(12, 2, 1, 12, 30.0))
        val s = WorkoutSummary.of(sets, today = 12, names = mapOf(1L to "Supino", 2L to "Remada"))
        assertEquals(3, s.sets)
        assertEquals(400.0 + 340.0 + 360.0, s.volumeKg, 0.001)
        assertEquals(listOf(RecordEntry(1, "Supino", 42.5, 40.0, 12)), s.records) // aquecimento de 60 kg não conta; Remada é a 1ª vez
        assertEquals(XpRules.SESSION + 3 * XpRules.SET + XpRules.RECORD, s.xp)
    }

    @Test fun diaSemSeriesValeSoASessao() {
        val s = WorkoutSummary.of(emptyList(), 12, emptyMap())
        assertEquals(0, s.sets); assertEquals(XpRules.SESSION, s.xp)
    }
}
