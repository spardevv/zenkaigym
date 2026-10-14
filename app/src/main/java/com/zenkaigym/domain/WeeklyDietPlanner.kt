package com.zenkaigym.domain

import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.MealType
import kotlin.math.roundToInt

/** Refeição feita fora da dieta, com as calorias estimadas pelo usuário. */
data class CheatMeal(val type: MealType, val kcal: Int)

/** "Dia do lixo": o dia da semana (1 = segunda ... 7 = domingo) e as refeições livres dele. */
data class CheatDay(val dayOfWeek: Int, val meals: List<CheatMeal>)

data class PlannedDietDay(val dayOfWeek: Int, val meals: List<PlannedMeal>, val cheat: List<CheatMeal>)

data class WeeklySuggestion(
    val days: List<PlannedDietDay>,
    /** Quantas kcal cada um dos outros dias perde para compensar o dia do lixo. */
    val compensationPerDay: Int,
    val warnings: List<String>
)

/**
 * Regras do dia do lixo. As refeições livres substituem refeições da dieta; o que passar do que elas
 * valeriam na dieta é descontado, em partes iguais, nos outros seis dias, de modo que a semana
 * continue somando 7 × a meta diária.
 */
object CheatRules {
    private const val OTHER_DAYS = 6

    /** O total de refeições livres do dia não passa da meta diária de calorias. */
    fun limit(goalKcal: Int): Int = goalKcal

    /** Quanto as refeições [cheat] valeriam na dieta de um dia com [allTypes]. */
    fun dietKcalOf(goalKcal: Int, allTypes: List<MealType>, cheat: List<CheatMeal>): Int =
        cheat.sumOf { goalKcal * DietPlanner.shareOf(it.type, allTypes) }.roundToInt()

    /** Reduz proporcionalmente as refeições livres se o total passar do limite diário. */
    fun clamp(goalKcal: Int, cheat: List<CheatMeal>): List<CheatMeal> {
        val total = cheat.sumOf { it.kcal }
        val limit = limit(goalKcal)
        if (total <= limit || total == 0) return cheat
        return cheat.map { it.copy(kcal = (it.kcal.toDouble() * limit / total / 10).roundToInt() * 10) }
    }

    /** Calorias além da dieta no dia do lixo (nunca negativo: refeição livre leve não "empresta" calorias). */
    fun excess(goalKcal: Int, allTypes: List<MealType>, cheat: List<CheatMeal>): Int =
        (cheat.sumOf { it.kcal } - dietKcalOf(goalKcal, allTypes, cheat)).coerceAtLeast(0)

    /** Quanto o dia deve somar: no dia do lixo, a dieta das outras refeições + as livres; nos demais, a meta menos a compensação. */
    fun dayTarget(goalKcal: Int, allTypes: List<MealType>, cheat: List<CheatMeal>, isCheatDay: Boolean): Int =
        if (isCheatDay) goalKcal - dietKcalOf(goalKcal, allTypes, cheat) + cheat.sumOf { it.kcal }
        else goalKcal - compensationPerDay(goalKcal, allTypes, cheat)

    fun compensationPerDay(goalKcal: Int, allTypes: List<MealType>, cheat: List<CheatMeal>): Int =
        (excess(goalKcal, allTypes, cheat).toDouble() / OTHER_DAYS).roundToInt()
}

/** Monta os sete dias do cardápio, variando os alimentos de um dia para o outro. */
object WeeklyDietPlanner {
    /** Cardápio em branco para o usuário montar à mão; o dia do lixo já vem marcado. */
    fun blank(target: NutritionTarget, types: List<MealType>, cheat: CheatDay?): WeeklySuggestion {
        val ordered = types.distinct().sortedBy { it.ordinal }
        val cheatMeals = CheatRules.clamp(target.kcal, cheat?.meals.orEmpty().filter { it.type in ordered })
        val cheatDay = cheat?.dayOfWeek?.takeIf { cheatMeals.isNotEmpty() }
        val days = (1..7).map { dow ->
            val dayCheat = if (dow == cheatDay) cheatMeals else emptyList()
            val meals = ordered.filter { type -> dayCheat.none { it.type == type } }.map { PlannedMeal(it.pt, it, emptyList()) }
            PlannedDietDay(dow, meals, dayCheat)
        }
        return WeeklySuggestion(days, CheatRules.compensationPerDay(target.kcal, ordered, cheatMeals), emptyList())
    }

    fun plan(target: NutritionTarget, foods: List<Food>, types: List<MealType>, cheat: CheatDay?): WeeklySuggestion {
        val ordered = types.distinct().sortedBy { it.ordinal }
        val cheatMeals = CheatRules.clamp(target.kcal, cheat?.meals.orEmpty().filter { it.type in ordered })
        val cheatDay = cheat?.dayOfWeek?.takeIf { cheatMeals.isNotEmpty() }
        val compensation = CheatRules.compensationPerDay(target.kcal, ordered, cheatMeals)
        val used = mutableMapOf<Long, Int>()
        val warnings = mutableListOf<String>()

        val days = (1..7).map { dow ->
            val dayCheat = if (dow == cheatDay) cheatMeals else emptyList()
            val dietTypes = ordered.filter { type -> dayCheat.none { it.type == type } }
            val dayTarget = when {
                dow == cheatDay -> {
                    // As refeições da dieta ficam como seriam; só as livres saem do cardápio.
                    val share = dietTypes.sumOf { DietPlanner.shareOf(it, ordered) }
                    NutritionCalculator.fromKcalAndProtein((target.kcal * share).roundToInt(), (target.protein * share).roundToInt())
                }
                else -> NutritionCalculator.fromKcalAndProtein(target.kcal - compensation, target.protein)
            }
            val suggestion = if (dietTypes.isEmpty()) DietSuggestion(emptyList(), emptyList())
            else DietPlanner.plan(dayTarget, foods, dietTypes, used)
            suggestion.warnings.filter { it !in warnings }.forEach { warnings += it }
            PlannedDietDay(dow, suggestion.meals, dayCheat)
        }
        return WeeklySuggestion(days, compensation, warnings)
    }
}
