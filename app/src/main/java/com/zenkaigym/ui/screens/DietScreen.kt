package com.zenkaigym.ui.screens

import com.zenkaigym.domain.tr
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
import com.zenkaigym.domain.CheatDay
import com.zenkaigym.domain.CheatMeal
import com.zenkaigym.domain.CheatRules
import com.zenkaigym.domain.DietPlanner
import com.zenkaigym.domain.Portions
import com.zenkaigym.util.WEEK_DAYS_SHORT
import com.zenkaigym.util.todayDayOfWeek
import androidx.compose.runtime.mutableStateMapOf
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
import com.zenkaigym.ui.theme.Sakura
import com.zenkaigym.ui.theme.Zen
import kotlin.math.roundToInt

private val WEEK_DAYS_LONG: List<String> get() = listOf(tr("Segunda"), tr("Terça"), tr("Quarta"), tr("Quinta"), tr("Sexta"), tr("Sábado"), tr("Domingo"))

/** Refeições de um dia; planos antigos (dia 0) valem para todos os dias. */
private fun DietPlanFull.mealsOf(day: Int) = meals.filter { it.meal.dayOfWeek == day || it.meal.dayOfWeek == 0 }

private fun MealFull.kcal(): Int =
    meal.cheatKcal ?: items.sumOf { it.food.kcalPer100 * it.item.grams / 100 }.roundToInt()

private fun MealFull.protein(): Int =
    if (meal.cheatKcal != null) 0 else items.sumOf { it.food.proteinPer100 * it.item.grams / 100 }.roundToInt()

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
    var day by remember { mutableStateOf(todayDayOfWeek()) }
    var setup by remember { mutableStateOf(false) }
    var shopping by remember { mutableStateOf(false) }

    ZenPage {
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ZenHeader(tr("Dieta"), "食", accent = Midori, subtitle = tr("Meta, horários e refeições"))
                    ZenButton(tr("Meus alimentos  →"), onOpenFoods, accent = Ao, fill = true, small = true)
                }
            }
            profile?.let { p -> item(key = "target") { TargetCard(p, suggested, viewModel) } }
            item(key = "schedule") { ScheduleCard(slots, viewModel) }
            item(key = "plan") {
                PlanCard(shown, isProposal, hasActive = active != null, slots = slots, viewModel = viewModel, onSetup = { setup = true }, onShopping = { shopping = true })
            }
            if (shown != null) {
                item(key = "days") { DayPicker(shown, day) { day = it } }
                item(key = "day-summary") { DaySummary(shown, day, slots) }
                items(shown.mealsOf(day).sortedBy { it.meal.position }, key = { it.meal.id }) { meal ->
                    // Ao trocar de dia ou mudar o número de refeições, os cartões deslizam para o lugar.
                    Column(Modifier.animateItem()) {
                        MealCard(
                            meal, MealSchedule.timeOf(meal.meal.type, slots).format(),
                            onEdit = { editingId = it }, onAdd = { addingTo = meal.meal.id }
                        )
                    }
                }
            }
        }
        ScrollHints(listState.canScrollBackward, listState.canScrollForward, Modifier.fillMaxSize())
    }

    if (shopping) {
        // Compra-se para o cardápio em uso; a proposta só entra se ainda não há um ativo.
        val plan = active ?: shown
        if (plan == null) shopping = false
        else ShoppingListDialog(plan) { shopping = false }
    }

    if (setup) {
        val goal = (profile?.targetKcal ?: suggested?.kcal) ?: shown?.plan?.kcal
        WeekSetupDialog(
            slots = slots, goalKcal = goal, initial = shown?.cheatDay(),
            onDismiss = { setup = false },
            onCreate = { manual, cheat -> viewModel.createWeek(manual, cheat); setup = false }
        )
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
        SectionTitle(if (accepted != null) tr("Sua meta diária") else tr("Meta sugerida"), accent = Midori)
        if (accepted == null) {
            Spacer(Modifier.height(4.dp))
            Text(tr("Calculada com seu corpo, objetivo e dias de treino. Aceite ou ajuste."), color = Zen.p.muted)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPill("kcal", "${shown.kcal}", Modifier.weight(1f), Aka)
            StatPill(tr("proteína"), "${shown.protein}g", Modifier.weight(1f), Ki)
            StatPill(tr("carbo"), "${shown.carbs}g", Modifier.weight(1f), Ao)
            StatPill(tr("gordura"), "${shown.fat}g", Modifier.weight(1f), Midori)
        }
        Spacer(Modifier.height(12.dp))
        if (editing) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ZenField(tr("Calorias"), kcalText, { kcalText = it }, Modifier.weight(1f), KeyboardType.Number)
                ZenField(tr("Proteína (g)"), proteinText, { proteinText = it }, Modifier.weight(1f), KeyboardType.Number)
            }
            Spacer(Modifier.height(10.dp))
            ZenButton(tr("Salvar minha meta"), {
                val k = kcalText.trim().toIntOrNull()
                val p = proteinText.trim().toIntOrNull()
                if (k != null && p != null && k in 800..6000 && p in 20..500) {
                    viewModel.acceptCustomTarget(k, p)
                    editing = false
                }
            }, accent = Midori, fill = true)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (accepted == null && suggested != null) ZenButton(tr("Aceitar"), { viewModel.acceptTarget(suggested) }, accent = Midori)
                ZenButton(tr("Ajustar"), { editing = true }, accent = Sakura)
                if (accepted != null && suggested != null && suggested != accepted && suggested.kcal != accepted.kcal) {
                    ZenButton(tr("Usar sugerida"), { viewModel.acceptTarget(suggested) }, accent = Zen.p.card)
                }
            }
        }
    }
}

// ---- Agenda de refeições ----

@Composable
private fun ScheduleCard(slots: List<MealSlot>, viewModel: ZenViewModel) {
    var picker by remember { mutableStateOf<MealType?>(null) }
    ZenCard(accent = Midori, entranceIndex = 1) {
        SectionTitle(tr("Refeições por dia"), accent = Midori)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (MealSchedule.MIN_MEALS..MealSchedule.MAX_MEALS).forEach { n ->
                ZenChip("$n", slots.size == n, { viewModel.setMealCount(n) }, Modifier.weight(1f), accent = Midori)
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

private fun DietPlanFull.cheatDay(): CheatDay? {
    val day = plan.cheatDay ?: return null
    val cheat = meals.filter { it.meal.dayOfWeek == day && it.meal.cheatKcal != null }
    if (cheat.isEmpty()) return null
    return CheatDay(day, cheat.map { CheatMeal(it.meal.type, it.meal.cheatKcal ?: 0) })
}

@Composable
private fun PlanCard(
    shown: DietPlanFull?,
    isProposal: Boolean,
    hasActive: Boolean,
    slots: List<MealSlot>,
    viewModel: ZenViewModel,
    onSetup: () -> Unit,
    onShopping: () -> Unit
) {
    if (shown == null) {
        ZenCard(accent = Midori, entranceIndex = 2) {
            SectionTitle(tr("Sem cardápio ainda"), accent = Midori)
            Spacer(Modifier.height(6.dp))
            Text(tr("Preencha o perfil e tenha alimentos cadastrados para montar o cardápio da semana."))
            Spacer(Modifier.height(12.dp))
            ZenButton(tr("Montar cardápio da semana"), onSetup, accent = Midori, fill = true)
        }
        return
    }
    val stale = shown.meals.map { it.meal.type }.toSet() != slots.map { it.type }.toSet()
    ZenCard(accent = Midori, entranceIndex = 2) {
        Text(if (isProposal) tr("CARDÁPIO DA SEMANA · REVISE E ACEITE") else tr("CARDÁPIO DA SEMANA ATIVO"), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        WeekTotals(shown)
        if (stale) {
            Spacer(Modifier.height(8.dp))
            Text(tr("Você mudou a quantidade de refeições. Monte um novo cardápio para aplicar."), color = Aka, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(10.dp))
        Text(tr("Escolha o dia abaixo. Toque num alimento para trocar, ajustar ou remover."), color = Zen.p.muted)
        Spacer(Modifier.height(10.dp))
        if (isProposal) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZenButton(tr("Aceitar"), { viewModel.acceptDiet(shown.plan.id) }, Modifier.weight(1f), accent = Midori, fill = true)
                ZenButton(tr("Outra"), viewModel::generateDiet, Modifier.weight(1f), accent = Sakura, fill = true)
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenButton(tr("Novo cardápio…"), onSetup, Modifier.weight(1f), accent = Sakura, fill = true, small = true)
            if (isProposal && hasActive) {
                ZenButton(tr("Manter a ativa"), viewModel::discardDiet, Modifier.weight(1f), accent = Zen.p.card, fill = true, small = true)
            }
        }
        Spacer(Modifier.height(8.dp))
        ZenButton(tr("🛒 Lista de compras"), onShopping, accent = Ao, fill = true, small = true)
    }
}

/** Média diária da semana contra a meta. */
@Composable
private fun WeekTotals(plan: DietPlanFull) {
    val perDay = (1..7).map { d -> plan.mealsOf(d).sumOf { it.kcal() } }
    val avg = perDay.average().roundToInt()
    Text(tr("Semana: média de {0} kcal/dia · meta {1}", avg, plan.plan.kcal), style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun DayPicker(plan: DietPlanFull, day: Int, onSelect: (Int) -> Unit) {
    val cheatDay = plan.plan.cheatDay
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Zen.week.days.forEach { dow ->
                val label = WEEK_DAYS_SHORT[dow - 1]
                ZenChip(if (dow == cheatDay) "$label★" else label, day == dow, { onSelect(dow) }, Modifier.weight(1f), accent = if (dow == cheatDay) Aka else Midori, compact = true)
            }
        }
        if (cheatDay != null) {
            Text(tr("★ Dia do lixo: {0}", WEEK_DAYS_LONG[cheatDay - 1]), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Total do dia escolhido e quanto ele deveria somar (a compensação do dia do lixo muda a meta dos outros dias). */
@Composable
private fun DaySummary(plan: DietPlanFull, day: Int, slots: List<MealSlot>) {
    val meals = plan.mealsOf(day)
    val kcal = meals.sumOf { it.kcal() }
    val protein = meals.sumOf { it.protein() }
    val cheat = plan.cheatDay()
    val types = slots.map { it.type }
    val isCheatDay = cheat != null && day == cheat.dayOfWeek
    val dayGoal = if (cheat != null) CheatRules.dayTarget(plan.plan.kcal, types, cheat.meals, isCheatDay) else plan.plan.kcal
    ZenCard(accent = if (isCheatDay) Aka else Midori, entranceIndex = -1) {
        Text(WEEK_DAYS_LONG[day - 1].uppercase() + if (isCheatDay) tr(" · DIA DO LIXO") else "", style = MaterialTheme.typography.titleMedium)
        Text(tr("{0} kcal · {1} g proteína · meta do dia {2} kcal", kcal, protein, dayGoal), color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
        if (cheat != null && !isCheatDay) {
            val comp = CheatRules.compensationPerDay(plan.plan.kcal, types, cheat.meals)
            if (comp > 0) Text(tr("−{0} kcal para compensar o dia do lixo", comp), color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ---- Montagem do cardápio semanal ----

/** Escolhe como montar a semana (sugestão ou à mão) e, se quiser, o dia do lixo com suas refeições livres. */
@Composable
private fun WeekSetupDialog(
    slots: List<MealSlot>,
    goalKcal: Int?,
    initial: CheatDay?,
    onDismiss: () -> Unit,
    onCreate: (manual: Boolean, cheat: CheatDay?) -> Unit
) {
    val types = remember(slots) { slots.sortedBy { it.position }.map { it.type } }
    val goal = goalKcal ?: 2000
    val limit = CheatRules.limit(goal)
    var manual by remember { mutableStateOf(false) }
    var useCheat by remember { mutableStateOf(initial != null) }
    var cheatDay by remember { mutableStateOf(initial?.dayOfWeek ?: 6) }
    val kcalByType = remember {
        mutableStateMapOf<MealType, Int>().apply { initial?.meals?.filter { it.type in types }?.forEach { put(it.type, it.kcal) } }
    }
    val chosen = types.filter { it in kcalByType }.map { CheatMeal(it, kcalByType.getValue(it)) }
    val total = chosen.sumOf { it.kcal }
    val comp = CheatRules.compensationPerDay(goal, types, chosen)

    ZenDialog(tr("Cardápio da semana"), onDismiss, accent = Midori) {
        Text(tr("COMO MONTAR"), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ZenChip(tr("Sugestão"), !manual, { manual = false }, Modifier.weight(1f), accent = Midori, compact = true)
            ZenChip(tr("À mão"), manual, { manual = true }, Modifier.weight(1f), accent = Midori, compact = true)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (manual) tr("Cria a semana em branco: você adiciona os alimentos de cada refeição.")
            else tr("O app monta os sete dias variando os pratos com seus alimentos. Dá para ajustar depois."),
            color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(14.dp))
        Text(tr("DIA DO LIXO"), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ZenChip(tr("Não"), !useCheat, { useCheat = false }, Modifier.weight(1f), accent = Midori, compact = true)
            ZenChip(tr("Sim"), useCheat, { useCheat = true }, Modifier.weight(1f), accent = Aka, compact = true)
        }
        if (useCheat) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Zen.week.days.forEach { dow ->
                    ZenChip(WEEK_DAYS_SHORT[dow - 1], cheatDay == dow, { cheatDay = dow }, Modifier.weight(1f), accent = Aka, compact = true)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(tr("REFEIÇÕES FORA DA DIETA"), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            types.forEach { type ->
                val on = type in kcalByType
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    ZenChip(type.label, on, {
                        if (on) kcalByType.remove(type)
                        else {
                            val room = (limit - total).coerceAtLeast(0)
                            val suggestion = (goal * DietPlanner.shareOf(type, types) * 1.5 / 50).roundToInt() * 50
                            kcalByType[type] = suggestion.coerceAtMost(room / 10 * 10)
                        }
                    }, Modifier.weight(1f), accent = Aka, compact = true)
                }
                if (on) {
                    Stepper(
                        "${kcalByType.getValue(type)} kcal",
                        onMinus = { kcalByType[type] = (kcalByType.getValue(type) - 50).coerceAtLeast(0) },
                        onPlus = { if (total + 50 <= limit) kcalByType[type] = kcalByType.getValue(type) + 50 },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                tr("Livres: {0} de no máximo {1} kcal (sua meta diária).", total, limit),
                color = if (total >= limit) Aka else Zen.p.muted, style = MaterialTheme.typography.bodyMedium
            )
            if (chosen.isNotEmpty()) {
                Text(
                    if (comp > 0) tr("Os outros 6 dias perdem cerca de {0} kcal cada para compensar.", comp)
                    else tr("Sem compensação: as refeições livres não passam do que a dieta teria."),
                    color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenButton(if (manual) tr("Criar em branco") else tr("Montar cardápio"), {
                val cheat = if (useCheat && chosen.isNotEmpty()) CheatDay(cheatDay, chosen) else null
                onCreate(manual, cheat)
            }, Modifier.weight(1f), accent = Midori, fill = true)
            ZenButton(tr("Cancelar"), onDismiss, accent = Zen.p.card)
        }
    }
}

@Composable
private fun MealCard(meal: MealFull, time: String, onEdit: (Long) -> Unit, onAdd: () -> Unit) {
    val cheatKcal = meal.meal.cheatKcal
    ZenCard(accent = if (cheatKcal != null) Aka else null, entranceIndex = -1) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr(meal.meal.name).uppercase(), style = MaterialTheme.typography.titleMedium)
                if (cheatKcal != null) {
                    Text(tr("Refeição livre · cerca de {0} kcal", cheatKcal), color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(tr("{0} kcal · {1} g proteína", meal.kcal(), meal.protein()), color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(time, style = MaterialTheme.typography.headlineSmall)
        }
        if (cheatKcal != null) {
            Spacer(Modifier.height(6.dp))
            Text(tr("Fora da dieta: coma o que quiser, sem passar desse valor."), color = Zen.p.muted)
            return@ZenCard
        }
        Spacer(Modifier.height(6.dp))
        if (meal.items.isEmpty()) {
            Text(tr("Nenhum alimento ainda. Toque em + Adicionar."), color = Zen.p.muted)
        }
        meal.items.forEach { entry ->
            Row(
                Modifier.fillMaxWidth().clickable { onEdit(entry.item.id) }.padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(tr(entry.food.name), style = MaterialTheme.typography.bodyLarge)
                    Text("${Portions.text(entry.food, entry.item.grams)} · ${entry.food.category.label}", color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
                }
                Text("⇄", style = MaterialTheme.typography.titleMedium, color = Zen.p.muted)
            }
        }
        Spacer(Modifier.height(6.dp))
        ZenButton(tr("+ Adicionar"), onAdd, accent = Ao, small = true)
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

    ZenDialog(tr(food.name), onClose, accent = food.category.accent()) {
        val step = Portions.stepGrams(food)
        Stepper(
            Portions.text(food, grams),
            onMinus = { viewModel.setMealItemGrams(entry.item.id, grams - step) },
            onPlus = { viewModel.setMealItemGrams(entry.item.id, grams + step) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            tr("{0} kcal · {1} g proteína", (food.kcalPer100 * grams / 100).roundToInt(), (food.proteinPer100 * grams / 100).roundToInt()),
            color = Zen.p.muted
        )
        Spacer(Modifier.height(14.dp))
        Text(tr("TROCAR POR ({0})", food.category.label.uppercase()), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        if (alternatives.isEmpty()) Text(tr("Nenhuma outra opção cadastrada."), color = Zen.p.muted)
        alternatives.forEach { alt ->
            val equivalent = DietPlanner.equivalentGrams(food, grams, alt)
            PickRow(tr(alt.name), tr("{0} kcal/100 g", alt.kcalPer100.toInt()), "≈ ${Portions.text(alt, equivalent)}", alt.category.accent()) {
                viewModel.replaceFood(entry.item.id, food, grams, alt)
                onClose()
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenButton(tr("+ Cadastrar outro"), { registering = true }, Modifier.weight(1f), accent = Ao, fill = true, small = true)
            ZenButton(tr("Remover"), { viewModel.removeMealItem(entry.item.id); onClose() }, Modifier.weight(1f), accent = Aka, fill = true, small = true)
        }
        Spacer(Modifier.height(8.dp))
        ZenButton(tr("Fechar"), onClose, accent = Zen.p.card, fill = true, small = true)
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

    ZenDialog(tr("Adicionar · {0}", tr(meal.meal.name)), onClose, accent = category.accent()) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FoodCategory.entries.forEach {
                ZenChip(it.shortLabel(), category == it, { category = it }, Modifier.weight(1f), accent = it.accent(), compact = true)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (options.isEmpty()) Text(tr("Nenhum alimento nesta categoria."), color = Zen.p.muted)
        options.forEach { food ->
            PickRow(tr(food.name), tr("{0} kcal/100 g", food.kcalPer100.toInt()), Portions.text(food, DietPlanner.defaultGrams(food)), food.category.accent()) {
                viewModel.addFoodToMeal(meal.meal.id, food)
                onClose()
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenButton(tr("+ Cadastrar novo"), { registering = true }, Modifier.weight(1f), accent = Ao, fill = true, small = true)
            ZenButton(tr("Fechar"), onClose, Modifier.weight(1f), accent = Zen.p.card, fill = true, small = true)
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
