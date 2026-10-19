package com.zenkaigym.domain

import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.data.entity.WorkoutDayFull
import com.zenkaigym.data.entity.WorkoutSwap

/** Trocas de exercício que valem só para um dia: a ficha não muda, só o que aparece no treino de hoje. */
object WorkoutSwaps {
    /** O dia com os exercícios trocados; trocas de itens que não existem (ou de exercícios apagados) são ignoradas. */
    fun apply(day: WorkoutDayFull, swaps: List<WorkoutSwap>, exercises: List<Exercise>): WorkoutDayFull {
        if (swaps.isEmpty()) return day
        val byId = exercises.associateBy { it.id }
        val byItem = swaps.associate { it.itemId to it.exerciseId }
        return day.copy(items = day.items.map { entry ->
            val swapped = byItem[entry.item.id]?.let(byId::get)
            if (swapped != null) entry.copy(exercise = swapped) else entry
        })
    }

    /** Opções para trocar um exercício: só do mesmo grupo muscular e que ainda não estão no treino do dia. */
    fun options(current: Exercise, exercises: List<Exercise>, inDay: Set<Long>): List<Exercise> =
        exercises.filter { it.group == current.group && it.id != current.id && it.id !in inDay }
}
