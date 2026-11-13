package com.zenkaigym.domain

import com.zenkaigym.data.entity.Food
import kotlin.math.roundToInt

/** Medidas caseiras: alimentos com unidade (ovo, fatia, banana...) mostram a quantidade em unidades e o peso. */
object Portions {
    private const val GRAM_STEP = 10

    fun unitGrams(food: Food): Double? = food.unitGrams?.takeIf { it > 0 && !food.unitLabel.isNullOrBlank() }

    /** Quanto o botão +/− muda: uma unidade quando o alimento tem unidade, senão 10 g. */
    fun stepGrams(food: Food): Int = unitGrams(food)?.roundToInt()?.coerceAtLeast(1) ?: GRAM_STEP

    /** Arredonda para um número inteiro de unidades (mínimo 1); alimentos sem unidade ficam como estão. */
    fun snap(food: Food, grams: Int): Int {
        val unit = unitGrams(food) ?: return grams
        val units = (grams / unit).roundToInt().coerceAtLeast(1)
        return (units * unit).roundToInt()
    }

    /** "2 ovos · 100 g" para alimentos com unidade, "100 g" para os demais (no japonês: "2個 · 100 g"). */
    fun text(food: Food, grams: Int): String {
        val unit = unitGrams(food) ?: return "$grams g"
        val qty = ((grams / unit) * 2).roundToInt() / 2.0
        val label = tr(food.unitLabel!!.trim())
        if (I18n.lang == Lang.JA) return "${qtyText(qty)}$label · $grams g"
        val word = if (qty <= 1.0) label else plural(label)
        return "${qtyText(qty)} $word · $grams g"
    }

    private fun qtyText(qty: Double): String {
        if (qty % 1.0 == 0.0) return qty.toInt().toString()
        return qty.toInt().toString() + if (I18n.lang == Lang.PT) ",5" else ".5"
    }

    fun plural(word: String): String = when (I18n.lang) {
        Lang.JA -> word
        Lang.EN -> if (word.endsWith("s") || word.endsWith("x") || word.endsWith("ch") || word.endsWith("sh")) word + "es" else word + "s"
        Lang.PT -> when {
            word == "pão" -> "pães"
            word.endsWith("ão") -> word.dropLast(2) + "ões"
            word.endsWith("r") || word.endsWith("z") -> word + "es"
            word.endsWith("s") -> word
            else -> word + "s"
        }
    }

    /** Confere a medida caseira digitada; null nos dois campos vazios significa "só por peso". */
    sealed interface UnitInput {
        data class Valid(val label: String?, val grams: Double?) : UnitInput
        data class Invalid(val message: String) : UnitInput
    }

    fun parseUnit(label: String, grams: String): UnitInput {
        val l = label.trim()
        val g = grams.trim()
        if (l.isEmpty() && g.isEmpty()) return UnitInput.Valid(null, null)
        if (l.isEmpty() || g.isEmpty()) return UnitInput.Invalid(tr("Preencha o nome e o peso da unidade, ou deixe os dois vazios."))
        val value = g.replace(',', '.').toDoubleOrNull()
        if (value == null || value < 1.0 || value > 1000.0) return UnitInput.Invalid(tr("Peso da unidade inválido (1–1000 g)."))
        return UnitInput.Valid(l, value)
    }
}
