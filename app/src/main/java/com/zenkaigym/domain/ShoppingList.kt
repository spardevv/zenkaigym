package com.zenkaigym.domain

import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.FoodCategory
import kotlin.math.ceil

/** Um alimento do cardápio num dia da semana (1 = segunda ... 7 = domingo; 0 = todos os dias). */
data class ShoppingEntry(val dayOfWeek: Int, val food: Food, val grams: Int)

/** Total a comprar de um alimento no período. */
data class ShoppingItem(val food: Food, val grams: Int)

data class ShoppingList(val days: Int, val groups: List<Pair<FoodCategory, List<ShoppingItem>>>) {
    val isEmpty: Boolean get() = groups.isEmpty()
}

/**
 * Lista de compras a partir do cardápio semanal: soma cada alimento pelo número de vezes que
 * cada dia da semana aparece no período (ex.: 30 dias a partir de hoje).
 */
object ShoppingPlanner {
    const val WEEK = 7
    const val FORTNIGHT = 14
    const val MONTH = 30

    /** Quantas vezes cada dia da semana (índice 1..7) cai em [days] dias começando em [startDow]. */
    fun weekdayCounts(startDow: Int, days: Int): IntArray {
        val counts = IntArray(8)
        repeat(days) { counts[(startDow - 1 + it) % 7 + 1]++ }
        return counts
    }

    fun build(entries: List<ShoppingEntry>, startDow: Int, days: Int): ShoppingList {
        val counts = weekdayCounts(startDow, days)
        val totals = LinkedHashMap<Long, ShoppingItem>()
        entries.forEach { e ->
            val times = if (e.dayOfWeek == 0) days else counts.getOrElse(e.dayOfWeek) { 0 }
            if (times == 0 || e.grams <= 0) return@forEach
            val prev = totals[e.food.id]
            totals[e.food.id] = ShoppingItem(e.food, (prev?.grams ?: 0) + e.grams * times)
        }
        val groups = totals.values
            .groupBy { it.food.category }
            .toSortedMap(compareBy { it.ordinal })
            .map { (cat, list) -> cat to list.sortedBy { tr(it.food.name).lowercase() } }
        return ShoppingList(days, groups)
    }

    /** "12 ovos · ~600 g" para alimentos com unidade (arredonda para cima); "850 g" ou "3,2 kg" para os demais. */
    fun quantity(food: Food, grams: Int): String {
        val unit = Portions.unitGrams(food)
        if (unit != null) {
            val units = ceil(grams / unit - 1e-9).toInt().coerceAtLeast(1)
            val label = tr(food.unitLabel!!.trim())
            val count = if (I18n.lang == Lang.JA) "$units$label" else "$units ${if (units == 1) label else Portions.plural(label)}"
            return "$count · ~${mass(grams)}"
        }
        return mass(grams)
    }

    /** Peso arredondado para cima: de 10 em 10 g até 1 kg; acima disso, em décimos de kg. */
    fun mass(grams: Int): String {
        if (grams < 1000) return "${(ceil(grams / 10.0) * 10).toInt()} g"
        val tenths = ceil(grams / 100.0).toInt()
        val whole = tenths / 10
        val frac = tenths % 10
        val sep = if (I18n.lang == Lang.PT) "," else "."
        return if (frac == 0) "$whole kg" else "$whole$sep$frac kg"
    }

    /** Texto pronto para compartilhar (WhatsApp, notas, e-mail...). [period] é o intervalo de datas já formatado. */
    fun text(list: ShoppingList, period: String): String = buildString {
        append(tr("Lista de compras · {0} dias", list.days))
        append('\n').append(period).append('\n')
        list.groups.forEach { (cat, items) ->
            append('\n').append(cat.label.uppercase()).append('\n')
            items.forEach { append("☐ ").append(tr(it.food.name)).append(": ").append(quantity(it.food, it.grams)).append('\n') }
        }
        append('\n').append(tr("Gerado pelo Zenkaigym com base no seu cardápio."))
    }
}
