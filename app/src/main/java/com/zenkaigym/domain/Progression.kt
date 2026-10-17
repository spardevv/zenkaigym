package com.zenkaigym.domain

import kotlin.math.max

/** Série realizada, sem dependência do banco. */
data class DoneSet(val reps: Int, val weightKg: Double)

/** Faixa de repetições planejada, ex.: "8-10" (ou só "12"). */
data class RepRange(val min: Int, val max: Int)

object Progression {
    const val STEP_KG = 2.0

    fun parseReps(text: String): RepRange? {
        val nums = Regex("\\d+").findAll(text).map { it.value.toInt() }.toList()
        return when (nums.size) {
            0 -> null
            1 -> RepRange(nums[0], nums[0])
            else -> RepRange(minOf(nums[0], nums[1]), max(nums[0], nums[1]))
        }
    }

    /**
     * Carga sugerida para a próxima vez. Se todas as séries da última vez chegaram ao topo da faixa,
     * sobe [STEP_KG]; senão mantém a maior carga usada. Sem histórico, devolve null.
     */
    fun nextWeight(last: List<DoneSet>, planned: String): Double? {
        if (last.isEmpty()) return null
        val top = last.maxOf { it.weightKg }
        val range = parseReps(planned) ?: return top
        val hitTop = last.all { it.reps >= range.max }
        return if (hitTop && top > 0) top + STEP_KG else top
    }

    /**
     * Valores iniciais da série [setIndex] (1 = primeira): repete a mesma série do treino anterior.
     * Se da última vez houve menos séries, usa a última feita. Sem histórico: sem carga e o topo da faixa planejada.
     */
    fun prefill(last: List<DoneSet>, setIndex: Int, planned: String): DoneSet? =
        last.getOrNull(setIndex - 1) ?: last.lastOrNull() ?: parseReps(planned)?.let { DoneSet(it.max, 0.0) }

    /** Recorde pessoal: carga maior que qualquer registro anterior (e maior que zero). */
    fun isPersonalRecord(weightKg: Double, previousBest: Double): Boolean = weightKg > 0 && weightKg > previousBest

    /** Descanso sugerido entre séries: séries pesadas (poucas repetições) pedem mais tempo. */
    fun restSeconds(reps: Int): Int = when {
        reps <= 6 -> 120
        reps <= 10 -> 90
        else -> 60
    }

    /** Ânimo da sessão pela fração de séries feitas: kanji + frase (em português, para tr()). */
    fun mood(done: Int, total: Int): Pair<String, String> {
        val f = if (total <= 0) 0f else done.toFloat() / total
        return when {
            f <= 0f -> "準" to "Aquecendo…"
            f < 0.5f -> "気" to "No ritmo!"
            f < 1f -> "根" to "Quase lá!"
            else -> "完" to "Perfeito!"
        }
    }

    /** Volume total (carga × repetições). */
    fun volume(sets: List<DoneSet>): Double = sets.sumOf { it.weightKg * it.reps }
}
