package com.zenkaigym.domain

import com.zenkaigym.data.ExerciseCatalog
import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.FoodCategory
import com.zenkaigym.data.entity.Goal
import com.zenkaigym.data.entity.Sex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PlannersTest {
    // Ids sequenciais, como o Room faria.
    private val catalog: List<Exercise> = ExerciseCatalog.all().mapIndexed { i, e -> e.copy(id = i + 1L) }

    @Test
    fun bmr_mifflinStJeor() {
        // 10*80 + 6.25*180 - 5*30 + 5 = 1780
        assertEquals(1780.0, NutritionCalculator.bmr(Sex.MALE, 80.0, 180, 30), 0.001)
        assertEquals(1780.0 - 166, NutritionCalculator.bmr(Sex.FEMALE, 80.0, 180, 30), 0.001)
    }

    @Test
    fun target_maintainThreeDays() {
        val t = NutritionCalculator.suggest(Sex.MALE, 80.0, 180, 30, Goal.MAINTAIN, trainingDays = 3)
        assertEquals(2760, t.kcal) // 1780 * 1.55 = 2759
        assertEquals(128, t.protein) // 1.6 g/kg
    }

    @Test
    fun target_goalsOrderCalories() {
        fun kcal(goal: Goal) = NutritionCalculator.suggest(Sex.FEMALE, 65.0, 165, 28, goal, 4).kcal
        assertTrue(kcal(Goal.LOSE) < kcal(Goal.MAINTAIN))
        assertTrue(kcal(Goal.MAINTAIN) < kcal(Goal.GAIN))
    }

    @Test
    fun workout_threeDaysSpreadMonWedFri() {
        val plan = WorkoutPlanner.plan(listOf(1, 2, 3, 4, 5, 6, 7), Goal.MAINTAIN, catalog)
        // 7 dias disponíveis -> no máximo 6 sessões
        assertEquals(6, plan.days.size)
        val three = WorkoutPlanner.pickSpread(listOf(1, 2, 3, 4, 5, 6, 7), 3)
        assertEquals(listOf(1, 3, 5), three)
    }

    @Test
    fun workout_noAvailabilityGivesEmptyPlan() {
        assertTrue(WorkoutPlanner.plan(emptyList(), Goal.GAIN, catalog).days.isEmpty())
    }

    @Test
    fun workout_noDuplicateExerciseInDay() {
        val plan = WorkoutPlanner.plan(listOf(1, 3, 5, 6), Goal.GAIN, catalog)
        plan.days.forEach { day ->
            val ids = day.items.map { it.exerciseId }
            assertEquals(ids.size, ids.distinct().size)
            assertTrue(day.items.isNotEmpty())
        }
    }

    @Test
    fun diet_macrosCloseToTarget() {
        val foods = listOf(
            Food(1, "Frango", FoodCategory.PROTEIN, 165.0, 31.0, 0.0, 3.6),
            Food(2, "Arroz", FoodCategory.CARB, 130.0, 2.5, 28.0, 0.3),
            Food(3, "Brócolis", FoodCategory.VEGETABLE, 35.0, 2.4, 7.2, 0.4),
            Food(4, "Alface", FoodCategory.SALAD, 17.0, 1.0, 3.0, 0.2)
        ).associateBy { it.id }
        val target = NutritionCalculator.fromKcalAndProtein(2400, 150)
        val suggestion = DietPlanner.plan(target, foods.values.toList(), MealSchedule.typesFor(6))

        var kcal = 0.0
        var protein = 0.0
        suggestion.meals.flatMap { it.items }.forEach {
            val food = foods.getValue(it.foodId)
            kcal += food.kcalPer100 * it.grams / 100
            protein += food.proteinPer100 * it.grams / 100
        }
        assertTrue("kcal $kcal", abs(kcal - target.kcal) / target.kcal < 0.10)
        assertTrue("protein $protein", abs(protein - target.protein) / target.protein < 0.10)
        assertTrue(suggestion.warnings.isEmpty())
    }

    @Test
    fun diet_warnsWhenMissingCategories() {
        val foods = listOf(Food(1, "Frango", FoodCategory.PROTEIN, 165.0, 31.0, 0.0, 3.6))
        val suggestion = DietPlanner.plan(NutritionCalculator.fromKcalAndProtein(2000, 120), foods)
        assertTrue(suggestion.warnings.any { it.contains("carboidrato") })
    }
}
