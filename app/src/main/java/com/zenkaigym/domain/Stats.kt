package com.zenkaigym.domain

import kotlin.math.roundToInt

/** Progresso de treino para o perfil: semanas da mais antiga para a atual. */
data class TrainingProgress(
    val weeklyVolume: List<Double>,
    val weeklySessions: List<Int>,
    /** Variação do volume desta semana sobre a anterior, em %; null sem base de comparação. */
    val volumeChange: Int?,
    val sessionsLast30: Int
) {
    val hasData: Boolean get() = weeklySessions.any { it > 0 }
}

/** Aderência da dieta: fração das refeições feitas por dia, do mais antigo para hoje. */
data class DietProgress(
    val daily: List<Float>,
    val averagePercent: Int,
    /** Dias seguidos com todas as refeições feitas. */
    val streak: Int
) {
    val hasData: Boolean get() = daily.any { it > 0f }
}

/** Regras puras dos gráficos de progresso (sem banco, para testar). Semanas começam no dia escolhido em Ajustes. */
object Stats {
    const val WEEKS = 8
    const val DIET_DAYS = 14

    /** Soma os valores por semana de calendário; índice 0 = mais antiga, último = semana atual (até hoje). */
    fun weekly(values: Map<Long, Double>, today: Long, weeks: Int = WEEKS, weekStart: WeekStart = WeekStart.MONDAY): List<Double> {
        val out = DoubleArray(weeks)
        val current = weekStart.weekStartOf(today)
        values.forEach { (day, value) ->
            if (day > today) return@forEach
            val back = (current - weekStart.weekStartOf(day)) / 7
            if (back in 0 until weeks) out[weeks - 1 - back.toInt()] += value
        }
        return out.toList()
    }

    /** Primeiro dia que entra nos gráficos semanais (início da semana mais antiga). */
    fun firstDay(today: Long, weekStart: WeekStart, weeks: Int = WEEKS): Long = weekStart.weekStartOf(today) - (weeks - 1) * 7L

    fun changePercent(previous: Double, current: Double): Int? =
        if (previous <= 0.0) null else ((current - previous) / previous * 100).roundToInt()

    /** Dias seguidos em que [hit] vale, contando de hoje (ou de ontem, se hoje ainda não conta). */
    fun streak(today: Long, hit: (Long) -> Boolean): Int {
        var day = if (hit(today)) today else today - 1
        var count = 0
        while (hit(day)) {
            count++
            day--
        }
        return count
    }

    /**
     * Treino: um dia conta como sessão se teve séries registradas ou foi marcado como concluído.
     * [volumePerDay] = soma de carga × repetições por dia.
     */
    fun training(volumePerDay: Map<Long, Double>, sessionDays: Set<Long>, today: Long, weekStart: WeekStart = WeekStart.MONDAY): TrainingProgress {
        val volume = weekly(volumePerDay, today, weekStart = weekStart)
        val sessions = weekly(sessionDays.associateWith { 1.0 }, today, weekStart = weekStart).map { it.toInt() }
        return TrainingProgress(
            weeklyVolume = volume,
            weeklySessions = sessions,
            volumeChange = changePercent(volume[volume.size - 2], volume.last()),
            sessionsLast30 = sessionDays.count { today - it in 0..29 }
        )
    }

    fun diet(donePerDay: Map<Long, Int>, mealsPerDay: Int, today: Long, days: Int = DIET_DAYS): DietProgress {
        fun fraction(day: Long) = if (mealsPerDay <= 0) 0f else ((donePerDay[day] ?: 0).toFloat() / mealsPerDay).coerceIn(0f, 1f)
        val daily = List(days) { i -> fraction(today - (days - 1 - i)) }
        return DietProgress(
            daily = daily,
            averagePercent = (daily.average() * 100).roundToInt(),
            streak = if (mealsPerDay <= 0) 0 else streak(today) { fraction(it) >= 1f }
        )
    }
}
