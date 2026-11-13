package com.zenkaigym.domain

import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.FoodCategory
import com.zenkaigym.data.entity.Meal
import com.zenkaigym.data.entity.MealExtra
import com.zenkaigym.data.entity.MealFull
import com.zenkaigym.data.entity.MealItem
import com.zenkaigym.data.entity.MealItemFull
import com.zenkaigym.data.entity.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DayAdjustTest {
    private val rice = Food(1, "Arroz", FoodCategory.CARB, 130.0, 2.5, 28.0, 0.3)
    private val chicken = Food(2, "Frango", FoodCategory.PROTEIN, 165.0, 31.0, 0.0, 3.6)
    private val broccoli = Food(3, "Brócolis", FoodCategory.VEGETABLE, 35.0, 2.4, 7.2, 0.4)

    private var nextItem = 1L

    private fun meal(type: MealType, vararg items: Pair<Food, Int>, cheatKcal: Int? = null) = MealFull(
        Meal(id = type.ordinal.toLong() + 1, planId = 1, name = type.pt, position = type.ordinal, type = type, cheatKcal = cheatKcal),
        items.map { (food, g) -> MealItemFull(MealItem(nextItem++, type.ordinal.toLong() + 1, food.id, g), food) }
    )

    private fun extra(type: MealType, kcal: Int) = MealExtra(epochDay = 1, type = type, name = "Pizza", kcal = kcal, protein = 20)

    private val day = listOf(
        meal(MealType.LUNCH, rice to 200, chicken to 150, broccoli to 150),
        meal(MealType.DINNER, rice to 200, chicken to 150, broccoli to 150)
    )

    @Test
    fun semExtras_nadaMuda() {
        assertEquals(DayAdjustment.NONE, DayAdjust.compute(day, setOf(MealType.LUNCH), emptyList()))
    }

    @Test
    fun extraCalorico_reduzAsProximasRefeicoes() {
        // Almoço trocado por 1200 kcal (planejado ~560): o jantar tem que encolher.
        val result = DayAdjust.compute(day, emptySet(), listOf(extra(MealType.LUNCH, 1200)))
        assertEquals(setOf(MealType.DINNER), result.adjusted)
        assertTrue(result.deltaKcal < 0)
        val dinnerRice = day[1].items.first { it.food == rice }.item.id
        assertTrue(result.grams.getValue(dinnerRice) < 200)
        // Legume não muda.
        val dinnerVeg = day[1].items.first { it.food == broccoli }.item.id
        assertFalse(dinnerVeg in result.grams)
    }

    @Test
    fun extraLeve_aumentaAsProximasRefeicoes() {
        val result = DayAdjust.compute(day, emptySet(), listOf(extra(MealType.LUNCH, 150)))
        assertTrue(result.deltaKcal > 0)
    }

    @Test
    fun ajusteFicaDentroDosLimites() {
        val result = DayAdjust.compute(day, emptySet(), listOf(extra(MealType.LUNCH, 5000)))
        val planned = DayAdjust.mealKcal(day[1])
        // No máximo metade do jantar pode sumir.
        assertTrue(result.deltaKcal >= -(planned * (1 - DayAdjust.MIN_FACTOR)).toInt() - 30)
    }

    @Test
    fun refeicaoFeitaNaoEAjustada() {
        val result = DayAdjust.compute(day, setOf(MealType.DINNER), listOf(extra(MealType.LUNCH, 1200)))
        assertEquals(DayAdjustment.NONE, result)
    }

    @Test
    fun refeicaoLivreNaoEAjustada() {
        val withCheat = listOf(day[0], meal(MealType.DINNER, cheatKcal = 900))
        assertEquals(DayAdjustment.NONE, DayAdjust.compute(withCheat, emptySet(), listOf(extra(MealType.LUNCH, 1200))))
    }
}
