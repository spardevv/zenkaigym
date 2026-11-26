package com.zenkaigym.domain

import com.zenkaigym.data.entity.ExerciseKind
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/** Série realizada, sem dependência do banco. [rir] = repetições que ainda sobravam (null = não anotado). */
data class DoneSet(val reps: Int, val weightKg: Double, val rir: Int? = null)

/** Faixa de repetições planejada, ex.: "8-10" (ou só "12"). */
data class RepRange(val min: Int, val max: Int)

/** O que fazer com a carga na próxima vez. */
sealed interface LoadAdvice {
    /** Todas as séries chegaram ao topo da faixa (ou sobraram muitas repetições): subir a carga. */
    data class AddWeight(val fromKg: Double, val toKg: Double) : LoadAdvice

    /** Dupla progressão: mesma carga, buscando mais repetições até o topo da faixa. */
    data class AddReps(val kg: Double, val targetReps: Int) : LoadAdvice

    /** Várias sessões sem evoluir: tirar ~10% da carga por uma semana e voltar a subir. */
    data class Deload(val fromKg: Double, val toKg: Double) : LoadAdvice
}

object Progression {
    /** Passo usado quando o tipo do exercício não é conhecido. */
    const val STEP_KG = 2.0

    /** Sessões seguidas sem superar o melhor 1RM estimado para sugerir deload. */
    const val STALL_SESSIONS = 3

    /** Corte do deload: 10% a menos. */
    const val DELOAD_FACTOR = 0.9

    /** Sobrando ao menos tantas repetições em todas as séries, a carga está leve. */
    const val EASY_RIR = 3

    fun parseReps(text: String): RepRange? {
        val nums = Regex("\\d+").findAll(text).map { it.value.toInt() }.toList()
        return when (nums.size) {
            0 -> null
            1 -> RepRange(nums[0], nums[0])
            else -> RepRange(minOf(nums[0], nums[1]), max(nums[0], nums[1]))
        }
    }

    /** Quanto subir de cada vez: compostos aguentam saltos maiores; isolados sobem devagar. Tempo/cardio não sobem carga. */
    fun stepKg(kind: ExerciseKind): Double = when (kind) {
        ExerciseKind.COMPOUND -> 2.5
        ExerciseKind.ISOLATION -> 1.0
        ExerciseKind.HOLD, ExerciseKind.CARDIO -> 0.0
    }

    /**
     * Carga sugerida para a próxima vez. Se todas as séries da última vez chegaram ao topo da faixa,
     * sobe [step]; senão mantém a maior carga usada. Sem histórico, devolve null.
     */
    fun nextWeight(last: List<DoneSet>, planned: String, step: Double = STEP_KG): Double? {
        if (last.isEmpty()) return null
        val top = last.maxOf { it.weightKg }
        val range = parseReps(planned) ?: return top
        val hitTop = last.all { it.reps >= range.max }
        return if (hitTop && top > 0) top + step else top
    }

    /**
     * Conselho de carga a partir das últimas sessões (da mais antiga para a mais recente):
     * subir a carga, buscar mais repetições (dupla progressão) ou fazer deload quando estagnou.
     * Null quando não há o que dizer (sem histórico, sem carga ou faixa sem números).
     */
    fun advise(sessions: List<List<DoneSet>>, planned: String, kind: ExerciseKind): LoadAdvice? {
        val last = sessions.lastOrNull()?.takeIf { it.isNotEmpty() } ?: return null
        val step = stepKg(kind)
        if (step <= 0.0) return null
        val top = last.maxOf { it.weightKg }
        if (top <= 0.0) return null
        val range = parseReps(planned) ?: return null
        // Só as séries na carga de trabalho contam (séries mais leves no fim não seguram a progressão).
        val working = last.filter { it.weightKg >= top }
        val hitTop = working.all { it.reps >= range.max }
        val easy = working.all { (it.rir ?: -1) >= EASY_RIR && it.reps >= range.min }
        if (hitTop || easy) return LoadAdvice.AddWeight(top, top + step)
        if (stalled(sessions)) {
            val lighter = roundDown(top * DELOAD_FACTOR, step)
            if (lighter > 0.0 && lighter < top) return LoadAdvice.Deload(top, lighter)
        }
        if (range.max > range.min) {
            val weakest = working.minOf { it.reps }
            return LoadAdvice.AddReps(top, (weakest + 1).coerceIn(range.min, range.max))
        }
        return null
    }

    /** As últimas [STALL_SESSIONS] sessões não passaram do melhor 1RM estimado de antes delas. */
    fun stalled(sessions: List<List<DoneSet>>): Boolean {
        val done = sessions.filter { it.isNotEmpty() }
        if (done.size <= STALL_SESSIONS) return false
        val before = done.dropLast(STALL_SESSIONS).maxOf { bestOneRepMax(it) }
        val recent = done.takeLast(STALL_SESSIONS).maxOf { bestOneRepMax(it) }
        return before > 0.0 && recent <= before
    }

    /** Arredonda para baixo no múltiplo de [step] (ex.: 40.5 com passo 2.5 vira 40). */
    fun roundDown(kg: Double, step: Double): Double = if (step <= 0.0) kg else floor(kg / step + 1e-9) * step

    /**
     * 1RM estimado pela fórmula de Epley: carga × (1 + repetições / 30). Uma repetição é o próprio 1RM.
     * Fica menos preciso acima de ~12 repetições; serve para comparar a evolução, não como teste de carga máxima.
     */
    fun oneRepMax(reps: Int, weightKg: Double): Double = when {
        reps <= 0 || weightKg <= 0.0 -> 0.0
        reps == 1 -> weightKg
        else -> weightKg * (1 + reps / 30.0)
    }

    fun bestOneRepMax(sets: List<DoneSet>): Double = sets.maxOfOrNull { oneRepMax(it.reps, it.weightKg) } ?: 0.0

    /**
     * Valores iniciais da série [setIndex] (1 = primeira): repete a mesma série do treino anterior.
     * Se da última vez houve menos séries, usa a última feita. Sem histórico: sem carga e o topo da faixa planejada.
     */
    fun prefill(last: List<DoneSet>, setIndex: Int, planned: String): DoneSet? =
        last.getOrNull(setIndex - 1)?.copy(rir = null) ?: last.lastOrNull()?.copy(rir = null)
            ?: parseReps(planned)?.let { DoneSet(it.max, 0.0) }

    /**
     * Aquecimento: a [index]ª série (1, 2…) usa 50% e depois 70% da carga de trabalho, com menos repetições,
     * arredondada para anilhas de 2,5 kg. Sem carga de trabalho, fica sem carga.
     */
    fun warmup(index: Int, workingKg: Double): DoneSet {
        val (factor, reps) = when (index) {
            1 -> 0.5 to 10
            2 -> 0.7 to 6
            else -> 0.85 to 3
        }
        val kg = if (workingKg > 0) roundDown(workingKg * factor, 2.5) else 0.0
        return DoneSet(reps, kg)
    }

    /** Recorde pessoal: carga maior que qualquer registro anterior (e maior que zero). */
    fun isPersonalRecord(weightKg: Double, previousBest: Double): Boolean = weightKg > 0 && weightKg > previousBest

    /** Descanso sugerido entre séries: séries pesadas (poucas repetições) pedem mais tempo. */
    fun restSeconds(reps: Int): Int = when {
        reps <= 6 -> 120
        reps <= 10 -> 90
        else -> 60
    }

    /** RPE equivalente ao RIR anotado (escala de 10): 0 sobrando = 10, 2 sobrando = 8. */
    fun rpe(rir: Int): Int = (10 - rir).coerceIn(5, 10)

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

    /** Carga em texto sem zeros inúteis: 22.0 -> "22", 22.5 -> "22.5". */
    fun kgText(kg: Double): String {
        val rounded = (kg * 100).roundToInt() / 100.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    }
}
