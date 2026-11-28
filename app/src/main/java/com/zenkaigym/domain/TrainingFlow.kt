package com.zenkaigym.domain

import com.zenkaigym.data.entity.SetLog

/** Séries de trabalho feitas e total da ficha de hoje. */
data class SetCount(val done: Int, val total: Int) {
    val complete: Boolean get() = done >= total
}

/**
 * Conta as séries de trabalho (setIndex de 1 até o nº de séries do exercício), igual ao modo treino.
 * [plan] = (exerciseId, nº de séries).
 */
fun setCount(plan: List<Pair<Long, Int>>, logged: Collection<SetLog>): SetCount {
    val keys = logged.map { it.exerciseId to it.setIndex }.toSet()
    val done = plan.sumOf { (ex, sets) -> (1..sets).count { (ex to it) in keys } }
    return SetCount(done, plan.sumOf { it.second })
}

/** Estado do cartão do treino no Dojo. */
enum class DojoWorkoutState { NO_PLAN, REST, PENDING, IN_PROGRESS, DONE, SKIPPED }

/** Botão principal do rodapé do modo treino. */
enum class TrainPrimary { LOG_NEXT, FINISH, SUMMARY }

object TrainingFlow {
    fun dojoState(hasPlan: Boolean, hasSession: Boolean, done: Boolean, skipped: Boolean, count: SetCount): DojoWorkoutState = when {
        !hasPlan -> DojoWorkoutState.NO_PLAN
        !hasSession -> DojoWorkoutState.REST
        done -> DojoWorkoutState.DONE
        skipped -> DojoWorkoutState.SKIPPED
        count.done > 0 -> DojoWorkoutState.IN_PROGRESS
        else -> DojoWorkoutState.PENDING
    }

    /** Botão principal: treino já concluído abre o resumo; tudo registrado pede para concluir; senão registra a próxima série. */
    fun primary(done: Boolean, count: SetCount): TrainPrimary = when {
        done -> TrainPrimary.SUMMARY
        count.complete -> TrainPrimary.FINISH
        else -> TrainPrimary.LOG_NEXT
    }

    /** Conclui sozinho só na transição: registrou a última série de trabalho pendente e ainda não estava concluído. */
    fun shouldAutoComplete(loggedLastPending: Boolean, done: Boolean): Boolean = loggedLastPending && !done

    /** Pergunta ao sair pelo X só se há séries de trabalho registradas e o treino não foi concluído. */
    fun askOnExit(count: SetCount, done: Boolean): Boolean = count.done > 0 && !done
}
