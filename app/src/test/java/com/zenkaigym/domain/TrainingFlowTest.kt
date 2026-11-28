package com.zenkaigym.domain

import com.zenkaigym.data.entity.SetLog
import com.zenkaigym.domain.DojoWorkoutState.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingFlowTest {
    private fun log(ex: Long, i: Int) = SetLog(epochDay = 0, exerciseId = ex, setIndex = i, reps = 10, weightKg = 20.0)

    @Test fun contaSoSeriesDeTrabalhoDaFicha() {
        val logged = listOf(log(1, 1), log(1, 2), log(1, -1), log(2, 1), log(9, 1), log(1, 4))
        assertEquals(SetCount(done = 3, total = 5), setCount(listOf(1L to 3, 2L to 2), logged))
    }

    @Test fun estadoDoCartaoDoTreino() {
        val none = SetCount(0, 5); val some = SetCount(2, 5)
        assertEquals(NO_PLAN, TrainingFlow.dojoState(false, false, false, false, none))
        assertEquals(REST, TrainingFlow.dojoState(true, false, false, false, none))
        assertEquals(PENDING, TrainingFlow.dojoState(true, true, false, false, none))
        assertEquals(IN_PROGRESS, TrainingFlow.dojoState(true, true, false, false, some))
        assertEquals(DONE, TrainingFlow.dojoState(true, true, true, false, some))
        assertEquals(SKIPPED, TrainingFlow.dojoState(true, true, false, true, some))
    }

    @Test fun conclusaoAutomaticaSoNaTransicao() {
        assertTrue(TrainingFlow.shouldAutoComplete(loggedLastPending = true, done = false))
        assertFalse(TrainingFlow.shouldAutoComplete(loggedLastPending = true, done = true)) // depois de "Marcar como feito" ou "Concluir assim"
        assertFalse(TrainingFlow.shouldAutoComplete(loggedLastPending = false, done = false)) // ainda faltam séries, ou foi um aquecimento
    }

    @Test fun botaoPrincipalDoRodape() {
        assertEquals(TrainPrimary.LOG_NEXT, TrainingFlow.primary(done = false, SetCount(3, 5)))
        assertEquals(TrainPrimary.FINISH, TrainingFlow.primary(false, SetCount(5, 5))) // desfez a conclusão com tudo registrado
        assertEquals(TrainPrimary.FINISH, TrainingFlow.primary(false, SetCount(0, 0)))
        assertEquals(TrainPrimary.SUMMARY, TrainingFlow.primary(true, SetCount(2, 5))) // aberto pelo "Treino feito"
    }

    @Test fun perguntaAoSairSoComSeriesENaoConcluido() {
        assertFalse(TrainingFlow.askOnExit(SetCount(0, 5), done = false))
        assertTrue(TrainingFlow.askOnExit(SetCount(1, 5), done = false))
        assertFalse(TrainingFlow.askOnExit(SetCount(3, 5), done = true))
    }
}
