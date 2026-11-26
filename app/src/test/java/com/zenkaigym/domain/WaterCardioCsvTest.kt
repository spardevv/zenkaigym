package com.zenkaigym.domain

import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.data.entity.ExerciseKind
import com.zenkaigym.data.entity.MealCheck
import com.zenkaigym.data.entity.MealType
import com.zenkaigym.data.entity.MuscleGroup
import com.zenkaigym.data.entity.SetLog
import com.zenkaigym.data.entity.WaterLog
import com.zenkaigym.data.entity.WeightEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Meta de água, estimativas de cardio e exportação em CSV. */
class WaterCardioCsvTest {
    @Before
    fun portugues() = I18n.select(AppLanguage.PT)

    // ---- Água ----

    @Test
    fun agua_metaPeloPesoArredondadaELimitada() {
        assertEquals(2450, Water.goalMl(70.0))
        assertEquals(2800, Water.goalMl(80.0))
        assertEquals(2450, Water.goalMl(null)) // sem peso: 70 kg
        assertEquals(Water.MIN_GOAL_ML, Water.goalMl(30.0))
        assertEquals(Water.MAX_GOAL_ML, Water.goalMl(200.0))
        assertEquals(2600, Water.goalMl(74.5)) // 2607,5 → 2600
    }

    @Test
    fun agua_somaSemFicarNegativaNemPassarDoTeto() {
        assertEquals(250, Water.add(0, 250))
        assertEquals(0, Water.add(100, -250))
        assertEquals(Water.MAX_DAY_ML, Water.add(9_900, 250))
    }

    @Test
    fun agua_fracaoERotulo() {
        assertEquals(0.5f, Water.fraction(1000, 2000), 0.001f)
        assertEquals(1f, Water.fraction(3000, 2000), 0f)
        assertEquals(0f, Water.fraction(100, 0), 0f)
        assertEquals("800 ml", Water.label(800))
        assertEquals("2 L", Water.label(2000))
        assertEquals("2.45 L", Water.label(2450))
        assertEquals("1.5 L", Water.label(1500))
    }

    // ---- Cardio ----

    @Test
    fun cardio_metPelaAtividade() {
        assertEquals(6.8, Cardio.met("Bicicleta ergométrica"), 0.0)
        assertEquals(8.5, Cardio.met("HIIT na bicicleta"), 0.0)
        assertEquals(6.0, Cardio.met("Caminhada inclinada na esteira"), 0.0)
        assertEquals(4.3, Cardio.met("Caminhada rápida"), 0.0)
        assertEquals(11.0, Cardio.met("Pular corda"), 0.0)
        assertEquals(Cardio.DEFAULT_MET, Cardio.met("Meu cardio inventado"), 0.0)
    }

    @Test
    fun cardio_esteiraComDistanciaUsaAVelocidade() {
        // 5 km em 30 min = 10 km/h (corrida): MET 9,8.
        assertEquals(9.8, Cardio.met("Esteira", 30, 5.0), 0.0)
        // 2 km em 30 min = 4 km/h (caminhada leve).
        assertEquals(3.5, Cardio.met("Esteira", 30, 2.0), 0.0)
        // Bicicleta não muda com distância.
        assertEquals(6.8, Cardio.met("Bicicleta ergométrica", 30, 15.0), 0.0)
    }

    @Test
    fun cardio_kcalPeloMetPesoETempo() {
        // 8 MET × 3,5 × 70 kg ÷ 200 × 30 min = 294 kcal
        assertEquals(294, Cardio.kcal(8.0, 70.0, 30))
        assertEquals(0, Cardio.kcal(8.0, 70.0, 0))
        assertEquals(0, Cardio.kcal(8.0, 0.0, 30))
        assertEquals(Cardio.kcal(6.8, 80.0, 20), Cardio.kcal("Bicicleta ergométrica", 80.0, 20))
    }

    @Test
    fun cardio_ritmoEVelocidade() {
        assertEquals("5:30/km", Cardio.paceLabel(33, 6.0))
        assertEquals(10.0, Cardio.speedKmh(30, 5.0)!!, 0.001)
        assertNull(Cardio.paceLabel(30, null))
        assertNull(Cardio.paceLabel(30, 0.0))
        assertNull(Cardio.speedKmh(0, 5.0))
    }

    // ---- CSV ----

    @Test
    fun csv_escapaVirgulaAspasEQuebra() {
        assertEquals("simples", Csv.escape("simples"))
        assertEquals("\"a,b\"", Csv.escape("a,b"))
        assertEquals("\"diz \"\"oi\"\"\"", Csv.escape("diz \"oi\""))
        assertEquals("\"linha\nnova\"", Csv.escape("linha\nnova"))
    }

    @Test
    fun csv_numerosComPontoEDatasIso() {
        assertEquals("42.5", Csv.number(42.5))
        assertEquals("40", Csv.number(40.0))
        assertEquals("0.33", Csv.number(1.0 / 3))
        assertEquals("1970-01-01", Csv.date(0))
        assertEquals("2026-11-16", Csv.date(20_773))
        assertEquals("1,,x,1,0", Csv.row(listOf(1, null, "x", true, false)))
    }

    @Test
    fun csv_seriesComAquecimentoRirCardioENota() {
        val bench = Exercise(1, "Supino reto com barra", MuscleGroup.CHEST, ExerciseKind.COMPOUND)
        val bike = Exercise(2, "Bicicleta ergométrica", MuscleGroup.CARDIO, ExerciseKind.CARDIO)
        val sets = listOf(
            SetLog(epochDay = 20_773, exerciseId = 1, setIndex = 1, reps = 10, weightKg = 60.0, rir = 2, note = "pegada, fechada"),
            SetLog(epochDay = 20_773, exerciseId = 1, setIndex = -1, reps = 10, weightKg = 30.0, warmup = true),
            SetLog(epochDay = 20_774, exerciseId = 2, setIndex = 1, reps = 30, weightKg = 0.0, distanceKm = 12.5, kcal = 250)
        )
        val lines = Csv.sets(sets, listOf(bench, bike)).trimEnd().split("\r\n")
        assertEquals(4, lines.size)
        assertTrue(lines[0].startsWith("data,exercício,grupo,série,aquecimento,kg"))
        // Ordem por dia e série: o aquecimento (-1) vem antes da 1ª série; ele não tem 1RM.
        assertEquals("2026-11-16,Supino reto com barra,Peito,1,1,30,10,,,,,,", lines[1])
        assertEquals("2026-11-16,Supino reto com barra,Peito,1,0,60,10,2,8,,,80,\"pegada, fechada\"", lines[2])
        assertEquals("2026-11-17,Bicicleta ergométrica,Cardio,1,0,0,30,,,12.5,250,,", lines[3])
    }

    @Test
    fun csv_pesoRefeicoesEAgua() {
        val weights = Csv.weights(listOf(WeightEntry(2, 20_774, 80.5), WeightEntry(1, 20_773, 81.0))).trimEnd().split("\r\n")
        assertEquals(listOf("data,peso (kg)", "2026-11-16,81", "2026-11-17,80.5"), weights)

        val meals = Csv.meals(
            listOf(MealCheck(MealType.LUNCH, 20_773, skipped = true), MealCheck(MealType.BREAKFAST, 20_773, kcal = 450))
        ).trimEnd().split("\r\n")
        assertEquals("data,refeição,situação,kcal", meals[0])
        assertEquals("2026-11-16,Café da manhã,feita,450", meals[1])
        assertEquals("2026-11-16,Almoço,não realizada,", meals[2])

        assertEquals("data,água (ml)\r\n2026-11-16,2000\r\n", Csv.water(listOf(WaterLog(20_773, 2000))))
    }
}
