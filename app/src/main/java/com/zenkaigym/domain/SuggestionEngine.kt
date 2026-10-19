package com.zenkaigym.domain

import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.Goal
import com.zenkaigym.data.entity.MealType
import com.zenkaigym.data.entity.UserProfile

/**
 * Ponto único de geração de sugestões. A v1 usa regras locais ([RuleBasedEngine]);
 * uma implementação com IA (ex.: Gemini free tier) pode ser plugada depois, com
 * fallback para as regras quando estiver offline.
 */
interface SuggestionEngine {
    suspend fun nutritionTarget(profile: UserProfile, trainingDays: Int): NutritionTarget
    suspend fun workoutPlan(availableDays: List<Int>, goal: Goal, catalog: List<Exercise>, weekStart: WeekStart = WeekStart.MONDAY): PlannedWorkout
    suspend fun dietPlan(target: NutritionTarget, foods: List<Food>, mealTypes: List<MealType>): DietSuggestion
    suspend fun weeklyDietPlan(target: NutritionTarget, foods: List<Food>, mealTypes: List<MealType>, cheat: CheatDay?): WeeklySuggestion
}

class RuleBasedEngine : SuggestionEngine {
    override suspend fun nutritionTarget(profile: UserProfile, trainingDays: Int) =
        NutritionCalculator.suggest(
            profile.sex, profile.weightKg, profile.heightCm, profile.age, profile.goal, trainingDays
        )

    override suspend fun workoutPlan(availableDays: List<Int>, goal: Goal, catalog: List<Exercise>, weekStart: WeekStart) =
        WorkoutPlanner.plan(availableDays, goal, catalog, weekStart)

    override suspend fun dietPlan(target: NutritionTarget, foods: List<Food>, mealTypes: List<MealType>) =
        DietPlanner.plan(target, foods, mealTypes)

    override suspend fun weeklyDietPlan(target: NutritionTarget, foods: List<Food>, mealTypes: List<MealType>, cheat: CheatDay?) =
        WeeklyDietPlanner.plan(target, foods, mealTypes, cheat)
}
