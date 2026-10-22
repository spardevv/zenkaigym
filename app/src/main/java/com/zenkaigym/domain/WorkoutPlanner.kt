package com.zenkaigym.domain

import com.zenkaigym.data.ExerciseCatalog
import com.zenkaigym.data.entity.CardioPref
import com.zenkaigym.data.entity.Equipment
import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.data.entity.ExerciseKind
import com.zenkaigym.data.entity.Goal
import com.zenkaigym.data.entity.Level
import com.zenkaigym.data.entity.MuscleGroup
import com.zenkaigym.data.entity.MuscleGroup.ABS
import com.zenkaigym.data.entity.MuscleGroup.ADDUCTORS
import com.zenkaigym.data.entity.MuscleGroup.BICEPS
import com.zenkaigym.data.entity.MuscleGroup.CALVES
import com.zenkaigym.data.entity.MuscleGroup.CARDIO
import com.zenkaigym.data.entity.MuscleGroup.CHEST
import com.zenkaigym.data.entity.MuscleGroup.FOREARMS
import com.zenkaigym.data.entity.MuscleGroup.GLUTES
import com.zenkaigym.data.entity.MuscleGroup.HAMSTRINGS
import com.zenkaigym.data.entity.MuscleGroup.LATS
import com.zenkaigym.data.entity.MuscleGroup.LOWER_BACK
import com.zenkaigym.data.entity.MuscleGroup.MID_BACK
import com.zenkaigym.data.entity.MuscleGroup.OBLIQUES
import com.zenkaigym.data.entity.MuscleGroup.QUADS
import com.zenkaigym.data.entity.MuscleGroup.SHOULDERS_FRONT
import com.zenkaigym.data.entity.MuscleGroup.SHOULDERS_REAR
import com.zenkaigym.data.entity.MuscleGroup.SHOULDERS_SIDE
import com.zenkaigym.data.entity.MuscleGroup.TRAPS
import com.zenkaigym.data.entity.MuscleGroup.TRICEPS
import com.zenkaigym.data.entity.TrainingFocus

data class PlannedItem(val exerciseId: Long, val sets: Int, val reps: String)
data class PlannedDay(val dayOfWeek: Int, val name: String, val items: List<PlannedItem>)
data class PlannedWorkout(val splitName: String, val days: List<PlannedDay>)

/**
 * O que o planejador precisa saber: os dias de treino com os minutos disponíveis em cada um e as
 * preferências do perfil.
 */
data class WorkoutRequest(
    val days: Map<Int, Int>,
    val goal: Goal,
    val level: Level = Level.INTERMEDIATE,
    val cardio: CardioPref = CardioPref.NONE,
    val focus: TrainingFocus = TrainingFocus.BALANCED,
    val equipment: Equipment = Equipment.GYM,
    val weekStart: WeekStart = WeekStart.MONDAY
)

/**
 * Monta a ficha pelo tempo disponível: cada grupo muscular tem uma meta semanal de séries (mínimo e alvo,
 * ajustados por nível e foco), e as séries são distribuídas pelas sessões até acabar o tempo de cada dia.
 * Primeiro garante o mínimo de todos (grandes antes dos pequenos), depois sobe em direção ao alvo.
 * Séries de exercícios compostos também contam (metade) para os músculos auxiliares.
 */
object WorkoutPlanner {
    private const val MAX_SESSIONS = 6
    const val WARMUP_MIN = 5
    const val DEFAULT_MINUTES = 60
    private const val MAX_DIRECT_PER_SESSION = 6

    private class Volume(val min: Int, val target: Int, val weight: Double)

    private val volume: Map<MuscleGroup, Volume> = mapOf(
        CHEST to Volume(6, 12, 1.0),
        LATS to Volume(6, 12, 1.0),
        MID_BACK to Volume(4, 10, 0.8),
        TRAPS to Volume(0, 6, 0.3),
        LOWER_BACK to Volume(0, 6, 0.35),
        // Os desenvolvimentos já treinam bastante o deltoide anterior: ele só ganha séries próprias com tempo de sobra.
        SHOULDERS_FRONT to Volume(0, 6, 0.4),
        SHOULDERS_SIDE to Volume(4, 12, 0.8),
        SHOULDERS_REAR to Volume(3, 8, 0.6),
        BICEPS to Volume(3, 8, 0.6),
        TRICEPS to Volume(3, 8, 0.6),
        FOREARMS to Volume(0, 4, 0.25),
        QUADS to Volume(6, 12, 1.0),
        HAMSTRINGS to Volume(5, 10, 1.0),
        GLUTES to Volume(4, 10, 0.9),
        ADDUCTORS to Volume(2, 6, 0.5),
        CALVES to Volume(3, 8, 0.6),
        ABS to Volume(3, 8, 0.7),
        OBLIQUES to Volume(2, 6, 0.5)
    )

    /** Grupos treinados com séries próprias, do mais prioritário ao menos. */
    val trained: List<MuscleGroup> = volume.keys.toList()

    /** Série mínima semanal de um grupo para o nível dado (usada nos testes e na leitura do plano). */
    fun minSets(group: MuscleGroup, level: Level): Double =
        (volume.getValue(group).min * when (level) { Level.BEGINNER -> 0.8; Level.INTERMEDIATE -> 1.0; Level.ADVANCED -> 1.15 })

    private fun targetSets(group: MuscleGroup, level: Level, focus: TrainingFocus): Double {
        val levelScale = when (level) { Level.BEGINNER -> 0.7; Level.INTERMEDIATE -> 1.0; Level.ADVANCED -> 1.3 }
        val focusScale = if (focus == TrainingFocus.BALANCED) 1.0 else if (group in focusGroups(focus)) 1.4 else 0.85
        return maxOf(minSets(group, level), volume.getValue(group).target * levelScale * focusScale)
    }

    private fun focusGroups(focus: TrainingFocus): Set<MuscleGroup> = when (focus) {
        TrainingFocus.BALANCED -> emptySet()
        TrainingFocus.UPPER -> setOf(CHEST, LATS, MID_BACK, TRAPS, SHOULDERS_FRONT, SHOULDERS_SIDE, SHOULDERS_REAR, BICEPS, TRICEPS)
        TrainingFocus.LOWER -> setOf(QUADS, HAMSTRINGS, GLUTES, ADDUCTORS, CALVES)
        TrainingFocus.GLUTES -> setOf(GLUTES, HAMSTRINGS, ADDUCTORS)
    }

    private class Session(val name: String, val groups: Set<MuscleGroup>)

    private val all = trained.toSet()
    private fun full(name: String) = Session(name, all)
    private val push = Session("Push · peito, ombro, tríceps", setOf(CHEST, SHOULDERS_FRONT, SHOULDERS_SIDE, TRICEPS, ABS))
    private val pull = Session(
        "Pull · costas, bíceps",
        setOf(LATS, MID_BACK, TRAPS, SHOULDERS_REAR, BICEPS, FOREARMS, LOWER_BACK, OBLIQUES)
    )
    private val legs = Session("Pernas", setOf(QUADS, HAMSTRINGS, GLUTES, ADDUCTORS, CALVES, ABS, OBLIQUES))
    private val upper = Session(
        "Superiores",
        setOf(CHEST, LATS, MID_BACK, TRAPS, SHOULDERS_FRONT, SHOULDERS_SIDE, SHOULDERS_REAR, BICEPS, TRICEPS, FOREARMS, ABS)
    )
    private val lower = Session("Inferiores", setOf(QUADS, HAMSTRINGS, GLUTES, ADDUCTORS, CALVES, LOWER_BACK, ABS, OBLIQUES))

    private fun split(sessions: Int): Pair<String, List<Session>> = when (sessions) {
        1 -> "Corpo inteiro" to listOf(full("Corpo inteiro"))
        2 -> "Corpo inteiro 2x" to listOf(full("Corpo inteiro A"), full("Corpo inteiro B"))
        3 -> "Corpo inteiro 3x" to listOf(full("Corpo inteiro A"), full("Corpo inteiro B"), full("Corpo inteiro C"))
        4 -> "Superiores / Inferiores" to listOf(upper, lower, upper, lower)
        5 -> "Push / Pull / Pernas + Sup. / Inf." to listOf(push, pull, legs, upper, lower)
        else -> "Push / Pull / Pernas 2x" to listOf(push, pull, legs, push, pull, legs)
    }

    private class Block(val exercise: Exercise, val group: MuscleGroup, var sets: Int)

    private class Slot(val index: Int, val session: Session, val minutes: Int, var left: Double) {
        val blocks = mutableListOf<Block>()
        fun setsOf(group: MuscleGroup) = blocks.filter { it.group == group }.sumOf { it.sets }
    }

    fun plan(request: WorkoutRequest, catalog: List<Exercise>): PlannedWorkout {
        val days = request.days.keys.filter { it in 1..7 }.sortedBy { request.weekStart.position(it) }
        if (days.isEmpty()) return PlannedWorkout("", emptyList())

        val count = minOf(days.size, MAX_SESSIONS)
        val (splitName, templates) = split(count)
        val trainingDays = pickSpread(days, count)
        val goal = request.goal

        val finisher = if (request.cardio == CardioPref.FINISHER) finisherMinutes(goal) else 0
        val slots = trainingDays.mapIndexed { i, dow ->
            val minutes = request.days[dow]?.takeIf { it > 0 } ?: DEFAULT_MINUTES
            val budget = (minutes - WARMUP_MIN - if (minutes >= FINISHER_FROM) finisher else 0).coerceAtLeast(0)
            Slot(i, templates[i], minutes, budget.toDouble())
        }

        val pools = trained.associateWith { group -> pool(catalog, group, request) }
        val uses = mutableMapOf<Long, Int>()
        val effective = trained.associateWith { 0.0 }.toMutableMap()

        // 0 = garante o mínimo de cada grupo, 1 = sobe até o alvo, 2 = usa o tempo que sobrar (até 1,5x o alvo).
        for (phase in 0..2) {
            val goalOf = { g: MuscleGroup ->
                when (phase) {
                    0 -> minSets(g, request.level)
                    1 -> targetSets(g, request.level, request.focus)
                    else -> targetSets(g, request.level, request.focus) * 1.5
                }
            }
            while (true) {
                var best: Pair<MuscleGroup, Slot>? = null
                var bestScore = 0.0
                for (g in trained) {
                    val wanted = goalOf(g)
                    val deficit = wanted - effective.getValue(g)
                    if (deficit <= 0.0) continue
                    val slot = chooseSlot(g, slots, pools.getValue(g), goal, uses) ?: continue
                    val score = deficit / wanted * volume.getValue(g).weight
                    if (score > bestScore) {
                        bestScore = score
                        best = g to slot
                    }
                }
                val (group, slot) = best ?: break
                addSets(group, slot, pools.getValue(group), goal, uses, effective)
            }
        }

        val cardioPool = pool(catalog, CARDIO, request)
        val planned = slots.map { slot ->
            val strength = slot.blocks
                .sortedWith(compareBy({ order(it.exercise.kind) }, { trained.indexOf(it.group) }))
                .map { PlannedItem(it.exercise.id, it.sets, repsFor(goal, it.exercise)) }
            val items = mutableListOf<PlannedItem>()
            val cardio = cardioPool.getOrNull(slot.index % cardioPool.size.coerceAtLeast(1))
            if (cardio != null && request.cardio == CardioPref.WARMUP) items += PlannedItem(cardio.id, 1, "${WARMUP_MIN}min")
            items += strength
            if (cardio != null && request.cardio == CardioPref.FINISHER && slot.minutes >= FINISHER_FROM) {
                items += PlannedItem(cardio.id, 1, "${finisher}min")
            }
            PlannedDay(trainingDays[slot.index], slot.session.name, items)
        }
        return PlannedWorkout(splitName, planned)
    }

    /** Atalho sem preferências: dias com a duração padrão e o restante nos valores neutros. */
    fun plan(availableDays: List<Int>, goal: Goal, catalog: List<Exercise>, weekStart: WeekStart = WeekStart.MONDAY): PlannedWorkout =
        plan(WorkoutRequest(availableDays.associateWith { DEFAULT_MINUTES }, goal, weekStart = weekStart), catalog)

    private const val FINISHER_FROM = 30

    private fun finisherMinutes(goal: Goal) = when (goal) { Goal.LOSE -> 10; Goal.MAINTAIN -> 8; Goal.GAIN -> 5 }

    private fun order(kind: ExerciseKind) = when (kind) {
        ExerciseKind.COMPOUND -> 0
        ExerciseKind.ISOLATION -> 1
        ExerciseKind.HOLD -> 2
        ExerciseKind.CARDIO -> 3
    }

    /** Exercícios do grupo que o usuário pode fazer (equipamento e nível), compostos primeiro. */
    private fun pool(catalog: List<Exercise>, group: MuscleGroup, request: WorkoutRequest): List<Exercise> =
        catalog.filter {
            it.group == group && request.equipment.allows(it.equipment) &&
                ExerciseCatalog.level(it).ordinal <= request.level.ordinal
        }

    private fun capOf(kind: ExerciseKind) = if (kind == ExerciseKind.COMPOUND) 4 else 3

    /** Minutos de uma série (execução + descanso). Sessões curtas usam descanso menor nos isolados. */
    fun setMinutes(kind: ExerciseKind, goal: Goal, sessionMinutes: Int): Double = when (kind) {
        ExerciseKind.COMPOUND -> if (goal == Goal.GAIN) 2.5 else 2.0
        ExerciseKind.ISOLATION -> if (sessionMinutes < 50) 1.25 else 1.5
        ExerciseKind.HOLD -> 1.0
        ExerciseKind.CARDIO -> 0.0
    }

    /** Sessão onde mais cabe uma série do grupo: a que tem menos séries dele e, em empate, mais tempo livre. */
    private fun chooseSlot(group: MuscleGroup, slots: List<Slot>, pool: List<Exercise>, goal: Goal, uses: Map<Long, Int>): Slot? =
        slots
            .filter { group in it.session.groups && it.setsOf(group) < MAX_DIRECT_PER_SESSION && canAdd(it, group, pool, goal, uses) }
            .minWithOrNull(compareBy<Slot>({ it.setsOf(group) }, { -it.left }, { it.index }))

    /** Bloco aberto do grupo na sessão (ainda com espaço para séries) ou, se não há, o próximo exercício a abrir. */
    private fun openBlock(slot: Slot, group: MuscleGroup) =
        slot.blocks.firstOrNull { it.group == group && it.sets < capOf(it.exercise.kind) }

    /** Exercício novo para o grupo: compostos primeiro, depois isolados; entre iguais, o menos usado na semana. */
    private fun nextExercise(slot: Slot, group: MuscleGroup, pool: List<Exercise>, uses: Map<Long, Int>): Exercise? {
        val hasCompound = slot.blocks.any { it.group == group && it.exercise.kind == ExerciseKind.COMPOUND }
        val preferred = if (hasCompound) ExerciseKind.ISOLATION else ExerciseKind.COMPOUND
        return pool
            .filter { e -> slot.blocks.none { it.exercise.id == e.id } }
            .minWithOrNull(compareBy({ if (it.kind == preferred) 0 else 1 }, { uses.getOrDefault(it.id, 0) }))
    }

    private fun canAdd(slot: Slot, group: MuscleGroup, pool: List<Exercise>, goal: Goal, uses: Map<Long, Int>): Boolean {
        val kind = openBlock(slot, group)?.exercise?.kind ?: nextExercise(slot, group, pool, uses)?.kind ?: return false
        return slot.left >= setMinutes(kind, goal, slot.minutes)
    }

    private fun addSets(
        group: MuscleGroup, slot: Slot, pool: List<Exercise>, goal: Goal,
        uses: MutableMap<Long, Int>, effective: MutableMap<MuscleGroup, Double>
    ) {
        val open = openBlock(slot, group)
        val block: Block
        var sets = 1
        if (open != null) {
            block = open
        } else {
            val exercise = nextExercise(slot, group, pool, uses)!!
            block = Block(exercise, group, 0)
            slot.blocks += block
            uses[exercise.id] = uses.getOrDefault(exercise.id, 0) + 1
            // Abre com duas séries quando há tempo: um exercício de uma série só raramente vale a pena.
            if (slot.left >= 2 * setMinutes(exercise.kind, goal, slot.minutes)) sets = 2
        }
        val cost = setMinutes(block.exercise.kind, goal, slot.minutes)
        block.sets += sets
        slot.left -= cost * sets
        effective[group] = effective.getValue(group) + sets
        ExerciseCatalog.secondary(block.exercise).forEach { aux ->
            if (aux in effective) effective[aux] = effective.getValue(aux) + sets * ExerciseCatalog.SECONDARY
        }
    }

    /** Escolhe [n] dias espalhados ao longo da lista, deixando o descanso o mais uniforme possível. */
    internal fun pickSpread(sortedDays: List<Int>, n: Int): List<Int> =
        if (n >= sortedDays.size) sortedDays
        else (0 until n).map { sortedDays[it * sortedDays.size / n] }

    private fun repsFor(goal: Goal, exercise: Exercise): String = when (exercise.kind) {
        ExerciseKind.HOLD -> "30-45s"
        ExerciseKind.CARDIO -> "10min"
        ExerciseKind.COMPOUND -> when (goal) { Goal.GAIN -> "6-10"; Goal.MAINTAIN -> "8-12"; Goal.LOSE -> "10-15" }
        ExerciseKind.ISOLATION ->
            if (exercise.group == ABS || exercise.group == OBLIQUES) "15-20"
            else when (goal) { Goal.GAIN -> "10-12"; Goal.MAINTAIN -> "10-15"; Goal.LOSE -> "12-15" }
    }

    // ---- Leitura de um plano (testes e avisos na tela) ----

    /** Minutos que o dia leva: séries com descanso + aquecimento (quando não é o próprio cardio) + cardio. */
    fun estimateMinutes(day: PlannedDay, byId: Map<Long, Exercise>, goal: Goal, sessionMinutes: Int): Double {
        var total = 0.0
        day.items.forEachIndexed { i, item ->
            val exercise = byId[item.exerciseId] ?: return@forEachIndexed
            if (exercise.kind == ExerciseKind.CARDIO) {
                total += Regex("\\d+").find(item.reps)?.value?.toInt() ?: 0
            } else {
                if (i == 0) total += WARMUP_MIN
                total += item.sets * setMinutes(exercise.kind, goal, sessionMinutes)
            }
        }
        return total
    }

    /** Séries semanais efetivas por grupo (séries próprias + metade das auxiliares). */
    fun weeklyVolume(plan: PlannedWorkout, byId: Map<Long, Exercise>): Map<MuscleGroup, Double> {
        val result = trained.associateWith { 0.0 }.toMutableMap()
        plan.days.flatMap { it.items }.forEach { item ->
            val exercise = byId[item.exerciseId] ?: return@forEach
            if (exercise.kind == ExerciseKind.CARDIO) return@forEach
            if (exercise.group in result) result[exercise.group] = result.getValue(exercise.group) + item.sets
            ExerciseCatalog.secondary(exercise).forEach { aux ->
                if (aux in result) result[aux] = result.getValue(aux) + item.sets * ExerciseCatalog.SECONDARY
            }
        }
        return result
    }
}
