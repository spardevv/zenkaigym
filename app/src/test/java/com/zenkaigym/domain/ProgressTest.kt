package com.zenkaigym.domain

import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.data.entity.Goal
import com.zenkaigym.data.entity.MealCheck
import com.zenkaigym.data.entity.MealType
import com.zenkaigym.data.entity.MuscleGroup
import com.zenkaigym.data.entity.SetLog
import com.zenkaigym.data.entity.WeightEntry
import com.zenkaigym.data.entity.WorkoutLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTest {
    // 2026-11-16 é segunda-feira; as contas abaixo usam dias relativos a essa data.
    private val today = 20_773L
    private val monday = WeekStart.MONDAY

    private val bench = Exercise(1, "Supino", MuscleGroup.CHEST)
    private val squat = Exercise(2, "Agachamento", MuscleGroup.QUADS)
    private val bike = Exercise(3, "Bicicleta", MuscleGroup.CARDIO)

    private fun set(day: Long, ex: Long, index: Int, reps: Int, kg: Double) = SetLog(epochDay = day, exerciseId = ex, setIndex = index, reps = reps, weightKg = kg)

    private fun input(
        sets: List<SetLog> = emptyList(),
        logs: List<WorkoutLog> = emptyList(),
        checks: List<MealCheck> = emptyList(),
        weights: List<WeightEntry> = emptyList(),
        goal: Goal = Goal.MAINTAIN,
        planned: Int = 3,
        meals: Int = 3,
        day: Long = today
    ) = ProgressInput(sets, listOf(bench, squat, bike), logs, checks, meals, weights, goal, planned, day, monday)

    // ---- Nível ----

    @Test
    fun nivel_comecaNoUmEAvancaPorDegraus() {
        assertEquals(1, Levels.of(0).level)
        assertEquals(1, Levels.of(299).level)
        assertEquals(2, Levels.of(300).level)
        assertEquals(3, Levels.of(900).level)
        assertEquals(0, Levels.xpToReach(1))
        assertEquals(300, Levels.xpToReach(2))
    }

    @Test
    fun nivel_progressoDentroDoNivel() {
        val info = Levels.of(450) // nível 2 vai de 300 a 900
        assertEquals(2, info.level)
        assertEquals(150, info.xpIntoLevel)
        assertEquals(600, info.xpForNext)
        assertEquals(0.25f, info.fraction, 0.001f)
    }

    @Test
    fun nivel_xpNegativoNaoQuebra() {
        assertEquals(1, Levels.of(-50).level)
    }

    @Test
    fun patentes_sobemComONivel() {
        assertEquals(Rank.NOVICE, Rank.of(1))
        assertEquals(Rank.APPRENTICE, Rank.of(3))
        assertEquals(Rank.WARRIOR, Rank.of(5))
        assertEquals(Rank.VETERAN, Rank.of(8))
        assertEquals(Rank.MASTER, Rank.of(12))
        assertEquals(Rank.LEGEND, Rank.of(30))
    }

    @Test
    fun xp_somaCadaAcao() {
        assertEquals(100 + 5 * 3 + 10 * 2 + 25 + 15, XpRules.total(sessions = 1, sets = 3, meals = 2, records = 1, weights = 1))
    }

    // ---- Treino ----

    @Test
    fun semDados_tudoZerado() {
        val s = Progress.compute(input())
        assertEquals(0, s.totalSessions)
        assertEquals(0, s.records)
        assertEquals(1, s.level.level)
        assertFalse(s.hasTrainingData)
        assertEquals(0, s.weekStreak)
        assertNull(s.weight)
        assertEquals(0, s.unlockedCount)
    }

    @Test
    fun sessao_contaPorSerieOuPorTreinoConcluido_naoPorPulado() {
        val s = Progress.compute(
            input(
                sets = listOf(set(today - 3, 1, 1, 10, 50.0), set(today - 3, 1, 2, 10, 50.0)),
                logs = listOf(WorkoutLog(today - 1, finished = true), WorkoutLog(today - 2, finished = false))
            )
        )
        assertEquals(2, s.totalSessions) // dia com séries + dia concluído; o pulado não conta
        assertEquals(2, s.totalSets)
        assertEquals(1000.0, s.totalVolumeKg, 0.001)
    }

    @Test
    fun recorde_primeiroRegistroEhBaseESoDepoisContam() {
        val sets = listOf(
            set(today - 10, 1, 1, 10, 40.0),  // base
            set(today - 7, 1, 1, 10, 40.0),   // igual: não é recorde
            set(today - 5, 1, 1, 8, 45.0),    // recorde
            set(today - 2, 1, 1, 8, 50.0),    // recorde
            set(today - 1, 1, 1, 8, 20.0)     // mais leve: não é
        )
        val records = Progress.personalRecords(sets, mapOf(1L to "Supino"))
        assertEquals(2, records.size)
        assertEquals(50.0, records.maxByOrNull { it.epochDay }!!.weightKg, 0.001)
        assertEquals(45.0, records.maxByOrNull { it.epochDay }!!.previousKg, 0.001)
    }

    @Test
    fun recorde_exercicioSemCargaNuncaContaRecorde() {
        val sets = listOf(set(today - 5, 3, 1, 20, 0.0), set(today - 2, 3, 1, 25, 0.0))
        assertTrue(Progress.personalRecords(sets, emptyMap()).isEmpty())
    }

    @Test
    fun semanaAtual_pontosEMeta() {
        // Segunda (hoje) e domingo da semana passada: só hoje cai na semana atual.
        val s = Progress.compute(input(sets = listOf(set(today, 1, 1, 10, 30.0), set(today - 1, 1, 1, 10, 30.0)), planned = 3))
        assertEquals(1, s.weekSessions)
        assertEquals(true, s.weekDots[0])
        assertEquals(listOf(true, false, false, false, false, false, false), s.weekDots)
    }

    @Test
    fun sequenciaDeSemanas_contaSemanasSeguidasNaMeta() {

        val prev = today - 7       // semana passada
        val before = today - 14
        // 2 treinos por semana planejados; as duas semanas anteriores foram cumpridas, a atual ainda não.
        val sets = listOf(
            set(prev, 1, 1, 10, 30.0), set(prev + 2, 1, 1, 10, 30.0),
            set(before, 1, 1, 10, 30.0), set(before + 2, 1, 1, 10, 30.0)
        )
        val s = Progress.compute(input(sets = sets, planned = 2))
        assertEquals(2, s.weekStreak)
        assertEquals(2, s.bestWeekStreak)
        assertEquals(2, s.perfectWeeks)
    }

    @Test
    fun sequenciaDeSemanas_semanaAtualCumpridaSomaENaoQuebra() {
        val sets = listOf(set(today - 7, 1, 1, 10, 30.0), set(today, 1, 1, 10, 30.0))
        val s = Progress.compute(input(sets = sets, planned = 1))
        assertEquals(2, s.weekStreak)
    }

    @Test
    fun sequenciaDeSemanas_semanaVaziaNoMeioQuebra() {
        val sets = listOf(set(today - 21, 1, 1, 10, 30.0), set(today - 7, 1, 1, 10, 30.0))
        val s = Progress.compute(input(sets = sets, planned = 1))
        assertEquals(1, s.weekStreak) // só a semana passada; a retrasada ficou vazia
        assertEquals(1, s.bestWeekStreak)
        assertEquals(2, s.perfectWeeks)
    }

    @Test
    fun semMetaPlanejada_naoHaSequenciaDeSemanas() {
        val s = Progress.compute(input(sets = listOf(set(today, 1, 1, 10, 30.0)), planned = 0))
        assertEquals(0, s.weekStreak)
        assertEquals(0, s.perfectWeeks)
    }

    @Test
    fun musculos_contamSeriesDosUltimos30DiasSemCardio() {
        val sets = listOf(
            set(today - 2, 1, 1, 10, 30.0), set(today - 2, 1, 2, 10, 30.0), set(today - 2, 1, 3, 10, 30.0),
            set(today - 3, 2, 1, 10, 60.0),
            set(today - 4, 3, 1, 20, 0.0),   // cardio: fora
            set(today - 60, 2, 1, 10, 60.0)  // antigo: fora
        )
        val groups = Progress.compute(input(sets = sets)).groups
        assertEquals(listOf(GroupShare(MuscleGroup.CHEST, 3), GroupShare(MuscleGroup.QUADS, 1)), groups)
    }

    @Test
    fun semanas_oitoSemanasDaMaisAntigaParaAAtual() {
        val s = Progress.compute(input(sets = listOf(set(today, 1, 1, 10, 30.0))))
        assertEquals(8, s.sessionsPerWeek.size)
        assertEquals(1, s.sessionsPerWeek.last())
        assertEquals(8, s.volumePerWeek.size)
    }

    // ---- Dieta ----

    private fun done(day: Long, type: MealType) = MealCheck(type, day, skipped = false, kcal = 500)

    @Test
    fun dieta_sequenciaEMelhorSequencia() {
        val all = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER)
        // Dias today-5..today-3 completos (3 dias), today-2 incompleto, today-1 e today completos (2 dias).
        val days = listOf(today - 5, today - 4, today - 3, today - 1, today)
        val checks = days.flatMap { d -> all.map { done(d, it) } } + done(today - 2, MealType.BREAKFAST)
        val s = Progress.compute(input(checks = checks, meals = 3))
        assertEquals(2, s.dietStreak)
        assertEquals(3, s.bestDietStreak)
        assertEquals(3 * 5 + 1, s.mealsDone)
        assertTrue(s.hasDietData)
    }

    @Test
    fun dieta_refeicaoPuladaNaoContaComoFeita() {
        val checks = listOf(MealCheck(MealType.LUNCH, today, skipped = true), done(today, MealType.BREAKFAST))
        val s = Progress.compute(input(checks = checks, meals = 2))
        assertEquals(1, s.mealsDone)
        assertEquals(0, s.dietStreak)
    }

    @Test
    fun dieta_semanalTemOitoPontosEAAtualSoContaOsDiasQueJaPassaram() {
        val checks = listOf(done(today, MealType.BREAKFAST), done(today, MealType.LUNCH), done(today, MealType.DINNER))
        val s = Progress.compute(input(checks = checks, meals = 3))
        assertEquals(8, s.dietWeekly.size)
        assertEquals(1f, s.dietWeekly.last(), 0.001f) // hoje é segunda: a semana só tem 1 dia e ele está completo
        assertEquals(100, s.weekDietPercent)
    }

    @Test
    fun longestRun_listaVaziaEEmbaralhada() {
        assertEquals(0, Progress.longestRun(emptyList()))
        assertEquals(3, Progress.longestRun(listOf(1, 2, 3, 7, 8)))
        assertEquals(1, Progress.longestRun(listOf(5)))
    }

    // ---- Peso ----

    private fun w(day: Long, kg: Double, id: Long = 0) = WeightEntry(id, day, kg)

    @Test
    fun peso_semRegistrosNaoTemResumo() {
        assertNull(Progress.weightSummary(emptyList(), Goal.LOSE, today))
    }

    @Test
    fun peso_umRegistroNaoTemVariacaoNemDirecao() {
        val s = Progress.weightSummary(listOf(w(today, 80.0)), Goal.LOSE, today)!!
        assertEquals(0.0, s.change, 0.001)
        assertNull(s.onTrack)
        assertNull(s.perWeek)
        assertNull(s.change30)
    }

    @Test
    fun peso_perdaVaiNaDirecaoDeQuemQuerEmagrecer() {
        val entries = listOf(w(today - 56, 90.0, 1), w(today - 28, 87.0, 2), w(today, 84.0, 3))
        val s = Progress.weightSummary(entries, Goal.LOSE, today)!!
        assertEquals(-6.0, s.change, 0.001)
        assertEquals(true, s.onTrack)
        assertEquals(-0.75, s.perWeek!!, 0.001) // 6 kg em 8 semanas (56 dias)
    }

    @Test
    fun peso_ganhoNaoEhBomParaQuemQuerEmagrecer() {
        val entries = listOf(w(today - 14, 80.0, 1), w(today, 82.0, 2))
        assertEquals(false, Progress.weightSummary(entries, Goal.LOSE, today)!!.onTrack)
        assertEquals(true, Progress.weightSummary(entries, Goal.GAIN, today)!!.onTrack)
    }

    @Test
    fun peso_manterToleraAteUmQuiloEMeio() {
        assertEquals(true, Progress.weightSummary(listOf(w(today - 7, 80.0, 1), w(today, 81.2, 2)), Goal.MAINTAIN, today)!!.onTrack)
        assertEquals(false, Progress.weightSummary(listOf(w(today - 7, 80.0, 1), w(today, 82.0, 2)), Goal.MAINTAIN, today)!!.onTrack)
    }

    @Test
    fun peso_variacaoDe30DiasUsaORegistroMaisProximoAntesDoPeriodo() {
        val entries = listOf(w(today - 90, 90.0, 1), w(today - 40, 86.0, 2), w(today - 5, 84.0, 3), w(today, 83.0, 4))
        val s = Progress.weightSummary(entries, Goal.LOSE, today)!!
        assertEquals(-3.0, s.change30!!, 0.001) // referência: o de 40 dias atrás (86 -> 83)
        assertEquals(83.0, s.current, 0.001)
        assertEquals(90.0, s.max, 0.001)
        assertEquals(83.0, s.min, 0.001)
    }

    // ---- Conquistas ----

    @Test
    fun conquistas_primeiroTreinoDesbloqueia() {
        val s = Progress.compute(input(sets = listOf(set(today, 1, 1, 10, 30.0))))
        val first = s.achievements.first { it.id == "first_session" }
        assertTrue(first.unlocked)
        assertEquals("1 / 1", first.progressText)
        assertFalse(s.achievements.first { it.id == "sessions_10" }.unlocked)
        assertEquals("1 / 10", s.achievements.first { it.id == "sessions_10" }.progressText)
    }

    @Test
    fun conquistas_progressoNuncaPassaDoAlvo() {
        val sets = (0 until 30).map { set(today - it, 1, 1, 10, 30.0) }
        val a = Progress.compute(input(sets = sets)).achievements.first { it.id == "sessions_10" }
        assertEquals(10, a.progress)
        assertEquals(1f, a.fraction, 0.001f)
    }

    @Test
    fun conquistas_pesoDesbloqueiaCom5Registros() {
        val weights = (0 until 5).map { w(today - it, 80.0, it.toLong()) }
        val a = Progress.compute(input(weights = weights)).achievements.first { it.id == "weight_5" }
        assertTrue(a.unlocked)
    }

    @Test
    fun conquistas_rumoAMetaPorObjetivo() {
        val lose = listOf(w(today - 30, 90.0, 1), w(today, 87.5, 2))
        assertTrue(Progress.compute(input(weights = lose, goal = Goal.LOSE)).achievements.first { it.id == "weight_goal" }.unlocked)
        // Quem quer ganhar e perdeu peso não avança nessa conquista.
        val wrong = Progress.compute(input(weights = lose, goal = Goal.GAIN)).achievements.first { it.id == "weight_goal" }
        assertFalse(wrong.unlocked)
        assertEquals(0, wrong.progress)
    }

    @Test
    fun conquistas_catorzeNoTotalETodasTemAlvoPositivo() {
        val list = Progress.compute(input()).achievements
        assertEquals(14, list.size)
        assertTrue(list.all { it.target > 0 })
        assertEquals(list.size, list.map { it.id }.toSet().size)
        assertNotNull(list.firstOrNull { it.id == "volume_1t" })
    }

    @Test
    fun xpEsNivelRefletemOsRegistros() {
        val s = Progress.compute(input(sets = listOf(set(today, 1, 1, 10, 30.0), set(today, 1, 2, 10, 30.0))))
        // 1 treino (100) + 2 séries (10) = 110 XP
        assertEquals(110, s.level.xp)
        assertEquals(1, s.level.level)
    }
}
