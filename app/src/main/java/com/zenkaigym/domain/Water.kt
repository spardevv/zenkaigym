package com.zenkaigym.domain

import kotlin.math.roundToInt

/** Hidratação: meta diária pelo peso e o quanto falta. */
object Water {
    /** Um copo: o passo do botão "+". */
    const val GLASS_ML = 250

    /** Recomendação usual para adultos ativos: cerca de 35 ml por kg de peso. */
    const val ML_PER_KG = 35

    const val MIN_GOAL_ML = 1500
    const val MAX_GOAL_ML = 5000

    /** Teto de um dia, para um toque repetido por engano não virar um número absurdo. */
    const val MAX_DAY_ML = 10_000

    /** Meta do dia em ml, arredondada a 50 ml; sem peso informado, usa 70 kg. */
    fun goalMl(weightKg: Double?): Int {
        val kg = weightKg?.takeIf { it > 0 } ?: 70.0
        return ((kg * ML_PER_KG / 50).roundToInt() * 50).coerceIn(MIN_GOAL_ML, MAX_GOAL_ML)
    }

    /** Soma [deltaMl] ao total do dia, sem ficar negativo nem passar do teto. */
    fun add(currentMl: Int, deltaMl: Int): Int = (currentMl + deltaMl).coerceIn(0, MAX_DAY_ML)

    fun fraction(ml: Int, goalMl: Int): Float = if (goalMl <= 0) 0f else (ml.toFloat() / goalMl).coerceIn(0f, 1f)

    /** "2.45 L", "2 L" ou "800 ml" (litros a partir de 1000 ml). */
    fun label(ml: Int): String {
        if (ml < 1000) return "$ml ml"
        val liters = String.format(java.util.Locale.US, "%.2f", ml / 1000.0).trimEnd('0').trimEnd('.')
        return "$liters L"
    }
}
