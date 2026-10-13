package com.zenkaigym.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IntroPagesTest {
    @Test
    fun hasAtLeastThreePages_withContent() {
        assertTrue(IntroPages.all.size >= 3)
        IntroPages.all.forEach {
            assertTrue(it.kanji.isNotBlank())
            assertTrue(it.title.isNotBlank())
            assertTrue(it.body.isNotBlank())
        }
    }

    @Test
    fun kanjiAreUnique() {
        assertEquals(IntroPages.all.size, IntroPages.all.map { it.kanji }.toSet().size)
    }

    @Test
    fun firstPageWelcomesToTheApp() {
        assertTrue(IntroPages.all.first().title.contains("Zenkaigym", ignoreCase = true))
    }

    @Test
    fun explainsThatSuggestionsAreUpToTheUser() {
        assertTrue(IntroPages.all.any { it.body.contains("você decide", ignoreCase = true) })
    }

    @Test
    fun coversTrainingAndDiet() {
        val text = IntroPages.all.joinToString(" ") { it.title + " " + it.body }.lowercase()
        assertTrue(text.contains("treino"))
        assertTrue(text.contains("dieta"))
    }
}

class IntroFlowTest {
    @Test
    fun startsOnFirstPage() {
        val flow = IntroFlow(pageCount = 4)
        assertEquals(0, flow.index)
        assertTrue(flow.isFirst)
        assertFalse(flow.isLast)
    }

    @Test
    fun nextAdvancesUntilLastAndThenStays() {
        var flow = IntroFlow(3)
        flow = flow.next()
        assertEquals(1, flow.index)
        flow = flow.next()
        assertEquals(2, flow.index)
        assertTrue(flow.isLast)
        assertEquals(2, flow.next().index)
    }

    @Test
    fun backNeverGoesBelowFirst() {
        assertEquals(0, IntroFlow(3).back().index)
        assertEquals(1, IntroFlow(3, index = 2).back().index)
    }

    @Test
    fun singlePageIsFirstAndLast() {
        val flow = IntroFlow(1)
        assertTrue(flow.isFirst && flow.isLast)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroPagesIsInvalid() {
        IntroFlow(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun indexOutOfRangeIsInvalid() {
        IntroFlow(3, index = 3)
    }
}

class IntroGateTest {
    private class FakeStore(var seen: Boolean = false) : IntroStore {
        override fun isSeen() = seen
        override fun setSeen() { seen = true }
    }

    @Test
    fun showsOnFirstLaunch() {
        assertTrue(IntroGate(FakeStore()).shouldShow())
    }

    @Test
    fun hiddenAfterMarkedAsSeen() {
        val store = FakeStore()
        val gate = IntroGate(store)
        gate.markSeen()
        assertFalse(gate.shouldShow())
        assertTrue(store.seen)
    }

    @Test
    fun staysHiddenForANewGateOverTheSameStore() {
        val store = FakeStore()
        IntroGate(store).markSeen()
        assertFalse(IntroGate(store).shouldShow())
    }
}
