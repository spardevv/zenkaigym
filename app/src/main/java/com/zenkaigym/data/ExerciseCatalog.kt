package com.zenkaigym.data

import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.FoodCategory
import com.zenkaigym.data.entity.FoodCategory.CARB
import com.zenkaigym.data.entity.FoodCategory.FRUIT
import com.zenkaigym.data.entity.FoodCategory.PROTEIN
import com.zenkaigym.data.entity.FoodCategory.SALAD
import com.zenkaigym.data.entity.FoodCategory.VEGETABLE
import com.zenkaigym.data.entity.MuscleGroup
import com.zenkaigym.data.entity.MuscleGroup.BACK
import com.zenkaigym.data.entity.MuscleGroup.BICEPS
import com.zenkaigym.data.entity.MuscleGroup.CHEST
import com.zenkaigym.data.entity.MuscleGroup.CORE
import com.zenkaigym.data.entity.MuscleGroup.LEGS
import com.zenkaigym.data.entity.MuscleGroup.SHOULDERS
import com.zenkaigym.data.entity.MuscleGroup.TRICEPS

/** Catálogo inicial de exercícios, inserido na primeira execução. */
object ExerciseCatalog {
    private val byGroup: Map<MuscleGroup, List<String>> = mapOf(
        CHEST to listOf("Supino reto com barra", "Supino inclinado com halteres", "Crucifixo na máquina", "Flexão de braço", "Crossover na polia"),
        BACK to listOf("Puxada frontal", "Remada curvada", "Remada baixa", "Barra fixa", "Remada unilateral com halter"),
        LEGS to listOf("Agachamento livre", "Leg press 45°", "Cadeira extensora", "Mesa flexora", "Stiff com barra", "Afundo com halteres", "Panturrilha em pé"),
        SHOULDERS to listOf("Desenvolvimento com halteres", "Elevação lateral", "Elevação frontal", "Crucifixo inverso"),
        BICEPS to listOf("Rosca direta", "Rosca alternada", "Rosca martelo"),
        TRICEPS to listOf("Tríceps testa", "Tríceps na polia", "Mergulho no banco"),
        CORE to listOf("Prancha", "Abdominal supra", "Elevação de pernas")
    )

    fun all(): List<Exercise> =
        byGroup.flatMap { (group, names) -> names.map { Exercise(name = it, group = group) } }
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
