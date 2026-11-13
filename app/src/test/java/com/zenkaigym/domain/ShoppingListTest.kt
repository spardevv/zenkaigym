package com.zenkaigym.domain

import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.FoodCategory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingListTest {
    private val egg = Food(1, "Ovo", FoodCategory.PROTEIN, 155.0, 13.0, 1.1, 11.0, "ovo", 50.0)
    private val rice = Food(2, "Arroz", FoodCategory.CARB, 130.0, 2.5, 28.0, 0.3)
    private val chicken = Food(3, "Frango", FoodCategory.PROTEIN, 165.0, 31.0, 0.0, 3.6)

    @After
    fun backToPortuguese() = I18n.select(AppLanguage.PT)

    @Test
    fun thirtyDaysCoverEachWeekdayFourOrFiveTimes() {
        val counts = ShoppingPlanner.weekdayCounts(startDow = 3, days = 30)
        assertEquals(30, counts.sum())
        // Começando numa quarta: quarta e quinta aparecem 5 vezes, o resto 4.
        assertEquals(listOf(4, 4, 5, 5, 4, 4, 4), (1..7).map { counts[it] })
    }

    @Test
    fun sumsEachFoodByHowOftenItsDayOccurs() {
        val entries = listOf(
            ShoppingEntry(1, rice, 150),
            ShoppingEntry(1, rice, 100),
            ShoppingEntry(3, rice, 200),
            ShoppingEntry(3, chicken, 120)
        )
        val list = ShoppingPlanner.build(entries, startDow = 3, days = 30)
        val byName = list.groups.flatMap { it.second }.associate { it.food.name to it.grams }
        assertEquals(250 * 4 + 200 * 5, byName["Arroz"])
        assertEquals(120 * 5, byName["Frango"])
        assertEquals(listOf(FoodCategory.PROTEIN, FoodCategory.CARB), list.groups.map { it.first })
    }

    @Test
    fun legacyPlansWithDayZeroCountEveryDay() {
        val list = ShoppingPlanner.build(listOf(ShoppingEntry(0, egg, 100)), startDow = 1, days = 7)
        assertEquals(700, list.groups.single().second.single().grams)
    }

    @Test
    fun quantitiesRoundUpForShopping() {
        assertEquals("130 g", ShoppingPlanner.mass(121))
        assertEquals("1 kg", ShoppingPlanner.mass(1000))
        assertEquals("3,2 kg", ShoppingPlanner.mass(3150))
        assertEquals("15 ovos · ~730 g", ShoppingPlanner.quantity(egg, 725))
        assertEquals("1 ovo · ~50 g", ShoppingPlanner.quantity(egg, 50))
        I18n.select(AppLanguage.EN)
        assertEquals("3.2 kg", ShoppingPlanner.mass(3150))
    }

    @Test
    fun sharedTextListsEveryItem() {
        val list = ShoppingPlanner.build(listOf(ShoppingEntry(4, rice, 100), ShoppingEntry(4, egg, 100)), startDow = 1, days = 30)
        val text = ShoppingPlanner.text(list, "De 01/01 a 30/01")
        assertTrue(text.contains("30 dias"))
        assertTrue(text.contains("☐ Arroz: 400 g"))
        assertTrue(text.contains("☐ Ovo: 8 ovos · ~400 g"))
    }
}
