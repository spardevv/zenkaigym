package com.zenkaigym.ui.screens

import com.zenkaigym.ui.components.ZenIcon
import com.zenkaigym.ui.components.ZenIcons
import androidx.compose.foundation.layout.Box
import com.zenkaigym.ui.components.ZenFixedHeader
import com.zenkaigym.ui.components.HeaderBar
import com.zenkaigym.ui.components.MoreButton
import com.zenkaigym.ui.components.SheetOption
import com.zenkaigym.ui.components.ZenSheet
import com.zenkaigym.ui.components.ZenTab
import com.zenkaigym.ui.components.ZenTabs
import com.zenkaigym.ui.components.AccordionCard
import com.zenkaigym.util.todayDayOfWeek
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.zenkaigym.data.ExerciseCatalog
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.heightIn
import com.zenkaigym.domain.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.data.entity.MuscleGroup
import com.zenkaigym.data.entity.WorkoutDayFull
import com.zenkaigym.data.entity.WorkoutItemFull
import com.zenkaigym.data.entity.WorkoutPlanFull
import com.zenkaigym.domain.TimeWindow
import com.zenkaigym.domain.timeWindow
import com.zenkaigym.domain.windowsByDay
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.components.ConfirmDialog
import com.zenkaigym.ui.components.PickRow
import com.zenkaigym.ui.components.SectionTitle
import com.zenkaigym.ui.components.Stepper
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenCard
import com.zenkaigym.ui.components.ZenChip
import com.zenkaigym.ui.components.ZenDialog
import com.zenkaigym.ui.components.ZenField
import com.zenkaigym.ui.components.ZenHeader
import com.zenkaigym.ui.components.ZenPage
import com.zenkaigym.ui.components.currentWindowLayout
import com.zenkaigym.ui.components.zenEntrance
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Sumi
import com.zenkaigym.ui.theme.Sakura
import com.zenkaigym.ui.theme.Zen
import com.zenkaigym.util.WEEK_DAYS_SHORT

@Composable
fun WorkoutScreen(viewModel: ZenViewModel, onBack: () -> Unit) {
    val proposed by viewModel.proposedWorkout.collectAsStateWithLifecycle()
    val active by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val availability by viewModel.availability.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val shown = proposed ?: active
    val week = Zen.week
    val isProposal = proposed != null
    val windows = remember(availability) { availability.windowsByDay() }

    // Os horários editáveis começam do que está salvo; as alterações são salvas sozinhas.
    val editable = remember {
        mutableStateMapOf<Int, TimeWindow>().apply {
            availability.filter { it.available }.forEach { put(it.dayOfWeek, it.timeWindow() ?: TimeWindow.DEFAULT) }
        }
    }

    // Sem nenhuma ficha ainda, já deixa uma sugestão pronta: o usuário só revisa e aceita.
    LaunchedEffect(Unit) { viewModel.ensureWorkoutSuggestion() }

    var editingId by remember { mutableStateOf<Long?>(null) }
    var addingTo by remember { mutableStateOf<Long?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    // Dia aberto na ficha: null = ainda o padrão (hoje ou o primeiro); CLOSED = a pessoa fechou todos.
    var openDayId by rememberSaveable { mutableStateOf<Long?>(null) }

    ZenPage {
      Column(Modifier.fillMaxSize()) {
        ZenFixedHeader(
            tr("Treino"), "体", accent = Ki, subtitle = tr("Dias, horários e ficha"),
            back = { HeaderBar(onBack) { MoreButton({ menuOpen = true }) } }
        )
        ZenTabs(WorkoutTab.entries.map { ZenTab(tr(it.pt), Ki) }, tabIndex, { tabIndex = it })
        Spacer(Modifier.height(10.dp))
        Box(Modifier.weight(1f)) {
        val listState = rememberLazyListState()
        LaunchedEffect(tabIndex) { listState.scrollToItem(0) }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = currentWindowLayout.gutter, end = currentWindowLayout.gutter, top = 4.dp, bottom = currentWindowLayout.gutter),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (WorkoutTab.entries[tabIndex.coerceIn(0, WorkoutTab.entries.lastIndex)]) {
                WorkoutTab.PLAN -> {
                    item(key = "plan") { PlanCard(shown, isProposal, hasActive = active != null, viewModel = viewModel) }
                    if (shown != null) {
                        val days = shown.days.sortedBy { week.position(it.day.dayOfWeek) }
                        // Hoje abre primeiro; sem sessão hoje, abre o primeiro dia.
                        val defaultId = (days.firstOrNull { it.day.dayOfWeek == todayDayOfWeek() } ?: days.firstOrNull())?.day?.id
                        val openId = when {
                            openDayId == CLOSED -> null
                            days.any { it.day.id == openDayId } -> openDayId
                            else -> defaultId
                        }
                        items(days, key = { it.day.id }) { day ->
                            val count = day.items.size
                            val window = windows[day.day.dayOfWeek]
                            val meta = listOfNotNull(window?.format(), tr("{0} exercícios", count)).joinToString(" · ")
                            // Dias que entram, saem ou mudam de lugar (nova sugestão) deslizam em vez de pular.
                            AccordionCard(
                                "${WEEK_DAYS_SHORT[day.day.dayOfWeek - 1].uppercase()} · ${tr(day.day.name)}", meta,
                                expanded = day.day.id == openId,
                                onToggle = { openDayId = if (day.day.id == openId) CLOSED else day.day.id },
                                modifier = Modifier.animateItem()
                            ) {
                                DayExercises(day, onEdit = { editingId = it }, onAdd = { addingTo = day.day.id })
                            }
                        }
                    }
                }
                WorkoutTab.SCHEDULE -> item(key = "agenda") { AvailabilitySection(editable, viewModel::saveAvailability, entranceIndex = 0) }
                WorkoutTab.EXERCISES -> item(key = "mine") { MyExercisesList(exercises, viewModel) }
            }
        }
        ScrollHints(listState.canScrollBackward, listState.canScrollForward, Modifier.fillMaxSize())
        }
      }
    }

    if (menuOpen) {
        ZenSheet(onDismiss = { menuOpen = false }, accent = Ki, title = tr("Treino")) {
            SheetOption(tr("Nova sugestão"), tr("Monta outra ficha para você revisar e aceitar."), viewModel::generateWorkout)
        }
    }

    val items = shown?.days?.flatMap { d -> d.items.map { d to it } }.orEmpty()
    editingId?.let { id ->
        val pair = items.firstOrNull { it.second.item.id == id }
        if (pair == null) editingId = null
        else ExerciseItemDialog(pair.first, pair.second, exercises, viewModel) { editingId = null }
    }
    addingTo?.let { id ->
        val day = shown?.days?.firstOrNull { it.day.id == id }
        if (day == null) addingTo = null
        else AddExerciseDialog(day, exercises, viewModel) { addingTo = null }
    }
}

@Composable
private fun PlanCard(shown: WorkoutPlanFull?, isProposal: Boolean, hasActive: Boolean, viewModel: ZenViewModel) {
    if (shown == null) {
        ZenCard(accent = Ki, entranceIndex = 1) {
            SectionTitle(tr("Sem ficha ainda"), accent = Ki)
            Spacer(Modifier.height(6.dp))
            Text(tr("Preencha o perfil e marque ao menos um dia de treino para receber uma sugestão."))
            Spacer(Modifier.height(12.dp))
            ZenButton(tr("Gerar sugestão"), viewModel::generateWorkout, accent = Sakura, fill = true)
        }
        return
    }
    ZenCard(accent = Ki, entranceIndex = 1) {
        Text(if (isProposal) tr("SUGESTÃO · REVISE E ACEITE") else tr("FICHA ATIVA"), style = MaterialTheme.typography.labelLarge)
        Text(tr(shown.plan.splitName), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        if (!isProposal) {
            Text(tr("{0} dias · {1} exercícios", shown.days.size, shown.days.sumOf { it.items.size }), color = Zen.p.muted)
            return@ZenCard
        }
        Text(tr("Toque num exercício para trocar, ajustar séries ou remover."), color = Zen.p.muted)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenButton(tr("Aceitar"), { viewModel.acceptWorkout(shown.plan.id) }, Modifier.weight(1f), accent = Midori, fill = true)
            ZenButton(tr("Outra"), viewModel::generateWorkout, Modifier.weight(1f), accent = Sakura, fill = true)
        }
        if (hasActive) {
            Spacer(Modifier.height(4.dp))
            ZenButton(tr("Descartar e manter a ativa"), viewModel::discardWorkout, accent = Zen.p.card, small = true)
        }
    }
}

/** Linhas do dia aberto na sanfona: toque numa linha abre a troca; "+ Adicionar" soma um exercício. */
@Composable
private fun DayExercises(day: WorkoutDayFull, onEdit: (Long) -> Unit, onAdd: () -> Unit) {
    Spacer(Modifier.height(6.dp))
    day.items.sortedBy { it.item.position }.forEach { entry ->
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { onEdit(entry.item.id) }.padding(vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(tr(entry.exercise.name), style = MaterialTheme.typography.bodyLarge)
                Text(
                    tr("{0} séries × {1} · {2}", entry.item.sets, entry.item.reps, entry.exercise.group.label),
                    color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
                )
            }
            ZenIcon(ZenIcons.Swap, size = 20.dp, color = Zen.p.muted)
        }
    }
    Spacer(Modifier.height(6.dp))
    ZenButton(tr("+ Adicionar"), onAdd, accent = Ao, small = true)
}

/** Abas internas do Treino. */
private enum class WorkoutTab(val pt: String) { PLAN("Ficha"), SCHEDULE("Agenda"), EXERCISES("Exercícios") }

private const val CLOSED = -1L

// ---- Janelas de troca ----

/** Séries, troca por outro exercício do mesmo grupo (do catálogo ou cadastrado na hora) ou remoção. */
@Composable
private fun ExerciseItemDialog(day: WorkoutDayFull, entry: WorkoutItemFull, exercises: List<Exercise>, viewModel: ZenViewModel, onClose: () -> Unit) {
    val group = entry.exercise.group
    var registering by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }
    val inDay = day.items.map { it.exercise.id }.toSet()
    val alternatives = remember(exercises, group, inDay) {
        exercises.filter { it.group == group && it.id !in inDay }
    }

    ZenDialog(tr(entry.exercise.name), onClose, accent = Ki, footerReserve = 120, footer = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenButton(tr("+ Cadastrar outro"), { registering = true }, Modifier.weight(1f), accent = Ao, fill = true, small = true)
            ZenButton(tr("Remover"), { removing = true }, Modifier.weight(1f), accent = Aka, fill = true, small = true)
        }
        Spacer(Modifier.height(8.dp))
        ZenButton(tr("Fechar"), onClose, accent = Zen.p.card, fill = true, small = true)
    }) {
        Stepper(
            tr("{0} séries", entry.item.sets),
            onMinus = { viewModel.setWorkoutSets(entry.item.id, entry.item.sets - 1) },
            onPlus = { viewModel.setWorkoutSets(entry.item.id, entry.item.sets + 1) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(tr("{0} repetições · {1}", entry.item.reps, group.label), color = Zen.p.muted)
        Spacer(Modifier.height(14.dp))
        Text(tr("TROCAR POR ({0})", group.label.uppercase()), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        if (alternatives.isEmpty()) Text(tr("Nenhuma outra opção neste grupo."), color = Zen.p.muted)
        alternatives.forEach { alt ->
            PickRow(tr(alt.name), group.label, null, Ki) {
                viewModel.replaceExercise(entry.item.id, alt)
                onClose()
            }
        }
    }

    if (removing) {
        ConfirmDialog(
            tr("Remover exercício?"), tr("{0} sai deste dia da ficha.", tr(entry.exercise.name)), tr("Remover"),
            onConfirm = { viewModel.removeWorkoutItem(entry.item.id); removing = false; onClose() },
            onDismiss = { removing = false }
        )
    }

    if (registering) {
        NewExerciseDialog(group, onDismiss = { registering = false }) { name ->
            viewModel.createExerciseAndReplace(name, group, entry.item.id)
            registering = false
            onClose()
        }
    }
}

/** Escolhe um exercício (por grupo muscular) para somar ao dia. */
@Composable
private fun AddExerciseDialog(day: WorkoutDayFull, exercises: List<Exercise>, viewModel: ZenViewModel, onClose: () -> Unit) {
    var group by remember { mutableStateOf(MuscleGroup.CHEST) }
    var registering by remember { mutableStateOf(false) }
    val inDay = day.items.map { it.exercise.id }.toSet()
    val options = remember(exercises, group, inDay) { exercises.filter { it.group == group && it.id !in inDay } }
    val groups = MuscleGroup.entries

    ZenDialog(tr("Adicionar · {0}", tr(day.day.name)), onClose, accent = Ki, footer = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenButton(tr("+ Cadastrar novo"), { registering = true }, Modifier.weight(1f), accent = Ao, fill = true, small = true)
            ZenButton(tr("Fechar"), onClose, Modifier.weight(1f), accent = Zen.p.card, fill = true, small = true)
        }
    }) {
        // A categoria escolhida é trazida para a vista, para não ficar escondida no fim da faixa.
        val chipsState = rememberLazyListState()
        LaunchedEffect(group) { chipsState.animateScrollToItem(groups.indexOf(group).coerceAtLeast(0)) }
        LazyRow(state = chipsState, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(groups) { ZenChip(it.label, group == it, { group = it }, accent = Ki) }
        }
        Spacer(Modifier.height(6.dp))
        if (options.isEmpty()) Text(tr("Nenhum exercício disponível neste grupo."), color = Zen.p.muted)
        options.forEach { ex ->
            PickRow(tr(ex.name), ex.group.label, null, Ki) {
                viewModel.addWorkoutItem(day.day.id, ex)
                onClose()
            }
        }
    }

    if (registering) {
        NewExerciseDialog(group, onDismiss = { registering = false }) { name ->
            viewModel.createExerciseAndAdd(name, group, day.day.id)
            registering = false
            onClose()
        }
    }
}

@Composable
internal fun NewExerciseDialog(group: MuscleGroup, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    ZenDialog(tr("Novo exercício · {0}", group.label), onDismiss, accent = Ki, footerReserve = if (error != null) 126 else 90, footer = {
        error?.let {
            Text(it, color = Aka, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenButton(tr("Salvar"), {
                if (name.isBlank()) error = tr("Informe o nome do exercício.") else onSave(name.trim())
            }, Modifier.weight(1f), accent = Ki, fill = true)
            ZenButton(tr("Cancelar"), onDismiss, Modifier.weight(1f), accent = Zen.p.card, fill = true)
        }
    }) {
        ZenField(tr("Nome"), name, { name = it }, placeholder = tr("ex.: Supino na máquina"))
    }
}
