package com.zenkaigym.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionTest {
    @Test
    fun parsesRanges() {
        assertEquals(RepRange(8, 10), Progression.parseReps("8-10"))
        assertEquals(RepRange(12, 12), Progression.parseReps("12"))
        assertEquals(RepRange(8, 10), Progression.parseReps("10-8"))
        assertNull(Progression.parseReps("até a falha"))
    }

    @Test
    fun noHistoryGivesNoSuggestion() {
        assertNull(Progression.nextWeight(emptyList(), "8-10"))
    }

    @Test
    fun raisesWeightWhenEverySetHitsTheTop() {
        val last = listOf(DoneSet(10, 20.0), DoneSet(10, 20.0), DoneSet(11, 20.0))
        assertEquals(22.0, Progression.nextWeight(last, "8-10")!!, 0.001)
    }

    @Test
    fun keepsWeightWhenSomeSetFellShort() {
        val last = listOf(DoneSet(10, 20.0), DoneSet(8, 20.0))
        assertEquals(20.0, Progression.nextWeight(last, "8-10")!!, 0.001)
    }

    @Test
    fun bodyweightExercisesDoNotGainWeight() {
        assertEquals(0.0, Progression.nextWeight(listOf(DoneSet(20, 0.0)), "15-20")!!, 0.001)
    }

    @Test
    fun personalRecordNeedsABiggerLoad() {
        assertTrue(Progression.isPersonalRecord(25.0, 22.5))
        assertFalse(Progression.isPersonalRecord(22.5, 22.5))
        assertFalse(Progression.isPersonalRecord(0.0, 0.0))
    }

    @Test
    fun prefillRepeatsThePreviousWorkout() {
        val last = listOf(DoneSet(10, 40.0), DoneSet(9, 42.5))
        assertEquals(DoneSet(10, 40.0), Progression.prefill(last, 1, "8-10"))
        assertEquals(DoneSet(9, 42.5), Progression.prefill(last, 2, "8-10"))
        // Série a mais que da última vez: repete a última feita.
        assertEquals(DoneSet(9, 42.5), Progression.prefill(last, 3, "8-10"))
        // Sem histórico: topo da faixa e sem carga.
        assertEquals(DoneSet(10, 0.0), Progression.prefill(emptyList(), 1, "8-10"))
        assertNull(Progression.prefill(emptyList(), 1, "até a falha"))
    }

    @Test
    fun heavierSetsRestLonger() {
        assertEquals(120, Progression.restSeconds(5))
        assertEquals(90, Progression.restSeconds(10))
        assertEquals(60, Progression.restSeconds(15))
    }

    @Test
    fun moodFollowsProgress() {
        assertEquals("Aquecendo…", Progression.mood(0, 12).second)
        assertEquals("No ritmo!", Progression.mood(3, 12).second)
        assertEquals("Quase lá!", Progression.mood(6, 12).second)
        assertEquals("Perfeito!", Progression.mood(12, 12).second)
        assertEquals("Aquecendo…", Progression.mood(0, 0).second)
    }

    @Test
    fun volumeSumsLoadTimesReps() {
        assertEquals(780.0, Progression.volume(listOf(DoneSet(10, 40.0), DoneSet(10, 38.0))), 0.001)
    }
}
