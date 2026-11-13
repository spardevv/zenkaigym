package com.zenkaigym.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class WorkoutItemFull(
    @Embedded val item: WorkoutItem,
    @Relation(parentColumn = "exerciseId", entityColumn = "id") val exercise: Exercise
)

data class WorkoutDayFull(
    @Embedded val day: WorkoutDay,
    @Relation(entity = WorkoutItem::class, parentColumn = "id", entityColumn = "dayId")
    val items: List<WorkoutItemFull>
)

data class WorkoutPlanFull(
    @Embedded val plan: WorkoutPlan,
    @Relation(entity = WorkoutDay::class, parentColumn = "id", entityColumn = "planId")
    val days: List<WorkoutDayFull>
)

data class MealItemFull(
    @Embedded val item: MealItem,
    @Relation(parentColumn = "foodId", entityColumn = "id") val food: Food
)

data class MealFull(
    @Embedded val meal: Meal,
    @Relation(entity = MealItem::class, parentColumn = "id", entityColumn = "mealId")
    val items: List<MealItemFull>
)

data class DietPlanFull(
    @Embedded val plan: DietPlan,
    @Relation(entity = Meal::class, parentColumn = "id", entityColumn = "planId")
    val meals: List<MealFull>
)
