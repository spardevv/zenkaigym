package com.zenkaigym.domain

import com.zenkaigym.data.FoodExamples
import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.FoodCategory
import com.zenkaigym.data.entity.MealSlot
import com.zenkaigym.data.entity.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DietPlannerTest {
    private val chicken = Food(1, "Frango", FoodCategory.PROTEIN, 165.0, 31.0, 0.0, 3.6)
    private val rice = Food(2, "Arroz", FoodCategory.CARB, 130.0, 2.5, 28.0, 0.3)
    private val broccoli = Food(3, "Brócolis", FoodCategory.VEGETABLE, 35.0, 2.4, 7.2, 0.4)
    private val lettuce = Food(4, "Alface", FoodCategory.SALAD, 17.0, 1.0, 3.0, 0.2)
    private val all = listOf(chicken, rice, broccoli, lettuce)
    private val six = MealSchedule.typesFor(6)

    // Alimentos reais do app, com ids sequenciais como o Room faria.
    private val examples = FoodExamples.all().mapIndexed { i, f -> f.copy(id = i + 1L) }
    private val examplesById = examples.associateBy { it.id }

    private fun target(kcal: Int, protein: Int) = NutritionCalculator.fromKcalAndProtein(kcal, protein)

    private fun totalKcal(s: DietSuggestion, byId: Map<Long, Food>) =
        s.meals.flatMap { it.items }.sumOf { byId.getValue(it.foodId).kcalPer100 * it.grams / 100 }

    @Test
    fun mealCountFollowsSchedule() {
        listOf(3, 4, 5, 6).forEach { n ->
            assertEquals(n, DietPlanner.plan(target(2400, 140), examples, MealSchedule.typesFor(n)).meals.size)
        }
    }

    @Test
    fun noPortionAbove600g() {
        DietPlanner.plan(target(2780, 132), all, six).meals.forEach { meal ->
            meal.items.forEach { assertTrue("${meal.name}: ${it.grams} g", it.grams <= 600) }
        }
    }

    @Test
    fun totalCaloriesCloseToTarget_withRealFoods() {
        listOf(1600, 2100, 2780, 3200).forEach { kcal ->
            val total = totalKcal(DietPlanner.plan(target(kcal, kcal / 21), examples, six), examplesById)
            assertTrue("meta $kcal, plano $total", abs(total - kcal) / kcal < 0.10)
        }
    }

    @Test
    fun vegetablesAndSaladOnlyAtLunchAndDinner() {
        DietPlanner.plan(target(2780, 132), examples, six).meals.forEach { meal ->
            val hasVeg = meal.items.any { examplesById.getValue(it.foodId).category in setOf(FoodCategory.VEGETABLE, FoodCategory.SALAD) }
            assertEquals(meal.name, meal.type == MealType.LUNCH || meal.type == MealType.DINNER, hasVeg)
        }
    }

    @Test
    fun mainMealsHaveProteinAndCarb() {
        DietPlanner.plan(target(2400, 140), examples, six).meals
            .filter { it.type == MealType.LUNCH || it.type == MealType.DINNER || it.type == MealType.BREAKFAST }
            .forEach { meal ->
                val cats = meal.items.map { examplesById.getValue(it.foodId).category }
                assertTrue(meal.name, FoodCategory.PROTEIN in cats && FoodCategory.CARB in cats)
            }
    }

    @Test
    fun breakfastUsesMorningFoods_lunchUsesMainFoods() {
        val plan = DietPlanner.plan(target(2400, 140), examples, six)
        fun names(type: MealType) = plan.meals.first { it.type == type }.items.map { examplesById.getValue(it.foodId).name }

        val breakfast = names(MealType.BREAKFAST)
        assertTrue("café: $breakfast", breakfast.any { n -> listOf("Ovo", "Queijo", "Whey", "Iogurte", "Peito de peru").any { n.startsWith(it) } })
        assertTrue("café: $breakfast", breakfast.any { n -> listOf("Pão", "Aveia", "Tapioca", "Cuscuz").any { n.startsWith(it) } })
        assertFalse("café: $breakfast", breakfast.any { it.contains("frango", true) || it.contains("Arroz") || it.contains("Alcatra") })

        val main = names(MealType.LUNCH) + names(MealType.DINNER)
        assertTrue("almoço/jantar: $main", main.any { n -> listOf("Peito de frango", "Carne", "Alcatra", "Tilápia", "Salmão", "Atum").any { n.startsWith(it) } })
        assertTrue("almoço/jantar: $main", main.any { n -> listOf("Arroz", "Feijão", "Batata", "Mandioca", "Macarrão").any { n.startsWith(it) } })
        assertFalse("almoço/jantar: $main", main.any { it.startsWith("Whey") || it.startsWith("Pão") || it.startsWith("Aveia") })
    }

    @Test
    fun supperIsLight_withoutMeatOrRice() {
        val plan = DietPlanner.plan(target(2400, 140), examples, six)
        val supper = plan.meals.first { it.type == MealType.SUPPER }.items.map { examplesById.getValue(it.foodId).name }
        assertFalse(supper.toString(), supper.any { it.contains("Arroz") || it.contains("Alcatra") || it.contains("Carne") || it.contains("Feijão") })
    }

    @Test
    fun gramsAreMultiplesOfFive() {
        DietPlanner.plan(target(2780, 132), examples, six).meals.flatMap { it.items }
            .forEach { assertEquals(0, it.grams % 5) }
    }

    @Test
    fun foodsRotateBetweenMeals() {
        val second = chicken.copy(id = 5, name = "Carne moída", kcalPer100 = 219.0, proteinPer100 = 27.0, fatPer100 = 12.0)
        val suggestion = DietPlanner.plan(target(2780, 132), all + second, six)
        val proteinIds = suggestion.meals.flatMap { it.items }.map { it.foodId }.filter { it == 1L || it == 5L }.toSet()
        assertEquals(setOf(1L, 5L), proteinIds)
    }

    @Test
    fun noFoods_givesWarningsAndEmptyMeals() {
        val suggestion = DietPlanner.plan(target(2000, 120), emptyList())
        assertTrue(suggestion.meals.all { it.items.isEmpty() })
        assertEquals(4, suggestion.warnings.size)
    }

    @Test
    fun missingVegetables_warnsButStillBuildsMeals() {
        val suggestion = DietPlanner.plan(target(2000, 120), listOf(chicken, rice))
        assertTrue(suggestion.warnings.any { it.contains("legume") })
        assertTrue(suggestion.meals.all { it.items.isNotEmpty() })
    }

    // ---- Troca de itens ----

    @Test
    fun equivalentGrams_keepsProteinForProteins_andKcalForCarbs() {
        val egg = Food(9, "Ovo", FoodCategory.PROTEIN, 155.0, 13.0, 1.1, 11.0)
        assertEquals(240, DietPlanner.equivalentGrams(chicken, 100, egg)) // 31 g de proteína ≈ 240 g de ovo
        val potato = Food(8, "Batata", FoodCategory.CARB, 86.0, 1.6, 20.0, 0.1)
        assertEquals(150, DietPlanner.equivalentGrams(rice, 100, potato)) // 130 kcal ≈ 150 g
        assertEquals(100, DietPlanner.equivalentGrams(broccoli, 100, broccoli.copy(id = 7, name = "Couve")))
    }

    @Test
    fun alternatives_putsSuitedFoodsFirst_andExcludesCurrent() {
        val result = DietPlanner.alternatives(examples, FoodCategory.PROTEIN, MealType.BREAKFAST, excludeId = 1L)
        assertTrue(result.none { it.id == 1L })
        assertTrue(result.all { it.category == FoodCategory.PROTEIN })
        val firstUnsuited = result.indexOfFirst { FoodSuitability.score(it, MealType.BREAKFAST) == 0 }
        val lastSuited = result.indexOfLast { FoodSuitability.score(it, MealType.BREAKFAST) > 0 }
        assertTrue(firstUnsuited == -1 || lastSuited < firstUnsuited)
    }
}

class FoodSuitabilityTest {
    private fun food(name: String, cat: FoodCategory) = Food(1, name, cat, 100.0, 10.0, 10.0, 1.0)

    @Test
    fun recognizesTypicalFoods() {
        assertEquals(2, FoodSuitability.score(food("Ovos mexidos", FoodCategory.PROTEIN), MealType.BREAKFAST))
        assertEquals(2, FoodSuitability.score(food("Pão francês", FoodCategory.CARB), MealType.BREAKFAST))
        assertEquals(0, FoodSuitability.score(food("Pão francês", FoodCategory.CARB), MealType.LUNCH))
        assertEquals(2, FoodSuitability.score(food("Contra-filé grelhado", FoodCategory.PROTEIN), MealType.DINNER))
        assertEquals(0, FoodSuitability.score(food("Whey protein", FoodCategory.PROTEIN), MealType.LUNCH))
    }

    @Test
    fun macarraoIsNotAFruit() {
        assertEquals(0, FoodSuitability.score(food("Macarrão", FoodCategory.CARB), MealType.SUPPER))
    }

    @Test
    fun unknownNames_areAllowedButRankLower() {
        assertEquals(1, FoodSuitability.score(food("Xyz", FoodCategory.PROTEIN), MealType.BREAKFAST))
        assertEquals(0, FoodSuitability.score(food("Xyz", FoodCategory.VEGETABLE), MealType.BREAKFAST))
    }
}

class MealScheduleTest {
    private fun slots(vararg pairs: Pair<MealType, String>) =
        pairs.mapIndexed { i, (t, time) -> MealSlot(t, i, time) }

    @Test
    fun build_keepsKnownTimesWhenCountChanges() {
        val four = MealSchedule.build(4).map { if (it.type == MealType.LUNCH) it.copy(time = "13:15") else it }
        val five = MealSchedule.build(5, four)
        assertEquals(5, five.size)
        assertEquals("13:15", five.first { it.type == MealType.LUNCH }.time)
        assertEquals(MealType.SNACK_AM.defaultTime, five.first { it.type == MealType.SNACK_AM }.time)
    }

    @Test
    fun next_picksFirstPendingMealStillInWindow() {
        val s = slots(MealType.BREAKFAST to "07:00", MealType.LUNCH to "12:30", MealType.DINNER to "19:30")
        val types = s.map { it.type }
        assertEquals(MealType.BREAKFAST, MealSchedule.next(types, s, emptySet(), 6 * 60))
        assertEquals(MealType.BREAKFAST, MealSchedule.next(types, s, emptySet(), 8 * 60))
        assertEquals(MealType.LUNCH, MealSchedule.next(types, s, setOf(MealType.BREAKFAST), 8 * 60))
        assertEquals(MealType.LUNCH, MealSchedule.next(types, s, emptySet(), 10 * 60))
    }

    @Test
    fun next_isNullWhenEverythingDoneOrPassed() {
        val s = slots(MealType.BREAKFAST to "07:00", MealType.LUNCH to "12:30")
        val types = s.map { it.type }
        assertEquals(null, MealSchedule.next(types, s, types.toSet(), 9 * 60))
        assertEquals(null, MealSchedule.next(types, s, emptySet(), 22 * 60))
    }
}
