package com.zenkaigym.data

import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.FoodCategory
import com.zenkaigym.data.entity.FoodCategory.CARB
import com.zenkaigym.data.entity.FoodCategory.FRUIT
import com.zenkaigym.data.entity.FoodCategory.PROTEIN
import com.zenkaigym.data.entity.FoodCategory.SALAD
import com.zenkaigym.data.entity.FoodCategory.VEGETABLE
import com.zenkaigym.data.entity.Equipment
import com.zenkaigym.data.entity.Equipment.BODYWEIGHT
import com.zenkaigym.data.entity.Equipment.DUMBBELLS
import com.zenkaigym.data.entity.Equipment.GYM
import com.zenkaigym.data.entity.ExerciseKind
import com.zenkaigym.data.entity.ExerciseKind.COMPOUND
import com.zenkaigym.data.entity.ExerciseKind.HOLD
import com.zenkaigym.data.entity.ExerciseKind.ISOLATION
import com.zenkaigym.data.entity.Level
import com.zenkaigym.data.entity.Level.ADVANCED
import com.zenkaigym.data.entity.Level.BEGINNER
import com.zenkaigym.data.entity.Level.INTERMEDIATE
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

/**
 * Exercício do catálogo: grupo principal, tipo, equipamento mínimo, nível a partir do qual é sugerido
 * e grupos trabalhados de forma secundária (cada série vale [ExerciseCatalog.SECONDARY] série para eles).
 */
data class CatalogEntry(
    val name: String,
    val group: MuscleGroup,
    val kind: ExerciseKind,
    val equipment: Equipment,
    val level: Level = BEGINNER,
    val secondary: List<MuscleGroup> = emptyList()
)

/** Catálogo de exercícios, inserido na primeira execução (e completado pela migração para quem já usa o app). */
object ExerciseCatalog {
    /** Quanto uma série vale para um grupo trabalhado de forma secundária. */
    const val SECONDARY = 0.5

    private val RUN = ExerciseKind.CARDIO

    private fun e(
        name: String, group: MuscleGroup, kind: ExerciseKind, equipment: Equipment,
        level: Level = BEGINNER, vararg secondary: MuscleGroup
    ) = CatalogEntry(name, group, kind, equipment, level, secondary.toList())

    val entries: List<CatalogEntry> = listOf(
        // Peitoral
        e("Supino reto com barra", CHEST, COMPOUND, GYM, BEGINNER, TRICEPS, SHOULDERS_FRONT),
        e("Supino na máquina", CHEST, COMPOUND, GYM, BEGINNER, TRICEPS, SHOULDERS_FRONT),
        e("Supino reto com halteres", CHEST, COMPOUND, DUMBBELLS, BEGINNER, TRICEPS, SHOULDERS_FRONT),
        e("Supino inclinado com halteres", CHEST, COMPOUND, DUMBBELLS, BEGINNER, TRICEPS, SHOULDERS_FRONT),
        e("Flexão de braço", CHEST, COMPOUND, BODYWEIGHT, BEGINNER, TRICEPS, SHOULDERS_FRONT),
        e("Crucifixo na máquina", CHEST, ISOLATION, GYM),
        e("Crossover na polia", CHEST, ISOLATION, GYM),
        e("Crucifixo com halteres", CHEST, ISOLATION, DUMBBELLS),
        // Grande dorsal (puxadas verticais e pullovers)
        e("Puxada frontal", LATS, COMPOUND, GYM, BEGINNER, BICEPS, MID_BACK),
        e("Puxada supinada", LATS, COMPOUND, GYM, BEGINNER, BICEPS),
        e("Barra fixa", LATS, COMPOUND, BODYWEIGHT, INTERMEDIATE, BICEPS, MID_BACK),
        e("Remada unilateral com halter", LATS, COMPOUND, DUMBBELLS, BEGINNER, BICEPS, MID_BACK),
        e("Pulldown braço reto", LATS, ISOLATION, GYM),
        e("Pullover com halter", LATS, ISOLATION, DUMBBELLS),
        // Meio das costas: romboides e trapézio médio (remadas horizontais)
        e("Remada baixa", MID_BACK, COMPOUND, GYM, BEGINNER, LATS, BICEPS, SHOULDERS_REAR),
        e("Remada na máquina", MID_BACK, COMPOUND, GYM, BEGINNER, LATS, BICEPS),
        e("Remada cavalinho", MID_BACK, COMPOUND, GYM, INTERMEDIATE, LATS, BICEPS),
        e("Remada curvada", MID_BACK, COMPOUND, GYM, INTERMEDIATE, LATS, BICEPS, LOWER_BACK),
        e("Remada curvada com halteres", MID_BACK, COMPOUND, DUMBBELLS, BEGINNER, LATS, BICEPS),
        e("Remada invertida", MID_BACK, COMPOUND, BODYWEIGHT, BEGINNER, LATS, BICEPS),
        // Trapézio superior e inferior
        e("Encolhimento com barra", TRAPS, ISOLATION, GYM, BEGINNER, FOREARMS),
        e("Encolhimento com halteres", TRAPS, ISOLATION, DUMBBELLS, BEGINNER, FOREARMS),
        e("Remada alta", TRAPS, COMPOUND, GYM, INTERMEDIATE, SHOULDERS_SIDE, BICEPS),
        e("Elevação Y deitado", TRAPS, ISOLATION, BODYWEIGHT),
        // Lombar (eretores da espinha)
        e("Extensão lombar", LOWER_BACK, ISOLATION, GYM, BEGINNER, GLUTES, HAMSTRINGS),
        e("Good morning", LOWER_BACK, COMPOUND, GYM, INTERMEDIATE, HAMSTRINGS, GLUTES),
        e("Superman", LOWER_BACK, ISOLATION, BODYWEIGHT, BEGINNER, GLUTES),
        // Deltoide anterior (desenvolvimentos e elevação frontal)
        e("Desenvolvimento na máquina", SHOULDERS_FRONT, COMPOUND, GYM, BEGINNER, SHOULDERS_SIDE, TRICEPS),
        e("Desenvolvimento com halteres", SHOULDERS_FRONT, COMPOUND, DUMBBELLS, BEGINNER, SHOULDERS_SIDE, TRICEPS),
        e("Flexão pike", SHOULDERS_FRONT, COMPOUND, BODYWEIGHT, INTERMEDIATE, SHOULDERS_SIDE, TRICEPS),
        e("Elevação frontal", SHOULDERS_FRONT, ISOLATION, DUMBBELLS),
        // Deltoide lateral
        e("Elevação lateral", SHOULDERS_SIDE, ISOLATION, DUMBBELLS),
        e("Elevação lateral na polia", SHOULDERS_SIDE, ISOLATION, GYM),
        e("Elevação lateral na máquina", SHOULDERS_SIDE, ISOLATION, GYM),
        // Deltoide posterior
        e("Crucifixo inverso", SHOULDERS_REAR, ISOLATION, DUMBBELLS, BEGINNER, MID_BACK),
        e("Crucifixo inverso na máquina", SHOULDERS_REAR, ISOLATION, GYM, BEGINNER, MID_BACK),
        e("Face pull", SHOULDERS_REAR, ISOLATION, GYM, BEGINNER, MID_BACK, TRAPS),
        // Bíceps
        e("Rosca direta", BICEPS, ISOLATION, GYM, BEGINNER, FOREARMS),
        e("Rosca na polia", BICEPS, ISOLATION, GYM),
        e("Rosca alternada", BICEPS, ISOLATION, DUMBBELLS),
        e("Rosca martelo", BICEPS, ISOLATION, DUMBBELLS, BEGINNER, FOREARMS),
        e("Rosca concentrada", BICEPS, ISOLATION, DUMBBELLS),
        // Tríceps
        e("Tríceps na polia", TRICEPS, ISOLATION, GYM),
        e("Tríceps testa", TRICEPS, ISOLATION, GYM),
        e("Tríceps francês com halter", TRICEPS, ISOLATION, DUMBBELLS),
        e("Mergulho no banco", TRICEPS, ISOLATION, BODYWEIGHT),
        e("Flexão diamante", TRICEPS, COMPOUND, BODYWEIGHT, INTERMEDIATE, CHEST),
        // Antebraço
        e("Rosca de punho", FOREARMS, ISOLATION, DUMBBELLS),
        e("Rosca inversa", FOREARMS, ISOLATION, GYM),
        e("Suspensão na barra", FOREARMS, HOLD, BODYWEIGHT, INTERMEDIATE),
        // Quadríceps
        e("Agachamento livre", QUADS, COMPOUND, GYM, INTERMEDIATE, GLUTES, ADDUCTORS),
        e("Leg press 45°", QUADS, COMPOUND, GYM, BEGINNER, GLUTES),
        e("Agachamento hack", QUADS, COMPOUND, GYM, BEGINNER, GLUTES),
        e("Agachamento goblet", QUADS, COMPOUND, DUMBBELLS, BEGINNER, GLUTES, ADDUCTORS),
        e("Afundo com halteres", QUADS, COMPOUND, DUMBBELLS, BEGINNER, GLUTES),
        e("Agachamento búlgaro", QUADS, COMPOUND, DUMBBELLS, INTERMEDIATE, GLUTES),
        e("Agachamento sem peso", QUADS, COMPOUND, BODYWEIGHT, BEGINNER, GLUTES),
        e("Cadeira extensora", QUADS, ISOLATION, GYM),
        // Posterior de coxa (isquiotibiais)
        e("Stiff com barra", HAMSTRINGS, COMPOUND, GYM, BEGINNER, GLUTES, LOWER_BACK),
        e("Levantamento terra", HAMSTRINGS, COMPOUND, GYM, ADVANCED, GLUTES, LOWER_BACK, TRAPS),
        e("Stiff com halteres", HAMSTRINGS, COMPOUND, DUMBBELLS, BEGINNER, GLUTES),
        e("Stiff unilateral sem peso", HAMSTRINGS, COMPOUND, BODYWEIGHT, BEGINNER, GLUTES),
        e("Mesa flexora", HAMSTRINGS, ISOLATION, GYM),
        e("Cadeira flexora", HAMSTRINGS, ISOLATION, GYM),
        e("Flexão nórdica", HAMSTRINGS, ISOLATION, BODYWEIGHT, ADVANCED),
        // Glúteos (máximo e médio)
        e("Elevação pélvica", GLUTES, COMPOUND, GYM, BEGINNER, HAMSTRINGS),
        e("Elevação pélvica com halter", GLUTES, COMPOUND, DUMBBELLS, BEGINNER, HAMSTRINGS),
        e("Ponte de glúteo", GLUTES, COMPOUND, BODYWEIGHT, BEGINNER, HAMSTRINGS),
        e("Glúteo na polia", GLUTES, ISOLATION, GYM),
        e("Cadeira abdutora", GLUTES, ISOLATION, GYM),
        e("Abdução deitado de lado", GLUTES, ISOLATION, BODYWEIGHT),
        // Adutores
        e("Cadeira adutora", ADDUCTORS, ISOLATION, GYM),
        e("Adução na polia", ADDUCTORS, ISOLATION, GYM),
        e("Agachamento sumô com halter", ADDUCTORS, COMPOUND, DUMBBELLS, BEGINNER, QUADS, GLUTES),
        e("Adução deitado de lado", ADDUCTORS, ISOLATION, BODYWEIGHT),
        e("Prancha de Copenhague", ADDUCTORS, HOLD, BODYWEIGHT, INTERMEDIATE, OBLIQUES),
        // Panturrilha (gastrocnêmio e sóleo)
        e("Panturrilha em pé", CALVES, ISOLATION, GYM),
        e("Panturrilha sentado", CALVES, ISOLATION, GYM),
        e("Panturrilha no leg press", CALVES, ISOLATION, GYM),
        e("Panturrilha unilateral com halter", CALVES, ISOLATION, DUMBBELLS),
        e("Panturrilha no degrau", CALVES, ISOLATION, BODYWEIGHT),
        // Abdômen (reto abdominal)
        e("Prancha", ABS, HOLD, BODYWEIGHT, BEGINNER, OBLIQUES),
        e("Abdominal supra", ABS, ISOLATION, BODYWEIGHT),
        e("Abdominal infra", ABS, ISOLATION, BODYWEIGHT),
        e("Elevação de pernas", ABS, ISOLATION, BODYWEIGHT),
        e("Abdominal bicicleta", ABS, ISOLATION, BODYWEIGHT, BEGINNER, OBLIQUES),
        e("Dead bug", ABS, ISOLATION, BODYWEIGHT),
        e("Abdominal na polia", ABS, ISOLATION, GYM),
        e("Roda abdominal", ABS, ISOLATION, DUMBBELLS, INTERMEDIATE),
        // Oblíquos e estabilidade do tronco
        e("Prancha lateral", OBLIQUES, HOLD, BODYWEIGHT, BEGINNER, ABS),
        e("Abdominal oblíquo", OBLIQUES, ISOLATION, BODYWEIGHT),
        e("Russian twist", OBLIQUES, ISOLATION, BODYWEIGHT),
        e("Lenhador na polia", OBLIQUES, ISOLATION, GYM),
        e("Pallof press", OBLIQUES, ISOLATION, GYM),
        // Cardio
        e("Bicicleta ergométrica", CARDIO, RUN, GYM),
        e("Esteira", CARDIO, RUN, GYM),
        e("Elíptico", CARDIO, RUN, GYM),
        e("Caminhada inclinada na esteira", CARDIO, RUN, GYM),
        e("Simulador de escada", CARDIO, RUN, GYM, INTERMEDIATE),
        e("HIIT na bicicleta", CARDIO, RUN, GYM, INTERMEDIATE),
        e("Caminhada rápida", CARDIO, RUN, BODYWEIGHT),
        e("Polichinelo", CARDIO, RUN, BODYWEIGHT),
        e("Pular corda", CARDIO, RUN, DUMBBELLS),
        e("Burpee", CARDIO, RUN, BODYWEIGHT, INTERMEDIATE)
    )

    private val byName = entries.associateBy { it.name }

    fun info(name: String): CatalogEntry? = byName[name]

    /** Grupos secundários de um exercício (exercícios próprios do usuário não têm). */
    fun secondary(exercise: Exercise): List<MuscleGroup> = byName[exercise.name]?.secondary.orEmpty()

    /** Nível a partir do qual o exercício é sugerido (próprios do usuário: qualquer nível). */
    fun level(exercise: Exercise): Level = byName[exercise.name]?.level ?: BEGINNER

    fun all(): List<Exercise> =
        entries.map { Exercise(name = it.name, group = it.group, kind = it.kind, equipment = it.equipment) }
}

/** Alimentos iniciais (valores aproximados por 100 g), inseridos na primeira execução. */
object FoodExamples {
    private fun f(
        name: String, cat: FoodCategory, kcal: Double, p: Double, c: Double, g: Double,
        unit: String? = null, unitGrams: Double? = null
    ) = Food(
        name = name, category = cat, kcalPer100 = kcal, proteinPer100 = p, carbsPer100 = c, fatPer100 = g,
        unitLabel = unit, unitGrams = unitGrams
    )

    fun all(): List<Food> =
        listOf(
            f("Peito de frango grelhado", PROTEIN, 165.0, 31.0, 0.0, 3.6),
            f("Ovo cozido", PROTEIN, 155.0, 13.0, 1.1, 11.0, "ovo", 50.0),
            f("Carne moída (patinho)", PROTEIN, 219.0, 27.0, 0.0, 12.0),
            f("Alcatra grelhada", PROTEIN, 241.0, 31.9, 0.0, 11.6),
            f("Tilápia grelhada", PROTEIN, 128.0, 26.0, 0.0, 2.7),
            f("Salmão grelhado", PROTEIN, 208.0, 22.0, 0.0, 13.0),
            f("Atum em lata (ao natural)", PROTEIN, 116.0, 26.0, 0.0, 1.0),
            f("Queijo minas frescal", PROTEIN, 264.0, 17.4, 3.2, 20.2, "fatia", 30.0),
            f("Queijo cottage", PROTEIN, 98.0, 11.0, 3.4, 4.3),
            f("Peito de peru", PROTEIN, 109.0, 17.0, 2.0, 3.0, "fatia", 15.0),
            f("Iogurte natural", PROTEIN, 61.0, 3.5, 4.7, 3.3, "pote", 170.0),
            f("Whey protein", PROTEIN, 400.0, 80.0, 6.0, 6.0, "scoop", 30.0),
            f("Arroz branco cozido", CARB, 130.0, 2.5, 28.0, 0.3),
            f("Arroz integral cozido", CARB, 124.0, 2.6, 25.8, 1.0),
            f("Feijão carioca cozido", CARB, 76.0, 4.8, 13.6, 0.5),
            f("Batata-doce cozida", CARB, 86.0, 1.6, 20.0, 0.1),
            f("Batata inglesa cozida", CARB, 52.0, 1.2, 11.9, 0.0),
            f("Mandioca cozida", CARB, 125.0, 0.6, 30.1, 0.3),
            f("Macarrão cozido", CARB, 158.0, 5.8, 30.9, 0.9),
            f("Pão francês", CARB, 300.0, 8.0, 58.6, 3.1, "unidade", 50.0),
            f("Pão integral", CARB, 253.0, 9.4, 49.9, 3.7, "fatia", 25.0),
            f("Aveia em flocos", CARB, 394.0, 13.9, 66.6, 8.5, "colher", 15.0),
            f("Tapioca", CARB, 240.0, 0.2, 59.0, 0.2),
            f("Cuscuz de milho", CARB, 113.0, 2.2, 25.3, 0.7),
            f("Banana prata", FRUIT, 98.0, 1.3, 26.0, 0.1, "unidade", 70.0),
            f("Maçã", FRUIT, 56.0, 0.3, 15.2, 0.0, "unidade", 130.0),
            f("Mamão papaia", FRUIT, 40.0, 0.5, 10.4, 0.1, "fatia", 120.0),
            f("Laranja", FRUIT, 37.0, 1.0, 8.9, 0.1, "unidade", 140.0),
            f("Morango", FRUIT, 30.0, 0.9, 6.8, 0.3, "unidade", 15.0),
            f("Brócolis cozido", VEGETABLE, 35.0, 2.4, 7.2, 0.4),
            f("Cenoura cozida", VEGETABLE, 35.0, 0.8, 8.2, 0.2),
            f("Abobrinha refogada", VEGETABLE, 25.0, 1.1, 4.5, 0.5),
            f("Couve-flor cozida", VEGETABLE, 23.0, 1.9, 4.5, 0.2),
            f("Vagem cozida", VEGETABLE, 25.0, 1.4, 5.3, 0.2),
            f("Alface e tomate", SALAD, 17.0, 1.0, 3.0, 0.2),
            f("Rúcula", SALAD, 13.0, 1.8, 2.2, 0.1),
            f("Pepino", SALAD, 10.0, 0.9, 2.0, 0.0)
        )
}
