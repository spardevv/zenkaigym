package com.zenkaigym.domain

import com.zenkaigym.util.toDecimalOrNull

sealed interface ProfileValidation {
    data class Valid(val heightCm: Int, val weightKg: Double, val age: Int) : ProfileValidation
    data class Invalid(val message: String) : ProfileValidation
}

object ProfileValidator {
    fun validate(height: String, weight: String, age: String): ProfileValidation {
        val h = height.trim().toIntOrNull()
        val w = weight.toDecimalOrNull()
        val a = age.trim().toIntOrNull()
        return when {
            h == null || h !in 100..250 -> ProfileValidation.Invalid(tr("Altura inválida (100–250 cm)."))
            w == null || w !in 30.0..300.0 -> ProfileValidation.Invalid(tr("Peso inválido (30–300 kg)."))
            a == null || a !in 10..100 -> ProfileValidation.Invalid(tr("Idade inválida (10–100)."))
            else -> ProfileValidation.Valid(h, w, a)
        }
    }
}

sealed interface FoodValidation {
    data class Valid(
        val name: String,
        val kcal: Double,
        val protein: Double,
        val carbs: Double,
        val fat: Double
    ) : FoodValidation

    data class Invalid(val message: String) : FoodValidation
}

object FoodValidator {
    private const val MAX_KCAL_PER_100G = 900.0
    private const val MAX_MACROS_PER_100G = 100.0

    /** Valores nutricionais são por 100 g do alimento. */
    fun validate(name: String, kcal: String, protein: String, carbs: String, fat: String): FoodValidation {
        if (name.isBlank()) return FoodValidation.Invalid(tr("Informe o nome do alimento."))
        val k = kcal.toDecimalOrNull()
        val p = protein.toDecimalOrNull()
        val c = carbs.toDecimalOrNull()
        val f = fat.toDecimalOrNull()
        if (k == null || p == null || c == null || f == null) {
            return FoodValidation.Invalid(tr("Preencha todos os valores nutricionais com números."))
        }
        if (k < 0 || p < 0 || c < 0 || f < 0) {
            return FoodValidation.Invalid(tr("Os valores não podem ser negativos."))
        }
        if (p + c + f > MAX_MACROS_PER_100G) {
            return FoodValidation.Invalid(tr("Proteína + carboidrato + gordura não podem passar de 100 g por 100 g."))
        }
        if (k > MAX_KCAL_PER_100G) {
            return FoodValidation.Invalid(tr("Mais de 900 kcal por 100 g não é possível."))
        }
        return FoodValidation.Valid(name.trim(), k, p, c, f)
    }
}

sealed interface ExtraValidation {
    data class Valid(val name: String, val kcal: Int, val protein: Int) : ExtraValidation
    data class Invalid(val message: String) : ExtraValidation
}

/** Prato livre comido fora do cardápio: nome, calorias estimadas e proteína (opcional). */
object ExtraValidator {
    fun validate(name: String, kcal: String, protein: String): ExtraValidation {
        if (name.isBlank()) return ExtraValidation.Invalid(tr("Informe o nome do prato."))
        val k = kcal.trim().toIntOrNull()
        if (k == null || k !in 1..5000) return ExtraValidation.Invalid(tr("Calorias inválidas (1–5000 kcal)."))
        val p = if (protein.isBlank()) 0 else protein.trim().toIntOrNull()
        if (p == null || p !in 0..300) return ExtraValidation.Invalid(tr("Proteína inválida (0–300 g)."))
        if (p * 4 > k) return ExtraValidation.Invalid(tr("A proteína não pode ter mais calorias que o prato inteiro."))
        return ExtraValidation.Valid(name.trim(), k, p)
    }
}
