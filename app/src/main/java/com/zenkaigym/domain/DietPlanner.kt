package com.zenkaigym.domain

import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.FoodCategory
import com.zenkaigym.data.entity.MealType
import kotlin.math.abs
import kotlin.math.roundToInt

data class PlannedMealItem(val foodId: Long, val grams: Int)
data class PlannedMeal(val name: String, val type: MealType, val items: List<PlannedMealItem>)
data class DietSuggestion(val meals: List<PlannedMeal>, val warnings: List<String>)

/**
 * Monta as refeições do dia a partir da agenda do usuário. Cada tipo de refeição escolhe
 * alimentos que combinam com ele (ver [FoodSuitability]): ovos, queijo, pão e whey de manhã;
 * frango, carne, arroz e legumes no almoço e no jantar; iogurte e fruta na ceia.
 */
object DietPlanner {
    private enum class Role { PROTEIN, PROTEIN2, CARB, VEGETABLE, SALAD, FRUIT }

    private val shares = mapOf(
        MealType.BREAKFAST to 0.24, MealType.SNACK_AM to 0.10, MealType.LUNCH to 0.30,
        MealType.SNACK_PM to 0.12, MealType.DINNER to 0.26, MealType.SUPPER to 0.08
    )

    /** Fatia das calorias do dia que [type] ocupa quando o dia tem as refeições [allTypes]. */
    fun shareOf(type: MealType, allTypes: List<MealType>): Double {
        val total = allTypes.distinct().sumOf { shares.getValue(it) }
        return if (total > 0) shares.getValue(type) / total else 0.0
    }

    private fun rolesOf(type: MealType): List<Role> = when (type) {
        MealType.BREAKFAST -> listOf(Role.PROTEIN, Role.PROTEIN2, Role.CARB, Role.FRUIT)
        MealType.SNACK_AM, MealType.SNACK_PM -> listOf(Role.PROTEIN, Role.FRUIT, Role.CARB)
        MealType.LUNCH, MealType.DINNER -> listOf(Role.VEGETABLE, Role.SALAD, Role.PROTEIN, Role.CARB)
        MealType.SUPPER -> listOf(Role.PROTEIN, Role.FRUIT)
    }

    private fun categoryOf(role: Role) = when (role) {
        Role.PROTEIN, Role.PROTEIN2 -> FoodCategory.PROTEIN
        Role.CARB -> FoodCategory.CARB
        Role.VEGETABLE -> FoodCategory.VEGETABLE
        Role.SALAD -> FoodCategory.SALAD
        Role.FRUIT -> FoodCategory.FRUIT
    }

    private const val VEG_GRAMS = 150
    private const val SALAD_GRAMS = 100

    fun plan(
        target: NutritionTarget,
        foods: List<Food>,
        types: List<MealType> = MealSchedule.typesFor(4),
        // Quantas vezes cada alimento já foi usado; compartilhar entre os dias da semana gera variedade.
        used: MutableMap<Long, Int> = mutableMapOf()
    ): DietSuggestion {
        val warnings = mutableListOf<String>()
        listOf(FoodCategory.PROTEIN, FoodCategory.CARB, FoodCategory.VEGETABLE, FoodCategory.SALAD).forEach { cat ->
            if (foods.none { it.category == cat }) {
                val required = cat == FoodCategory.PROTEIN || cat == FoodCategory.CARB
                warnings += if (required) tr("Cadastre ao menos um alimento de {0}.", cat.label.lowercase())
                else tr("Sem {0} cadastrado: a dieta ficará sem esse item.", cat.label.lowercase())
            }
        }

        val ordered = types.distinct().sortedBy { it.ordinal }
        val totalShare = ordered.sumOf { shares.getValue(it) }
        val byId = foods.associateBy { it.id }

        fun pick(category: FoodCategory, type: MealType, exclude: Set<Long>): Food? {
            val pool = foods.filter { it.category == category && it.id !in exclude }
            if (pool.isEmpty()) return null
            val suitable = pool.filter { FoodSuitability.score(it, type) > 0 }.ifEmpty { pool }
            val chosen = suitable.sortedWith(
                compareBy<Food>({ used.getOrDefault(it.id, 0) }, { -FoodSuitability.score(it, type) }, { it.id })
            ).first()
            used[chosen.id] = used.getOrDefault(chosen.id, 0) + 1
            return chosen
        }

        val meals = ordered.map { type ->
            val share = shares.getValue(type) / totalShare
            val mealKcal = target.kcal * share
            val mealProtein = target.protein * share
            val chosen = mutableSetOf<Long>()
            val picked = mutableMapOf<Role, Food>()
            for (role in rolesOf(type)) {
                // Lanches usam carboidrato só quando não há fruta.
                val snack = type == MealType.SNACK_AM || type == MealType.SNACK_PM
                if (role == Role.CARB && snack && Role.FRUIT in picked) continue
                pick(categoryOf(role), type, chosen)?.let { picked[role] = it; chosen += it.id }
            }
            if (type == MealType.SUPPER && Role.FRUIT !in picked) {
                pick(FoodCategory.CARB, type, chosen)?.let { picked[Role.CARB] = it }
            }

            val grams = mutableMapOf<Role, Int>()
            var fixedKcal = 0.0
            var fixedProtein = 0.0
            fun fix(role: Role, g: Int) {
                val f = picked[role] ?: return
                grams[role] = g
                fixedKcal += f.kcalPer100 * g / 100
                fixedProtein += f.proteinPer100 * g / 100
            }
            fix(Role.VEGETABLE, VEG_GRAMS)
            fix(Role.SALAD, SALAD_GRAMS)
            picked[Role.FRUIT]?.let { fruit ->
                val cap = if (fruit.kcalPer100 > 0) 0.25 * mealKcal / fruit.kcalPer100 * 100 else 150.0
                fix(Role.FRUIT, round5(cap.coerceAtMost(150.0)).coerceAtLeast(50))
            }

            val proteins = listOf(Role.PROTEIN, Role.PROTEIN2).filter { it in picked }
            val weights = if (proteins.size == 2) mapOf(Role.PROTEIN to 0.6, Role.PROTEIN2 to 0.4) else mapOf(Role.PROTEIN to 1.0)
            val needProtein = (mealProtein - fixedProtein).coerceAtLeast(0.0)
            val needKcal = (mealKcal - fixedKcal).coerceAtLeast(0.0)
            val carb = picked[Role.CARB]

            // O carboidrato também traz proteína e a proteína traz calorias: itera para fechar os dois.
            var carbG = 0.0
            val proteinG = mutableMapOf<Role, Double>()
            repeat(4) {
                val carbProtein = if (carb != null) carbG * carb.proteinPer100 / 100 else 0.0
                val proteinNeeded = (needProtein - carbProtein).coerceAtLeast(0.0)
                var proteinKcal = 0.0
                proteins.forEach { role ->
                    val f = picked.getValue(role)
                    val perGram = f.proteinPer100 / 100
                    val raw = if (perGram > 0) proteinNeeded * weights.getValue(role) / perGram else 100.0
                    val g = raw.coerceIn(minProtein(f), maxProtein(f, mealKcal))
                    proteinG[role] = g
                    proteinKcal += f.kcalPer100 * g / 100
                }
                // Com carboidrato na refeição, a proteína não pode ocupar todas as calorias dela.
                val budget = 0.55 * needKcal
                if (carb != null && proteinKcal > budget && proteinKcal > 0) {
                    val factor = budget / proteinKcal
                    proteinKcal = 0.0
                    proteins.forEach { role ->
                        val f = picked.getValue(role)
                        val g = (proteinG.getValue(role) * factor).coerceAtLeast(minProtein(f))
                        proteinG[role] = g
                        proteinKcal += f.kcalPer100 * g / 100
                    }
                }
                carbG = if (carb != null && carb.kcalPer100 > 0) {
                    ((needKcal - proteinKcal).coerceAtLeast(0.0) / (carb.kcalPer100 / 100))
                        .coerceAtMost(maxCarb(carb, mealKcal))
                } else 0.0
            }
            proteins.forEach { grams[it] = round5(proteinG.getValue(it)) }
            if (carb != null && round5(carbG) >= 20) grams[Role.CARB] = round5(carbG)

            val items = rolesOf(type).mapNotNull { role ->
                val f = picked[role]
                val g = grams[role]
                if (f != null && g != null) PlannedMealItem(f.id, g) else null
            }
            PlannedMeal(type.pt, type, items)
        }
        return DietSuggestion(snapToUnits(balanceCarbs(meals, byId, target), byId), warnings)
    }

    /** Alimentos com medida caseira (ovo, fatia, fruta) entram em unidades inteiras. */
    private fun snapToUnits(meals: List<PlannedMeal>, byId: Map<Long, Food>): List<PlannedMeal> =
        meals.map { meal -> meal.copy(items = meal.items.map { it.copy(grams = Portions.snap(byId.getValue(it.foodId), it.grams)) }) }

    private fun minProtein(f: Food) = if (f.proteinPer100 >= 60) 20.0 else 30.0

    private fun maxProtein(f: Food, mealKcal: Double): Double {
        if (f.proteinPer100 >= 60) return 60.0
        val byKcal = if (f.kcalPer100 > 0) 0.6 * mealKcal / f.kcalPer100 * 100 else 350.0
        return byKcal.coerceIn(60.0, 350.0)
    }

    private fun maxCarb(f: Food, mealKcal: Double): Double =
        (0.7 * mealKcal / f.kcalPer100 * 100).coerceIn(60.0, 500.0)

    /** Ajuste final: o carboidrato absorve a diferença de calorias entre o plano e a meta do dia. */
    private fun balanceCarbs(meals: List<PlannedMeal>, byId: Map<Long, Food>, target: NutritionTarget): List<PlannedMeal> {
        val all = meals.flatMap { it.items }
        fun kcal(i: PlannedMealItem) = byId.getValue(i.foodId).kcalPer100 * i.grams / 100
        val carbKcal = all.filter { byId.getValue(it.foodId).category == FoodCategory.CARB }.sumOf { kcal(it) }
        if (carbKcal <= 0) return meals
        val otherKcal = all.sumOf { kcal(it) } - carbKcal
        val factor = ((target.kcal - otherKcal) / carbKcal).coerceIn(0.7, 1.5)
        if (abs(factor - 1.0) < 0.02) return meals
        return meals.map { meal ->
            meal.copy(items = meal.items.map { item ->
                if (byId.getValue(item.foodId).category == FoodCategory.CARB) {
                    item.copy(grams = round5(item.grams * factor).coerceIn(20, if (meal.type == MealType.LUNCH || meal.type == MealType.DINNER) 600 else 350))
                } else item
            })
        }
    }

    // ---- Troca de itens ----

    /** Quantidade do novo alimento que equivale ao atual: mesma proteína (proteínas) ou mesmas calorias. */
    fun equivalentGrams(old: Food, oldGrams: Int, new: Food): Int {
        val grams = when (old.category) {
            FoodCategory.VEGETABLE, FoodCategory.SALAD -> oldGrams.toDouble()
            FoodCategory.PROTEIN ->
                if (old.proteinPer100 > 0 && new.proteinPer100 > 0) old.proteinPer100 * oldGrams / new.proteinPer100
                else kcalEquivalent(old, oldGrams, new)
            else -> kcalEquivalent(old, oldGrams, new)
        }
        return Portions.snap(new, round5(grams).coerceIn(5, 1000))
    }

    private fun kcalEquivalent(old: Food, oldGrams: Int, new: Food): Double =
        if (new.kcalPer100 > 0) old.kcalPer100 * oldGrams / new.kcalPer100 else oldGrams.toDouble()

    /** Porção inicial ao adicionar um alimento a uma refeição. */
    fun defaultGrams(food: Food): Int =
        if (Portions.unitGrams(food) != null) Portions.snap(food, 1)
        else if (food.category == FoodCategory.SALAD) 80 else 100

    /** Alimentos da mesma categoria, os que combinam com a refeição primeiro. */
    fun alternatives(foods: List<Food>, category: FoodCategory, type: MealType, excludeId: Long? = null): List<Food> =
        foods.filter { it.category == category && it.id != excludeId }
            .sortedWith(compareBy<Food>({ -FoodSuitability.score(it, type) }, { it.name.lowercase() }))

    private fun round5(value: Double): Int = (value / 5).roundToInt() * 5
}
