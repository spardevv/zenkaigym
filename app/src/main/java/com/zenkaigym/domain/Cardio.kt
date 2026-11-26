package com.zenkaigym.domain

import kotlin.math.roundToInt

/**
 * Cardio com números de verdade: gasto estimado pelo MET da atividade (Compêndio de Atividades Físicas)
 * e ritmo quando há distância. São estimativas para acompanhar a evolução, não medições.
 */
object Cardio {
    const val DEFAULT_MET = 6.0

    /** MET típico de cada cardio do catálogo (pelo nome em português); exercícios próprios usam [DEFAULT_MET]. */
    fun met(exerciseName: String, minutes: Int = 0, distanceKm: Double? = null): Double {
        val name = exerciseName.lowercase()
        // Esteira e caminhadas: com distância, a velocidade diz se foi caminhada, trote ou corrida.
        val speed = speedKmh(minutes, distanceKm)
        if (speed != null && (name.contains("esteira") || name.contains("caminhada") || name.contains("corrida"))) {
            return when {
                speed < 5.0 -> 3.5
                speed < 6.5 -> 4.3
                speed < 8.5 -> 8.3
                speed < 10.5 -> 9.8
                speed < 12.0 -> 11.0
                else -> 11.8
            }
        }
        return when {
            name.contains("hiit") -> 8.5
            name.contains("bicicleta") || name.contains("bike") -> 6.8
            name.contains("caminhada inclinada") -> 6.0
            name.contains("caminhada") -> 4.3
            name.contains("esteira") || name.contains("corrida") -> 8.3
            name.contains("elíptico") || name.contains("eliptico") -> 5.0
            name.contains("escada") -> 9.0
            name.contains("corda") -> 11.0
            name.contains("polichinelo") || name.contains("burpee") -> 8.0
            name.contains("remo") -> 7.0
            name.contains("natação") || name.contains("natacao") -> 7.0
            else -> DEFAULT_MET
        }
    }

    /** kcal = MET × 3,5 × peso (kg) ÷ 200 × minutos. */
    fun kcal(met: Double, weightKg: Double, minutes: Int): Int =
        if (minutes <= 0 || weightKg <= 0) 0 else (met * 3.5 * weightKg / 200.0 * minutes).roundToInt()

    fun kcal(exerciseName: String, weightKg: Double, minutes: Int, distanceKm: Double? = null): Int =
        kcal(met(exerciseName, minutes, distanceKm), weightKg, minutes)

    fun speedKmh(minutes: Int, distanceKm: Double?): Double? =
        if (distanceKm == null || distanceKm <= 0 || minutes <= 0) null else distanceKm / (minutes / 60.0)

    /** Ritmo em minutos por km, ex.: 5.5 = 5:30/km. */
    fun paceMinPerKm(minutes: Int, distanceKm: Double?): Double? =
        if (distanceKm == null || distanceKm <= 0 || minutes <= 0) null else minutes / distanceKm

    /** "5:30/km". */
    fun paceLabel(minutes: Int, distanceKm: Double?): String? {
        val pace = paceMinPerKm(minutes, distanceKm) ?: return null
        val totalSeconds = (pace * 60).roundToInt()
        return "%d:%02d/km".format(totalSeconds / 60, totalSeconds % 60)
    }
}
