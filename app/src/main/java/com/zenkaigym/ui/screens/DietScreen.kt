package com.zenkaigym.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.fillMaxSize
import com.zenkaigym.ui.components.ScrollHints
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenkaigym.data.entity.DietPlanFull
import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.FoodCategory
import com.zenkaigym.data.entity.MealFull
import com.zenkaigym.data.entity.MealItemFull
import com.zenkaigym.data.entity.MealSlot
import com.zenkaigym.data.entity.MealType
import com.zenkaigym.data.entity.UserProfile
import com.zenkaigym.domain.DietPlanner
import com.zenkaigym.domain.MealSchedule
import com.zenkaigym.domain.NutritionTarget
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.components.FoodFormDialog
import com.zenkaigym.ui.components.PickRow
import com.zenkaigym.ui.components.SectionTitle
import com.zenkaigym.ui.components.StatPill
import com.zenkaigym.ui.components.Stepper
import com.zenkaigym.ui.components.TimeChip
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenCard
import com.zenkaigym.ui.components.ZenChip
import com.zenkaigym.ui.components.ZenDialog
import com.zenkaigym.ui.components.ZenField
import com.zenkaigym.ui.components.ZenHeader
import com.zenkaigym.ui.components.ZenPage
import com.zenkaigym.ui.components.ZenTimePickerDialog
import com.zenkaigym.ui.components.accent
import com.zenkaigym.ui.components.shortLabel
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Zen
import kotlin.math.roundToInt

private const val GRAM_STEP = 10

@Composable
fun DietScreen(viewModel: ZenViewModel, onOpenFoods: () -> Unit) {
    val proposed by viewModel.proposedDiet.collectAsStateWithLifecycle()
    val active by viewModel.activeDiet.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val suggested by viewModel.suggestedTarget.collectAsStateWithLifecycle()
    val slots by viewModel.mealSlots.collectAsStateWithLifecycle()
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    val shown = proposed ?: active
    val isProposal = proposed != null

    // Sem nenhum plano ainda, já deixa uma sugestão pronta: o usuário só revisa e aceita.
    LaunchedEffect(Unit) { viewModel.ensureDietSuggestion() }

    var editingId by remember { mutableStateOf<Long?>(null) }
    var addingTo by remember { mutableStateOf<Long?>(null) }

    ZenPage {
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ZenHeader("Dieta", "食", accent = Midori, subtitle = "Meta, horários e refeições")
                    ZenButton("Meus alimentos  →", onOpenFoods, accent = Ao, fill = true, small = true)
                }
            }
            profile?.let { p -> item(key = "target") { TargetCard(p, suggested, viewModel) } }
            item(key = "schedule") { ScheduleCard(slots, viewModel) }
            item(key = "plan") {
                PlanCard(shown, isProposal, hasActive = active != null, slots = slots, viewModel = viewModel)
            }
            if (shown != null) {
                items(shown.meals.sortedBy { it.meal.position }, key = { it.meal.id }) { meal ->
                    MealCard(
                        meal, MealSchedule.timeOf(meal.meal.type, slots).format(),
                        onEdit = { editingId = it }, onAdd = { addingTo = meal.meal.id }
                    )
                }
            }
        }
        ScrollHints(listState.canScrollBackward, listState.canScrollForward, Modifier.fillMaxSize())
    }

    // As janelas procuram o item/refeição no plano atual, então refletem cada alteração na hora.
    val items = shown?.meals?.flatMap { m -> m.items.map { m to it } }.orEmpty()
    editingId?.let { id ->
        val pair = items.firstOrNull { it.second.item.id == id }
        if (pair == null) editingId = null
        else FoodItemDialog(pair.first, pair.second, foods, viewModel) { editingId = null }
    }
    addingTo?.let { id ->
        val meal = shown?.meals?.firstOrNull { it.meal.id == id }
        if (meal == null) addingTo = null
        else AddFoodDialog(meal, foods, viewModel) { addingTo = null }
    }
}

// ---- Meta diária ----

@Composable
private fun TargetCard(profile: UserProfile, suggested: NutritionTarget?, viewModel: ZenViewModel) {
    val kcal = profile.targetKcal
    val protein = profile.targetProtein
    val accepted = if (kcal != null && protein != null) {
        NutritionTarget(kcal, protein, profile.targetCarbs ?: 0, profile.targetFat ?: 0)
    } else null
    val shown = accepted ?: suggested ?: return
    var editing by remember { mutableStateOf(false) }
    var kcalText by remember(shown) { mutableStateOf(shown.kcal.toString()) }
    var proteinText by remember(shown) { mutableStateOf(shown.protein.toString()) }

    ZenCard(accent = Midori, entranceIndex = 0) {
        SectionTitle(if (accepted != null) "Sua meta diária" else "Meta sugerida", accent = Midori)
        if (accepted == null) {
            Spacer(Modifier.height(4.dp))
            Text("Calculada com seu corpo, objetivo e dias de treino. Aceite ou ajuste.", color = Zen.p.muted)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPill("kcal", "${shown.kcal}", Modifier.weight(1f), Aka)
            StatPill("proteína", "${shown.protein}g", Modifier.weight(1f), Ki)
            StatPill("carbo", "${shown.carbs}g", Modifier.weight(1f), Ao)
            StatPill("gordura", "${shown.fat}g", Modifier.weight(1f), Midori)
        }
        Spacer(Modifier.height(12.dp))
        if (editing) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ZenField("Calorias", kcalText, { kcalText = it }, Modifier.weight(1f), KeyboardType.Number)
                ZenField("Proteína (g)", proteinText, { proteinText = it }, Modifier.weight(1f), KeyboardType.Number)
            }
            Spacer(Modifier.height(10.dp))
            ZenButton("Salvar minha meta", {
                val k = kcalText.trim().toIntOrNull()
                val p = proteinText.trim().toIntOrNull()
                if (k != null && p != null && k in 800..6000 && p in 20..500) {
                    viewModel.acceptCustomTarget(k, p)
                    editing = false
                }
            }, accent = Midori, fill = true)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (accepted == null && suggested != null) ZenButton("Aceitar", { viewModel.acceptTarget(suggested) }, accent = Midori)
                ZenButton("Ajustar", { editing = true }, accent = Ki)
                if (accepted != null && suggested != null && suggested != accepted && suggested.kcal != accepted.kcal) {
                    ZenButton("Usar sugerida", { viewModel.acceptTarget(suggested) }, accent = Zen.p.card)
                }
            }
        }
    }
}

// ---- Agenda de refeições ----

@Composable
private fun ScheduleCard(slots: List<MealSlot>, viewModel: ZenViewModel) {
    var picker by remember { mutableStateOf<MealType?>(null) }
    ZenCard(accent = Ki, entranceIndex = 1) {
        SectionTitle("Refeições por dia", accent = Ki)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (MealSchedule.MIN_MEALS..MealSchedule.MAX_MEALS).forEach { n ->
                ZenChip("$n", slots.size == n, { viewModel.setMealCount(n) }, Modifier.weight(1f), accent = Ki)
            }
        }
        Spacer(Modifier.height(10.dp))
        slots.sortedBy { it.position }.forEach { slot ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(slot.type.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                TimeChip(MealSchedule.timeOf(slot.type, slots).format(), { picker = slot.type })
            }
        }
    }
    picker?.let { type ->
        ZenTimePickerDialog(
            title = type.label,
            initial = MealSchedule.timeOf(type, slots),
            onDismiss = { picker = null },
            onConfirm = { viewModel.setMealTime(type, it.format()); picker = null }
        )
    }
}

// ---- Plano ----

@Composable
private fun PlanCard(
    shown: DietPlanFull?,
    isProposal: Boolean,
    hasActive: Boolean,
    slots: List<MealSlot>,
    viewModel: ZenViewModel
) {
    if (shown == null) {
        ZenCard(accent = Midori, entranceIndex = 2) {
            SectionTitle("Sem dieta ainda", accent = Midori)
            Spacer(Modifier.height(6.dp))
            Text("Preencha o perfil e tenha alimentos cadastrados para receber uma sugestão.")
            Spacer(Modifier.height(12.dp))
            ZenButton("Gerar sugestão", viewModel::generateDiet, accent = Midori, fill = true)
        }
        return
    }
    val stale = shown.meals.map { it.meal.type }.toSet() != slots.map { it.type }.toSet()
    ZenCard(accent = if (isProposal) Ki else Midori, entranceIndex = 2) {
        Text(if (isProposal) "SUGESTÃO · REVISE E ACEITE" else "DIETA ATIVA", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Totals(shown)
        if (stale) {
            Spacer(Modifier.height(8.dp))
            Text("Você mudou a quantidade de refeições. Gere uma nova sugestão para aplicar.", color = Aka, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(10.dp))
        if (isProposal) {
            Text("Toque num alimento para trocar, ajustar ou remover.", color = Zen.p.muted)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZenButton("Aceitar", { viewModel.acceptDiet(shown.plan.id) }, Modifier.weight(1f), accent = Midori, fill = true)
                ZenButton("Outra", viewModel::generateDiet, Modifier.weight(1f), accent = Ki, fill = true)
            }
            if (hasActive) {
                Spacer(Modifier.height(4.dp))
                ZenButton("Descartar e manter a ativa", viewModel::discardDiet, accent = Zen.p.card, small = true)
            }
        } else {
            Text("Toque num alimento para trocar, ajustar ou remover.", color = Zen.p.muted)
            Spacer(Modifier.height(10.dp))
            ZenButton("Nova sugestão", viewModel::generateDiet, accent = if (stale) Ki else Midori, small = !stale)
        }
    }
}

/** Uma linha com o total do plano (a meta fica no cartão de cima, sem repetir os números em destaque). */
@Composable
private fun Totals(plan: DietPlanFull) {
    val items = plan.meals.flatMap { it.items }
    val kcal = items.sumOf { it.food.kcalPer100 * it.item.grams / 100 }.roundToInt()
    val protein = items.sumOf { it.food.proteinPer100 * it.item.grams / 100 }.roundToInt()
    Text("Plano: $kcal kcal · $protein g de proteína", style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun MealCard(meal: MealFull, time: String, onEdit: (Long) -> Unit, onAdd: () -> Unit) {
    ZenCard(entranceIndex = -1) {
        val kcal = meal.items.sumOf { it.food.kcalPer100 * it.item.grams / 100 }.roundToInt()
        val protein = meal.items.sumOf { it.food.proteinPer100 * it.item.grams / 100 }.roundToInt()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(meal.meal.name.uppercase(), style = MaterialTheme.typography.titleMedium)
                Text("$kcal kcal · $protein g proteína", color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
            }
            Text(time, style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(6.dp))
        meal.items.forEach { entry ->
            Row(
                Modifier.fillMaxWidth().clickable { onEdit(entry.item.id) }.padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(entry.food.name, style = MaterialTheme.typography.bodyLarge)
                    Text("${entry.item.grams} g · ${entry.food.category.label}", color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
                }
                Text("⇄", style = MaterialTheme.typography.titleMedium, color = Zen.p.muted)
            }
        }
        Spacer(Modifier.height(6.dp))
        ZenButton("+ Adicionar", onAdd, accent = Zen.p.card, small = true)
    }
}

// ---- Janelas de troca ----

/** Ajustar quantidade, trocar por outro alimento (sugerido ou cadastrado na hora) ou remover. */
@Composable
private fun FoodItemDialog(meal: MealFull, entry: MealItemFull, foods: List<Food>, viewModel: ZenViewModel, onClose: () -> Unit) {
    val food = entry.food
    val grams = entry.item.grams
    var registering by remember { mutableStateOf(false) }
    val alternatives = remember(foods, food.id, meal.meal.type) {
        DietPlanner.alternatives(foods, food.category, meal.meal.type, excludeId = food.id)
    }

    ZenDialog(food.name, onClose, accent = food.category.accent()) {
        Stepper(
            "$grams g",
            onMinus = { viewModel.setMealItemGrams(entry.item.id, grams - GRAM_STEP) },
            onPlus = { viewModel.setMealItemGrams(entry.item.id, grams + GRAM_STEP) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${(food.kcalPer100 * grams / 100).roundToInt()} kcal · ${(food.proteinPer100 * grams / 100).roundToInt()} g proteína",
            color = Zen.p.muted
        )
        Spacer(Modifier.height(14.dp))
        Text("TROCAR POR (${food.category.label.uppercase()})", color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        if (alternatives.isEmpty()) Text("Nenhuma outra opção cadastrada.", color = Zen.p.muted)
        alternatives.forEach { alt ->
            val equivalent = DietPlanner.equivalentGrams(food, grams, alt)
            PickRow(alt.name, "${alt.kcalPer100.toInt()} kcal/100 g", "≈ $equivalent g", alt.category.accent()) {
                viewModel.replaceFood(entry.item.id, food, grams, alt)
                onClose()
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenButton("+ Cadastrar outro", { registering = true }, Modifier.weight(1f), accent = Ao, fill = true, small = true)
            ZenButton("Remover", { viewModel.removeMealItem(entry.item.id); onClose() }, Modifier.weight(1f), accent = Aka, fill = true, small = true)
        }
        Spacer(Modifier.height(8.dp))
        ZenButton("Fechar", onClose, accent = Zen.p.card, fill = true, small = true)
    }

    if (registering) {
        FoodFormDialog(
            initial = null,
            fixedCategory = food.category,
            onDismiss = { registering = false },
            onSave = {
                viewModel.createFoodAndReplace(it, entry.item.id, food, grams)
                registering = false
                onClose()
            }
        )
    }
}

/** Escolhe um alimento (por categoria) para somar à refeição. */
@Composable
private fun AddFoodDialog(meal: MealFull, foods: List<Food>, viewModel: ZenViewModel, onClose: () -> Unit) {
    var category by remember { mutableStateOf(FoodCategory.PROTEIN) }
    var registering by remember { mutableStateOf(false) }
    val options = remember(foods, category, meal.meal.type) { DietPlanner.alternatives(foods, category, meal.meal.type) }

    ZenDialog("Adicionar · ${meal.meal.name}", onClose, accent = category.accent()) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FoodCategory.entries.forEach {
                ZenChip(it.shortLabel(), category == it, { category = it }, Modifier.weight(1f), accent = it.accent(), compact = true)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (options.isEmpty()) Text("Nenhum alimento nesta categoria.", color = Zen.p.muted)
        options.forEach { food ->
            PickRow(food.name, "${food.kcalPer100.toInt()} kcal/100 g", "${DietPlanner.defaultGrams(food)} g", food.category.accent()) {
                viewModel.addFoodToMeal(meal.meal.id, food)
                onClose()
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenButton("+ Cadastrar novo", { registering = true }, Modifier.weight(1f), accent = Ao, fill = true, small = true)
            ZenButton("Fechar", onClose, Modifier.weight(1f), accent = Zen.p.card, fill = true, small = true)
        }
    }

    if (registering) {
        FoodFormDialog(
            initial = null,
            fixedCategory = category,
            onDismiss = { registering = false },
            onSave = {
                viewModel.createFoodAndAdd(it, meal.meal.id)
                registering = false
                onClose()
            }
        )
    }
}
