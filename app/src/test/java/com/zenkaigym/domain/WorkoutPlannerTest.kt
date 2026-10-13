package com.zenkaigym.domain

import com.zenkaigym.data.ExerciseCatalog
import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.data.entity.Goal
import com.zenkaigym.data.entity.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutPlannerTest {
    private val catalog: List<Exercise> = ExerciseCatalog.all().mapIndexed { i, e -> e.copy(id = i + 1L) }
    private val groupOf = catalog.associate { it.id to it.group }

    private fun groupsOn(plan: PlannedWorkout, day: Int): Set<MuscleGroup> =
        plan.days.first { it.dayOfWeek == day }.items.map { groupOf.getValue(it.exerciseId) }.toSet()

    @Test
    fun noMuscleGroupOnConsecutiveDays_fromThreeSessionsUp() {
        for (sessions in 3..6) {
            val days = (1..sessions).toList()
            val plan = WorkoutPlanner.plan(days, Goal.GAIN, catalog)
            for (day in days.dropLast(1)) {
                val overlap = groupsOn(plan, day).intersect(groupsOn(plan, day + 1))
                assertTrue("$sessions sessões: $overlap treinado nos dias $day e ${day + 1}", overlap.isEmpty())
            }
        }
    }

    @Test
    fun sessionsMatchAvailableDays_capped_atSix() {
        assertEquals(1, WorkoutPlanner.plan(listOf(3), Goal.MAINTAIN, catalog).days.size)
        assertEquals(4, WorkoutPlanner.plan(listOf(1, 2, 4, 6), Goal.MAINTAIN, catalog).days.size)
        assertEquals(6, WorkoutPlanner.plan((1..7).toList(), Goal.MAINTAIN, catalog).days.size)
    }

    @Test
    fun trainingDaysAreOnlyAvailableDays() {
        val available = listOf(2, 4, 7)
        val plan = WorkoutPlanner.plan(available, Goal.LOSE, catalog)
        assertTrue(plan.days.all { it.dayOfWeek in available })
    }

    @Test
    fun invalidAndDuplicateDaysAreIgnored() {
        val plan = WorkoutPlanner.plan(listOf(0, 3, 3, 9), Goal.MAINTAIN, catalog)
        assertEquals(listOf(3), plan.days.map { it.dayOfWeek })
    }

    @Test
    fun everyTrainingDayHasBetweenFourAndEightExercises() {
        for (n in 1..7) {
            WorkoutPlanner.plan((1..n).toList(), Goal.MAINTAIN, catalog).days.forEach {
                assertTrue("dia ${it.dayOfWeek} com ${it.items.size} exercícios", it.items.size in 4..8)
            }
        }
    }

    @Test
    fun goalDrivesSetsAndReps() {
        val gain = WorkoutPlanner.plan(listOf(1), Goal.GAIN, catalog).days.single().items.first()
        val lose = WorkoutPlanner.plan(listOf(1), Goal.LOSE, catalog).days.single().items.first()
        assertEquals(4, gain.sets)
        assertEquals("8-10", gain.reps)
        assertEquals(3, lose.sets)
        assertEquals("12-15", lose.reps)
    }

    @Test
    fun plankIsTimedAndCoreUsesHighReps() {
        val items = WorkoutPlanner.plan(listOf(1), Goal.GAIN, catalog).days.single().items
        val plank = catalog.first { it.name == "Prancha" }
        items.filter { it.exerciseId == plank.id }.forEach { assertEquals("30-60s", it.reps) }
        items.filter { groupOf[it.exerciseId] == MuscleGroup.CORE && it.exerciseId != plank.id }
            .forEach { assertEquals("15-20", it.reps) }
    }
}
