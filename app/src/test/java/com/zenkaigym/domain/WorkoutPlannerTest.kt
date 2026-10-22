package com.zenkaigym.domain

import com.zenkaigym.data.ExerciseCatalog
import com.zenkaigym.data.entity.CardioPref
import com.zenkaigym.data.entity.Equipment
import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.data.entity.ExerciseKind
import com.zenkaigym.data.entity.Goal
import com.zenkaigym.data.entity.Level
import com.zenkaigym.data.entity.MuscleGroup
import com.zenkaigym.data.entity.TrainingFocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutPlannerTest {
    private val catalog: List<Exercise> = ExerciseCatalog.all().mapIndexed { i, e -> e.copy(id = i + 1L) }
    private val byId = catalog.associateBy { it.id }
    private val groupOf = catalog.associate { it.id to it.group }

    private fun request(
        days: Map<Int, Int>, goal: Goal = Goal.MAINTAIN, level: Level = Level.INTERMEDIATE,
        cardio: CardioPref = CardioPref.NONE, focus: TrainingFocus = TrainingFocus.BALANCED,
        equipment: Equipment = Equipment.GYM
    ) = WorkoutRequest(days, goal, level, cardio, focus, equipment)

    private fun worstCase(goal: Goal, cardio: CardioPref = CardioPref.NONE) =
        request(mapOf(1 to 40, 3 to 40, 5 to 40), goal, cardio = cardio)

    /** Tronco e lombar entram em quase toda sessão e recuperam rápido: ficam fora da regra de dias seguidos. */
    private val tronco = setOf(MuscleGroup.ABS, MuscleGroup.OBLIQUES, MuscleGroup.LOWER_BACK, MuscleGroup.CARDIO)

    private fun groupsOn(plan: PlannedWorkout, day: Int): Set<MuscleGroup> =
        plan.days.first { it.dayOfWeek == day }.items.map { groupOf.getValue(it.exerciseId) }.toSet() - tronco

    @Test
    fun worstCase_threeDaysOf40Minutes_fitsTheTimeInEveryGoal() {
        for (goal in Goal.entries) for (cardio in CardioPref.entries) {
            val plan = WorkoutPlanner.plan(worstCase(goal, cardio), catalog)
            assertEquals(3, plan.days.size)
            plan.days.forEach {
                val minutes = WorkoutPlanner.estimateMinutes(it, byId, goal, 40)
                assertTrue("$goal/$cardio dia ${it.dayOfWeek}: $minutes min", minutes <= 40.0)
                assertTrue("$goal/$cardio dia ${it.dayOfWeek} sem exercícios", it.items.isNotEmpty())
            }
        }
    }

    @Test
    fun worstCase_everyBigMuscleGetsItsWeeklyMinimum_andCoreIsTrained() {
        for (goal in Goal.entries) {
            val volume = WorkoutPlanner.weeklyVolume(WorkoutPlanner.plan(worstCase(goal), catalog), byId)
            val big = listOf(MuscleGroup.CHEST, MuscleGroup.LATS, MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES)
            big.forEach {
                assertTrue("$goal: $it com ${volume.getValue(it)} séries", volume.getValue(it) >= WorkoutPlanner.minSets(it, Level.INTERMEDIATE))
            }
            assertTrue("$goal: core com ${volume.getValue(MuscleGroup.ABS)} séries", volume.getValue(MuscleGroup.ABS) >= 3.0)
        }
    }

    @Test
    fun worstCase_withCardio_hasCardioEverySession() {
        val finisher = WorkoutPlanner.plan(worstCase(Goal.LOSE, CardioPref.FINISHER), catalog)
        finisher.days.forEach {
            val last = it.items.last()
            assertEquals(MuscleGroup.CARDIO, groupOf.getValue(last.exerciseId))
            assertEquals("10min", last.reps)
        }
        val warmup = WorkoutPlanner.plan(worstCase(Goal.LOSE, CardioPref.WARMUP), catalog)
        warmup.days.forEach {
            assertEquals(MuscleGroup.CARDIO, groupOf.getValue(it.items.first().exerciseId))
            assertEquals("5min", it.items.first().reps)
        }
        val none = WorkoutPlanner.plan(worstCase(Goal.LOSE), catalog)
        assertTrue(none.days.flatMap { it.items }.none { groupOf.getValue(it.exerciseId) == MuscleGroup.CARDIO })
    }

    @Test
    fun fullBodySessions_doNotRepeatTheSameExerciseInOneDay() {
        val plan = WorkoutPlanner.plan(worstCase(Goal.GAIN), catalog)
        plan.days.forEach { day ->
            val ids = day.items.map { it.exerciseId }
            assertEquals(ids.size, ids.toSet().size)
        }
    }

    @Test
    fun noMuscleGroupOnConsecutiveDays_fromFourSessionsUp() {
        for (sessions in 4..6) {
            val days = (1..sessions).toList()
            val plan = WorkoutPlanner.plan(request(days.associateWith { 60 }, Goal.GAIN), catalog)
            for (day in days.dropLast(1)) {
                val overlap = groupsOn(plan, day).intersect(groupsOn(plan, day + 1)) - MuscleGroup.CARDIO
                assertTrue("$sessions sessões: $overlap treinado nos dias $day e ${day + 1}", overlap.isEmpty())
            }
        }
    }

    @Test
    fun moreTimeMeansMoreVolume() {
        val short = WorkoutPlanner.plan(request(mapOf(1 to 40, 3 to 40, 5 to 40)), catalog)
        val long = WorkoutPlanner.plan(request(mapOf(1 to 90, 3 to 90, 5 to 90)), catalog)
        fun total(p: PlannedWorkout) = p.days.sumOf { d -> d.items.sumOf { it.sets } }
        assertTrue(total(long) > total(short))
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
    fun goalDrivesRepsOfCompoundExercises() {
        fun firstCompoundReps(goal: Goal) = WorkoutPlanner.plan(listOf(1), goal, catalog).days.single().items
            .first { byId.getValue(it.exerciseId).kind == ExerciseKind.COMPOUND }.reps
        assertEquals("6-10", firstCompoundReps(Goal.GAIN))
        assertEquals("10-15", firstCompoundReps(Goal.LOSE))
    }

    @Test
    fun plankIsTimedAndCoreUsesHighReps() {
        val items = WorkoutPlanner.plan(worstCase(Goal.GAIN), catalog).days.flatMap { it.items }
        val core = items.filter { groupOf[it.exerciseId] in setOf(MuscleGroup.ABS, MuscleGroup.OBLIQUES) }
        assertTrue(core.isNotEmpty())
        core.forEach {
            val kind = byId.getValue(it.exerciseId).kind
            assertEquals(if (kind == ExerciseKind.HOLD) "30-45s" else "15-20", it.reps)
        }
    }

    @Test
    fun bodyweightOnly_usesOnlyBodyweightExercises() {
        val plan = WorkoutPlanner.plan(
            request(mapOf(1 to 45, 3 to 45, 5 to 45), cardio = CardioPref.FINISHER, equipment = Equipment.BODYWEIGHT), catalog
        )
        plan.days.flatMap { it.items }.forEach {
            assertEquals(Equipment.BODYWEIGHT, byId.getValue(it.exerciseId).equipment)
        }
    }

    @Test
    fun beginnerAvoidsAdvancedExercises_andDoesLessVolume() {
        // Com tempo de sobra, o alvo semanal é que separa os níveis.
        val beginner = WorkoutPlanner.plan(request(mapOf(1 to 150, 3 to 150, 5 to 150), level = Level.BEGINNER), catalog)
        val advanced = WorkoutPlanner.plan(request(mapOf(1 to 150, 3 to 150, 5 to 150), level = Level.ADVANCED), catalog)
        beginner.days.flatMap { it.items }.forEach {
            assertEquals(com.zenkaigym.data.entity.Level.BEGINNER, ExerciseCatalog.level(byId.getValue(it.exerciseId)))
        }
        fun total(p: PlannedWorkout) = p.days.sumOf { d -> d.items.sumOf { it.sets } }
        assertTrue(total(advanced) >= total(beginner))
    }

    @Test
    fun focusAddsVolumeToTheFocusedMuscles() {
        val days = mapOf(1 to 75, 3 to 75, 5 to 75)
        val base = WorkoutPlanner.weeklyVolume(WorkoutPlanner.plan(request(days), catalog), byId)
        val glutes = WorkoutPlanner.weeklyVolume(WorkoutPlanner.plan(request(days, focus = TrainingFocus.GLUTES), catalog), byId)
        assertTrue(glutes.getValue(MuscleGroup.GLUTES) > base.getValue(MuscleGroup.GLUTES))
    }

    @Test
    fun everyDayHasAtLeastFourExercises_withAnHourOrMore() {
        for (n in 1..7) {
            WorkoutPlanner.plan(request((1..n).associateWith { 60 }), catalog).days.forEach {
                assertTrue("dia ${it.dayOfWeek} com ${it.items.size} exercícios", it.items.size >= 4)
            }
        }
    }
}
