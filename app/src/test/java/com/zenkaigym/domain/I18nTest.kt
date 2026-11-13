package com.zenkaigym.domain

import com.zenkaigym.data.FoodExamples
import com.zenkaigym.data.entity.MealType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class I18nTest {
    @After
    fun backToPortuguese() = I18n.select(AppLanguage.PT)

    @Test
    fun systemLanguageDecidesWhenNothingIsChosen() {
        assertEquals(Lang.PT, I18n.resolve(AppLanguage.SYSTEM, "pt"))
        assertEquals(Lang.JA, I18n.resolve(AppLanguage.SYSTEM, "ja"))
        assertEquals(Lang.EN, I18n.resolve(AppLanguage.SYSTEM, "en"))
        assertEquals(Lang.EN, I18n.resolve(AppLanguage.SYSTEM, "de"))
    }

    @Test
    fun chosenLanguageOverridesTheSystem() {
        assertEquals(Lang.JA, I18n.resolve(AppLanguage.JA, "pt"))
        assertEquals(Lang.PT, I18n.resolve(AppLanguage.PT, "ja"))
    }

    @Test
    fun translatesAndFillsPlaceholders() {
        I18n.select(AppLanguage.EN)
        assertEquals("Cancel", tr("Cancelar"))
        assertEquals("Today · Mon", tr("Hoje · {0}", "Mon"))
        I18n.select(AppLanguage.JA)
        assertEquals("キャンセル", tr("Cancelar"))
        I18n.select(AppLanguage.PT)
        assertEquals("Cancelar", tr("Cancelar"))
    }

    @Test
    fun unknownTextFallsBackToPortuguese() {
        I18n.select(AppLanguage.EN)
        assertEquals("Prato do usuário", tr("Prato do usuário"))
    }

    @Test
    fun mealTypesAndExampleFoodsAreTranslated() {
        I18n.select(AppLanguage.EN)
        MealType.entries.forEach { assertNotEquals(it.pt, it.label) }
        I18n.select(AppLanguage.JA)
        FoodExamples.all().forEach { assertNotEquals("sem tradução: ${it.name}", it.name, tr(it.name)) }
    }
}
