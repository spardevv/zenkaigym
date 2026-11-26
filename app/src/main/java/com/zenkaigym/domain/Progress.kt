package com.zenkaigym.domain

import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.data.entity.Goal
import com.zenkaigym.data.entity.MealCheck
import com.zenkaigym.data.entity.MuscleGroup
import com.zenkaigym.data.entity.ExerciseKind
import com.zenkaigym.data.entity.SetLog
import com.zenkaigym.data.entity.WaterLog
import com.zenkaigym.data.entity.WeightEntry
import com.zenkaigym.data.entity.WorkoutLog
import kotlin.math.abs
import kotlin.math.roundToInt

// ---- Nível e XP -------------------------------------------------------------------------------------------

/** Patente do jogador, por faixa de nível: dá um nome e um kanji a cada fase da jornada. */
enum class Rank(val kanji: String, val pt: String) {
    NOVICE("初", "Novato"),
    APPRENTICE("修", "Aprendiz"),
    WARRIOR("戦", "Guerreiro"),
    VETERAN("豪", "Veterano"),
    MASTER("師", "Mestre"),
    LEGEND("伝", "Lenda");

    val label: String get() = tr(pt)

    companion object {
        fun of(level: Int): Rank = when {
            level < 3 -> NOVICE
            level < 5 -> APPRENTICE
            level < 8 -> WARRIOR
            level < 12 -> VETERAN
            level < 18 -> MASTER
            else -> LEGEND
        }
    }
}

/** Nível atual e quanto falta para o próximo. */
data class LevelInfo(val level: Int, val xp: Int, val xpIntoLevel: Int, val xpForNext: Int) {
    val fraction: Float get() = if (xpForNext <= 0) 1f else (xpIntoLevel.toFloat() / xpForNext).coerceIn(0f, 1f)
    val rank: Rank get() = Rank.of(level)
}

/** Quanto XP cada ação vale. Tudo vem do que o usuário já registra; nada é cobrado nem perdido. */
object XpRules {
    const val SESSION = 100
    const val SET = 5
    const val MEAL = 10
    const val RECORD = 25
    const val WEIGHT = 15
    const val CHALLENGE = Challenges.XP

    fun total(sessions: Int, sets: Int, meals: Int, records: Int, weights: Int, challenges: Int = 0): Int =
        sessions * SESSION + sets * SET + meals * MEAL + records * RECORD + weights * WEIGHT + challenges * CHALLENGE
}

object Levels {
    private const val STEP = 150

    /** XP total necessário para chegar ao [level] (nível 1 = 0). A cada nível o degrau cresce um pouco. */
    fun xpToReach(level: Int): Int = STEP * level * (level - 1)

    fun of(xp: Int): LevelInfo {
        val safe = xp.coerceAtLeast(0)
        var level = 1
        while (xpToReach(level + 1) <= safe) level++
        val base = xpToReach(level)
        return LevelInfo(level, safe, safe - base, xpToReach(level + 1) - base)
    }
}

// ---- Resultados -------------------------------------------------------------------------------------------

/** Conquista: [title] e [description] estão em português (passam por tr() na tela). */
data class Achievement(
    val id: String,
    val kanji: String,
    val title: String,
    val description: String,
    val progress: Int,
    val target: Int,
    /** Progresso legível, ex.: "7 / 10" ou "1.3 / 2 kg". */
    val progressText: String
) {
    val unlocked: Boolean get() = progress >= target
    val fraction: Float get() = if (target <= 0) 1f else (progress.toFloat() / target).coerceIn(0f, 1f)
}

/** Novo recorde de carga num exercício: [weightKg] superou [previousKg] em [epochDay]. */
data class RecordEntry(val exerciseId: Long, val name: String, val weightKg: Double, val previousKg: Double, val epochDay: Long)

/** Séries feitas num grupo muscular (últimos 30 dias). */
data class GroupShare(val group: MuscleGroup, val sets: Int)

data class WeightSummary(
    val current: Double,
    val start: Double,
    /** Variação desde o primeiro registro. */
    val change: Double,
    /** Variação nos últimos ~30 dias; null sem base de comparação. */
    val change30: Double?,
    /** Média por semana desde o primeiro registro; null com menos de uma semana de dados. */
    val perWeek: Double?,
    val min: Double,
    val max: Double,
    val entries: Int,
    /** Se a variação vai na direção do objetivo; null com menos de dois registros. */
    val onTrack: Boolean?
)

/** Tudo o que a tela de evolução mostra, já calculado. */
data class ProgressStats(
    val level: LevelInfo,
    val totalSessions: Int,
    val totalSets: Int,
    val totalVolumeKg: Double,
    val records: Int,
    val recentRecords: List<RecordEntry>,
    // Semana atual.
    val plannedPerWeek: Int,
    val weekSessions: Int,
    /** Sete dias da semana (na ordem de Ajustes): true = houve treino. */
    val weekDots: List<Boolean>,
    val weekDietPercent: Int,
    // Treino por semana (8 semanas, da mais antiga para a atual).
    val sessionsPerWeek: List<Int>,
    val volumePerWeek: List<Double>,
    val bestWeekSessions: Int,
    val avgSessionsPerWeek: Double,
    val weekStreak: Int,
    val bestWeekStreak: Int,
    val perfectWeeks: Int,
    val groups: List<GroupShare>,
    // Dieta.
    val dietDaily: List<Float>,
    val dietWeekly: List<Float>,
    val dietAveragePercent: Int,
    val dietStreak: Int,
    val bestDietStreak: Int,
    val mealsDone: Int,
    val hasDietData: Boolean,
    // Peso e conquistas.
    val weight: WeightSummary?,
    val achievements: List<Achievement>,
    // Escudos de sequência: guardados agora e quantas vezes já salvaram uma sequência.
    val weekShields: Int = 0,
    val dietShields: Int = 0,
    val shieldsUsed: Int = 0,
    // Desafios da semana atual e total cumprido desde o início.
    val challenges: List<Challenge> = emptyList(),
    val challengesCompleted: Int = 0,
    /** O que foi feito na semana atual (cartão para compartilhar). */
    val thisWeek: WeekActivity = WeekActivity(),
    /** Primeiro dia (epochDay) da semana atual. */
    val weekFirstDay: Long = 0,
    // Cardio e água no total.
    val cardioMinutes: Int = 0,
    val cardioKcal: Int = 0,
    val waterGoalDays: Int = 0
) {
    val unlockedCount: Int get() = achievements.count { it.unlocked }
    val hasTrainingData: Boolean get() = totalSessions > 0
}

/** Entrada do cálculo: os registros crus do banco e a configuração do usuário. */
class ProgressInput(
    val sets: List<SetLog>,
    val exercises: List<Exercise>,
    val workoutLogs: List<WorkoutLog>,
    val checks: List<MealCheck>,
    val mealsPerDay: Int,
    val weights: List<WeightEntry>,
    val goal: Goal,
    val plannedPerWeek: Int,
    val today: Long,
    val weekStart: WeekStart,
    /** Água bebida por dia e a meta atual (para os dias de meta batida). */
    val water: List<WaterLog> = emptyList(),
    val waterGoalMl: Int = Water.goalMl(null)
)

/** Regras puras de nível, sequências, recordes e conquistas (sem banco, para testar). */
object Progress {
    const val WEEKS = 8
    const val GROUP_DAYS = 30
    const val WEEK_SHIELD_EVERY = 4
    const val MAX_WEEK_SHIELDS = 1
    const val DIET_SHIELD_EVERY = 7
    const val MAX_DIET_SHIELDS = 2

    fun compute(input: ProgressInput): ProgressStats {
        val today = input.today
        val start = input.weekStart
        val names = input.exercises.associate { it.id to it.name }
        val groupOf = input.exercises.associate { it.id to it.group }

        // Um dia conta como treino se teve séries registradas ou foi marcado como concluído.
        val sessionDays: Set<Long> = input.sets.map { it.epochDay }.toSet() +
            input.workoutLogs.filter { it.finished }.map { it.epochDay }
        val totalVolume = input.sets.sumOf { it.weightKg * it.reps }

        val records = personalRecords(input.sets, names)

        // Semana atual.
        val currentWeek = start.weekStartOf(today)
        val weekSessions = sessionDays.count { it in currentWeek..today }
        val weekDots = List(7) { (currentWeek + it) in sessionDays }
        val sessionsPerWeek = Stats.weekly(sessionDays.associateWith { 1.0 }, today, WEEKS, start).map { it.toInt() }
        val volumeByDay = input.sets.groupBy { it.epochDay }.mapValues { (_, list) -> list.sumOf { it.weightKg * it.reps } }
        val volumePerWeek = Stats.weekly(volumeByDay, today, WEEKS, start)

        // Metas semanais: semanas seguidas cumprindo todos os treinos planejados.
        val perWeekCounts = sessionDays.groupingBy { start.weekStartOf(it) }.eachCount()
        val planned = input.plannedPerWeek
        val firstWeek = sessionDays.minOrNull()?.let { start.weekStartOf(it) } ?: currentWeek
        val weeks = generateSequence(firstWeek) { it + 7 }.takeWhile { it <= currentWeek }.toList()
        val weekMet = weeks.map { planned > 0 && (perWeekCounts[it] ?: 0) >= planned }
        val perfectWeeks = weekMet.count { it }
        // Sequência de semanas na meta, com escudo a cada 4 semanas cumpridas (a semana em andamento só soma se já cumpriu).
        val weekRun = if (planned > 0) Shields.streak(weekMet, WEEK_SHIELD_EVERY, MAX_WEEK_SHIELDS) else StreakResult(0, 0, 0, 0)

        // Músculos mais treinados (séries nos últimos 30 dias; cardio fica de fora).
        val groups = input.sets
            .filter { today - it.epochDay in 0 until GROUP_DAYS }
            .mapNotNull { groupOf[it.exerciseId] }
            .filter { it != MuscleGroup.CARDIO }
            .groupingBy { it }.eachCount()
            .map { GroupShare(it.key, it.value) }
            .sortedByDescending { it.sets }

        // Dieta.
        val donePerDay = input.checks.filter { !it.skipped }.groupingBy { it.epochDay }.eachCount()
        val mealsDone = donePerDay.values.sum()
        fun fraction(day: Long) =
            if (input.mealsPerDay <= 0) 0f else ((donePerDay[day] ?: 0).toFloat() / input.mealsPerDay).coerceIn(0f, 1f)
        val diet = Stats.diet(donePerDay, input.mealsPerDay, today)
        val dietWeekly = List(WEEKS) { i ->
            val weekFirst = currentWeek - (WEEKS - 1 - i) * 7L
            val last = minOf(weekFirst + 6, today)
            val days = (last - weekFirst + 1).toInt()
            if (days <= 0) 0f else (0 until days).map { fraction(weekFirst + it) }.average().toFloat()
        }
        val weekDiet = dietWeekly.last()
        // Dias com todas as refeições, com escudo a cada 7 dias cumpridos (hoje só soma se já completou).
        val firstDietDay = donePerDay.keys.minOrNull()
        val dietRun = if (firstDietDay == null || input.mealsPerDay <= 0) StreakResult(0, 0, 0, 0)
        else Shields.streak((firstDietDay..today).map { fraction(it) >= 1f }, DIET_SHIELD_EVERY, MAX_DIET_SHIELDS)

        // Cardio (minutos ficam em reps) e água.
        val kindOf = input.exercises.associate { it.id to it.kind }
        val cardioSets = input.sets.filter { kindOf[it.exerciseId] == ExerciseKind.CARDIO || groupOf[it.exerciseId] == MuscleGroup.CARDIO }
        val waterGoalDays = input.water.filter { it.ml >= input.waterGoalMl }.map { it.epochDay }.toSet()
        val fullDietDays = donePerDay.keys.filter { fraction(it) >= 1f }.toSet()

        // Desafios: cada semana desde a primeira atividade tem os seus; os cumpridos valem XP uma vez.
        fun activity(weekFirst: Long): WeekActivity {
            val range = weekFirst..(weekFirst + 6)
            val weekSets = input.sets.filter { it.epochDay in range }
            val weekCardio = cardioSets.filter { it.epochDay in range }
            return WeekActivity(
                sessions = sessionDays.count { it in range },
                sets = weekSets.size,
                volumeKg = weekSets.sumOf { it.weightKg * it.reps },
                mealsDone = input.checks.count { !it.skipped && it.epochDay in range },
                fullDietDays = fullDietDays.count { it in range },
                weighIns = input.weights.count { it.epochDay in range },
                waterGoalDays = waterGoalDays.count { it in range },
                cardioMinutes = weekCardio.sumOf { it.reps },
                cardioKcal = weekCardio.sumOf { it.kcal ?: 0 },
                records = records.count { it.epochDay in range }
            )
        }
        val firstActivity = listOfNotNull(
            sessionDays.minOrNull(), firstDietDay, input.weights.minOfOrNull { it.epochDay }, input.water.minOfOrNull { it.epochDay }
        ).minOrNull()?.coerceAtMost(today) ?: today
        val activityWeeks = generateSequence(start.weekStartOf(firstActivity)) { it + 7 }.takeWhile { it <= currentWeek }.toList()
        val mealsPerDay = input.mealsPerDay
        val challengesByWeek = activityWeeks.associateWith { Challenges.forWeek(it, planned, mealsPerDay, activity(it)) }
        val challengesCompleted = challengesByWeek.values.sumOf { list -> list.count { it.completed } }
        val thisWeek = activity(currentWeek)

        val weight = weightSummary(input.weights, input.goal, today)
        val xp = XpRules.total(sessionDays.size, input.sets.size, mealsDone, records.size, input.weights.size, challengesCompleted)

        val partial = ProgressStats(
            level = Levels.of(xp),
            totalSessions = sessionDays.size,
            totalSets = input.sets.size,
            totalVolumeKg = totalVolume,
            records = records.size,
            recentRecords = records.sortedByDescending { it.epochDay }.take(5),
            plannedPerWeek = planned,
            weekSessions = weekSessions,
            weekDots = weekDots,
            weekDietPercent = (weekDiet * 100).roundToInt(),
            sessionsPerWeek = sessionsPerWeek,
            volumePerWeek = volumePerWeek,
            bestWeekSessions = perWeekCounts.values.maxOrNull() ?: 0,
            avgSessionsPerWeek = averagePerWeek(sessionsPerWeek, firstWeek, currentWeek),
            weekStreak = weekRun.current,
            bestWeekStreak = weekRun.best,
            perfectWeeks = perfectWeeks,
            groups = groups,
            dietDaily = diet.daily,
            dietWeekly = dietWeekly,
            dietAveragePercent = diet.averagePercent,
            dietStreak = dietRun.current,
            bestDietStreak = dietRun.best,
            mealsDone = mealsDone,
            hasDietData = diet.hasData || dietWeekly.any { it > 0f },
            weight = weight,
            achievements = emptyList(),
            weekShields = weekRun.shields,
            dietShields = dietRun.shields,
            shieldsUsed = weekRun.shieldsUsed + dietRun.shieldsUsed,
            challenges = challengesByWeek[currentWeek] ?: Challenges.forWeek(currentWeek, planned, mealsPerDay, thisWeek),
            challengesCompleted = challengesCompleted,
            thisWeek = thisWeek,
            weekFirstDay = currentWeek,
            cardioMinutes = cardioSets.sumOf { it.reps },
            cardioKcal = cardioSets.sumOf { it.kcal ?: 0 },
            waterGoalDays = waterGoalDays.size
        )
        return partial.copy(achievements = achievements(partial, input.goal, input.weights.size))
    }

    /** Média de treinos por semana nas últimas [WEEKS] semanas, sem contar semanas anteriores ao primeiro treino. */
    private fun averagePerWeek(perWeek: List<Int>, firstWeek: Long, currentWeek: Long): Double {
        val weeksSinceFirst = ((currentWeek - firstWeek) / 7).toInt() + 1
        val weeks = minOf(WEEKS, weeksSinceFirst).coerceAtLeast(1)
        return perWeek.takeLast(weeks).sum().toDouble() / weeks
    }

    /** Maior sequência de dias consecutivos numa lista de dias ordenada. */
    fun longestRun(sortedDays: List<Long>): Int {
        var best = 0
        var run = 0
        var previous: Long? = null
        for (d in sortedDays) {
            run = if (previous != null && d == previous + 1) run + 1 else 1
            best = maxOf(best, run)
            previous = d
        }
        return best
    }

    /** Recorde = a carga mais alta de um dia que supera tudo o que o exercício já tinha (o 1º registro só vira base). */
    fun personalRecords(sets: List<SetLog>, names: Map<Long, String>): List<RecordEntry> {
        val out = mutableListOf<RecordEntry>()
        sets.groupBy { it.exerciseId }.forEach { (exerciseId, list) ->
            var best = 0.0
            list.groupBy { it.epochDay }.toSortedMap().forEach { (day, daySets) ->
                val top = daySets.maxOf { it.weightKg }
                if (top > best) {
                    if (best > 0.0) out += RecordEntry(exerciseId, names[exerciseId].orEmpty(), top, best, day)
                    best = top
                }
            }
        }
        return out
    }

    fun weightSummary(entries: List<WeightEntry>, goal: Goal, today: Long): WeightSummary? {
        val sorted = entries.sortedWith(compareBy({ it.epochDay }, { it.id }))
        if (sorted.isEmpty()) return null
        val first = sorted.first()
        val last = sorted.last()
        val change = last.weightKg - first.weightKg
        val reference = sorted.lastOrNull { it.epochDay <= last.epochDay - 30 } ?: first
        val change30 = if (reference === last || sorted.size < 2) null else last.weightKg - reference.weightKg
        val span = last.epochDay - first.epochDay
        val perWeek = if (span >= 7) change / (span / 7.0) else null
        val onTrack = if (sorted.size < 2) null else when (goal) {
            Goal.LOSE -> change < 0
            Goal.GAIN -> change > 0
            Goal.MAINTAIN -> abs(change) <= 1.5
        }
        return WeightSummary(
            current = last.weightKg, start = first.weightKg, change = change, change30 = change30, perWeek = perWeek,
            min = sorted.minOf { it.weightKg }, max = sorted.maxOf { it.weightKg }, entries = sorted.size, onTrack = onTrack
        )
    }

    private fun achievements(s: ProgressStats, goal: Goal, weightEntries: Int): List<Achievement> {
        fun count(id: String, kanji: String, title: String, description: String, value: Int, target: Int) =
            Achievement(id, kanji, title, description, value.coerceAtMost(target), target, "${value.coerceAtMost(target)} / $target")

        // Peso: ganho/perda na direção do objetivo (em décimos de kg, para caber num inteiro); "manter" pede estabilidade.
        val w = s.weight
        val towards = when {
            w == null || w.entries < 2 -> 0.0
            goal == Goal.LOSE -> (-w.change).coerceAtLeast(0.0)
            goal == Goal.GAIN -> w.change.coerceAtLeast(0.0)
            else -> if (w.entries >= 3 && abs(w.change) <= 1.0) 2.0 else 0.0
        }
        val towardsTenths = (towards * 10).roundToInt().coerceAtMost(20)
        val kgVolume = s.totalVolumeKg.toInt()

        return listOf(
            count("first_session", "初", "Primeiro passo", "Conclua seu primeiro treino.", s.totalSessions, 1),
            count("sessions_10", "拍", "Pegando o ritmo", "Conclua 10 treinos.", s.totalSessions, 10),
            count("sessions_50", "律", "Disciplina", "Conclua 50 treinos.", s.totalSessions, 50),
            count("sessions_100", "百", "Centurião", "Conclua 100 treinos.", s.totalSessions, 100),
            count("record_1", "新", "Novo recorde", "Bata um recorde de carga.", s.records, 1),
            count("record_10", "覇", "Caçador de recordes", "Bata 10 recordes de carga.", s.records, 10),
            Achievement("volume_1t", "重", "Uma tonelada", "Levante 1 tonelada no total.", kgVolume.coerceAtMost(1000), 1000, "${kgVolume.coerceAtMost(1000)} / 1000 kg"),
            Achievement("volume_10t", "岩", "Dez toneladas", "Levante 10 toneladas no total.", kgVolume.coerceAtMost(10000), 10000, "${kgVolume.coerceAtMost(10000)} / 10000 kg"),
            count("week_goal", "週", "Semana cheia", "Cumpra todos os treinos planejados de uma semana.", s.perfectWeeks, 1),
            count("week_streak_4", "鉄", "Mês de ferro", "Cumpra a meta de treinos por 4 semanas seguidas.", s.bestWeekStreak, 4),
            count("diet_7", "食", "Mesa em dia", "Faça todas as refeições por 7 dias seguidos.", s.bestDietStreak, 7),
            count("diet_30", "膳", "Chef disciplinado", "Faça todas as refeições por 30 dias seguidos.", s.bestDietStreak, 30),
            count("weight_5", "秤", "Na balança", "Registre seu peso 5 vezes.", weightEntries, 5),
            count("challenges_10", "挑", "Desafiante", "Cumpra 10 desafios semanais.", s.challengesCompleted, 10),
            count("cardio_300", "走", "Fôlego de aço", "Faça 300 minutos de cardio.", s.cardioMinutes, 300),
            count("water_7", "水", "Bem hidratado", "Bata a meta de água em 7 dias.", s.waterGoalDays, 7),
            Achievement(
                "weight_goal", "的", "Rumo à meta", "Varie 2 kg na direção do seu objetivo (ou fique estável, se o objetivo é manter).",
                towardsTenths, 20, "%.1f / 2 kg".format(towardsTenths / 10.0)
            )
        )
    }
}
