package com.zenkaigym.domain

import com.zenkaigym.data.entity.MuscleGroup
import com.zenkaigym.data.entity.WorkoutDayFull
import com.zenkaigym.data.entity.WorkoutLog
import com.zenkaigym.util.dayOfWeekFromEpochDay

/**
 * Qual sessão da ficha cai em cada dia da semana. A ficha tem uma sessão por dia de treino; quando uma
 * sessão fica "não realizada" ela passa para o próximo dia de treino e as seguintes deslizam. Se sobrarem
 * mais sessões do que dias, as de menor prioridade ficam de fora (e o app avisa).
 */
object WorkoutWeek {
    /**
     * [session] é a sessão de hoje (null = descanso), [moved] diz que ela veio de outro dia da ficha e
     * [dropped] são as sessões que não cabem mais nesta semana.
     */
    data class Assignment(
        val session: WorkoutDayFull?,
        val moved: Boolean = false,
        val dropped: List<WorkoutDayFull> = emptyList()
    ) {
        companion object {
            val REST = Assignment(null)
        }
    }

    private val big = setOf(MuscleGroup.CHEST, MuscleGroup.LATS, MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES)

    /** Quanto a sessão vale: séries dos grupos grandes (mais séries = mais difícil de abrir mão). */
    fun priority(day: WorkoutDayFull): Int =
        day.items.filter { it.exercise.group in big }.sumOf { it.item.sets }

    fun assign(sessions: List<WorkoutDayFull>, today: Long, weekStart: WeekStart, logs: List<WorkoutLog>): Assignment {
        if (sessions.isEmpty()) return Assignment.REST
        val start = weekStart.weekStartOf(today)
        val ordered = sessions.sortedBy { weekStart.position(it.day.dayOfWeek) }
        val dates = ordered.map { start + weekStart.position(it.day.dayOfWeek) }
        if (today !in dates) return Assignment.REST

        val marked = logs.associateBy { it.epochDay }
        val queue = ArrayDeque(ordered)
        for (date in dates) {
            if (date >= today || queue.isEmpty()) break
            // Só o "não realizado" devolve a sessão à fila; feito, ou esquecido sem marcar, ela se foi.
            if (marked[date]?.finished == false) continue
            queue.removeFirst()
        }
        val head = queue.firstOrNull() ?: return Assignment.REST

        val daysLeft = dates.count { it > today }
        val rest = queue.drop(1)
        val overflow = rest.size - daysLeft
        val dropped = if (overflow <= 0) emptyList() else {
            rest.withIndex()
                .sortedWith(compareBy({ priority(it.value) }, { -it.index }))
                .take(overflow)
                .map { it.value }
        }
        return Assignment(head, moved = head.day.dayOfWeek != dayOfWeekFromEpochDay(today), dropped = dropped)
    }
}
