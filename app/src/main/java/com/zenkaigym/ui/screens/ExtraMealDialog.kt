package com.zenkaigym.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.FoodCategory
import com.zenkaigym.data.entity.MealType
import com.zenkaigym.domain.DietPlanner
import com.zenkaigym.domain.ExtraValidation
import com.zenkaigym.domain.ExtraValidator
import com.zenkaigym.domain.Portions
import com.zenkaigym.domain.tr
import com.zenkaigym.ui.components.PickRow
import com.zenkaigym.ui.components.Stepper
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenChip
import com.zenkaigym.ui.components.ZenDialog
import com.zenkaigym.ui.components.ZenField
import com.zenkaigym.ui.components.accent
import com.zenkaigym.ui.components.shortLabel
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Zen
import kotlin.math.roundToInt

/**
 * "Comi outra coisa": registra um alimento da lista (com a quantidade) ou um prato livre
 * (nome e valores estimados) numa refeição de hoje. As refeições seguintes do dia se reajustam.
 */
@Composable
fun AddExtraDialog(
    mealName: String,
    type: MealType,
    foods: List<Food>,
    onFood: (Food, Int) -> Unit,
    onFree: (String, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var fromList by remember { mutableStateOf(true) }
    var category by remember { mutableStateOf(FoodCategory.PROTEIN) }
    var chosen by remember { mutableStateOf<Food?>(null) }
    var grams by remember { mutableIntStateOf(100) }
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val options = remember(foods, category, type) { DietPlanner.alternatives(foods, category, type) }

    ZenDialog(tr("Comi outra coisa · {0}", mealName), onDismiss, accent = Ao) {
        Text(tr("Anote o que você comeu. As próximas refeições de hoje se ajustam; o cardápio não muda."), color = Zen.p.muted)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenChip(tr("Da lista"), fromList, { fromList = true; error = null }, Modifier.weight(1f), accent = Ao)
            ZenChip(tr("Prato livre"), !fromList, { fromList = false; error = null }, Modifier.weight(1f), accent = Ao)
        }
        Spacer(Modifier.height(12.dp))

        val food = chosen
        if (fromList && food != null) {
            // Alimento escolhido: só falta a quantidade.
            Text(tr(food.name).uppercase(), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            val step = Portions.stepGrams(food)
            Stepper(
                Portions.text(food, grams),
                onMinus = { grams = (grams - step).coerceAtLeast(5) },
                onPlus = { grams = (grams + step).coerceAtMost(1000) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(
                tr("{0} kcal · {1} g proteína", (food.kcalPer100 * grams / 100).roundToInt(), (food.proteinPer100 * grams / 100).roundToInt()),
                color = Zen.p.muted
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZenButton(tr("Anotar"), { onFood(food, grams) }, Modifier.weight(1f), accent = Ao, fill = true)
                ZenButton(tr("Voltar"), { chosen = null }, accent = Zen.p.card)
            }
        } else if (fromList) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FoodCategory.entries.forEach {
                    ZenChip(it.shortLabel(), category == it, { category = it }, Modifier.weight(1f), accent = it.accent(), compact = true)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (options.isEmpty()) Text(tr("Nenhum alimento nesta categoria."), color = Zen.p.muted)
            options.forEach { f ->
                PickRow(tr(f.name), tr("{0} kcal/100 g", f.kcalPer100.toInt()), Portions.text(f, DietPlanner.defaultGrams(f)), f.category.accent()) {
                    chosen = f
                    grams = DietPlanner.defaultGrams(f)
                }
            }
            Spacer(Modifier.height(8.dp))
            ZenButton(tr("Fechar"), onDismiss, accent = Zen.p.card, fill = true, small = true)
        } else {
            ZenField(tr("Prato"), name, { name = it }, placeholder = tr("ex.: Pizza, 2 fatias"))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZenField(tr("Kcal (estimado)"), kcal, { kcal = it }, Modifier.weight(1f), KeyboardType.Number)
                ZenField(tr("Proteína (g)"), protein, { protein = it }, Modifier.weight(1f), KeyboardType.Number, placeholder = "0")
            }
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Aka, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZenButton(tr("Anotar"), {
                    when (val r = ExtraValidator.validate(name, kcal, protein)) {
                        is ExtraValidation.Invalid -> error = r.message
                        is ExtraValidation.Valid -> onFree(r.name, r.kcal, r.protein)
                    }
                }, Modifier.weight(1f), accent = Ao, fill = true)
                ZenButton(tr("Cancelar"), onDismiss, accent = Zen.p.card)
            }
        }
    }
}
