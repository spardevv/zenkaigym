package com.zenkaigym.ui.screens

import com.zenkaigym.ui.components.color
import com.zenkaigym.ui.components.ZenIcon
import com.zenkaigym.ui.components.ZenIcons
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.ui.components.ExerciseImage
import com.zenkaigym.domain.WorkoutSwaps
import com.zenkaigym.domain.kanji
import com.zenkaigym.domain.SetCount
import com.zenkaigym.domain.TrainingFlow
import com.zenkaigym.domain.setCount
import com.zenkaigym.ui.components.MoreButton
import com.zenkaigym.ui.components.SheetOption
import com.zenkaigym.ui.components.ZenProgressBar
import com.zenkaigym.ui.components.ZenSheet
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.zenkaigym.ui.components.PickRow
import com.zenkaigym.ui.components.currentWindowLayout
import com.zenkaigym.ui.components.ZenDialog
import com.zenkaigym.ui.components.drawSparkle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material3.Icon
import com.zenkaigym.data.entity.ExerciseKind
import com.zenkaigym.data.entity.MuscleGroup
import com.zenkaigym.data.entity.SetLog
import com.zenkaigym.data.entity.WorkoutDayFull
import com.zenkaigym.data.entity.WorkoutItemFull
import com.zenkaigym.domain.Progression
import com.zenkaigym.domain.LoadAdvice
import com.zenkaigym.domain.Cardio
import com.zenkaigym.ui.components.ZenChip
import com.zenkaigym.ui.components.ZenField
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.zenkaigym.domain.tr
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.components.KanjiBadge
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenCard
import com.zenkaigym.ui.components.ScrollHints
import com.zenkaigym.ui.components.pressScale
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Daidai
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Sakura
import com.zenkaigym.ui.theme.Sumi
import com.zenkaigym.ui.theme.Zen
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private val CellHeight = 42.dp
private val SideCell = 40.dp

/** Texto de carga sem zeros inúteis: 22.0 -> "22", 22.5 -> "22.5". */
private fun kgText(kg: Double): String = Progression.kgText(kg)

/**
 * Rascunho de uma série: carga e repetições (no cardio, minutos e [km]), além do esforço ([rir]) e da [note].
 * Séries com índice negativo são aquecimentos.
 */
private data class SetDraftState(val weight: String, val reps: String, val rir: Int? = null, val note: String = "", val km: String = "")

/** Guarda os rascunhos digitados para sobreviver à rotação da tela: [id, série, carga, reps, rir, nota, km] em sequência. */
private val DraftsSaver = listSaver<SnapshotStateMap<Pair<Long, Int>, SetDraftState>, String>(
    save = { map ->
        map.flatMap { (k, v) -> listOf(k.first.toString(), k.second.toString(), v.weight, v.reps, v.rir?.toString().orEmpty(), v.note, v.km) }
    },
    restore = { list ->
        mutableStateMapOf<Pair<Long, Int>, SetDraftState>().also { map ->
            list.chunked(7).forEach { map[it[0].toLong() to it[1].toInt()] = SetDraftState(it[2], it[3], it[4].toIntOrNull(), it[5], it[6]) }
        }
    }
)

/** Série registrada como texto dos campos (carga zero = campo vazio). */
private fun SetLog.asDraft() = SetDraftState(
    if (weightKg > 0) kgText(weightKg) else "", reps.toString(), rir, note.orEmpty(), distanceKm?.let(::kgText).orEmpty()
)

/** Texto decimal digitado ("2,5" ou "2.5") como número; null se vazio ou inválido. */
private fun String.decimal(): Double? = trim().replace(',', '.').toDoubleOrNull()

/** Dica de carga exibida no painel: etiqueta, texto e cor. */
private data class Hint(val tag: String, val text: String, val color: Color)

private fun LoadAdvice.hint(): Hint = when (this) {
    is LoadAdvice.AddWeight -> Hint(tr("Suba!"), tr("Última vez: {0} kg · hoje: {1} kg", kgText(fromKg), kgText(toKg)), Ki)
    is LoadAdvice.AddReps -> Hint(tr("Mais reps!"), tr("Fique em {0} kg e busque {1} reps por série", kgText(kg), targetReps), Midori)
    is LoadAdvice.Deload -> Hint(tr("Deload"), tr("3 sessões sem evoluir: tente {0} kg nesta semana e volte a subir", kgText(toKg)), Ao)
}

/** Registro do treino de hoje: carga e repetições de cada série, já preenchidas com a última vez. */
@Composable
fun TrainSessionScreen(day: WorkoutDayFull, viewModel: ZenViewModel, onClose: () -> Unit) {
    val logged by viewModel.setsToday.collectAsStateWithLifecycle()
    val done by viewModel.workoutDone.collectAsStateWithLifecycle()
    val swaps by viewModel.swapsToday.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    // Trocas feitas só para hoje entram por cima da ficha (que continua igual).
    val items = remember(day, swaps, exercises) { WorkoutSwaps.apply(day, swaps, exercises).items.sortedBy { it.item.position } }
    val originals = remember(day) { day.items.associate { it.item.id to it.exercise } }
    // Rascunho por (exercício, série): começa com os mesmos kg e reps do treino anterior e é editável.
    val drafts = rememberSaveable(day, saver = DraftsSaver) { mutableStateMapOf<Pair<Long, Int>, SetDraftState>() }
    val advice = remember(day) { mutableStateMapOf<Long, LoadAdvice>() }
    var swapping by remember { mutableStateOf<WorkoutItemFull?>(null) }
    var editing by remember { mutableStateOf<Pair<WorkoutItemFull, Int>?>(null) }

    // Notificação do descanso: pede a permissão uma vez, ao abrir o modo treino pela primeira vez.
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val missing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (missing && viewModel.shouldAskRestPermission()) {
            viewModel.markRestPermissionAsked()
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Recalcula ao trocar um exercício: o novo ganha a carga da última vez em que foi feito.
    LaunchedEffect(items.map { it.exercise.id }) {
        items.forEach { entry ->
            // Falha ao ler o histórico não pode derrubar o treino: segue sem sugestão de carga.
            val history = runCatching { viewModel.history(entry.exercise.id) }.getOrNull() ?: return@forEach
            // Conselho de carga (subir, mais repetições ou deload); os campos continuam com o valor anterior.
            Progression.advise(history.sessions, entry.item.reps, entry.exercise.kind)?.let { advice[entry.exercise.id] = it }
            for (i in 1..entry.item.sets) {
                val key = entry.exercise.id to i
                if (key in drafts) continue
                val prev = Progression.prefill(history.last, i, entry.item.reps)
                drafts[key] = SetDraftState(
                    weight = prev?.weightKg?.takeIf { it > 0 }?.let(::kgText).orEmpty(),
                    reps = prev?.reps?.toString().orEmpty()
                )
            }
        }
    }

    // Séries feitas hoje mostram o que foi registrado, não o rascunho.
    val loggedByKey = logged.associateBy { it.exerciseId to it.setIndex }
    val loggedKeys = loggedByKey.keys
    val count = setCount(items.map { it.exercise.id to it.item.sets }, logged)
    val totalSets = count.total
    val doneSets = count.done
    val allDone = totalSets > 0 && doneSets == totalSets

    // Tela de conclusão por cima do treino; sobrevive ao giro de tela.
    var showConclusion by rememberSaveable { mutableStateOf(false) }
    val summary by viewModel.todaySummary.collectAsStateWithLifecycle()
    val progress by viewModel.progressStats.collectAsStateWithLifecycle()

    // O X e o voltar do sistema pedem a mesma saída: com séries registradas e treino aberto, pergunta antes.
    var askingExit by remember { mutableStateOf(false) }
    fun requestExit() {
        if (TrainingFlow.askOnExit(count, done)) askingExit = true else onClose()
    }
    BackHandler(onBack = ::requestExit)

    // Tela acesa enquanto o modo treino estiver aberto (se ligado em Ajustes).
    val keepOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val restPreset by viewModel.restPreset.collectAsStateWithLifecycle()
    val view = LocalView.current
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }

    val p = Zen.p
    val scope = rememberCoroutineScope()
    // Um exercício por página: desliza na horizontal entre eles.
    val pager = rememberPagerState { items.size }

    // Séries pendentes na ordem da ficha.
    val pending = items.flatMap { e -> (1..e.item.sets).map { e.exercise.id to it } }.filter { it !in loggedKeys }
    val next = pending.firstOrNull()

    fun pageOf(exerciseId: Long) = items.indexOfFirst { it.exercise.id == exerciseId }

    // Texto do botão principal: exercício e o que está preenchido para a próxima série.
    val nextSet = next?.let { (id, i) ->
        val entry = items.firstOrNull { it.exercise.id == id } ?: return@let null
        val draft = loggedByKey[id to i]?.asDraft() ?: drafts[id to i] ?: SetDraftState("", "")
        // Repetições vazias não são registradas (logDraft recusa): o detalhe não finge que a ficha vale.
        val reps = draft.reps.trim().ifEmpty { "—" }
        val kg = draft.weight.decimal()?.takeIf { it > 0 }
        val detail = when (entry.exercise.kind) {
            ExerciseKind.HOLD -> tr("{0} s", reps)
            ExerciseKind.CARDIO -> tr("{0} min", reps)
            else -> if (kg != null) "${kgText(kg)} kg × $reps" else "× $reps"
        }
        NextSet(i, tr(entry.exercise.name), detail)
    }

    /** Registra a série com o rascunho atual e já inicia o descanso. Devolve false se faltam repetições. */
    fun logDraft(exerciseId: Long, setIndex: Int, draft: SetDraftState): Boolean {
        val reps = draft.reps.trim().toIntOrNull()?.takeIf { it > 0 } ?: return false
        val kg = draft.weight.decimal() ?: 0.0
        val exercise = items.firstOrNull { it.exercise.id == exerciseId }?.exercise ?: return false
        val warmup = setIndex < 0
        viewModel.logSet(exercise, setIndex, reps, kg, draft.rir, draft.note, warmup, draft.km.decimal())
        // A última série pendente fecha o treino: nada de descanso depois dela.
        val isLast = !warmup && pending.size == 1 && pending.first() == (exerciseId to setIndex)
        // Cardio e aquecimento não têm descanso marcado; a prancha pede um intervalo curto.
        val kind = exercise.kind
        when {
            isLast || warmup || kind == ExerciseKind.CARDIO -> Unit
            kind == ExerciseKind.HOLD -> viewModel.startRest(45)
            else -> viewModel.startRest(restPreset.takeIf { it > 0 } ?: Progression.restSeconds(reps))
        }
        // Conclusão automática só se o treino ainda não estava feito: registrar de novo depois não reabre.
        if (TrainingFlow.shouldAutoComplete(isLast, done)) {
            viewModel.completeWorkout()
            showConclusion = true
        }
        return true
    }

    /** Registra a próxima série pendente e desliza até o exercício da série seguinte. */
    fun onNext() {
        val target = next ?: return
        if (logDraft(target.first, target.second, drafts[target] ?: SetDraftState("", ""))) {
            val after = pending.getOrNull(1) ?: return
            val page = pageOf(after.first)
            if (page >= 0 && page != pager.currentPage) scope.launch { pager.animateScrollToPage(page) }
        }
    }

    // Ao reabrir um treino já começado, abre direto no exercício da próxima série.
    var positioned by remember(day) { mutableStateOf(false) }
    LaunchedEffect(logged) {
        if (positioned || logged.isEmpty()) return@LaunchedEffect
        positioned = true
        next?.let { pageOf(it.first) }?.takeIf { it >= 0 }?.let { pager.scrollToPage(it) }
    }

    // Com todas as séries feitas não há mais descanso: o cronômetro volta ao estado inicial.
    LaunchedEffect(allDone) { if (allDone) viewModel.stopRest() }

    // Em telas largas o treino ocupa uma coluna central; o fundo continua cobrindo a tela toda.
    val layout = currentWindowLayout
    val sideSpace = ((layout.widthDp - layout.contentMaxWidth.value) / 2).coerceAtLeast(0f).dp
    Column(Modifier.fillMaxSize().background(p.bg).statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = sideSpace)) {
        TrainHeader(tr(day.day.name), count, done, ::requestExit)
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            ExerciseTabs(items, loggedKeys, pager.currentPage) { scope.launch { pager.animateScrollToPage(it) } }
        }
        HorizontalPager(
            pager,
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            verticalAlignment = Alignment.Top,
            key = { items.getOrNull(it)?.item?.id ?: -(it + 1L) }
        ) { index ->
            val entry = items.getOrNull(index) ?: return@HorizontalPager
            val id = entry.exercise.id
            val scroll = rememberScrollState()
            // Aquecimentos à vista: os registrados hoje e os adicionados agora (rascunhos com índice negativo).
            val warmups = (loggedKeys.filter { it.first == id && it.second < 0 }.map { -it.second } +
                drafts.keys.filter { it.first == id && it.second < 0 }.map { -it.second }).distinct().sorted()
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.verticalScroll(scroll).padding(top = 12.dp, bottom = 12.dp)) {
                    ExercisePanel(
                        entry = entry,
                        entranceIndex = 1,
                        hint = advice[id]?.hint(),
                        swappedFrom = originals[entry.item.id]?.takeIf { it.id != id },
                        warmups = warmups,
                        draftOf = { i -> loggedByKey[id to i]?.asDraft() ?: drafts[id to i] ?: SetDraftState("", "") },
                        loggedOf = { loggedByKey[id to it] },
                        isLogged = { (id to it) in loggedKeys },
                        onDraft = { i, d -> drafts[id to i] = d },
                        onToggle = { i, d, wasLogged ->
                            if (wasLogged) {
                                // Ao desfazer, os campos ficam com o que tinha sido registrado, para só corrigir.
                                loggedByKey[id to i]?.let { drafts[id to i] = it.asDraft() }
                                viewModel.unlogSet(id, i)
                            } else logDraft(id, i, d)
                        },
                        onAddWarmup = {
                            // 50% e depois 70% da carga da 1ª série de trabalho, arredondado para anilhas.
                            val n = (warmups.maxOrNull() ?: 0) + 1
                            val working = (loggedByKey[id to 1]?.weightKg ?: drafts[id to 1]?.weight?.decimal()) ?: 0.0
                            val w = Progression.warmup(n, working)
                            drafts[id to -n] = SetDraftState(if (w.weightKg > 0) kgText(w.weightKg) else "", w.reps.toString())
                        },
                        onDetails = { editing = entry to it },
                        onSwap = { swapping = entry }
                    )
                }
                ScrollHints(scroll.canScrollBackward, scroll.canScrollForward, Modifier.matchParentSize())
            }
        }
        TrainFooter(
            viewModel = viewModel,
            primary = TrainingFlow.primary(done, count),
            next = nextSet,
            restBlocked = allDone || done,
            onLogNext = ::onNext,
            onFinish = { viewModel.completeWorkout(); showConclusion = true },
            onSummary = { showConclusion = true }
        )
    }

    if (askingExit) {
        ZenDialog(tr("Sair do treino?"), { askingExit = false }, accent = Midori, footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZenButton(tr("Concluir assim"), { askingExit = false; viewModel.completeWorkout(); showConclusion = true }, Modifier.weight(1f), accent = Midori, fill = true, small = true)
                ZenButton(tr("Continuar depois"), { askingExit = false; onClose() }, Modifier.weight(1f), accent = Zen.p.card, fill = true, small = true)
            }
        }) {
            Text(tr("Você registrou {0} de {1} séries.", count.done, count.total), color = Zen.p.muted)
        }
    }

    if (showConclusion) {
        TrainConclusion(
            dayName = tr(day.day.name),
            summary = summary,
            level = progress?.level,
            onBackToDojo = onClose,
            onUndo = { viewModel.setWorkoutDone(false); showConclusion = false }
        )
    }

    editing?.let { (entry, setIndex) ->
        val id = entry.exercise.id
        val logged = loggedByKey[id to setIndex]
        val draft = logged?.asDraft() ?: drafts[id to setIndex] ?: SetDraftState("", "")
        SetDetailsDialog(
            title = if (setIndex < 0) tr("Aquecimento {0}", -setIndex) else tr("Série {0}", setIndex),
            exerciseName = tr(entry.exercise.name),
            accent = entry.exercise.group.color(),
            initialRir = draft.rir,
            initialNote = draft.note,
            showRir = setIndex > 0 && entry.exercise.kind != ExerciseKind.CARDIO,
            onRemove = if (setIndex < 0 && logged == null) ({ drafts.remove(id to setIndex); editing = null }) else null,
            onSave = { rir, note ->
                if (logged != null) viewModel.updateSetDetails(id, setIndex, rir, note)
                else drafts[id to setIndex] = draft.copy(rir = rir, note = note)
                editing = null
            },
            onDismiss = { editing = null }
        )
    }

    swapping?.let { entry ->
        SwapExerciseDialog(
            entry = entry,
            original = originals[entry.item.id],
            exercises = exercises,
            inDay = items.map { it.exercise.id }.toSet(),
            onSwap = { viewModel.swapExerciseToday(entry.item.id, it); swapping = null },
            onCreate = { name -> viewModel.createExerciseAndSwapToday(name, entry.exercise.group, entry.item.id); swapping = null },
            onDismiss = { swapping = null }
        )
    }
}

/**
 * Troca do exercício só para o treino de hoje. Só aparecem exercícios do mesmo grupo muscular
 * (elevação lateral pode virar frontal, nunca supino); a ficha não muda.
 */
@Composable
private fun SwapExerciseDialog(
    entry: WorkoutItemFull,
    original: Exercise?,
    exercises: List<Exercise>,
    inDay: Set<Long>,
    onSwap: (Exercise) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val group = entry.exercise.group
    val accent = group.color()
    var registering by remember { mutableStateOf(false) }
    val swapped = original != null && original.id != entry.exercise.id
    val options = remember(entry, exercises, inDay) {
        WorkoutSwaps.options(entry.exercise, exercises, inDay).filter { !swapped || it.id != original?.id }
    }

    ZenDialog(tr("Trocar · {0}", tr(entry.exercise.name)), onDismiss, accent = accent, footer = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenButton(tr("+ Cadastrar outro"), { registering = true }, Modifier.weight(1f), accent = Ao, fill = true, small = true)
            ZenButton(tr("Fechar"), onDismiss, Modifier.weight(1f), accent = Zen.p.card, fill = true, small = true)
        }
    }) {
        Text(tr("Vale só para o treino de hoje; a ficha continua igual."), color = Zen.p.muted)
        Spacer(Modifier.height(10.dp))
        if (swapped && original != null) {
            Text(tr("VOLTAR AO DA FICHA"), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            PickRow(tr(original.name), group.label, null, accent, trailingIcon = ZenIcons.Undo) { onSwap(original) }
            Spacer(Modifier.height(10.dp))
        }
        Text(tr("TROCAR POR ({0})", group.label.uppercase()), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        if (options.isEmpty()) Text(tr("Nenhuma outra opção neste grupo."), color = Zen.p.muted)
        options.forEach { alt -> PickRow(tr(alt.name), group.label, null, accent) { onSwap(alt) } }
    }

    if (registering) NewExerciseDialog(group, onDismiss = { registering = false }) { onCreate(it) }
}

/**
 * Barra de exercícios: uma aba por exercício, com um tracinho por série que fica verde quando a série é feita.
 * A aba do exercício à vista fica destacada e a barra rola sozinha para mantê-la visível ao deslizar.
 */
@Composable
private fun ExerciseTabs(items: List<WorkoutItemFull>, loggedKeys: Set<Pair<Long, Int>>, current: Int, onSelect: (Int) -> Unit) {
    val p = Zen.p
    val look = Zen.look
    val list = rememberLazyListState()
    // Mantém a aba atual à vista (com a anterior aparecendo, para dar contexto).
    LaunchedEffect(current) { if (items.isNotEmpty()) list.animateScrollToItem((current - 1).coerceAtLeast(0)) }
    LazyRow(
        Modifier.fillMaxWidth(),
        state = list,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp, end = 4.dp)
    ) {
        itemsIndexed(items, key = { _, e -> e.item.id }) { index, e ->
            val selected = index == current
            val complete = (1..e.item.sets).all { (e.exercise.id to it) in loggedKeys }
            val accent = e.exercise.group.color()
            val bg by animateColorAsState(
                when {
                    selected -> accent
                    complete -> Midori
                    else -> p.card
                },
                tween(200), label = "tab-bg"
            )
            val lift by animateFloatAsState(if (selected) 1f else 0f, spring(stiffness = Spring.StiffnessMediumLow), label = "tab-lift")
            val interaction = remember { MutableInteractionSource() }
            Column(
                Modifier
                    .graphicsLayer { translationY = -3.dp.toPx() * lift }
                    .pressScale(interaction)
                    .clip(look.smallShape)
                    .background(bg)
                    .border(if (selected) look.border else look.thinBorder, p.border, look.smallShape)
                    .clickable(interaction, null) { onSelect(index) }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    e.exercise.group.kanji(),
                    color = if (selected || complete) Sumi else p.ink,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (i in 1..e.item.sets) {
                        val setDone = (e.exercise.id to i) in loggedKeys
                        val tick by animateColorAsState(if (setDone) Midori else p.bg, tween(220), label = "tab-set")
                        Box(
                            Modifier
                                .size(width = 7.dp, height = 9.dp)
                                .clip(look.smallShape)
                                .background(tick)
                                .border(1.dp, p.border, look.smallShape)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cabeçalho do modo treino numa faixa só: X, "Modo treino" e o dia, o kanji do ânimo
 * e, embaixo, a barra com "9 / 33 séries" (ou "Concluído" com o treino já feito).
 */
@Composable
private fun TrainHeader(dayName: String, count: SetCount, done: Boolean, onClose: () -> Unit) {
    val p = Zen.p
    val kanji = Progression.mood(count.done, count.total).first
    Column(Modifier.background(p.card)) {
        Box(Modifier.fillMaxWidth().height(8.dp).background(Aka))
        Row(Modifier.padding(start = 12.dp, end = 16.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            val interaction = remember { MutableInteractionSource() }
            val closeLabel = tr("Fechar")
            Box(
                Modifier
                    .size(48.dp)
                    .pressScale(interaction)
                    .clip(Zen.look.smallShape)
                    .background(p.bg)
                    .border(Zen.look.thinBorder, p.border, Zen.look.smallShape)
                    .semantics { contentDescription = closeLabel }
                    .clickable(interaction, null, role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                ZenIcon(ZenIcons.Close, size = 20.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("MODO TREINO"), color = p.muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(
                    dayName.uppercase(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black,
                    fontStyle = if (Zen.look.upright) FontStyle.Normal else FontStyle.Italic, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            KanjiBadge(kanji, color = if (done || count.complete) Midori else Aka, size = 28.dp)
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp)) {
            ZenProgressBar(
                if (count.total == 0) 0f else count.done.toFloat() / count.total,
                if (done || count.complete) Midori else Aka, height = 10.dp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (done) tr("Concluído") else tr("{0} / {1} séries", count.done, count.total),
                color = p.muted, style = MaterialTheme.typography.labelLarge
            )
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(p.border))
    }
}

@Composable
private fun ExercisePanel(
    entry: WorkoutItemFull,
    entranceIndex: Int,
    hint: Hint?,
    swappedFrom: Exercise?,
    warmups: List<Int>,
    draftOf: (Int) -> SetDraftState,
    loggedOf: (Int) -> SetLog?,
    isLogged: (Int) -> Boolean,
    onDraft: (Int, SetDraftState) -> Unit,
    onToggle: (Int, SetDraftState, Boolean) -> Unit,
    onAddWarmup: () -> Unit,
    onDetails: (Int) -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val group = entry.exercise.group
    val accent = group.color()
    val sets = entry.item.sets
    val doneCount = (1..sets).count(isLogged)
    val complete = doneCount == sets
    // Prancha e cardio se medem em tempo: sem carga, e o valor é segundos ou minutos (o cardio ganha a distância).
    val kind = entry.exercise.kind
    val timed = kind == ExerciseKind.HOLD || kind == ExerciseKind.CARDIO
    val cardio = kind == ExerciseKind.CARDIO

    var menuOpen by remember { mutableStateOf(false) }
    var imageOpen by remember { mutableStateOf(false) }
    // Com séries já registradas a troca fica travada: o registro pertence ao exercício atual.
    val hasWarmupLogged = warmups.any { isLogged(-it) }
    val canSwap = doneCount == 0 && !hasWarmupLogged
    // Aquecimento só faz sentido com carga (não em prancha e cardio), e no máximo três.
    val canWarmup = !timed && warmups.size < 3

    ZenCard(modifier, accent = accent, entranceIndex = entranceIndex) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Miniatura: tocar amplia a imagem num diálogo. Sem imagem, fica o kanji do grupo.
            val zoomLabel = tr("Ampliar imagem")
            Box(
                Modifier
                    .size(56.dp)
                    .clip(Zen.look.smallShape)
                    .background(Zen.p.bg)
                    .border(Zen.look.thinBorder, Zen.p.border, Zen.look.smallShape)
                    .semantics { contentDescription = zoomLabel }
                    .clickable(role = Role.Button) { imageOpen = true },
                contentAlignment = Alignment.Center
            ) {
                KanjiBadge(group.kanji(), color = accent, size = 38.dp)
                ExerciseImage(entry.exercise.name, showCredit = false)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tr(entry.exercise.name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (kind == ExerciseKind.CARDIO) entry.item.reps else tr("{0} séries × {1} · {2}", sets, entry.item.reps, group.label),
                    color = Zen.p.muted, style = MaterialTheme.typography.labelLarge
                )
            }
            Box(contentAlignment = Alignment.Center) {
                if (complete) HankoStamp()
            }
            Spacer(Modifier.width(6.dp))
            MoreButton({ menuOpen = true }, tr("Mais opções do exercício"))
        }
        if (swappedFrom != null) {
            Spacer(Modifier.height(4.dp))
            Text(tr("Trocado hoje · era {0}", tr(swappedFrom.name)), color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
        }
        // A dica de carga só vale até a primeira série de trabalho ser registrada.
        if (hint != null && doneCount == 0) {
            Spacer(Modifier.height(6.dp))
            SpeechTag(hint)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ColumnLabel(tr("Série"), Modifier.width(SideCell))
            if (!timed) ColumnLabel(tr("kg"), Modifier.weight(1f))
            ColumnLabel(
                when (kind) { ExerciseKind.HOLD -> tr("seg"); ExerciseKind.CARDIO -> tr("min"); else -> tr("reps") },
                Modifier.weight(1f)
            )
            if (cardio) ColumnLabel(tr("km"), Modifier.weight(1f))
            Spacer(Modifier.width(SideCell))
        }
        Spacer(Modifier.height(4.dp))
        // Aquecimentos (A1, A2…) antes das séries de trabalho; não contam no volume nem nos recordes.
        warmups.forEach { n ->
            val i = -n
            val draft = draftOf(i)
            val logged = isLogged(i)
            SetRow(
                label = "A$n",
                badge = if (draft.note.isNotBlank()) "✎" else null,
                accent = Zen.p.muted,
                draft = draft,
                logged = logged,
                showWeight = true,
                showKm = false,
                onWeight = { onDraft(i, draft.copy(weight = it)) },
                onReps = { onDraft(i, draft.copy(reps = it)) },
                onKm = { },
                onIndex = { onDetails(i) },
                onToggle = { onToggle(i, draft, logged) }
            )
            Spacer(Modifier.height(6.dp))
        }
        for (i in 1..sets) {
            val draft = draftOf(i)
            val logged = isLogged(i)
            SetRow(
                label = "$i",
                badge = draft.rir?.let { "RIR$it" } ?: if (draft.note.isNotBlank()) "✎" else null,
                accent = accent,
                draft = draft,
                logged = logged,
                showWeight = !timed,
                showKm = cardio,
                onWeight = { onDraft(i, draft.copy(weight = it)) },
                onReps = { onDraft(i, draft.copy(reps = it)) },
                onKm = { onDraft(i, draft.copy(km = it)) },
                onIndex = { onDetails(i) },
                onToggle = { onToggle(i, draft, logged) }
            )
            if (i < sets) Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(8.dp))
        if (cardio) {
            // Gasto e ritmo do cardio registrado hoje (estimativa pelo MET da atividade).
            val done = (1..sets).mapNotNull(loggedOf)
            val minutes = done.sumOf { it.reps }
            val km = done.mapNotNull { it.distanceKm }.sum().takeIf { it > 0 }
            val kcal = done.sumOf { it.kcal ?: 0 }
            Text(
                if (done.isEmpty()) tr("Informe os minutos e, se quiser, a distância. As calorias são estimadas.")
                else listOfNotNull(
                    tr("{0} min", minutes),
                    km?.let { tr("{0} km", kgText(it)) },
                    Cardio.paceLabel(minutes, km),
                    tr("≈ {0} kcal", kcal)
                ).joinToString(" · "),
                color = Zen.p.muted, style = MaterialTheme.typography.labelLarge
            )
        } else {
            Text(
                tr("Toque no número da série para anotar o esforço (RIR) e uma nota."),
                color = Zen.p.muted, style = MaterialTheme.typography.labelSmall
            )
        }
    }

    if (menuOpen) {
        ZenSheet(onDismiss = { menuOpen = false }, accent = accent, title = tr(entry.exercise.name)) {
            SheetOption(
                title = tr("Trocar ({0})", group.label),
                description = if (canSwap) tr("Vale só para o treino de hoje; a ficha continua igual.")
                else tr("Para trocar, desfaça as séries deste exercício."),
                onClick = onSwap,
                enabled = canSwap
            )
            if (canWarmup) {
                SheetOption(tr("+ Aquecimento"), tr("Adiciona uma série leve antes das de trabalho (até 3)."), onAddWarmup)
            }
        }
    }
    if (imageOpen) {
        ZenDialog(tr(entry.exercise.name), onDismiss = { imageOpen = false }, accent = accent) {
            ExerciseImage(entry.exercise.name)
        }
    }
}

/**
 * Detalhes de uma série: esforço em RIR (repetições que ainda sobravam; RPE ≈ 10 − RIR) e uma nota livre.
 * Aquecimentos ainda não registrados podem ser removidos aqui.
 */
@Composable
private fun SetDetailsDialog(
    title: String,
    exerciseName: String,
    accent: Color,
    initialRir: Int?,
    initialNote: String,
    showRir: Boolean,
    onRemove: (() -> Unit)?,
    onSave: (Int?, String) -> Unit,
    onDismiss: () -> Unit
) {
    var rir by remember { mutableStateOf(initialRir) }
    var note by remember { mutableStateOf(initialNote) }
    ZenDialog("$title · $exerciseName", onDismiss, accent = accent, footer = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onRemove != null) ZenButton(tr("Remover"), onRemove, Modifier.weight(1f), accent = Aka, fill = true, small = true)
            ZenButton(tr("Salvar"), { onSave(rir, note) }, Modifier.weight(1f), accent = Midori, fill = true, small = true)
            ZenButton(tr("Cancelar"), onDismiss, Modifier.weight(1f), accent = Zen.p.card, fill = true, small = true)
        }
    }) {
        if (showRir) {
            Text(tr("QUANTAS REPETIÇÕES AINDA SOBRAVAM? (RIR)"), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ZenChip("—", rir == null, { rir = null }, Modifier.weight(1f), accent = accent, compact = true)
                (0..5).forEach { v ->
                    ZenChip(if (v == 5) "5+" else "$v", rir == v, { rir = v }, Modifier.weight(1f), accent = accent, compact = true)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                rir?.let { tr("RPE {0} · {1}", Progression.rpe(it), rirMeaning(it)) }
                    ?: tr("0 = falha; 1–2 = pesado; 3+ = sobrou força. Com 3+ em todas as séries, o app sugere subir a carga."),
                color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
        }
        ZenField(tr("Nota"), note, { note = it.take(200) }, placeholder = tr("Ex.: pegada fechada, dor no ombro…"))
    }
}

private fun rirMeaning(rir: Int): String = when (rir) {
    0 -> tr("até a falha")
    1, 2 -> tr("pesado")
    else -> tr("sobrou força")
}

/** Carimbo "完" (concluído) que cai girando sobre o painel quando todas as séries são feitas. */
@Composable
internal fun HankoStamp(size: Dp = 40.dp) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)) }
    Box(
        Modifier
            .graphicsLayer {
                val v = anim.value
                val scale = 2.2f - 1.2f * v
                scaleX = scale
                scaleY = scale
                rotationZ = -14f * v
                alpha = v.coerceIn(0f, 1f)
            }
            .size(size)
            .border(3.dp, Aka, CircleShape)
            .padding(3.dp)
            .border(1.dp, Aka, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(if (Zen.look.kawaii) "♥︎" else "完", color = Aka, fontWeight = FontWeight.Black, fontSize = (18f * size.value / 40f).sp)
    }
}

/** Balão de fala com a dica de progressão de carga (subir, mais repetições ou deload). */
@Composable
private fun SpeechTag(hint: Hint) {
    val p = Zen.p
    val border = Zen.look.thinBorder
    // Pontinha para cima, apontando para o nome do exercício; desenhada por cima da borda do balão.
    Column {
        Canvas(Modifier.padding(start = 16.dp).size(width = 14.dp, height = 7.dp)) {
            val tail = Path().apply {
                moveTo(0f, size.height); lineTo(size.width / 2, 0f); lineTo(size.width, size.height)
            }
            drawPath(tail, hint.color)
            drawPath(tail, p.border, style = Stroke(width = border.toPx().coerceAtLeast(1f)))
        }
        SpeechBubble(hint)
    }
}

@Composable
private fun SpeechBubble(hint: Hint) {
    val p = Zen.p
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            hint.tag,
            color = Sumi,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.clip(Zen.look.smallShape).background(hint.color).border(Zen.look.thinBorder, p.border, Zen.look.smallShape).padding(horizontal = 6.dp, vertical = 3.dp)
        )
        Text(
            hint.text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(start = 4.dp)
                .clip(Zen.look.smallShape)
                .background(p.bg)
                .border(Zen.look.thinBorder, p.border, Zen.look.smallShape)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun ColumnLabel(text: String, modifier: Modifier) {
    Text(text.uppercase(), modifier, color = Zen.p.muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
}

@Composable
private fun SetRow(
    label: String,
    badge: String?,
    accent: Color,
    draft: SetDraftState,
    logged: Boolean,
    showWeight: Boolean,
    showKm: Boolean,
    onWeight: (String) -> Unit,
    onReps: (String) -> Unit,
    onKm: (String) -> Unit,
    onIndex: () -> Unit,
    onToggle: () -> Unit
) {
    val p = Zen.p
    val look = Zen.look
    val canLog = logged || (draft.reps.trim().toIntOrNull() ?: 0) > 0
    val indexBg by animateColorAsState(if (logged) accent else p.bg, tween(220), label = "set-index")
    val checkBg by animateColorAsState(if (logged) Midori else p.card, tween(200), label = "set-check")

    // "Pop" com quique + linhas de impacto (集中線) a cada série registrada.
    // Só anima quando a série é marcada agora, não ao reabrir a janela com séries já registradas.
    val pop = remember { Animatable(1f) }
    val burst = remember { Animatable(1f) }
    val firstRun = remember { booleanArrayOf(true) }
    LaunchedEffect(logged) {
        val initial = firstRun[0]
        firstRun[0] = false
        if (initial || !logged) return@LaunchedEffect
        pop.snapTo(0.6f)
        burst.snapTo(0f)
        coroutineScope {
            launch { pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium)) }
            launch { burst.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        // Número da série: toque para anotar esforço e nota; a marca embaixo mostra o que já foi anotado.
        val detailsLabel = tr("Detalhes da série {0}", label)
        Box(
            Modifier.size(SideCell, CellHeight).clip(look.smallShape).background(indexBg).border(look.thinBorder, p.border, look.smallShape)
                .semantics { contentDescription = detailsLabel }
                .clickable(role = Role.Button, onClick = onIndex),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, color = if (logged) Sumi else p.ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1)
                if (badge != null) {
                    Text(badge, color = if (logged) Sumi else p.muted, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
        if (showWeight) SetInput(draft.weight, onWeight, KeyboardType.Decimal, placeholder = "0", enabled = !logged, modifier = Modifier.weight(1f))
        SetInput(draft.reps, onReps, KeyboardType.Number, placeholder = "–", enabled = !logged, modifier = Modifier.weight(1f))
        if (showKm) SetInput(draft.km, onKm, KeyboardType.Decimal, placeholder = "–", enabled = !logged, modifier = Modifier.weight(1f))
        val interaction = remember { MutableInteractionSource() }
        Box(
            Modifier
                .size(SideCell, CellHeight)
                .drawBehind {
                    val b = burst.value
                    if (b >= 1f) return@drawBehind
                    val c = Offset(size.width / 2, size.height / 2)
                    val inner = size.minDimension * (0.55f + 0.35f * b)
                    val outer = inner + size.minDimension * 0.35f * (1f - b)
                    if (look.kawaii) {
                        // Kawaii: estrelinhas se espalhando em vez das linhas de impacto.
                        repeat(6) { k ->
                            val a = Math.toRadians(k * 60.0 + 30.0)
                            val r = inner + (outer - inner) * 0.5f
                            drawSparkle(Offset(c.x + cos(a).toFloat() * r, c.y + sin(a).toFloat() * r), 4.dp.toPx(), accent.copy(alpha = 1f - b))
                        }
                        return@drawBehind
                    }
                    repeat(10) { k ->
                        val a = Math.toRadians(k * 36.0 + 18.0)
                        val dx = cos(a).toFloat()
                        val dy = sin(a).toFloat()
                        drawLine(
                            accent.copy(alpha = 1f - b),
                            Offset(c.x + dx * inner, c.y + dy * inner),
                            Offset(c.x + dx * outer, c.y + dy * outer),
                            3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                .pressScale(interaction)
                .clip(look.smallShape)
                .background(checkBg)
                .border(look.thinBorder, p.border, look.smallShape)
                .clickable(interaction, null, enabled = canLog, onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            ZenIcon(
                ZenIcons.Check,
                size = 20.dp,
                color = when {
                    logged -> Sumi
                    canLog -> p.ink
                    else -> p.muted.copy(alpha = 0.4f)
                }
            )
        }
    }
}

/** Campo numérico compacto e centralizado; fica esmaecido (e travado) quando a série já foi registrada. */
@Composable
private fun SetInput(
    value: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType,
    placeholder: String,
    enabled: Boolean,
    modifier: Modifier
) {
    val p = Zen.p
    val ink: Color = if (enabled) p.ink else p.muted
    BasicTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter { it.isDigit() || it == '.' || it == ',' }.take(6)) },
        enabled = enabled,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(color = ink, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
        cursorBrush = SolidColor(Aka),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.height(CellHeight).clip(Zen.look.smallShape).background(p.bg)
            .border(Zen.look.thinBorder, if (enabled) p.border else p.muted.copy(alpha = 0.5f), Zen.look.smallShape),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth().padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                if (value.isEmpty()) Text(placeholder, color = p.muted, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                inner()
            }
        }
    )
}
