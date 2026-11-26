package com.zenkaigym.domain

import kotlin.random.Random

// ---- Desafios semanais ------------------------------------------------------------------------------------

/** O que foi feito numa semana; base dos desafios e do cartão de resumo. */
data class WeekActivity(
    val sessions: Int = 0,
    val sets: Int = 0,
    val volumeKg: Double = 0.0,
    val mealsDone: Int = 0,
    val fullDietDays: Int = 0,
    val weighIns: Int = 0,
    val waterGoalDays: Int = 0,
    val cardioMinutes: Int = 0,
    val cardioKcal: Int = 0,
    val records: Int = 0
)

/** Um desafio da semana: [title] está em português com {0} = [target] (passa por tr() na tela). */
data class Challenge(val id: String, val kanji: String, val title: String, val target: Int, val progress: Int) {
    val completed: Boolean get() = progress >= target
    val fraction: Float get() = if (target <= 0) 1f else (progress.toFloat() / target).coerceIn(0f, 1f)
}

/**
 * Três desafios por semana: o de treinos sempre aparece e os outros dois mudam a cada semana, sorteados
 * de forma fixa pelo início da semana (a mesma semana mostra sempre os mesmos desafios).
 * Cumprir um desafio vale [XP] uma única vez.
 */
object Challenges {
    const val XP = 40
    const val PER_WEEK = 3

    private class Template(val id: String, val kanji: String, val title: String, val target: (Int, Int) -> Int, val value: (WeekActivity) -> Int)

    /** Alvos crescem com os treinos planejados e as refeições do dia, para servirem a qualquer rotina. */
    private val rotating = listOf(
        Template("sets", "組", "Registre {0} séries", { planned, _ -> 12 * planned.coerceAtLeast(2) }, { it.sets }),
        Template("diet_days", "膳", "Faça todas as refeições em {0} dias", { _, _ -> 4 }, { it.fullDietDays }),
        Template("meals", "食", "Marque {0} refeições", { _, meals -> 5 * meals.coerceAtLeast(3) }, { it.mealsDone }),
        Template("weigh_in", "秤", "Registre seu peso {0} vez", { _, _ -> 1 }, { it.weighIns }),
        Template("water", "水", "Bata a meta de água em {0} dias", { _, _ -> 4 }, { it.waterGoalDays }),
        Template("cardio", "走", "Faça {0} min de cardio", { _, _ -> 45 }, { it.cardioMinutes }),
        Template("record", "新", "Bata {0} recorde de carga", { _, _ -> 1 }, { it.records })
    )

    private val sessions = Template("sessions", "週", "Faça {0} treinos", { planned, _ -> planned.coerceAtLeast(2) }, { it.sessions })

    /** Desafios da semana que começa em [weekStartEpochDay], com o progresso de [activity]. */
    fun forWeek(weekStartEpochDay: Long, plannedPerWeek: Int, mealsPerDay: Int, activity: WeekActivity): List<Challenge> {
        val picked = rotating.shuffled(Random(weekStartEpochDay)).take(PER_WEEK - 1)
        return (listOf(sessions) + picked).map { t ->
            val target = t.target(plannedPerWeek, mealsPerDay)
            Challenge(t.id, t.kanji, t.title, target, t.value(activity).coerceAtMost(target))
        }
    }
}

// ---- Sequências com escudo --------------------------------------------------------------------------------

/** Sequência atual e melhor, com os escudos guardados e quantos já salvaram a sequência. */
data class StreakResult(val current: Int, val best: Int, val shields: Int, val shieldsUsed: Int)

/**
 * Escudo de sequência: a cada [earnEvery] períodos cumpridos seguidos ganha-se um escudo (até [maxShields]).
 * Um período perdido com escudo guardado gasta o escudo e não zera a sequência (o período perdido não soma).
 * Assim uma falha isolada não apaga semanas de constância.
 */
object Shields {
    /**
     * [met]: cada período, do mais antigo ao atual (o último está em andamento: ainda não cumprido
     * não quebra nada, só não soma).
     */
    fun streak(met: List<Boolean>, earnEvery: Int, maxShields: Int): StreakResult {
        var run = 0
        var best = 0
        var shields = 0
        var used = 0
        var sinceEarn = 0
        met.forEachIndexed { i, ok ->
            when {
                ok -> {
                    run++
                    best = maxOf(best, run)
                    sinceEarn++
                    if (earnEvery > 0 && sinceEarn >= earnEvery) {
                        sinceEarn = 0
                        if (shields < maxShields) shields++
                    }
                }
                i == met.lastIndex -> Unit // período atual ainda em aberto
                shields > 0 -> {
                    shields--
                    used++
                }
                else -> {
                    run = 0
                    sinceEarn = 0
                }
            }
        }
        return StreakResult(run, best, shields, used)
    }
}

// ---- Subida de nível ---------------------------------------------------------------------------------------

/** Mudança de nível para a cena de comemoração; [rankUp] quando a patente também mudou. */
data class LevelUp(val fromLevel: Int, val toLevel: Int) {
    val fromRank: Rank get() = Rank.of(fromLevel)
    val toRank: Rank get() = Rank.of(toLevel)
    val rankUp: Boolean get() = toRank != fromRank

    companion object {
        /** Compara o último nível já comemorado com o atual; null se não subiu (ou se ainda não havia referência). */
        fun detect(lastSeen: Int, current: Int): LevelUp? =
            if (lastSeen in 1 until current) LevelUp(lastSeen, current) else null
    }
}
