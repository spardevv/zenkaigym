package com.zenkaigym.ui.screens

import com.zenkaigym.domain.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenkaigym.data.entity.DietPlanFull
import com.zenkaigym.data.entity.MealExtra
import com.zenkaigym.data.entity.MealFull
import com.zenkaigym.data.entity.WorkoutDayFull
import com.zenkaigym.domain.DayAdjust
import com.zenkaigym.domain.DayAdjustment
import com.zenkaigym.domain.WorkoutSwaps
import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zenkaigym.data.entity.MealSlot
import com.zenkaigym.data.entity.MealType
import com.zenkaigym.data.entity.WorkoutPlanFull
import com.zenkaigym.domain.MealSchedule
import com.zenkaigym.domain.Portions
import com.zenkaigym.util.dayOfWeekFromEpochDay
import com.zenkaigym.domain.TimeWindow
import com.zenkaigym.domain.windowsByDay
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.components.KanjiBadge
import com.zenkaigym.ui.components.SectionTitle
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenCard
import com.zenkaigym.ui.components.ZenChip
import com.zenkaigym.ui.components.ZenHeader
import com.zenkaigym.ui.components.ZenPage
import com.zenkaigym.ui.nav.Routes
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Zen
import com.zenkaigym.util.WEEK_DAYS_SHORT
import com.zenkaigym.util.todayDayOfWeek

/** Dojo: só o essencial do dia — o treino de hoje e a próxima refeição, com um toque para marcar. */
@Composable
fun HomeScreen(viewModel: ZenViewModel, onNavigate: (String) -> Unit) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val workout by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val diet by viewModel.activeDiet.collectAsStateWithLifecycle()
    val availability by viewModel.availability.collectAsStateWithLifecycle()
    val slots by viewModel.mealSlots.collectAsStateWithLifecycle()
    val done by viewModel.mealsDone.collectAsStateWithLifecycle()
    val clock by viewModel.clock.collectAsStateWithLifecycle()
    val workoutDone by viewModel.workoutDone.collectAsStateWithLifecycle()
    val swaps by viewModel.swapsToday.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val extras by viewModel.extrasToday.collectAsStateWithLifecycle()
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    val dow = todayDayOfWeek()
    var addingExtra by remember { mutableStateOf<MealFull?>(null) }

    ZenPage {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ZenHeader(tr("Dojo"), "道", subtitle = tr("Hoje · {0}", WEEK_DAYS_SHORT[dow - 1]))
            if (profile == null) {
                ZenCard(accent = Aka) {
                    Text(tr("COMECE SUA JORNADA"), style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(tr("Informe seus dados. O app sugere treino e dieta — você decide o que aceitar."))
                    Spacer(Modifier.height(12.dp))
                    ZenButton(tr("Criar perfil"), { onNavigate(Routes.PROFILE) }, fill = true)
                }
            } else {
                val window = remember(availability) { availability.windowsByDay()[dow] }
                TodayWorkout(workout, window, dow, onNavigate, workoutDone, onTrain = viewModel::startTraining) { day ->
                    // O treino de hoje já aparece com as trocas feitas só para hoje.
                    WorkoutSwaps.apply(day, swaps, exercises)
                }
                NextMeal(
                    diet, slots, done, extras, clock.minutes, dayOfWeekFromEpochDay(clock.epochDay), onNavigate, viewModel::setMealDone,
                    onAddExtra = { addingExtra = it },
                    onRemoveExtra = viewModel::removeExtra
                )
            }
        }
    }

    addingExtra?.let { meal ->
        AddExtraDialog(
            mealName = tr(meal.meal.name),
            type = meal.meal.type,
            foods = foods,
            onFood = { food, grams -> viewModel.addFoodExtra(meal.meal.type, food, grams); addingExtra = null },
            onFree = { name, kcal, protein -> viewModel.addFreeExtra(meal.meal.type, name, kcal, protein); addingExtra = null },
            onDismiss = { addingExtra = null }
        )
    }
}

@Composable
private fun TodayWorkout(
    workout: WorkoutPlanFull?,
    window: TimeWindow?,
    dow: Int,
    onNavigate: (String) -> Unit,
    done: Boolean,
    onTrain: () -> Unit,
    withSwaps: (WorkoutDayFull) -> WorkoutDayFull
) {
    ZenCard(accent = Aka, entranceIndex = 0) {
        SectionTitle(tr("Treino de hoje"), accent = Aka)
        Spacer(Modifier.height(10.dp))
        val today = workout?.days?.firstOrNull { it.day.dayOfWeek == dow }?.let(withSwaps)
        when {
            workout == null -> {
                Text(tr("Você ainda não tem uma ficha ativa."))
                Spacer(Modifier.height(10.dp))
                ZenButton(tr("Ver treino"), { onNavigate(Routes.WORKOUT) }, accent = Aka, fill = true)
            }
            today == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                KanjiBadge("休", color = Aka)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(tr("DESCANSO"), style = MaterialTheme.typography.titleMedium)
                    Text(tr("Hoje o corpo cresce. Hidrate-se e durma bem."), color = Zen.p.muted)
                }
            }
            else -> {
                Text(tr(today.day.name).uppercase(), style = MaterialTheme.typography.titleMedium)
                if (window != null) {
                    Text("${window.format()} · ${window.durationLabel()}", color = Zen.p.muted, style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(8.dp))
                today.items.sortedBy { it.item.position }.forEach {
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text(tr(it.exercise.name), Modifier.weight(1f))
                        Text(tr("{0}×{1}", it.item.sets, it.item.reps), color = Zen.p.muted, style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.height(10.dp))
                ZenButton(if (done) tr("TREINO FEITO ✓") else tr("Treinar agora"), onTrain, accent = if (done) Midori else Aka, fill = true)
            }
        }
    }
}

@Composable
private fun NextMeal(
    diet: DietPlanFull?,
    slots: List<MealSlot>,
    done: Set<MealType>,
    extras: List<MealExtra>,
    nowMinutes: Int,
    today: Int,
    onNavigate: (String) -> Unit,
    onToggle: (MealType, Boolean) -> Unit,
    onAddExtra: (MealFull) -> Unit,
    onRemoveExtra: (Long) -> Unit
) {
    ZenCard(accent = Aka, entranceIndex = 1) {
        if (diet == null) {
            SectionTitle(tr("Próxima refeição"), accent = Aka)
            Spacer(Modifier.height(10.dp))
            Text(tr("Você ainda não tem uma dieta ativa."))
            Spacer(Modifier.height(10.dp))
            ZenButton(tr("Ver dieta"), { onNavigate(Routes.DIET) }, accent = Aka, fill = true)
            return@ZenCard
        }
        val meals = diet.meals.filter { it.meal.dayOfWeek == today || it.meal.dayOfWeek == 0 }.sortedBy { MealSchedule.timeOf(it.meal.type, slots).totalMinutes }
        val nextType = MealSchedule.next(meals.map { it.meal.type }, slots, done, nowMinutes)
        // Sempre abre (e volta, após marcar) na próxima refeição; deslizar mostra as outras do dia.
        val target = meals.indexOfFirst { it.meal.type == nextType }.takeIf { it >= 0 }
            ?: meals.indexOfFirst { it.meal.type !in done }.takeIf { it >= 0 }
            ?: 0
        val pager = rememberPagerState(initialPage = target) { meals.size }
        LaunchedEffect(nextType, meals.size) {
            if (meals.isNotEmpty()) pager.animateScrollToPage(target.coerceIn(0, meals.lastIndex))
        }

        if (meals.isEmpty()) {
            SectionTitle(tr("Próxima refeição"), accent = Aka)
            Spacer(Modifier.height(10.dp))
            Text(tr("A dieta ativa está sem refeições."), color = Zen.p.muted)
            return@ZenCard
        }
        // O título acompanha a refeição que está à vista ao deslizar.
        val nextIndex = meals.indexOfFirst { it.meal.type == nextType }
        val shownIndex = pager.currentPage.coerceIn(0, meals.lastIndex)
        val shownMeal = meals[shownIndex]
        SectionTitle(
            when {
                shownMeal.meal.type in done -> tr("Refeição realizada")
                shownMeal.meal.type == nextType -> tr("Próxima refeição")
                nextIndex >= 0 && shownIndex > nextIndex -> tr("Futuras refeições")
                else -> tr("Refeição pendente")
            },
            accent = Aka
        )
        // Pratos fora do cardápio reajustam as refeições que faltam (só hoje; o cardápio não muda).
        val adjustment = remember(meals, done, extras) { DayAdjust.compute(meals, done, extras) }
        if (adjustment != DayAdjustment.NONE) {
            Spacer(Modifier.height(6.dp))
            Text(
                tr("Próximas refeições ajustadas ao que você comeu: {0} kcal (só hoje).", signed(adjustment.deltaKcal)),
                color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(10.dp))
        HorizontalPager(pager, Modifier.fillMaxWidth(), pageSpacing = 16.dp, verticalAlignment = Alignment.Top) { index ->
            val meal = meals[index]
            val isDone = meal.meal.type in done
            val tag = when {
                meal.meal.type == nextType -> tr("PRÓXIMA")
                isDone -> tr("FEITA ✓")
                else -> null
            }
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        if (tag != null) Text(tag, color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
                        Text(tr(meal.meal.name).uppercase(), style = MaterialTheme.typography.titleMedium)
                    }
                    Text(MealSchedule.timeOf(meal.meal.type, slots).format(), style = MaterialTheme.typography.headlineSmall)
                }
                Spacer(Modifier.height(8.dp))
                val cheatKcal = meal.meal.cheatKcal
                if (cheatKcal != null) {
                    Text(tr("Refeição livre · cerca de {0} kcal", cheatKcal), color = Zen.p.muted)
                }
                val mealExtras = extras.filter { it.type == meal.meal.type }
                // Com um prato extra e sem ✓, o extra tomou o lugar da refeição: o planejado fica esmaecido.
                val replaced = mealExtras.isNotEmpty() && !isDone
                meal.items.forEach {
                    val adjusted = adjustment.grams[it.item.id]
                    Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(tr(it.food.name), Modifier.weight(1f), color = if (replaced) Zen.p.muted else Zen.p.ink)
                        Column(horizontalAlignment = Alignment.End) {
                            Text(Portions.text(it.food, adjusted ?: it.item.grams), color = Zen.p.muted, style = MaterialTheme.typography.labelLarge)
                            if (adjusted != null) {
                                Text(tr("ajustado · era {0}", Portions.text(it.food, it.item.grams)), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                if (mealExtras.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(tr("COMIDO FORA DO CARDÁPIO"), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
                    mealExtras.forEach { extra ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onRemoveExtra(extra.id) }.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("+ " + tr(extra.name) + (extra.grams?.let { " · $it g" } ?: ""), Modifier.weight(1f))
                            Text(tr("{0} kcal", extra.kcal), color = Zen.p.muted, style = MaterialTheme.typography.labelLarge)
                            Text("  ✕", color = Zen.p.muted, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (isDone) ZenButton(tr("Desfazer"), { onToggle(meal.meal.type, false) }, accent = Zen.p.card, fill = true, small = true)
                else ZenButton(tr("Feito  ✓"), { onToggle(meal.meal.type, true) }, accent = Midori, fill = true)
                Spacer(Modifier.height(6.dp))
                ZenButton(tr("+ Comi outra coisa"), { onAddExtra(meal) }, accent = Ao, fill = true, small = true)
            }
        }
        if (meals.size > 1) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(tr("◀  deslize para ver as outras refeições  ▶"), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
            }
        }

        // Linha do dia: verde = feita; toque para ir até aquela refeição.
        val scope = rememberCoroutineScope()
        Spacer(Modifier.height(14.dp))
        Text(tr("HOJE · {0}/{1}", done.count { t -> meals.any { it.meal.type == t } }, meals.size), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            meals.forEachIndexed { index, meal ->
                val isDone = meal.meal.type in done
                ZenChip(
                    (if (isDone && meals.size <= 4) "✓ " else "") + MealSchedule.timeOf(meal.meal.type, slots).format(),
                    isDone,
                    { scope.launch { pager.animateScrollToPage(index) } },
                    Modifier.weight(1f), accent = Aka, compact = true
                )
            }
        }
    }
}

/** Número com sinal: +120 / −180. */
private fun signed(value: Int): String = if (value > 0) "+$value" else if (value < 0) "−${-value}" else "0"
