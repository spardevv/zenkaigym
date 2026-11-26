package com.zenkaigym.domain

import com.zenkaigym.data.entity.ExerciseKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Conselho de carga (subir, dupla progressão, deload), 1RM estimado, aquecimento e RPE. */
class LoadAdviceTest {
    private fun session(vararg sets: Pair<Int, Double>) = sets.map { DoneSet(it.first, it.second) }

    // ---- Passo de carga ----

    @Test
    fun passo_compostoSobeMaisQueIsolado_eTempoNaoSobe() {
        assertEquals(2.5, Progression.stepKg(ExerciseKind.COMPOUND), 0.0)
        assertEquals(1.0, Progression.stepKg(ExerciseKind.ISOLATION), 0.0)
        assertEquals(0.0, Progression.stepKg(ExerciseKind.HOLD), 0.0)
        assertEquals(0.0, Progression.stepKg(ExerciseKind.CARDIO), 0.0)
    }

    // ---- Subir a carga ----

    @Test
    fun topoDaFaixaEmTodasAsSeries_sobeOPassoDoTipo() {
        val last = session(10 to 60.0, 10 to 60.0, 10 to 60.0)
        assertEquals(LoadAdvice.AddWeight(60.0, 62.5), Progression.advise(listOf(last), "8-10", ExerciseKind.COMPOUND))
        assertEquals(LoadAdvice.AddWeight(60.0, 61.0), Progression.advise(listOf(last), "8-10", ExerciseKind.ISOLATION))
    }

    @Test
    fun seriesMaisLevesNoFimNaoSeguramAProgressao() {
        // Duas séries no topo com 40 kg e uma de "back-off" mais leve com menos repetições.
        val last = session(12 to 40.0, 12 to 40.0, 8 to 30.0)
        assertEquals(LoadAdvice.AddWeight(40.0, 41.0), Progression.advise(listOf(last), "10-12", ExerciseKind.ISOLATION))
    }

    @Test
    fun sobrandoTresRepeticoesEmTodas_sobeMesmoSemChegarAoTopo() {
        val last = listOf(DoneSet(8, 50.0, rir = 3), DoneSet(8, 50.0, rir = 4))
        assertEquals(LoadAdvice.AddWeight(50.0, 52.5), Progression.advise(listOf(last), "8-12", ExerciseKind.COMPOUND))
    }

    @Test
    fun rirAltoAbaixoDoMinimoNaoContaComoLeve() {
        val last = listOf(DoneSet(6, 50.0, rir = 3), DoneSet(6, 50.0, rir = 3))
        assertEquals(LoadAdvice.AddReps(50.0, 8), Progression.advise(listOf(last), "8-12", ExerciseKind.COMPOUND))
    }

    // ---- Dupla progressão ----

    @Test
    fun abaixoDoTopo_buscaUmaRepeticaoAMaisNaSerieMaisFraca() {
        val last = session(10 to 40.0, 9 to 40.0, 8 to 40.0)
        assertEquals(LoadAdvice.AddReps(40.0, 9), Progression.advise(listOf(last), "8-12", ExerciseKind.ISOLATION))
    }

    @Test
    fun alvoDeRepeticoesFicaDentroDaFaixa() {
        val last = session(5 to 40.0, 5 to 40.0)
        assertEquals(LoadAdvice.AddReps(40.0, 8), Progression.advise(listOf(last), "8-12", ExerciseKind.ISOLATION))
    }

    @Test
    fun faixaFixaSemTopoAlcancado_naoDaConselho() {
        assertNull(Progression.advise(listOf(session(8 to 40.0)), "10", ExerciseKind.ISOLATION))
    }

    // ---- Deload ----

    @Test
    fun tresSessoesSemPassarDoMelhor_sugereDeloadDeDezPorCento() {
        val sessions = listOf(
            session(8 to 100.0, 8 to 100.0), // melhor 1RM: 126,7
            session(7 to 100.0, 7 to 100.0),
            session(6 to 100.0, 6 to 100.0),
            session(7 to 100.0, 6 to 100.0)
        )
        assertTrue(Progression.stalled(sessions))
        assertEquals(LoadAdvice.Deload(100.0, 90.0), Progression.advise(sessions, "8-10", ExerciseKind.COMPOUND))
    }

    @Test
    fun deloadArredondaParaOPassoDoExercicio() {
        val sessions = listOf(session(10 to 44.0)) + List(3) { session(8 to 44.0) }
        // 44 × 0,9 = 39,6 → 39 (passo de 1 kg nos isolados)
        assertEquals(LoadAdvice.Deload(44.0, 39.0), Progression.advise(sessions, "10-12", ExerciseKind.ISOLATION))
    }

    @Test
    fun evoluindoNaoEstagna() {
        val sessions = listOf(session(8 to 100.0), session(8 to 102.5), session(8 to 105.0), session(9 to 105.0))
        assertFalse(Progression.stalled(sessions))
        assertEquals(LoadAdvice.AddReps(105.0, 10), Progression.advise(sessions, "8-10", ExerciseKind.COMPOUND))
    }

    @Test
    fun poucasSessoesNuncaEstagnam() {
        assertFalse(Progression.stalled(List(3) { session(8 to 100.0) }))
    }

    // ---- Sem conselho ----

    @Test
    fun semHistoricoSemCargaOuTempo_naoDaConselho() {
        assertNull(Progression.advise(emptyList(), "8-10", ExerciseKind.COMPOUND))
        assertNull(Progression.advise(listOf(emptyList()), "8-10", ExerciseKind.COMPOUND))
        assertNull(Progression.advise(listOf(session(15 to 0.0)), "10-15", ExerciseKind.COMPOUND))
        assertNull(Progression.advise(listOf(session(30 to 0.0)), "20-30 min", ExerciseKind.CARDIO))
        assertNull(Progression.advise(listOf(session(10 to 20.0)), "até a falha", ExerciseKind.ISOLATION))
    }

    // ---- 1RM, aquecimento, RPE ----

    @Test
    fun umRmPelaFormulaDeEpley() {
        assertEquals(100.0, Progression.oneRepMax(1, 100.0), 0.001)
        assertEquals(133.333, Progression.oneRepMax(10, 100.0), 0.001)
        assertEquals(0.0, Progression.oneRepMax(10, 0.0), 0.0)
        assertEquals(0.0, Progression.oneRepMax(0, 100.0), 0.0)
        assertEquals(116.667, Progression.bestOneRepMax(session(5 to 100.0, 1 to 110.0)), 0.001)
    }

    @Test
    fun aquecimentoUsaMetadeEDepoisSetentaPorCentoArredondado() {
        assertEquals(DoneSet(10, 50.0), Progression.warmup(1, 100.0))
        assertEquals(DoneSet(6, 70.0), Progression.warmup(2, 100.0))
        assertEquals(DoneSet(3, 80.0), Progression.warmup(3, 95.0)) // 80,75 → 80
        assertEquals(DoneSet(10, 0.0), Progression.warmup(1, 0.0))
    }

    @Test
    fun rpeEhDezMenosRir() {
        assertEquals(10, Progression.rpe(0))
        assertEquals(8, Progression.rpe(2))
        assertEquals(5, Progression.rpe(7))
    }

    @Test
    fun arredondamentoEDescricaoDaCarga() {
        assertEquals(40.0, Progression.roundDown(40.5, 2.5), 0.0)
        assertEquals(42.5, Progression.roundDown(42.5, 2.5), 0.0)
        assertEquals("22", Progression.kgText(22.0))
        assertEquals("22.5", Progression.kgText(22.5))
        assertEquals("90.1", Progression.kgText(90.1000000001))
    }

    @Test
    fun prefillNaoCopiaOEsforcoDaVezAnterior() {
        val last = listOf(DoneSet(10, 40.0, rir = 2))
        assertEquals(DoneSet(10, 40.0), Progression.prefill(last, 1, "8-10"))
    }
}
