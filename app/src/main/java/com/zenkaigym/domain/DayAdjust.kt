package com.zenkaigym.domain

import com.zenkaigym.data.entity.FoodCategory
import com.zenkaigym.data.entity.Goal
import com.zenkaigym.data.entity.MealExtra
import com.zenkaigym.data.entity.MealFull
import com.zenkaigym.data.entity.MealType
import kotlin.math.roundToInt

/**
 * Resultado do reajuste do dia: novas gramas por item de refeição (id do MealItem), quanto as
 * próximas refeições mudaram em kcal e quais refeições foram ajustadas.
 */
data class DayAdjustment(
    val grams: Map<Long, Int>,
    val deltaKcal: Int,
    val adjusted: Set<MealType>
) {
    companion object {
        val NONE = DayAdjustment(emptyMap(), 0, emptySet())
    }
}

/**
 * Reajuste do dia, a partir do que foi realmente comido. O cardápio não muda: o app só mostra
 * porções diferentes nas refeições que ainda faltam, para o dia fechar perto do planejado.
 *
 * - Comido = refeições marcadas como feitas (com as kcal gravadas, se houver) + pratos extras.
 * - Uma refeição com extra e sem ✓ conta só o extra (o extra tomou o lugar dela).
 * - Refeição "não realizada" sai da conta: o que faltou é recuperado nas próximas, exceto para quem quer emagrecer.
 * - [dayDelta] soma ao alvo do dia o saldo da semana e o gasto de um treino pulado (negativo = comer menos).
 * - Ajustam-se as refeições não feitas, sem extras e que não são refeição livre.
 * - O carboidrato absorve a diferença primeiro; o que sobrar vai para proteína e fruta. Legume e salada ficam iguais.
 */
object DayAdjust {
    const val MIN_FACTOR = 0.5
    const val MAX_FACTOR = 1.5

    fun mealKcal(meal: MealFull): Double =
        meal.meal.cheatKcal?.toDouble() ?: meal.items.sumOf { it.food.kcalPer100 * it.item.grams / 100 }

    /** Calorias da refeição já com as porções ajustadas. */
    fun adjustedKcal(meal: MealFull, adjustment: DayAdjustment): Double =
        meal.meal.cheatKcal?.toDouble()
            ?: meal.items.sumOf { it.food.kcalPer100 * (adjustment.grams[it.item.id] ?: it.item.grams) / 100 }

    fun compute(
        meals: List<MealFull>,
        done: Set<MealType>,
        extras: List<MealExtra>,
        skipped: Set<MealType> = emptySet(),
        recorded: Map<MealType, Int> = emptyMap(),
        goal: Goal = Goal.MAINTAIN,
        dayDelta: Double = 0.0
    ): DayAdjustment {
        val withExtras = extras.map { it.type }.toSet()
        val missed = skipped - withExtras
        val dayTotal = meals.sumOf(::mealKcal) + dayDelta
        val eaten = meals.filter { it.meal.type in done }.sumOf { recorded[it.meal.type]?.toDouble() ?: mealKcal(it) } +
            extras.sumOf { it.kcal }
        fun open(m: MealFull) = m.meal.type !in done && m.meal.type !in withExtras && m.meal.type !in missed
        val upcoming = meals.filter { open(it) && it.meal.cheatKcal == null && it.items.isNotEmpty() }
        // Refeições livres ainda por vir continuam com as calorias previstas.
        val fixed = meals.filter { open(it) && it.meal.cheatKcal != null }.sumOf(::mealKcal)
        val planned = upcoming.sumOf(::mealKcal)
        if (planned <= 0) return DayAdjustment.NONE

        // Quanto falta (ou sobra) para o dia fechar na meta, já contando o que ainda está planejado.
        var gap = dayTotal - eaten - fixed - planned
        // Quem quer emagrecer não recupera o que deixou de comer.
        if (goal == Goal.LOSE && gap > 0) gap = 0.0
        val budget = planned + gap
        val factor = (budget / planned).coerceIn(MIN_FACTOR, MAX_FACTOR)
        val wanted = planned * (factor - 1)
        if (kotlin.math.abs(wanted) < 15) return DayAdjustment.NONE

        val items = upcoming.flatMap { it.items }
        fun kcalOf(cats: Set<FoodCategory>) = items.filter { it.food.category in cats }.sumOf { it.food.kcalPer100 * it.item.grams / 100 }
        val carbs = setOf(FoodCategory.CARB)
        val others = setOf(FoodCategory.PROTEIN, FoodCategory.FRUIT)
        val carbKcal = kcalOf(carbs)
        val carbFactor = if (carbKcal > 0) (1 + wanted / carbKcal).coerceIn(0.3, 2.0) else 1.0
        val rest = wanted - carbKcal * (carbFactor - 1)
        val otherKcal = kcalOf(others)
        val otherFactor = if (otherKcal > 0) (1 + rest / otherKcal).coerceIn(0.6, 1.4) else 1.0

        val grams = mutableMapOf<Long, Int>()
        var after = 0.0
        items.forEach { entry ->
            val f = when (entry.food.category) {
                FoodCategory.CARB -> carbFactor
                FoodCategory.PROTEIN, FoodCategory.FRUIT -> otherFactor
                else -> 1.0
            }
            val g = if (f == 1.0) entry.item.grams else Portions.snap(entry.food, round5(entry.item.grams * f).coerceIn(5, 1000))
            if (g != entry.item.grams) grams[entry.item.id] = g
            after += entry.food.kcalPer100 * g / 100
        }
        if (grams.isEmpty()) return DayAdjustment.NONE
        return DayAdjustment(grams, (after - planned).roundToInt(), upcoming.map { it.meal.type }.toSet())
    }

    private fun round5(value: Double): Int = (value / 5).roundToInt() * 5
}
