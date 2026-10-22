package com.zenkaigym.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zenkaigym.domain.tr

enum class Sex(val pt: String) {
    MALE("Masculino"), FEMALE("Feminino");

    val label: String get() = tr(pt)
}

enum class Goal(val pt: String) {
    LOSE("Emagrecer"),
    MAINTAIN("Manter"),
    GAIN("Ganhar massa");

    val label: String get() = tr(pt)
}

enum class FoodCategory(val pt: String, val kanji: String) {
    PROTEIN("Proteína", "肉"),
    CARB("Carboidrato", "米"),
    VEGETABLE("Legume", "野"),
    SALAD("Salada", "菜"),
    FRUIT("Fruta", "果");

    val label: String get() = tr(pt)
}

/** Tipo de refeição; define quais alimentos combinam e o horário padrão. */
enum class MealType(val pt: String, val defaultTime: String) {
    BREAKFAST("Café da manhã", "07:00"),
    SNACK_AM("Lanche da manhã", "10:00"),
    LUNCH("Almoço", "12:30"),
    SNACK_PM("Lanche da tarde", "16:00"),
    DINNER("Jantar", "19:30"),
    SUPPER("Ceia", "22:00");

    /** Nome no idioma atual; [pt] é o nome guardado nas refeições do cardápio. */
    val label: String get() = tr(pt)
}

enum class MuscleGroup(val pt: String) {
    CHEST("Peito"), LATS("Grande dorsal"), UPPER_BACK("Pequena dorsal"), SHOULDERS("Ombros"),
    BICEPS("Bíceps"), TRICEPS("Tríceps"), QUADS("Quadríceps"), HAMSTRINGS("Posterior de coxa"),
    GLUTES("Glúteos"), ADDUCTORS("Adutores"), CALVES("Panturrilha"), CORE("Core"), CARDIO("Cardio");

    val label: String get() = tr(pt)
}

/** Como o exercício é feito: define tempo por série, repetições e o que se registra no modo treino. */
enum class ExerciseKind { COMPOUND, ISOLATION, HOLD, CARDIO }

/** Equipamento mínimo do exercício, ou o que o usuário tem; cada nível inclui os de baixo. */
enum class Equipment(val pt: String) {
    GYM("Academia completa"), DUMBBELLS("Halteres em casa"), BODYWEIGHT("Peso corporal");

    val label: String get() = tr(pt)

    /** O que o usuário tem ([this]) permite um exercício que pede [needed]. */
    fun allows(needed: Equipment): Boolean = needed.ordinal >= ordinal
}

enum class Level(val pt: String) {
    BEGINNER("Iniciante"), INTERMEDIATE("Intermediário"), ADVANCED("Avançado");

    val label: String get() = tr(pt)
}

enum class CardioPref(val pt: String) {
    NONE("Sem cardio"), WARMUP("Só aquecimento"), FINISHER("No fim do treino");

    val label: String get() = tr(pt)
}

enum class TrainingFocus(val pt: String) {
    BALANCED("Equilibrado"), UPPER("Superiores"), LOWER("Inferiores"), GLUTES("Glúteos");

    val label: String get() = tr(pt)
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
    val targetFat: Int? = null,
    // Preferências de treino (orientam a ficha sugerida).
    val level: Level = Level.INTERMEDIATE,
    val cardio: CardioPref = CardioPref.NONE,
    val focus: TrainingFocus = TrainingFocus.BALANCED,
    val equipment: Equipment = Equipment.GYM
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
    val fatPer100: Double,
    /** Medida caseira (ex.: "ovo", "fatia") e o peso de uma unidade em gramas; ambos nulos se o alimento é só por peso. */
    val unitLabel: String? = null,
    val unitGrams: Double? = null
)

@Entity(tableName = "exercise")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val group: MuscleGroup,
    val kind: ExerciseKind = ExerciseKind.ISOLATION,
    val equipment: Equipment = Equipment.GYM
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
    val createdAt: Long,
    /** Dia da semana (1 = segunda ... 7 = domingo) com refeições livres ("dia do lixo"); null se não há. */
    val cheatDay: Int? = null
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
    val type: MealType = MealType.LUNCH,
    /** 1 = segunda ... 7 = domingo; 0 = vale para todos os dias (planos de antes do cardápio semanal). */
    val dayOfWeek: Int = 0,
    /** Não nulo = refeição livre (fora da dieta) com essas calorias estimadas; não tem itens. */
    val cheatKcal: Int? = null
)

/** Treino de um dia marcado como concluído ([finished]) ou como não realizado (independe da ficha, que pode ser trocada). */
@Entity(tableName = "workout_log")
data class WorkoutLog(
    @PrimaryKey val epochDay: Long,
    val finished: Boolean = true
)

/** Uma série realizada. Guarda o exercício (não o item da ficha) para o histórico sobreviver a trocas de ficha. */
@Entity(
    tableName = "set_log",
    indices = [Index(value = ["epochDay", "exerciseId", "setIndex"], unique = true)]
)
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val exerciseId: Long,
    val setIndex: Int,
    val reps: Int,
    val weightKg: Double
)

/** Agenda de refeições do usuário: uma linha por refeição do dia, com seu horário (HH:mm). */
@Entity(tableName = "meal_slot")
data class MealSlot(
    @PrimaryKey val type: MealType,
    val position: Int,
    val time: String
)

/**
 * Refeição marcada num dia (independe do plano, que pode ser trocado): feita ou, com [skipped], não realizada.
 * [kcal] guarda o que foi comido de fato (já com o ajuste do dia) quando a refeição é marcada como feita.
 */
@Entity(tableName = "meal_check", primaryKeys = ["type", "epochDay"])
data class MealCheck(
    val type: MealType,
    val epochDay: Long,
    val skipped: Boolean = false,
    val kcal: Int? = null
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

/** Exercício trocado só num dia: o item da ficha continua igual, mas naquele dia usa [exerciseId]. */
@Entity(tableName = "workout_swap", primaryKeys = ["epochDay", "itemId"])
data class WorkoutSwap(
    val epochDay: Long,
    val itemId: Long,
    val exerciseId: Long
)

/**
 * Algo comido fora do cardápio numa refeição de um dia: um alimento da lista ([foodId] + [grams])
 * ou um prato livre ([name] com valores estimados). [kcal] e [protein] ficam gravados na hora.
 */
@Entity(tableName = "meal_extra", indices = [Index("epochDay")])
data class MealExtra(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val type: MealType,
    val foodId: Long? = null,
    val grams: Int? = null,
    val name: String,
    val kcal: Int,
    val protein: Int
)
