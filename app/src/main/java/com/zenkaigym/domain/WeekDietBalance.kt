package com.zenkaigym.domain

import com.zenkaigym.data.entity.DietPlanFull
import com.zenkaigym.data.entity.Goal
import com.zenkaigym.data.entity.MealCheck
import com.zenkaigym.data.entity.MealExtra
import com.zenkaigym.util.dayOfWeekFromEpochDay
import kotlin.math.roundToInt

/** Saldo da semana até ontem e quanto dele entra no dia de hoje. */
data class WeekBalance(
    /** Calorias a mais (positivo) ou a menos (negativo) que o planejado nos dias que já passaram. */
    val surplusKcal: Int,
    /** Ajuste do alvo de hoje (negativo = comer menos), já filtrado pelo objetivo e limitado. */
    val todayKcal: Int
) {
    companion object {
        val NONE = WeekBalance(0, 0)
    }
}

/**
 * Compensação do saldo calórico dentro da semana. O que passou ou faltou nos dias anteriores é dividido
 * pelos dias que restam, no máximo [MAX_SHARE] do dia por dia:
 * - emagrecer: só compensa excesso; falta não vira comida extra;
 * - ganhar massa: só repõe o que faltou;
 * - manter: compensa os dois lados.
 * Refeição não marcada num dia que passou conta como comida conforme o planejado; "não realizada" conta zero.
 * Nada é gravado: cada dia recalcula a partir do histórico.
 */
object WeekDietBalance {
    const val MAX_SHARE = 0.10
    private const val WORKOUT_MET = 5.0

    /** Gasto estimado de um treino (MET 5) de [minutes] minutos. */
    fun workoutBurnKcal(weightKg: Double, minutes: Int): Int = (WORKOUT_MET * weightKg * minutes / 60.0).roundToInt()

    /** Gasto estimado dos treinos não realizados que já passaram, por dia, para [compute]. */
    fun skippedBurn(
        logs: List<com.zenkaigym.data.entity.WorkoutLog>, today: Long, weightKg: Double, minutesOf: (Int) -> Int
    ): Map<Long, Int> =
        logs.filter { !it.finished && it.epochDay < today }
            .associate { it.epochDay to workoutBurnKcal(weightKg, minutesOf(dayOfWeekFromEpochDay(it.epochDay))) }

    /**
     * @param skippedWorkoutBurn gasto estimado dos treinos não realizados, por dia (só conta para emagrecer/manter).
     */
    fun compute(
        diet: DietPlanFull?,
        today: Long,
        weekStart: WeekStart,
        goal: Goal,
        checks: List<MealCheck>,
        extras: List<MealExtra>,
        skippedWorkoutBurn: Map<Long, Int> = emptyMap()
    ): WeekBalance {
        if (diet == null) return WeekBalance.NONE
        val start = weekStart.weekStartOf(today)
        if (today <= start) return WeekBalance.NONE

        var surplus = 0.0
        for (day in start until today) {
            val meals = diet.meals.filter { it.meal.dayOfWeek == dayOfWeekFromEpochDay(day) || it.meal.dayOfWeek == 0 }
            if (meals.isEmpty()) continue
            val dayChecks = checks.filter { it.epochDay == day }.associateBy { it.type }
            val dayExtras = extras.filter { it.epochDay == day }
            var planned = 0.0
            var eaten = 0.0
            for (meal in meals) {
                val kcal = DayAdjust.mealKcal(meal)
                val check = dayChecks[meal.meal.type]
                val mine = dayExtras.filter { it.type == meal.meal.type }.sumOf { it.kcal }
                planned += kcal
                eaten += when {
                    check != null && check.skipped -> mine.toDouble()
                    check != null -> (check.kcal?.toDouble() ?: kcal) + mine
                    mine > 0 -> mine.toDouble() // o prato extra tomou o lugar da refeição
                    else -> kcal
                }
            }
            if (goal != Goal.GAIN) planned -= skippedWorkoutBurn[day] ?: 0
            surplus += eaten - planned
        }

        val wanted = when (goal) {
            Goal.LOSE -> maxOf(surplus, 0.0)
            Goal.GAIN -> minOf(surplus, 0.0)
            Goal.MAINTAIN -> surplus
        }
        if (wanted == 0.0) return WeekBalance(surplus.roundToInt(), 0)

        val daysLeft = 7 - weekStart.position(dayOfWeekFromEpochDay(today))
        val todayMeals = diet.meals.filter { it.meal.dayOfWeek == dayOfWeekFromEpochDay(today) || it.meal.dayOfWeek == 0 }
        val todayTotal = todayMeals.sumOf(DayAdjust::mealKcal)
        val limit = todayTotal * MAX_SHARE
        val share = (-wanted / daysLeft).coerceIn(-limit, limit)
        return WeekBalance(surplus.roundToInt(), share.roundToInt())
    }
}
