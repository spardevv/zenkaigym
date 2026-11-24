package com.zenkaigym.domain

import com.zenkaigym.data.entity.DietPlan
import com.zenkaigym.data.entity.DietPlanFull
import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.FoodCategory
import com.zenkaigym.data.entity.Goal
import com.zenkaigym.data.entity.Meal
import com.zenkaigym.data.entity.MealCheck
import com.zenkaigym.data.entity.MealFull
import com.zenkaigym.data.entity.MealItem
import com.zenkaigym.data.entity.MealItemFull
import com.zenkaigym.data.entity.MealType
import com.zenkaigym.data.entity.MuscleGroup
import com.zenkaigym.data.entity.PlanStatus
import com.zenkaigym.data.entity.WorkoutDay
import com.zenkaigym.data.entity.WorkoutDayFull
import com.zenkaigym.data.entity.WorkoutItem
import com.zenkaigym.data.entity.WorkoutItemFull
import com.zenkaigym.data.entity.WorkoutLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Semana de treino que desliza quando uma sessão é pulada e saldo calórico da dieta na semana. */
class WeekPlanningTest {
    // 19996 é uma segunda-feira.
    private val monday = 19996L
    private val quads = Exercise(1, "Agachamento livre", MuscleGroup.QUADS)

    private fun session(id: Long, dayOfWeek: Int, sets: Int) = WorkoutDayFull(
        WorkoutDay(id, 1, dayOfWeek, "Sessão $id"),
        listOf(WorkoutItemFull(WorkoutItem(id, id, quads.id, sets, "8-10", 0), quads))
    )

    // Segunda (4 séries), quarta (8) e sexta (2).
    private val sessions = listOf(session(1, 1, 4), session(2, 3, 8), session(3, 5, 2))

    private fun assign(today: Long, vararg logs: WorkoutLog) = WorkoutWeek.assign(sessions, today, WeekStart.MONDAY, logs.toList())

    @Test
    fun semMarcas_cadaDiaTemASuaSessao() {
        assertEquals(2L, assign(monday + 2).session?.day?.id)
        assertFalse(assign(monday + 2).moved)
    }

    @Test
    fun diaSemTreino_ehDescanso() {
        assertNull(assign(monday + 1).session)
    }

    @Test
    fun sessaoNaoRealizada_passaParaOProximoDiaDeTreino() {
        val result = assign(monday + 2, WorkoutLog(monday, finished = false))
        assertEquals(1L, result.session?.day?.id)
        assertTrue(result.moved)
    }

    @Test
    fun semaCheia_saiAsessaoDeMenorPrioridade() {
        // Segunda pulada: sobram A, B, C para quarta e sexta; a de menos séries dos grupos grandes (C) sai.
        val result = assign(monday + 2, WorkoutLog(monday, finished = false))
        assertEquals(listOf(3L), result.dropped.map { it.day.id })
    }

    @Test
    fun treinoFeito_oSeguinteSegueNormal() {
        assertEquals(2L, assign(monday + 2, WorkoutLog(monday, finished = true)).session?.day?.id)
    }

    @Test
    fun dataSemMarca_contaComoPassada() {
        assertEquals(2L, assign(monday + 2).session?.day?.id)
    }

    @Test
    fun hojeNaoRealizado_aSessaoDeHojeContinuaSendoDeHoje() {
        assertEquals(2L, assign(monday + 2, WorkoutLog(monday + 2, finished = false)).session?.day?.id)
    }

    // ---- Troca manual do treino de hoje ----

    private fun assignWith(today: Long, override: WorkoutWeek.Override?, vararg logs: WorkoutLog) =
        WorkoutWeek.assign(sessions, today, WeekStart.MONDAY, logs.toList(), override)

    @Test
    fun trocaDeHoje_rotacionaASequenciaDaSemana() {
        // Segunda: escolhe a sessão 3 (sexta). Quarta vira a 1 e sexta a 2.
        val o = WorkoutWeek.Override(monday, 3L)
        assertEquals(3L, assignWith(monday, o).session?.day?.id)
        assertEquals(1L, assignWith(monday + 2, o).session?.day?.id)
        assertEquals(2L, assignWith(monday + 4, o).session?.day?.id)
    }

    @Test
    fun trocaDeHoje_naoMexeNosDiasQueJaPassaram() {
        // Troca feita na quarta (sessão 3): segunda continua como foi e sexta fica com a 2.
        val o = WorkoutWeek.Override(monday + 2, 3L)
        assertEquals(1L, assignWith(monday, o).session?.day?.id)
        assertEquals(3L, assignWith(monday + 2, o).session?.day?.id)
        assertEquals(2L, assignWith(monday + 4, o).session?.day?.id)
    }

    @Test
    fun trocaDeHoje_comSessaoInexistente_eIgnorada() {
        assertEquals(2L, assignWith(monday + 2, WorkoutWeek.Override(monday + 2, 99L)).session?.day?.id)
    }

    @Test
    fun trocaDeHoje_juntoComNaoRealizado() {
        // Segunda não realizada; quarta escolhe a 3. Fila na quarta: 1,2,3 -> 3,1,2; sexta fica com a 1.
        val o = WorkoutWeek.Override(monday + 2, 3L)
        val logs = arrayOf(WorkoutLog(monday, finished = false))
        assertEquals(3L, assignWith(monday + 2, o, *logs).session?.day?.id)
        assertEquals(1L, assignWith(monday + 4, o, *logs, WorkoutLog(monday + 2, finished = true)).session?.day?.id)
    }

    // ---- Dieta ----

    private val rice = Food(1, "Arroz", FoodCategory.CARB, 100.0, 2.0, 20.0, 0.0)

    private fun meal(id: Long, type: MealType, dayOfWeek: Int, grams: Int) = MealFull(
        Meal(id, 1, type.pt, type.ordinal, type, dayOfWeek),
        listOf(MealItemFull(MealItem(id, id, rice.id, grams), rice))
    )

    // Todos os dias: almoço de 600 kcal e jantar de 600 kcal.
    private val diet = DietPlanFull(
        DietPlan(1, PlanStatus.ACTIVE, 1200, 100, 100, 40, 0),
        (1..7).flatMap { d -> listOf(meal(d * 10L, MealType.LUNCH, d, 600), meal(d * 10L + 1, MealType.DINNER, d, 600)) }
    )

    private fun balance(goal: Goal, vararg checks: MealCheck, today: Long = monday + 1) =
        WeekDietBalance.compute(diet, today, WeekStart.MONDAY, goal, checks.toList(), emptyList())

    @Test
    fun primeiroDiaDaSemana_semSaldo() {
        assertEquals(WeekBalance.NONE, balance(Goal.MAINTAIN, today = monday))
    }

    @Test
    fun refeicaoNaoRealizada_deixaFaltaDeCalorias() {
        // Segunda sem o jantar (-600), olhando de terça: 6 dias restantes.
        val b = balance(Goal.MAINTAIN, MealCheck(MealType.DINNER, monday, skipped = true))
        assertEquals(-600, b.surplusKcal)
        assertEquals(100, b.todayKcal) // 600 / 6 = 100, dentro do limite de 10% de 1200
    }

    @Test
    fun emagrecer_naoRecuperaOQueFaltou() {
        val b = balance(Goal.LOSE, MealCheck(MealType.DINNER, monday, skipped = true))
        assertEquals(0, b.todayKcal)
    }

    @Test
    fun emagrecer_compensaExcesso() {
        // Almoço de segunda registrado com 1200 kcal (600 a mais).
        val b = balance(Goal.LOSE, MealCheck(MealType.LUNCH, monday, kcal = 1200))
        assertEquals(600, b.surplusKcal)
        assertEquals(-100, b.todayKcal)
    }

    @Test
    fun ganharMassa_naoCortaPorExcesso() {
        val b = balance(Goal.GAIN, MealCheck(MealType.LUNCH, monday, kcal = 1200))
        assertEquals(0, b.todayKcal)
    }

    @Test
    fun compensacaoDiaria_temLimiteDeDezPorCento() {
        // Dois dias sem nenhuma refeição olhando de quarta (5 dias restantes): 2400 / 5 = 480, limitado a 120.
        val checks = arrayOf(MealType.LUNCH, MealType.DINNER).flatMap { t ->
            listOf(MealCheck(t, monday, skipped = true), MealCheck(t, monday + 1, skipped = true))
        }.toTypedArray()
        val b = balance(Goal.MAINTAIN, *checks, today = monday + 2)
        assertEquals(120, b.todayKcal)
    }

    @Test
    fun treinoPulado_reduzOPlanejadoDeQuemNaoQuerGanhar() {
        val burn = mapOf(monday to 300)
        val lose = WeekDietBalance.compute(diet, monday + 1, WeekStart.MONDAY, Goal.LOSE, emptyList(), emptyList(), burn)
        val gain = WeekDietBalance.compute(diet, monday + 1, WeekStart.MONDAY, Goal.GAIN, emptyList(), emptyList(), burn)
        assertEquals(300, lose.surplusKcal)
        assertEquals(0, gain.surplusKcal)
    }

    @Test
    fun refeicaoPulada_hojeReajustaOResto_menosParaEmagrecer() {
        val day = listOf(meal(1, MealType.LUNCH, 1, 600), meal(2, MealType.DINNER, 1, 600))
        val gain = DayAdjust.compute(day, emptySet(), emptyList(), skipped = setOf(MealType.LUNCH), goal = Goal.GAIN)
        val lose = DayAdjust.compute(day, emptySet(), emptyList(), skipped = setOf(MealType.LUNCH), goal = Goal.LOSE)
        assertTrue(gain.deltaKcal > 0)
        assertEquals(DayAdjustment.NONE, lose)
    }

    @Test
    fun caloriasGravadas_entramNaContaDoDia() {
        val day = listOf(meal(1, MealType.LUNCH, 1, 600), meal(2, MealType.DINNER, 1, 600))
        // O almoço foi feito com 900 kcal: o jantar encolhe.
        val result = DayAdjust.compute(day, setOf(MealType.LUNCH), emptyList(), recorded = mapOf(MealType.LUNCH to 900))
        assertTrue(result.deltaKcal < 0)
    }
}
