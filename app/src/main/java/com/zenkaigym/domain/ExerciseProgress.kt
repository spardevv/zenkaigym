package com.zenkaigym.domain

import com.zenkaigym.data.entity.SetLog

/**
 * Uma ida ao exercício: as séries feitas num dia (sem aquecimento), as anotações e, no cardio,
 * a distância e as calorias somadas.
 */
data class SessionSummary(
    val epochDay: Long,
    val sets: List<DoneSet>,
    val notes: List<String> = emptyList(),
    val distanceKm: Double? = null,
    val kcal: Int? = null
) {
    val topKg: Double get() = sets.maxOfOrNull { it.weightKg } ?: 0.0
    val totalReps: Int get() = sets.sumOf { it.reps }

    /** Melhor 1RM estimado do dia (Epley). */
    val oneRepMax: Double get() = Progression.bestOneRepMax(sets)
}

/** Resumo do histórico de um exercício, das sessões mais antigas para as mais recentes. */
data class ExerciseProgress(val sessions: List<SessionSummary>) {
    /** Exercícios sem carga (prancha, corrida…) acompanham as repetições, não os quilos. */
    val byLoad: Boolean get() = sessions.any { it.topKg > 0 }

    /** Valor de cada sessão no gráfico: carga máxima, ou repetições totais se não houver carga. */
    val trend: List<Double> get() = sessions.map { if (byLoad) it.topKg else it.totalReps.toDouble() }

    /** 1RM estimado de cada sessão (zero nas sessões sem carga). */
    val oneRepMaxTrend: List<Double> get() = sessions.map { it.oneRepMax }

    val bestOneRepMax: Double get() = oneRepMaxTrend.maxOrNull() ?: 0.0

    val best: Double get() = trend.maxOrNull() ?: 0.0

    /** Dia do recorde; em empate vale o mais antigo, que foi quando ele surgiu. */
    val bestDay: Long?
        get() {
            val values = trend
            val top = values.maxOrNull() ?: return null
            return sessions[values.indexOfFirst { it == top }].epochDay
        }

    /** Diferença entre a última sessão e a primeira; null com menos de duas sessões. */
    val change: Double? get() = if (sessions.size < 2) null else trend.last() - trend.first()

    companion object {
        /** Agrupa as séries registradas por dia; dentro do dia, na ordem das séries. Aquecimentos ficam de fora. */
        fun from(logs: List<SetLog>): ExerciseProgress = ExerciseProgress(
            logs.filter { !it.warmup }
                .groupBy { it.epochDay }
                .toSortedMap()
                .map { (day, sets) ->
                    val ordered = sets.sortedBy { it.setIndex }
                    SessionSummary(
                        epochDay = day,
                        sets = ordered.map { DoneSet(it.reps, it.weightKg, it.rir) },
                        notes = ordered.mapNotNull { it.note?.trim()?.takeIf(String::isNotEmpty) },
                        distanceKm = ordered.mapNotNull { it.distanceKm }.takeIf { it.isNotEmpty() }?.sum(),
                        kcal = ordered.mapNotNull { it.kcal }.takeIf { it.isNotEmpty() }?.sum()
                    )
                }
        )
    }
}
