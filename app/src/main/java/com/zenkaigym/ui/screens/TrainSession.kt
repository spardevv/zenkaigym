package com.zenkaigym.ui.screens

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
import com.zenkaigym.domain.WorkoutSwaps
import com.zenkaigym.ui.components.PickRow
import com.zenkaigym.ui.components.ZenDialog
import com.zenkaigym.ui.components.drawSparkle
import com.zenkaigym.ui.components.zenShadow
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import com.zenkaigym.data.entity.ExerciseKind
import com.zenkaigym.data.entity.MuscleGroup
import com.zenkaigym.data.entity.SetLog
import com.zenkaigym.data.entity.WorkoutDayFull
import com.zenkaigym.data.entity.WorkoutItemFull
import com.zenkaigym.domain.Progression
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private val CellHeight = 42.dp
private val SideCell = 40.dp

/** Texto de carga sem zeros inúteis: 22.0 -> "22", 22.5 -> "22.5". */
private fun kgText(kg: Double): String = if (kg % 1.0 == 0.0) kg.toLong().toString() else kg.toString()

private data class SetDraftState(val weight: String, val reps: String)

/** Série registrada como texto dos campos (carga zero = campo vazio). */
private fun SetLog.asDraft() = SetDraftState(if (weightKg > 0) kgText(weightKg) else "", reps.toString())

/** Kanji e cor de cada grupo muscular: dão identidade a cada painel do treino. */
private fun MuscleGroup.kanji(): String = when (this) {
    MuscleGroup.CHEST -> "胸"
    MuscleGroup.LATS, MuscleGroup.UPPER_BACK -> "背"
    MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.ADDUCTORS, MuscleGroup.CALVES -> "脚"
    MuscleGroup.SHOULDERS -> "肩"
    MuscleGroup.BICEPS -> "腕"
    MuscleGroup.TRICEPS -> "腕"
    MuscleGroup.CORE -> "腹"
    MuscleGroup.CARDIO -> "走"
}

private fun MuscleGroup.color(): Color = when (this) {
    MuscleGroup.CHEST -> Aka
    MuscleGroup.LATS, MuscleGroup.UPPER_BACK -> Ao
    MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.ADDUCTORS, MuscleGroup.CALVES -> Ki
    MuscleGroup.SHOULDERS -> Daidai
    MuscleGroup.BICEPS, MuscleGroup.TRICEPS -> Sakura
    MuscleGroup.CORE, MuscleGroup.CARDIO -> Midori
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
    val drafts = remember(day) { mutableStateMapOf<Pair<Long, Int>, SetDraftState>() }
    val hints = remember(day) { mutableStateMapOf<Long, String>() }
    var swapping by remember { mutableStateOf<WorkoutItemFull?>(null) }

    // Recalcula ao trocar um exercício: o novo ganha a carga da última vez em que foi feito.
    LaunchedEffect(items.map { it.exercise.id }) {
        items.forEach { entry ->
            val history = viewModel.history(entry.exercise.id)
            // A dica só aparece quando dá para subir a carga; os campos continuam com o valor anterior.
            val suggested = Progression.nextWeight(history.last, entry.item.reps)
            val lastTop = history.last.maxOfOrNull { it.weightKg }
            if (suggested != null && lastTop != null && suggested > lastTop) {
                hints[entry.exercise.id] = tr("Última vez: {0} kg · hoje: {1} kg", kgText(lastTop), kgText(suggested))
            }
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
    val totalSets = items.sumOf { it.item.sets }
    val doneSets = items.sumOf { e -> (1..e.item.sets).count { (e.exercise.id to it) in loggedKeys } }
    val allDone = totalSets > 0 && doneSets == totalSets

    // Voltar do sistema sai do modo treino (as séries já registradas ficam salvas).
    BackHandler(onBack = onClose)

    // Tela acesa enquanto o modo treino estiver aberto (se ligado em Ajustes).
    val keepOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
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

    // Ao reabrir um treino já começado, abre direto no exercício da próxima série.
    var positioned by remember(day) { mutableStateOf(false) }
    LaunchedEffect(logged) {
        if (positioned || logged.isEmpty()) return@LaunchedEffect
        positioned = true
        next?.let { pageOf(it.first) }?.takeIf { it >= 0 }?.let { pager.scrollToPage(it) }
    }

    // Com todas as séries feitas não há mais descanso: o cronômetro volta ao estado inicial.
    LaunchedEffect(allDone) { if (allDone) viewModel.stopRest() }

    /** Registra a série com o rascunho atual e já inicia o descanso. Devolve false se faltam repetições. */
    fun logDraft(exerciseId: Long, setIndex: Int, draft: SetDraftState): Boolean {
        val reps = draft.reps.trim().toIntOrNull()?.takeIf { it > 0 } ?: return false
        val kg = draft.weight.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
        viewModel.logSet(exerciseId, setIndex, reps, kg)
        // A última série pendente fecha o treino: nada de descanso depois dela.
        val isLast = pending.size == 1 && pending.first() == (exerciseId to setIndex)
        // Cardio não tem descanso entre séries; a prancha pede um intervalo curto.
        val kind = items.firstOrNull { it.exercise.id == exerciseId }?.exercise?.kind
        when {
            isLast || kind == ExerciseKind.CARDIO -> Unit
            kind == ExerciseKind.HOLD -> viewModel.startRest(45)
            else -> viewModel.startRest(Progression.restSeconds(reps))
        }
        return true
    }

    Column(Modifier.fillMaxSize().background(p.bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        TrainHeader(tr(day.day.name), onClose)
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            PowerGauge(doneSets, totalSets)
            Spacer(Modifier.height(10.dp))
            ExerciseTabs(items, loggedKeys, pager.currentPage) { scope.launch { pager.animateScrollToPage(it) } }
        }
        HorizontalPager(
            pager,
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            verticalAlignment = Alignment.Top,
            key = { items[it].item.id }
        ) { index ->
            val entry = items[index]
            val id = entry.exercise.id
            val scroll = rememberScrollState()
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.verticalScroll(scroll).padding(top = 12.dp, bottom = 12.dp)) {
                    ExercisePanel(
                        entry = entry,
                        entranceIndex = 1,
                        hint = hints[id],
                        swappedFrom = originals[entry.item.id]?.takeIf { it.id != id },
                        draftOf = { i -> loggedByKey[id to i]?.asDraft() ?: drafts[id to i] ?: SetDraftState("", "") },
                        isLogged = { (id to it) in loggedKeys },
                        onDraft = { i, d -> drafts[id to i] = d },
                        onToggle = { i, d, wasLogged ->
                            if (wasLogged) {
                                // Ao desfazer, os campos ficam com o que tinha sido registrado, para só corrigir.
                                loggedByKey[id to i]?.let { drafts[id to i] = it.asDraft() }
                                viewModel.unlogSet(id, i)
                            } else logDraft(id, i, d)
                        },
                        onSwap = { swapping = entry }
                    )
                }
                ScrollHints(scroll.canScrollBackward, scroll.canScrollForward, Modifier.matchParentSize())
            }
        }
        TrainFooter(
            viewModel = viewModel,
            done = done,
            allDone = allDone,
            nextLabel = next?.let { (id, i) ->
                val name = items.firstOrNull { it.exercise.id == id }?.exercise?.name.orEmpty()
                tr("{0} · série {1}", tr(name), i)
            },
            onNext = {
                val target = next ?: return@TrainFooter
                if (logDraft(target.first, target.second, drafts[target] ?: SetDraftState("", ""))) {
                    // Desliza até o exercício da série seguinte.
                    val after = pending.getOrNull(1) ?: return@TrainFooter
                    val page = pageOf(after.first)
                    if (page >= 0 && page != pager.currentPage) scope.launch { pager.animateScrollToPage(page) }
                }
            },
            onClose = onClose
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

    ZenDialog(tr("Trocar · {0}", tr(entry.exercise.name)), onDismiss, accent = accent) {
        Text(tr("Vale só para o treino de hoje; a ficha continua igual."), color = Zen.p.muted)
        Spacer(Modifier.height(10.dp))
        if (swapped && original != null) {
            Text(tr("VOLTAR AO DA FICHA"), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            PickRow(tr(original.name), group.label, "↺", accent) { onSwap(original) }
            Spacer(Modifier.height(10.dp))
        }
        Text(tr("TROCAR POR ({0})", group.label.uppercase()), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        if (options.isEmpty()) Text(tr("Nenhuma outra opção neste grupo."), color = Zen.p.muted)
        options.forEach { alt -> PickRow(tr(alt.name), group.label, null, accent) { onSwap(alt) } }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenButton(tr("+ Cadastrar outro"), { registering = true }, Modifier.weight(1f), accent = Ao, fill = true, small = true)
            ZenButton(tr("Fechar"), onDismiss, Modifier.weight(1f), accent = Zen.p.card, fill = true, small = true)
        }
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
 * Rodapé fixo do modo treino. Linha de ferramentas (descanso e próxima série, em destaque)
 * e linha de ações (sair e concluir/desfazer).
 */
@Composable
private fun TrainFooter(
    viewModel: ZenViewModel,
    done: Boolean,
    allDone: Boolean,
    nextLabel: String?,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    val p = Zen.p
    val rest by viewModel.rest.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Relógio do descanso: atualiza o visor e vibra ao terminar.
    val now by produceState(System.currentTimeMillis(), rest) {
        while (rest != null) {
            value = System.currentTimeMillis()
            delay(200)
        }
    }
    LaunchedEffect(rest) {
        val r = rest ?: return@LaunchedEffect
        delay((r.endsAt - System.currentTimeMillis()).coerceAtLeast(0))
        vibrateRestOver(context)
        viewModel.stopRest()
    }

    Column(Modifier.background(p.card)) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(p.border))
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val r = rest
                val remainingMs = r?.let { (it.endsAt - now).coerceAtLeast(0) } ?: 0L
                // Treino finalizado (todas as séries ou marcado como concluído): sem descanso.
                val restEnabled = !allDone && !done
                ToolButton(
                    top = if (r != null) tr("DESCANSO · PULAR") else tr("DESCANSO"),
                    main = if (r != null) "%d:%02d".format(remainingMs / 60_000, remainingMs / 1000 % 60) else "⏱ 1:30",
                    onClick = { if (r != null) viewModel.stopRest() else viewModel.startRest(90) },
                    modifier = Modifier.weight(1f),
                    accent = Ao,
                    progress = r?.let { 1f - remainingMs.toFloat() / it.totalMs },
                    active = r != null,
                    enabled = restEnabled
                )
                // Ação principal do treino: vermelho sólido e mais larga, para achar sem procurar.
                ToolButton(
                    top = tr("PRÓXIMA SÉRIE") + "  ▶",
                    main = nextLabel ?: tr("Tudo feito!"),
                    onClick = onNext,
                    modifier = Modifier.weight(2f),
                    accent = Aka,
                    enabled = nextLabel != null,
                    filled = nextLabel != null
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZenButton(tr("Sair"), onClose, Modifier.weight(1f), accent = p.card, fill = true)
                if (done) {
                    // Sempre visível: mostra que já foi concluído e permite desfazer.
                    ZenButton(tr("Treino feito ✓ · desfazer"), { viewModel.setWorkoutDone(false) }, Modifier.weight(1.4f), accent = Midori, fill = true)
                } else {
                    // Com todas as séries feitas, o botão pulsa chamando para fechar o treino.
                    val pulse = if (allDone) {
                        rememberInfiniteTransition(label = "finish-pulse").animateFloat(
                            1f, 1.05f, infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "finish-scale"
                        ).value
                    } else 1f
                    ZenButton(
                        tr("Concluir treino"), { viewModel.setWorkoutDone(true) },
                        Modifier.weight(1.4f).graphicsLayer { scaleX = pulse; scaleY = pulse },
                        accent = Midori, fill = true
                    )
                }
            }
        }
    }
}

/**
 * Botão de ferramenta do rodapé: rótulo pequeno em cima e valor grande embaixo, com sombra de mangá.
 * [progress] (0..1) desenha uma faixa enchendo por trás (usado pelo descanso).
 */
@Composable
private fun ToolButton(
    top: String,
    main: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color,
    progress: Float? = null,
    active: Boolean = false,
    filled: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    iconDescription: String? = null
) {
    val p = Zen.p
    val look = Zen.look
    val interaction = remember { MutableInteractionSource() }
    // Cores opacas (sem transparência): misturar o acento com o fundo deixava o botão com tom barrento.
    val bg by animateColorAsState(
        when {
            filled -> accent
            active -> p.card
            else -> p.bg
        },
        tween(200), label = "tool-bg"
    )
    val topColor = when {
        filled -> Sumi
        enabled -> p.muted
        else -> p.muted.copy(alpha = 0.5f)
    }
    val mainColor = when {
        filled -> Sumi
        enabled -> p.ink
        else -> p.muted
    }
    Box(
        modifier
            .padding(end = 3.dp, bottom = 3.dp)
            .zenShadow(3.dp, p.shadow, look.corner / 2)
            .pressScale(interaction)
            .height(52.dp)
            .clip(look.smallShape)
            .background(bg)
            .drawBehind {
                if (progress != null) drawRect(accent, size = size.copy(width = size.width * progress.coerceIn(0f, 1f)))
            }
            .border(look.thinBorder, p.border, look.smallShape)
            .clickable(interaction, null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(top, color = topColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            // Ícone vetorial em vez de símbolo de texto: símbolos como ☀ viram emoji colorido e ignoram a cor.
            if (icon != null) {
                Icon(icon, contentDescription = iconDescription, tint = mainColor, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    main,
                    color = mainColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Vibração curta dupla avisando que o descanso acabou. */
private fun vibrateRestOver(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Vibrator::class.java)
    } ?: return
    val pattern = longArrayOf(0, 220, 120, 220)
    if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    else @Suppress("DEPRECATION") vibrator.vibrate(pattern, -1)
}

/** Cabeçalho do modo treino: faixa vermelha, título grande e botão de sair. */
@Composable
private fun TrainHeader(dayName: String, onClose: () -> Unit) {
    val p = Zen.p
    Column(Modifier.background(p.card)) {
        Box(Modifier.fillMaxWidth().height(8.dp).background(Aka))
        Row(Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("MODO TREINO"), color = p.muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(dayName.uppercase(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, fontStyle = if (Zen.look.kawaii) FontStyle.Normal else FontStyle.Italic)
            }
            KanjiBadge(if (Zen.look.kawaii) "♡" else "体", color = Ki, size = 40.dp)
            Spacer(Modifier.width(10.dp))
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .size(40.dp)
                    .pressScale(interaction)
                    .clip(Zen.look.smallShape)
                    .background(p.bg)
                    .border(Zen.look.thinBorder, p.border, Zen.look.smallShape)
                    .clickable(interaction, null, onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            }
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(p.border))
    }
}

/**
 * Medidor de "energia" do treino: kanji, frase de ânimo e porcentagem mudando conforme o progresso.
 * O progresso de cada exercício fica na fileira de abas logo abaixo ([ExerciseTabs]).
 */
@Composable
private fun PowerGauge(done: Int, total: Int) {
    val p = Zen.p
    val percent by animateIntAsState(if (total == 0) 0 else done * 100 / total, tween(450), label = "gauge-percent")
    val (kanji, phrase) = Progression.mood(done, total)
    val color by animateColorAsState(if (total > 0 && done == total) Midori else Aka, tween(300), label = "gauge-color")

    Row(verticalAlignment = Alignment.CenterVertically) {
        AnimatedContent(
            kanji,
            transitionSpec = { (scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()) togetherWith fadeOut(tween(120)) },
            label = "gauge-kanji"
        ) { KanjiBadge(it, color = color, size = 44.dp) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            AnimatedContent(
                phrase,
                transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
                label = "gauge-phrase"
            ) { Text(tr(it).uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) }
            Text(tr("{0} de {1} séries", done, total), color = p.muted, style = MaterialTheme.typography.labelLarge)
        }
        Text("$percent%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, fontStyle = if (Zen.look.kawaii) FontStyle.Normal else FontStyle.Italic)
    }
}

@Composable
private fun ExercisePanel(
    entry: WorkoutItemFull,
    entranceIndex: Int,
    hint: String?,
    swappedFrom: Exercise?,
    draftOf: (Int) -> SetDraftState,
    isLogged: (Int) -> Boolean,
    onDraft: (Int, SetDraftState) -> Unit,
    onToggle: (Int, SetDraftState, Boolean) -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val group = entry.exercise.group
    val accent = group.color()
    val sets = entry.item.sets
    val doneCount = (1..sets).count(isLogged)
    val complete = doneCount == sets
    // Prancha e cardio se medem em tempo: sem carga, e o valor é segundos ou minutos.
    val kind = entry.exercise.kind
    val timed = kind == ExerciseKind.HOLD || kind == ExerciseKind.CARDIO

    ZenCard(modifier, accent = accent, entranceIndex = entranceIndex) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KanjiBadge(group.kanji(), color = accent, size = 38.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tr(entry.exercise.name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (kind == ExerciseKind.CARDIO) entry.item.reps else tr("{0} séries × {1}", sets, entry.item.reps),
                    color = Zen.p.muted, style = MaterialTheme.typography.labelLarge
                )
            }
            Box(contentAlignment = Alignment.Center) {
                if (complete) HankoStamp()
            }
        }
        if (swappedFrom != null) {
            Spacer(Modifier.height(4.dp))
            Text(tr("Trocado hoje · era {0}", tr(swappedFrom.name)), color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))
        // Com séries já registradas a troca fica travada: o registro pertence ao exercício atual.
        if (doneCount == 0) {
            ZenButton(tr("⇄ Trocar exercício ({0})", group.label), onSwap, accent = Zen.p.card, small = true)
        } else {
            Text(tr("Para trocar, desfaça as séries deste exercício."), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
        }
        hint?.let {
            Spacer(Modifier.height(8.dp))
            SpeechTag(it)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ColumnLabel(tr("Série"), Modifier.width(SideCell))
            if (!timed) ColumnLabel(tr("kg"), Modifier.weight(1f))
            ColumnLabel(
                when (kind) { ExerciseKind.HOLD -> tr("seg"); ExerciseKind.CARDIO -> tr("min"); else -> tr("reps") },
                Modifier.weight(1f)
            )
            Spacer(Modifier.width(SideCell))
        }
        Spacer(Modifier.height(4.dp))
        for (i in 1..sets) {
            val draft = draftOf(i)
            val logged = isLogged(i)
            SetRow(
                index = i,
                accent = accent,
                draft = draft,
                logged = logged,
                showWeight = !timed,
                onWeight = { onDraft(i, draft.copy(weight = it)) },
                onReps = { onDraft(i, draft.copy(reps = it)) },
                onToggle = { onToggle(i, draft, logged) }
            )
            if (i < sets) Spacer(Modifier.height(6.dp))
        }
    }
}

/** Carimbo "完" (concluído) que cai girando sobre o painel quando todas as séries são feitas. */
@Composable
private fun HankoStamp() {
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
            .size(40.dp)
            .border(3.dp, Aka, CircleShape)
            .padding(3.dp)
            .border(1.dp, Aka, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(if (Zen.look.kawaii) "♥" else "完", color = Aka, fontWeight = FontWeight.Black, fontSize = 18.sp)
    }
}

/** Balão de fala com a dica de progressão de carga. */
@Composable
private fun SpeechTag(text: String) {
    val p = Zen.p
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            tr("Suba!"),
            color = Sumi,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            modifier = Modifier.clip(Zen.look.smallShape).background(Ki).border(Zen.look.thinBorder, p.border, Zen.look.smallShape).padding(horizontal = 6.dp, vertical = 3.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
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
    index: Int,
    accent: Color,
    draft: SetDraftState,
    logged: Boolean,
    showWeight: Boolean,
    onWeight: (String) -> Unit,
    onReps: (String) -> Unit,
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
        Box(
            Modifier.size(SideCell, CellHeight).clip(look.smallShape).background(indexBg).border(look.thinBorder, p.border, look.smallShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$index", color = if (logged) Sumi else p.ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
        if (showWeight) SetInput(draft.weight, onWeight, KeyboardType.Decimal, placeholder = "0", enabled = !logged, modifier = Modifier.weight(1f))
        SetInput(draft.reps, onReps, KeyboardType.Number, placeholder = "–", enabled = !logged, modifier = Modifier.weight(1f))
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
            Text(
                "✓",
                color = when {
                    logged -> Sumi
                    canLog -> p.ink
                    else -> p.muted.copy(alpha = 0.4f)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
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
