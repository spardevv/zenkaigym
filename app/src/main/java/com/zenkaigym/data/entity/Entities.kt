package com.zenkaigym.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Sex(val label: String) { MALE("Masculino"), FEMALE("Feminino") }

enum class Goal(val label: String) {
    LOSE("Emagrecer"),
    MAINTAIN("Manter"),
    GAIN("Ganhar massa")
}

enum class FoodCategory(val label: String, val kanji: String) {
    PROTEIN("Proteína", "肉"),
    CARB("Carboidrato", "米"),
    VEGETABLE("Legume", "野"),
    SALAD("Salada", "菜"),
    FRUIT("Fruta", "果")
}

/** Tipo de refeição; define quais alimentos combinam e o horário padrão. */
enum class MealType(val label: String, val defaultTime: String) {
    BREAKFAST("Café da manhã", "07:00"),
    SNACK_AM("Lanche da manhã", "10:00"),
    LUNCH("Almoço", "12:30"),
    SNACK_PM("Lanche da tarde", "16:00"),
    DINNER("Jantar", "19:30"),
    SUPPER("Ceia", "22:00")
}

enum class MuscleGroup(val label: String) {
    CHEST("Peito"), BACK("Costas"), LEGS("Pernas"), SHOULDERS("Ombros"),
    BICEPS("Bíceps"), TRICEPS("Tríceps"), CORE("Abdômen")
}

enum class PlanStatus { PROPOSED, ACTIVE }

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val heightCm: Int,
    val weightKg: Double,
    val age: Int,
    val sex: Sex,
    val goal: Goal,
    // Metas aceitas pelo usuário (null enquanto nada foi aceito).
    val targetKcal: Int? = null,
    val targetProtein: Int? = null,
    val targetCarbs: Int? = null,
    val targetFat: Int? = null
)

@Entity(tableName = "weight_entry")
data class WeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val weightKg: Double
)

/** dayOfWeek: 1 = segunda ... 7 = domingo. */
@Entity(tableName = "availability")
data class Availability(
    @PrimaryKey val dayOfWeek: Int,
    val available: Boolean,
    val startTime: String? = null,
    val endTime: String? = null
)

@Entity(tableName = "food")
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: FoodCategory,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double
)

@Entity(tableName = "exercise")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val group: MuscleGroup
)

@Entity(tableName = "workout_plan")
data class WorkoutPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val status: PlanStatus,
    val splitName: String,
    val createdAt: Long
)

@Entity(
    tableName = "workout_day",
    foreignKeys = [ForeignKey(WorkoutPlan::class, ["id"], ["planId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("planId")]
)
data class WorkoutDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val dayOfWeek: Int,
    val name: String
)

@Entity(
    tableName = "workout_item",
    foreignKeys = [
        ForeignKey(WorkoutDay::class, ["id"], ["dayId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Exercise::class, ["id"], ["exerciseId"])
    ],
    indices = [Index("dayId"), Index("exerciseId")]
)
data class WorkoutItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long,
    val exerciseId: Long,
    val sets: Int,
    val reps: String,
    val position: Int
)

@Entity(tableName = "diet_plan")
data class DietPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val status: PlanStatus,
    val kcal: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val createdAt: Long
)

@Entity(
    tableName = "meal",
    foreignKeys = [ForeignKey(DietPlan::class, ["id"], ["planId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("planId")]
)
data class Meal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val name: String,
    val position: Int,
    val type: MealType = MealType.LUNCH
)

/** Agenda de refeições do usuário: uma linha por refeição do dia, com seu horário (HH:mm). */
@Entity(tableName = "meal_slot")
data class MealSlot(
    @PrimaryKey val type: MealType,
    val position: Int,
    val time: String
)

/** Refeição marcada como feita em um dia (independe do plano, que pode ser trocado). */
@Entity(tableName = "meal_check", primaryKeys = ["type", "epochDay"])
data class MealCheck(
    val type: MealType,
    val epochDay: Long
)

@Entity(
    tableName = "meal_item",
    foreignKeys = [
        ForeignKey(Meal::class, ["id"], ["mealId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Food::class, ["id"], ["foodId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("mealId"), Index("foodId")]
)
data class MealItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
    val foodId: Long,
    val grams: Int
)
