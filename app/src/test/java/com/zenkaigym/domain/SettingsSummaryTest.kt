package com.zenkaigym.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsSummaryTest {
    @Test fun resumoDosLembretes() {
        assertEquals("Refeições e água", SettingsSummary.reminders(meals = true, water = true))
        assertEquals("Refeições", SettingsSummary.reminders(true, false))
        assertEquals("Só água", SettingsSummary.reminders(false, true))
        assertEquals("Desligados", SettingsSummary.reminders(false, false))
    }

    @Test fun codigoDoIdioma() {
        assertEquals("PT", SettingsSummary.languageCode(AppLanguage.PT))
        assertEquals("EN", SettingsSummary.languageCode(AppLanguage.EN))
        assertEquals("JA", SettingsSummary.languageCode(AppLanguage.JA))
        assertEquals("Sistema", SettingsSummary.languageCode(AppLanguage.SYSTEM))
    }
}
