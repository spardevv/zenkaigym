package com.zenkaigym.domain

import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.data.entity.MuscleGroup
import com.zenkaigym.data.entity.WorkoutDay
import com.zenkaigym.data.entity.WorkoutDayFull
import com.zenkaigym.data.entity.WorkoutItem
import com.zenkaigym.data.entity.WorkoutItemFull
import com.zenkaigym.data.entity.WorkoutSwap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class WorkoutSwapsTest {
    private val lateral = Exercise(1, "Elevação lateral", MuscleGroup.SHOULDERS_SIDE)
    private val frontal = Exercise(2, "Elevação frontal", MuscleGroup.SHOULDERS_SIDE)
    private val press = Exercise(3, "Desenvolvimento com halteres", MuscleGroup.SHOULDERS_SIDE)
    private val bench = Exercise(4, "Supino reto com barra", MuscleGroup.CHEST)
    private val all = listOf(lateral, frontal, press, bench)

    private val day = WorkoutDayFull(
        WorkoutDay(id = 1, planId = 1, dayOfWeek = 1, name = "Ombros"),
        listOf(
            WorkoutItemFull(WorkoutItem(10, 1, lateral.id, 3, "10-12", 0), lateral),
            WorkoutItemFull(WorkoutItem(11, 1, press.id, 3, "8-10", 1), press)
        )
    )

    @Test
    fun semTrocas_devolveODiaIgual() {
        assertSame(day, WorkoutSwaps.apply(day, emptyList(), all))
    }

    @Test
    fun trocaSoOItemIndicado() {
        val swapped = WorkoutSwaps.apply(day, listOf(WorkoutSwap(1, 10, frontal.id)), all)
        assertEquals(listOf(frontal, press), swapped.items.map { it.exercise })
        // Séries e repetições da ficha continuam.
        assertEquals(day.items.map { it.item }, swapped.items.map { it.item })
    }

    @Test
    fun trocaDeItemOuExercicioInexistenteEIgnorada() {
        val swapped = WorkoutSwaps.apply(day, listOf(WorkoutSwap(1, 99, frontal.id), WorkoutSwap(1, 11, 999)), all)
        assertEquals(listOf(lateral, press), swapped.items.map { it.exercise })
    }

    @Test
    fun opcoes_soDoMesmoGrupoEForaDoTreino() {
        val options = WorkoutSwaps.options(lateral, all, inDay = setOf(lateral.id, press.id))
        assertEquals(listOf(frontal), options)
    }
}
