package com.zenkaigym.domain

import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.data.entity.Goal
import com.zenkaigym.data.entity.MuscleGroup
import com.zenkaigym.data.entity.MuscleGroup.BACK
import com.zenkaigym.data.entity.MuscleGroup.BICEPS
import com.zenkaigym.data.entity.MuscleGroup.CHEST
import com.zenkaigym.data.entity.MuscleGroup.CORE
import com.zenkaigym.data.entity.MuscleGroup.LEGS
import com.zenkaigym.data.entity.MuscleGroup.SHOULDERS
import com.zenkaigym.data.entity.MuscleGroup.TRICEPS

data class PlannedItem(val exerciseId: Long, val sets: Int, val reps: String)
data class PlannedDay(val dayOfWeek: Int, val name: String, val items: List<PlannedItem>)
data class PlannedWorkout(val splitName: String, val days: List<PlannedDay>)

object WorkoutPlanner {
    private const val MAX_SESSIONS = 6

    private class Session(val name: String, val slots: List<Pair<MuscleGroup, Int>>)

    private val full = Session("Corpo inteiro", listOf(CHEST to 1, BACK to 1, LEGS to 2, SHOULDERS to 1, CORE to 1))
    private val push = Session("Push · peito, ombro, tríceps", listOf(CHEST to 2, SHOULDERS to 2, TRICEPS to 2))
    private val pull = Session("Pull · costas, bíceps", listOf(BACK to 3, BICEPS to 2))
    private val legs = Session("Pernas", listOf(LEGS to 5, CORE to 1))
    private val upper = Session("Superiores", listOf(CHEST to 2, BACK to 2, SHOULDERS to 1, BICEPS to 1, TRICEPS to 1))
    private val lower = Session("Inferiores", listOf(LEGS to 5, CORE to 1))

    fun plan(availableDays: List<Int>, goal: Goal, catalog: List<Exercise>): PlannedWorkout {
        val days = availableDays.filter { it in 1..7 }.distinct().sorted()
        if (days.isEmpty()) return PlannedWorkout("", emptyList())

        val sessions = minOf(days.size, MAX_SESSIONS)
        val (splitName, templates) = when (sessions) {
            1 -> "Corpo inteiro" to listOf(full)
            2 -> "Corpo inteiro 2x" to listOf(full, full)
            3 -> "ABC · Push / Pull / Pernas" to listOf(push, pull, legs)
            4 -> "Superiores / Inferiores" to listOf(upper, lower, upper, lower)
            5 -> "Push / Pull / Pernas + Sup. / Inf." to listOf(push, pull, legs, upper, lower)
            else -> "Push / Pull / Pernas 2x" to listOf(push, pull, legs, push, pull, legs)
        }

        val trainingDays = pickSpread(days, sessions)
        val taken = mutableMapOf<MuscleGroup, Int>()
        val planned = trainingDays.mapIndexed { i, dow ->
            val template = templates[i]
            val items = template.slots.flatMap { (group, count) ->
                val pool = catalog.filter { it.group == group }
                val n = minOf(count, pool.size)
                (0 until n).map {
                    val index = taken.getOrDefault(group, 0)
                    taken[group] = index + 1
                    val exercise = pool[index % pool.size]
                    PlannedItem(exercise.id, setsFor(goal), repsFor(goal, exercise))
                }
            }
            PlannedDay(dow, template.name, items)
        }
        return PlannedWorkout(splitName, planned)
    }

    /** Escolhe [n] dias espalhados ao longo da lista, deixando o descanso o mais uniforme possível. */
    internal fun pickSpread(sortedDays: List<Int>, n: Int): List<Int> =
        if (n >= sortedDays.size) sortedDays
        else (0 until n).map { sortedDays[it * sortedDays.size / n] }

    private fun setsFor(goal: Goal) = if (goal == Goal.GAIN) 4 else 3

    private fun repsFor(goal: Goal, exercise: Exercise): String = when {
        exercise.name == "Prancha" -> "30-60s"
        exercise.group == CORE -> "15-20"
        goal == Goal.GAIN -> "8-10"
        goal == Goal.LOSE -> "12-15"
        else -> "10-12"
    }
}
