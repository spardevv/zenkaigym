package com.zenkaigym.domain

import com.zenkaigym.data.FoodExamples
import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.FoodCategory
import com.zenkaigym.data.entity.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class WeeklyDietTest {
    private val examples = FoodExamples.all().mapIndexed { i, f -> f.copy(id = i + 1L) }
    private val byId = examples.associateBy { it.id }
    private val types = MealSchedule.typesFor(4)
    private val target = NutritionCalculator.fromKcalAndProtein(2000, 130)

    private val egg = Food(1, "Ovo", FoodCategory.PROTEIN, 155.0, 13.0, 1.1, 11.0, "ovo", 50.0)
    private val rice = Food(2, "Arroz", FoodCategory.CARB, 130.0, 2.5, 28.0, 0.3)

    private fun dayKcal(day: PlannedDietDay) =
        day.meals.flatMap { it.items }.sumOf { byId.getValue(it.foodId).kcalPer100 * it.grams / 100 } + day.cheat.sumOf { it.kcal }

    // ---- Medidas caseiras ----

    @Test
    fun portionTextShowsUnitsAndGrams() {
        assertEquals("1 ovo · 50 g", Portions.text(egg, 50))
        assertEquals("2 ovos · 100 g", Portions.text(egg, 100))
        assertEquals("1,5 ovos · 75 g", Portions.text(egg, 75))
        assertEquals("120 g", Portions.text(rice, 120))
    }

    @Test
    fun snapRoundsToWholeUnitsWithMinimumOfOne() {
        assertEquals(100, Portions.snap(egg, 90))
        assertEquals(50, Portions.snap(egg, 10))
        assertEquals(90, Portions.snap(rice, 90))
    }

    @Test
    fun unitInputNeedsBothFieldsOrNone() {
        assertTrue(Portions.parseUnit("", "") is Portions.UnitInput.Valid)
        assertTrue(Portions.parseUnit("ovo", "") is Portions.UnitInput.Invalid)
        assertTrue(Portions.parseUnit("ovo", "0") is Portions.UnitInput.Invalid)
        assertEquals(50.0, (Portions.parseUnit("ovo", "50") as Portions.UnitInput.Valid).grams!!, 0.0)
    }

    @Test
    fun plannedFoodsWithUnitsComeInWholeUnits() {
        DietPlanner.plan(target, examples, MealSchedule.typesFor(6)).meals.flatMap { it.items }.forEach { item ->
            val food = byId.getValue(item.foodId)
            val unit = Portions.unitGrams(food) ?: return@forEach
            assertEquals(0.0, (item.grams / unit) % 1.0, 0.001)
        }
    }

    // ---- Dia do lixo ----

    @Test
    fun cheatMealsAreCappedAtDailyGoal() {
        val capped = CheatRules.clamp(2000, listOf(CheatMeal(MealType.LUNCH, 1500), CheatMeal(MealType.DINNER, 1500)))
        assertTrue(capped.sumOf { it.kcal } <= 2000)
    }

    @Test
    fun excessIsWhatCheatMealsAddOverTheirDietShare() {
        val cheat = listOf(CheatMeal(MealType.LUNCH, 1000))
        val dietShare = CheatRules.dietKcalOf(2000, types, cheat)
        assertEquals(1000 - dietShare, CheatRules.excess(2000, types, cheat))
        assertEquals(0, CheatRules.excess(2000, types, listOf(CheatMeal(MealType.LUNCH, 100))))
    }

    @Test
    fun otherDaysPayBackTheCheatDay() {
        val week = WeeklyDietPlanner.plan(target, examples, types, CheatDay(6, listOf(CheatMeal(MealType.LUNCH, 1000))))
        assertTrue(week.compensationPerDay > 0)
        val total = week.days.sumOf { dayKcal(it) }
        assertTrue("semana $total", abs(total - 7 * 2000) / (7 * 2000.0) < 0.10)
        val normal = week.days.filter { it.dayOfWeek != 6 }.map { dayKcal(it) }.average()
        assertTrue("dia comum $normal", normal < 2000 - week.compensationPerDay / 2)
    }

    @Test
    fun cheatMealLeavesTheDietAndKeepsOnlyOnItsDay() {
        val week = WeeklyDietPlanner.plan(target, examples, types, CheatDay(6, listOf(CheatMeal(MealType.LUNCH, 800))))
        val saturday = week.days.first { it.dayOfWeek == 6 }
        assertTrue(saturday.meals.none { it.type == MealType.LUNCH })
        assertEquals(listOf(CheatMeal(MealType.LUNCH, 800)), saturday.cheat)
        week.days.filter { it.dayOfWeek != 6 }.forEach { day ->
            assertEquals(types.size, day.meals.size)
            assertTrue(day.cheat.isEmpty())
        }
    }

    @Test
    fun withoutCheatDayEveryDayHitsTheGoal() {
        val week = WeeklyDietPlanner.plan(target, examples, types, null)
        assertEquals(0, week.compensationPerDay)
        assertEquals(7, week.days.size)
        week.days.forEach { assertTrue("dia ${it.dayOfWeek}: ${dayKcal(it)}", abs(dayKcal(it) - 2000) / 2000.0 < 0.10) }
    }

    // ---- Variedade ----

    @Test
    fun weekRotatesFoodsAcrossDays() {
        val week = WeeklyDietPlanner.plan(target, examples, types, null)
        val lunchProteins = week.days.mapNotNull { day ->
            day.meals.first { it.type == MealType.LUNCH }.items
                .map { byId.getValue(it.foodId) }.firstOrNull { it.category == FoodCategory.PROTEIN }?.id
        }.toSet()
        assertTrue("proteínas do almoço: $lunchProteins", lunchProteins.size >= 3)
    }

    @Test
    fun blankWeekHasEmptyMealsAndMarksCheatDay() {
        val week = WeeklyDietPlanner.blank(target, types, CheatDay(7, listOf(CheatMeal(MealType.DINNER, 700))))
        assertEquals(7, week.days.size)
        assertTrue(week.days.all { day -> day.meals.all { it.items.isEmpty() } })
        assertEquals(1, week.days.count { it.cheat.isNotEmpty() })
    }
}
