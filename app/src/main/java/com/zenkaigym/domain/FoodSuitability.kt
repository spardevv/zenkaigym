package com.zenkaigym.domain

import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.FoodCategory
import com.zenkaigym.data.entity.MealType
import com.zenkaigym.data.entity.MealType.BREAKFAST
import com.zenkaigym.data.entity.MealType.DINNER
import com.zenkaigym.data.entity.MealType.LUNCH
import com.zenkaigym.data.entity.MealType.SNACK_AM
import com.zenkaigym.data.entity.MealType.SNACK_PM
import com.zenkaigym.data.entity.MealType.SUPPER
import java.text.Normalizer

/**
 * Descobre, pelo nome, em quais refeições um alimento faz sentido (ovo no café, frango no almoço...).
 * Alimentos cadastrados pelo usuário com nome desconhecido ficam liberados para qualquer refeição,
 * mas com prioridade menor que os reconhecidos.
 */
object FoodSuitability {
    private val snacks = setOf(BREAKFAST, SNACK_AM, SNACK_PM)
    private val snacksAndNight = snacks + SUPPER
    private val main = setOf(LUNCH, DINNER)

    private val rules: List<Pair<List<String>, Set<MealType>>> = listOf(
        listOf("ovo", "omelete", "clara") to (snacksAndNight + DINNER),
        listOf("queijo", "requeijao", "cottage", "ricota") to snacksAndNight,
        listOf("presunto", "peru", "blanquet") to snacks,
        listOf("whey", "albumina", "proteina em po", "isolado", "hipercalorico") to snacksAndNight,
        listOf("leite", "iogurte", "coalhada", "kefir") to snacksAndNight,
        listOf("pao", "paes", "torrada", "tapioca", "aveia", "granola", "cuscuz", "biscoito", "bolacha", "cereal", "wrap", "crepioca", "cream cracker") to snacks,
        listOf("frango", "peito", "coxa", "sobrecoxa", "carne", "patinho", "alcatra", "acem", "contrafile", "fraldinha", "file", "maminha", "coxao", "bife", "boi", "bovina", "vermelha",
            "porco", "suino", "lombo", "peixe", "tilapia", "salmao", "merluza", "sardinha", "camarao", "bacalhau", "linguica", "hamburguer", "figado") to main,
        listOf("atum") to (main + SNACK_PM),
        listOf("arroz", "feijao", "lentilha", "grao", "ervilha", "macarrao", "massa", "espaguete", "macaxeira", "mandioca", "aipim", "inhame", "pure", "quinoa", "polenta", "farofa", "nhoque") to main,
        listOf("batata") to (main + SNACK_PM),
        listOf("banana", "maca", "mamao", "laranja", "morango", "abacaxi", "manga", "uva", "melancia", "pera", "kiwi", "mexerica", "tangerina", "melao", "goiaba", "abacate", "fruta", "ameixa", "pessego") to snacksAndNight
    )

    private fun tokens(name: String): List<String> =
        Normalizer.normalize(name.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")
            .split(Regex("[^a-z]+"))
            .filter { it.isNotEmpty() }

    private fun matches(words: List<String>, normalizedText: String, key: String) =
        if (' ' in key) normalizedText.contains(key)
        else words.any { it == key || it == key + "s" || (key.length >= 5 && it.startsWith(key)) }

    /** Refeições reconhecidas pelo nome; vazio quando o nome não diz nada. */
    fun recognized(food: Food): Set<MealType> {
        val words = tokens(food.name)
        val text = words.joinToString(" ")
        return rules.filter { (keys, _) -> keys.any { matches(words, text, it) } }
            .flatMap { it.second }.toSet()
    }

    private fun categoryDefault(category: FoodCategory): Set<MealType> = when (category) {
        FoodCategory.VEGETABLE, FoodCategory.SALAD -> main
        FoodCategory.FRUIT -> snacksAndNight
        else -> MealType.entries.toSet()
    }

    /** 2 = combina com a refeição, 1 = nome desconhecido (liberado), 0 = não combina. */
    fun score(food: Food, type: MealType): Int {
        val known = recognized(food)
        return when {
            known.isEmpty() -> if (type in categoryDefault(food.category)) 1 else 0
            type in known -> 2
            else -> 0
        }
    }
}
