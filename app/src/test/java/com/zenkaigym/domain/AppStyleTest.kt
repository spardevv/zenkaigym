package com.zenkaigym.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AppStyleTest {
    @Test
    fun fromName_defaultsToShonen() {
        assertEquals(AppStyle.KAWAII, AppStyle.fromName("KAWAII"))
        assertEquals(AppStyle.SHONEN, AppStyle.fromName(null))
        assertEquals(AppStyle.SHONEN, AppStyle.fromName("outro"))
    }

    @Test
    fun extraValidator_aceitaPratoLivreEBarraValoresRuins() {
        assertEquals(ExtraValidation.Valid("Pizza", 800, 30), ExtraValidator.validate(" Pizza ", "800", "30"))
        assertEquals(ExtraValidation.Valid("Bolo", 300, 0), ExtraValidator.validate("Bolo", "300", ""))
        assert(ExtraValidator.validate("", "300", "") is ExtraValidation.Invalid)
        assert(ExtraValidator.validate("Bolo", "0", "") is ExtraValidation.Invalid)
        assert(ExtraValidator.validate("Bolo", "100", "50") is ExtraValidation.Invalid)
    }
}
