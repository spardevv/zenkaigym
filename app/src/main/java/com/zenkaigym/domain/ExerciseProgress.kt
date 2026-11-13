package com.zenkaigym.domain

import com.zenkaigym.data.entity.SetLog

/** Uma ida ao exercício: as séries feitas num dia. */
data class SessionSummary(val epochDay: Long, val sets: List<DoneSet>) {
    val topKg: Double get() = sets.maxOfOrNull { it.weightKg } ?: 0.0
    val totalReps: Int get() = sets.sumOf { it.reps }
}

/** Resumo do histórico de um exercício, das sessões mais antigas para as mais recentes. */
data class ExerciseProgress(val sessions: List<SessionSummary>) {
    /** Exercícios sem carga (prancha, corrida…) acompanham as repetições, não os quilos. */
    val byLoad: Boolean get() = sessions.any { it.topKg > 0 }

    /** Valor de cada sessão no gráfico: carga máxima, ou repetições totais se não houver carga. */
    val trend: List<Double> get() = sessions.map { if (byLoad) it.topKg else it.totalReps.toDouble() }

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
        /** Agrupa as séries registradas por dia; dentro do dia, na ordem das séries. */
        fun from(logs: List<SetLog>): ExerciseProgress = ExerciseProgress(
            logs.groupBy { it.epochDay }
                .toSortedMap()
                .map { (day, sets) -> SessionSummary(day, sets.sortedBy { it.setIndex }.map { DoneSet(it.reps, it.weightKg) }) }
        )
    }
}
