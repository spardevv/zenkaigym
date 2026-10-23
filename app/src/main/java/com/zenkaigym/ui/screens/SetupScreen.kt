package com.zenkaigym.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.zenkaigym.ui.components.LocalMotionEnabled
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenkaigym.data.entity.CardioPref
import com.zenkaigym.data.entity.DietPlanFull
import com.zenkaigym.data.entity.Goal
import com.zenkaigym.data.entity.Sex
import com.zenkaigym.data.entity.UserProfile
import com.zenkaigym.data.entity.WorkoutPlanFull
import com.zenkaigym.domain.AppStyle
import com.zenkaigym.domain.ProfileValidation
import com.zenkaigym.domain.ProfileValidator
import com.zenkaigym.domain.ThemeMode
import com.zenkaigym.domain.TimeWindow
import com.zenkaigym.domain.WeekStart
import com.zenkaigym.domain.availabilityOf
import com.zenkaigym.domain.tr
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.components.FeedbackHost
import com.zenkaigym.ui.components.SectionTitle
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenCard
import com.zenkaigym.ui.components.ZenChip
import com.zenkaigym.ui.components.ZenField
import com.zenkaigym.ui.components.ZenHeader
import com.zenkaigym.ui.components.ZenPage
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Sakura
import com.zenkaigym.ui.theme.Zen
import com.zenkaigym.util.WEEK_DAYS_SHORT

private const val STEPS = 4

/**
 * Configuração inicial em quatro passos: você (dados, aparência e início da semana), treino (dias, horários
 * e preferências), revisão do treino sugerido e dieta. Só ao aceitar a dieta o app abre o Dojo, já com tudo ativo.
 */
@Composable
fun SetupScreen(viewModel: ZenViewModel) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableIntStateOf(0) }
    val activeWorkout by viewModel.activeWorkout.collectAsStateWithLifecycle()
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf(Sex.MALE) }
    var goal by remember { mutableStateOf(Goal.MAINTAIN) }
    var prefs by remember { mutableStateOf(TrainingChoices(cardio = CardioPref.WARMUP)) }
    // Cardio sugerido pelo objetivo, até o usuário mexer nele.
    var cardioTouched by remember { mutableStateOf(false) }
    // Segunda, quarta e sexta já vêm marcadas; é só ajustar.
    val windows = remember { mutableStateMapOf<Int, TimeWindow>().apply { listOf(1, 3, 5).forEach { put(it, TimeWindow.DEFAULT) } } }
    var error by remember { mutableStateOf<String?>(null) }
    val motion = LocalMotionEnabled.current

    // Voltar do sistema acompanha o botão "Voltar" de cada etapa, em vez de fechar o app.
    BackHandler(enabled = step > 0) {
        error = null
        step--
    }

    ZenPage {
        Column(
            Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ZenHeader(
                when (step) {
                    0 -> tr("Vamos começar")
                    1 -> tr("Seu treino")
                    2 -> tr("Treino sugerido")
                    else -> tr("Sua dieta")
                },
                when (step) { 0 -> "始"; 1 -> "鍛"; 2 -> "道"; else -> "食" },
                subtitle = tr("Etapa {0} de {1}", step + 1, STEPS)
            )

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (!motion) EnterTransition.None togetherWith ExitTransition.None
                    else {
                        val dir = if (targetState > initialState) 1 else -1
                        (fadeIn(tween(200, delayMillis = 40)) + slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { it / 8 * dir }) togetherWith
                            (fadeOut(tween(120)) + slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { -it / 8 * dir })
                    }
                },
                label = "setup-step"
            ) { shown ->
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (shown) {
                        0 -> YouStep(
                            viewModel, height, { height = it }, weight, { weight = it }, age, { age = it }, sex, { sex = it },
                            goal, {
                                goal = it
                                if (!cardioTouched) prefs = prefs.copy(cardio = if (it == Goal.LOSE) CardioPref.FINISHER else CardioPref.WARMUP)
                            }, error
                        )
                        1 -> TrainingStep(windows, prefs, { cardioTouched = cardioTouched || it.cardio != prefs.cardio; prefs = it }, error)
                        2 -> WorkoutReviewStep(viewModel)
                        else -> DietStep(viewModel)
                    }
                }
            }

            when (step) {
                0 -> ZenButton(tr("Continuar"), {
                    when (val r = ProfileValidator.validate(height, weight, age)) {
                        is ProfileValidation.Invalid -> error = r.message
                        is ProfileValidation.Valid -> { error = null; step = 1 }
                    }
                }, fill = true, enabled = busy == null)
                1 -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZenButton(tr("Voltar"), { error = null; step = 0 }, accent = Zen.p.card)
                    ZenButton(tr("Montar meu treino"), {
                        val r = ProfileValidator.validate(height, weight, age)
                        if (r !is ProfileValidation.Valid) {
                            step = 0
                        } else if (windows.isEmpty()) {
                            error = tr("Marque ao menos um dia de treino.")
                        } else {
                            error = null
                            viewModel.saveSetupProfile(
                                UserProfile(
                                    heightCm = r.heightCm, weightKg = r.weightKg, age = r.age, sex = sex, goal = goal,
                                    level = prefs.level, cardio = prefs.cardio, focus = prefs.focus, equipment = prefs.equipment
                                ),
                                (1..7).map { availabilityOf(it, windows[it]) }
                            )
                            step = 2
                        }
                    }, Modifier.weight(1f), fill = true, enabled = busy == null)
                }
                2 -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZenButton(tr("Voltar"), { step = 1 }, accent = Zen.p.card)
                    ZenButton(tr("Continuar para a dieta"), { step = 3 }, Modifier.weight(1f), accent = Midori, fill = true, enabled = busy == null && activeWorkout != null)
                }
                else -> ZenButton(tr("Voltar"), { step = 2 }, accent = Zen.p.card, small = true)
            }
        }
        Box(Modifier.statusBarsPadding()) { FeedbackHost(busy, null, Modifier.align(Alignment.TopCenter)) }
    }
}

@Composable
private fun YouStep(
    viewModel: ZenViewModel,
    height: String, onHeight: (String) -> Unit,
    weight: String, onWeight: (String) -> Unit,
    age: String, onAge: (String) -> Unit,
    sex: Sex, onSex: (Sex) -> Unit,
    goal: Goal, onGoal: (Goal) -> Unit,
    error: String?
) {
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val style by viewModel.style.collectAsStateWithLifecycle()
    val weekStart by viewModel.weekStart.collectAsStateWithLifecycle()

    ZenCard(accent = Ao, entranceIndex = 0) {
        SectionTitle(tr("Aparência"), accent = Ao)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach {
                ZenChip(it.label, theme == it, { viewModel.setThemeMode(it) }, Modifier.weight(1f), accent = Ao)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppStyle.entries.forEach {
                ZenChip(it.label, style == it, { viewModel.setStyle(it) }, Modifier.weight(1f), accent = Sakura)
            }
        }
        Spacer(Modifier.height(12.dp))
        Label(tr("A SEMANA COMEÇA NA"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WeekStart.entries.forEach {
                ZenChip(it.label, weekStart == it, { viewModel.setWeekStart(it) }, Modifier.weight(1f), accent = Midori)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(tr("Você pode mudar isso depois em Ajustes."), color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
    }

    ZenCard(accent = Aka, entranceIndex = 1) {
        SectionTitle(tr("Seus dados"))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ZenField(tr("Altura (cm)"), height, onHeight, Modifier.weight(1f), KeyboardType.Number, imeAction = ImeAction.Next)
            ZenField(tr("Peso (kg)"), weight, onWeight, Modifier.weight(1f), KeyboardType.Decimal, imeAction = ImeAction.Next)
            ZenField(tr("Idade"), age, onAge, Modifier.weight(1f), KeyboardType.Number)
        }
        Spacer(Modifier.height(12.dp))
        Label(tr("Sexo"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Sex.entries.forEach { ZenChip(it.label, sex == it, { onSex(it) }, accent = Ao) }
        }
        Spacer(Modifier.height(12.dp))
        Label(tr("Objetivo"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Goal.entries.forEach { ZenChip(it.label, goal == it, { onGoal(it) }) }
        }
        Spacer(Modifier.height(6.dp))
        Text(tr("Tudo fica salvo só neste aparelho."), color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
        error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = Aka, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TrainingStep(
    windows: SnapshotStateMap<Int, TimeWindow>,
    prefs: TrainingChoices,
    onPrefs: (TrainingChoices) -> Unit,
    error: String?
) {
    // Os dias só são gravados ao montar o treino; aqui a seção apenas edita a lista local.
    AvailabilitySection(windows, onSave = {}, entranceIndex = 0)
    ZenCard(accent = Ki, entranceIndex = 1) {
        SectionTitle(tr("Como você treina"), accent = Ki)
        Spacer(Modifier.height(10.dp))
        TrainingPrefs(prefs, onPrefs, accent = Ki)
        Text(
            tr("Com poucos dias ou pouco tempo, o treino prioriza os músculos grandes e usa exercícios que trabalham vários de uma vez."),
            color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
        )
        error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = Aka, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun WorkoutReviewStep(viewModel: ZenViewModel) {
    val proposed by viewModel.proposedWorkout.collectAsStateWithLifecycle()
    val active by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val availability by viewModel.availability.collectAsStateWithLifecycle()
    val weekStart by viewModel.weekStart.collectAsStateWithLifecycle()
    val plan: WorkoutPlanFull? = proposed ?: active

    if (plan == null) {
        ZenCard(accent = Ki, entranceIndex = 0) {
            Text(tr("Montando seu treino…"))
            Spacer(Modifier.height(10.dp))
            ZenButton(tr("Gerar sugestão"), viewModel::generateWorkout, accent = Sakura, fill = true)
        }
        return
    }
    ZenCard(accent = Ki, entranceIndex = 0) {
        Text(tr(plan.plan.splitName), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            tr("Distribuído pelos seus dias e pelo tempo de cada um. Depois você pode trocar exercícios e séries na aba Treino."),
            color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(10.dp))
        plan.days.sortedBy { weekStart.position(it.day.dayOfWeek) }.forEach { day ->
            val minutes = availability.firstOrNull { it.dayOfWeek == day.day.dayOfWeek }
                ?.let { TimeWindow.fromStored(it.startTime, it.endTime) }?.durationLabel()
            Text(
                WEEK_DAYS_SHORT[day.day.dayOfWeek - 1].uppercase() + " · " + tr(day.day.name) + (minutes?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 10.dp)
            )
            day.items.sortedBy { it.item.position }.forEach {
                Row(Modifier.padding(vertical = 1.dp)) {
                    Text(tr(it.exercise.name), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (it.exercise.kind == com.zenkaigym.data.entity.ExerciseKind.CARDIO) it.item.reps else tr("{0}×{1}", it.item.sets, it.item.reps),
                        color = Zen.p.muted, style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (proposed != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZenButton(tr("Aceitar treino"), { viewModel.acceptWorkout(proposed!!.plan.id) }, Modifier.weight(1f), accent = Midori, fill = true)
                ZenButton(tr("Outra"), viewModel::generateWorkout, Modifier.weight(1f), accent = Sakura, fill = true)
            }
        } else {
            Text(tr("Treino aceito ✓"), color = Midori, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun DietStep(viewModel: ZenViewModel) {
    val proposed by viewModel.proposedDiet.collectAsStateWithLifecycle()
    val slots by viewModel.mealSlots.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val suggested by viewModel.suggestedTarget.collectAsStateWithLifecycle()
    var custom by remember { mutableStateOf(false) }
    val plan: DietPlanFull? = proposed

    ScheduleCard(slots, viewModel)

    ZenCard(accent = Midori, entranceIndex = 1) {
        if (plan == null) {
            Text(tr("Montando seu cardápio…"))
            Spacer(Modifier.height(10.dp))
            ZenButton(tr("Montar cardápio da semana"), { custom = true }, accent = Midori, fill = true)
        } else {
            Text(tr("CARDÁPIO DA SEMANA · REVISE E ACEITE"), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            WeekTotals(plan)
            Spacer(Modifier.height(4.dp))
            Text(
                tr("Proteína {0} g · carboidratos {1} g · gorduras {2} g por dia.", plan.plan.protein, plan.plan.carbs, plan.plan.fat),
                color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
            )
            Text(
                tr("Os pratos variam ao longo da semana. Depois você ajusta alimentos e porções na aba Dieta."),
                color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(12.dp))
            ZenButton(tr("Aceitar dieta e ir para o Dojo"), { viewModel.acceptDietAndFinish(plan.plan.id) }, accent = Midori, fill = true)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZenButton(tr("Outra"), viewModel::generateDiet, Modifier.weight(1f), accent = Sakura, fill = true, small = true)
                ZenButton(tr("Dia do lixo…"), { custom = true }, Modifier.weight(1f), accent = Zen.p.card, fill = true, small = true)
            }
        }
    }

    if (custom) {
        WeekSetupDialog(
            slots = slots, goalKcal = profile?.targetKcal ?: suggested?.kcal ?: plan?.plan?.kcal, initial = null,
            onDismiss = { custom = false },
            onCreate = { manual, cheat -> viewModel.createWeek(manual, cheat); custom = false }
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 4.dp))
}
