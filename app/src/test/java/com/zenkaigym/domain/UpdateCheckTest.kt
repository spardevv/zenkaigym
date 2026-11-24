package com.zenkaigym.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckTest {
    @Test
    fun comparaVersoes() {
        assertTrue(UpdateCheck.isNewer("1.2.0", "1.1.9"))
        assertTrue(UpdateCheck.isNewer("v2.0", "1.9.9"))
        assertTrue(UpdateCheck.isNewer("1.0.1", "1.0"))
        assertFalse(UpdateCheck.isNewer("1.0", "1.0.0"))
        assertFalse(UpdateCheck.isNewer("1.0.9", "1.1"))
        assertFalse(UpdateCheck.isNewer("1.4.2", "1.4.2"))
    }
}
