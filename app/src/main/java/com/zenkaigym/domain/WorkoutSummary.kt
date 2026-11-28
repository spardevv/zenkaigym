package com.zenkaigym.domain

import com.zenkaigym.data.entity.SetLog

/** Resumo do treino do dia mostrado na conclusão. */
data class DaySummary(val sets: Int, val volumeKg: Double, val records: List<RecordEntry>, val xp: Int)

object WorkoutSummary {
    /** Só séries de trabalho contam; aquecimento não entra em volume, recorde nem XP. */
    fun of(sets: List<SetLog>, today: Long, names: Map<Long, String>): DaySummary {
        val work = sets.filter { !it.warmup }
        val day = work.filter { it.epochDay == today }
        val records = Progress.personalRecords(work, names).filter { it.epochDay == today }
        return DaySummary(
            sets = day.size,
            volumeKg = day.sumOf { it.weightKg * it.reps },
            records = records,
            xp = XpRules.SESSION + day.size * XpRules.SET + records.size * XpRules.RECORD
        )
    }
}
