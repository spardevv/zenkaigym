package com.zenkaigym.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionStaggerTest {
    @Test
    fun firstItemHasNoDelay_andEachStepAddsFiftyMs() {
        assertEquals(0, Motion.staggerDelayMs(0))
        assertEquals(50, Motion.staggerDelayMs(1))
        assertEquals(150, Motion.staggerDelayMs(3))
    }

    @Test
    fun delayIsCapped_soLongListsDoNotWaitForever() {
        assertEquals(300, Motion.staggerDelayMs(6))
        assertEquals(300, Motion.staggerDelayMs(100))
    }

    @Test
    fun negativeIndexIsTreatedAsFirst() {
        assertEquals(0, Motion.staggerDelayMs(-4))
    }
}

class MotionTabDirectionTest {
    private val order = listOf("home", "workout", "diet", "foods", "profile")

    @Test
    fun movingRightIsForward() {
        assertEquals(1, Motion.tabDirection(order, "home", "workout"))
        assertEquals(1, Motion.tabDirection(order, "home", "profile"))
    }

    @Test
    fun movingLeftIsBackward() {
        assertEquals(-1, Motion.tabDirection(order, "diet", "home"))
        assertEquals(-1, Motion.tabDirection(order, "profile", "foods"))
    }

    @Test
    fun sameOrUnknownTabHasNoDirection() {
        assertEquals(0, Motion.tabDirection(order, "diet", "diet"))
        assertEquals(0, Motion.tabDirection(order, "diet", "settings"))
        assertEquals(0, Motion.tabDirection(order, null, "diet"))
        assertEquals(0, Motion.tabDirection(order, "diet", null))
    }
}

class MotionSegmentFractionsTest {
    private fun assertFractions(expected: List<Float>, actual: List<Float>) {
        assertEquals(expected.size, actual.size)
        expected.indices.forEach { assertEquals("segmento $it", expected[it], actual[it], 1e-6f) }
    }

    @Test
    fun noProgressDrawsNothing_fullProgressDrawsEverything() {
        assertFractions(listOf(0f, 0f, 0f), Motion.segmentFractions(0f, 3))
        assertFractions(listOf(1f, 1f, 1f), Motion.segmentFractions(1f, 3))
    }

    @Test
    fun segmentsAreDrawnOneAfterAnother() {
        assertFractions(listOf(1f, 1f, 0f, 0f), Motion.segmentFractions(0.5f, 4))
        assertFractions(listOf(1f, 0.5f, 0f, 0f), Motion.segmentFractions(0.375f, 4))
    }

    @Test
    fun progressIsClamped() {
        assertFractions(listOf(0f, 0f), Motion.segmentFractions(-1f, 2))
        assertFractions(listOf(1f, 1f), Motion.segmentFractions(2f, 2))
    }

    @Test
    fun singleSegmentFollowsProgress() {
        assertFractions(listOf(0.5f), Motion.segmentFractions(0.5f, 1))
    }

    @Test
    fun noSegmentsGivesEmptyList() {
        assertTrue(Motion.segmentFractions(0.5f, 0).isEmpty())
    }

    @Test
    fun fractionsNeverDecreaseAlongTheLine() {
        val f = Motion.segmentFractions(0.62f, 7)
        assertTrue(f.zipWithNext().all { (a, b) -> a >= b })
    }
}
