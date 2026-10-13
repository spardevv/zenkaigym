package com.zenkaigym.domain

import com.zenkaigym.data.entity.Goal
import com.zenkaigym.data.entity.Sex
import kotlin.math.roundToInt

data class NutritionTarget(val kcal: Int, val protein: Int, val carbs: Int, val fat: Int)

object NutritionCalculator {
    private const val FAT_SHARE = 0.25

    /** Taxa metabólica basal — Mifflin-St Jeor. */
    fun bmr(sex: Sex, weightKg: Double, heightCm: Int, age: Int): Double =
        10 * weightKg + 6.25 * heightCm - 5 * age + if (sex == Sex.MALE) 5 else -161

    /** O nível de atividade é derivado da quantidade de dias de treino na semana. */
    fun activityFactor(trainingDays: Int): Double = when {
        trainingDays <= 0 -> 1.2
        trainingDays <= 2 -> 1.375
        trainingDays <= 4 -> 1.55
        else -> 1.725
    }

    fun suggest(
        sex: Sex,
        weightKg: Double,
        heightCm: Int,
        age: Int,
        goal: Goal,
        trainingDays: Int
    ): NutritionTarget {
        val tdee = bmr(sex, weightKg, heightCm, age) * activityFactor(trainingDays)
        val adjusted = tdee * when (goal) {
            Goal.LOSE -> 0.80
            Goal.MAINTAIN -> 1.0
            Goal.GAIN -> 1.10
        }
        val kcal = (adjusted / 10).roundToInt() * 10
        val protein = (weightKg * when (goal) {
            Goal.LOSE -> 2.0
            Goal.MAINTAIN -> 1.6
            Goal.GAIN -> 1.8
        }).roundToInt()
        return fromKcalAndProtein(kcal, protein)
    }

    /** Completa a meta: gordura = 25% das calorias e carboidrato = o que sobrar. */
    fun fromKcalAndProtein(kcal: Int, protein: Int): NutritionTarget {
        val fat = (kcal * FAT_SHARE / 9).roundToInt()
        val carbs = ((kcal - protein * 4 - fat * 9) / 4.0).roundToInt().coerceAtLeast(0)
        return NutritionTarget(kcal, protein, carbs, fat)
    }
}
