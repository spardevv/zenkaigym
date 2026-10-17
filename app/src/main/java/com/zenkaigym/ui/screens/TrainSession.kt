package com.zenkaigym.ui.screens

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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
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
private val CellBorder = 2.dp

/** Texto de carga sem zeros inúteis: 22.0 -> "22", 22.5 -> "22.5". */
private fun kgText(kg: Double): String = if (kg % 1.0 == 0.0) kg.toLong().toString() else kg.toString()

private data class SetDraftState(val weight: String, val reps: String)

/** Série registrada como texto dos campos (carga zero = campo vazio). */
private fun SetLog.asDraft() = SetDraftState(if (weightKg > 0) kgText(weightKg) else "", reps.toString())

/** Kanji e cor de cada grupo muscular: dão identidade a cada painel do treino. */
private fun MuscleGroup.kanji(): String = when (this) {
    MuscleGroup.CHEST -> "胸"
    MuscleGroup.BACK -> "背"
    MuscleGroup.LEGS -> "脚"
    MuscleGroup.SHOULDERS -> "肩"
    MuscleGroup.BICEPS -> "腕"
    MuscleGroup.TRICEPS -> "腕"
    MuscleGroup.CORE -> "腹"
}

private fun MuscleGroup.color(): Color = when (this) {
    MuscleGroup.CHEST -> Aka
    MuscleGroup.BACK -> Ao
    MuscleGroup.LEGS -> Ki
    MuscleGroup.SHOULDERS -> Daidai
    MuscleGroup.BICEPS, MuscleGroup.TRICEPS -> Sakura
    MuscleGroup.CORE -> Midori
}

/** Registro do treino de hoje: carga e repetições de cada série, já preenchidas com a última vez. */
@Composable
fun TrainSessionScreen(day: WorkoutDayFull, viewModel: ZenViewModel, onClose: () -> Unit) {
    val logged by viewModel.setsToday.collectAsStateWithLifecycle()
    val done by viewModel.workoutDone.collectAsStateWithLifecycle()
    val items = remember(day) { day.items.sortedBy { it.item.position } }
    // Rascunho por (exercício, série): começa com os mesmos kg e reps do treino anterior e é editável.
    val drafts = remember(day) { mutableStateMapOf<Pair<Long, Int>, SetDraftState>() }
    val hints = remember(day) { mutableStateMapOf<Long, String>() }

    LaunchedEffect(day) {
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

    // Tela acesa enquanto o modo treino estiver aberto (se o usuário quiser).
    val keepOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val view = LocalView.current
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }

    val p = Zen.p
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    // Posição (no conteúdo rolável) do painel de cada exercício, para rolar até a próxima série.
    val panelY = remember(day) { mutableStateMapOf<Long, Int>() }

    // Séries pendentes na ordem da ficha.
    val pending = items.flatMap { e -> (1..e.item.sets).map { e.exercise.id to it } }.filter { it !in loggedKeys }
    val next = pending.firstOrNull()

    // Com todas as séries feitas não há mais descanso: o cronômetro volta ao estado inicial.
    LaunchedEffect(allDone) { if (allDone) viewModel.stopRest() }

    /** Registra a série com o rascunho atual e já inicia o descanso. Devolve false se faltam repetições. */
    fun logDraft(exerciseId: Long, setIndex: Int, draft: SetDraftState): Boolean {
        val reps = draft.reps.trim().toIntOrNull()?.takeIf { it > 0 } ?: return false
        val kg = draft.weight.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
        viewModel.logSet(exerciseId, setIndex, reps, kg)
        // A última série pendente fecha o treino: nada de descanso depois dela.
        val isLast = pending.size == 1 && pending.first() == (exerciseId to setIndex)
        if (!isLast) viewModel.startRest(Progression.restSeconds(reps))
        return true
    }

    Column(Modifier.fillMaxSize().background(p.bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
        TrainHeader(tr(day.day.name), onClose)
        Box(Modifier.weight(1f)) {
            Column(Modifier.verticalScroll(scroll).padding(16.dp)) {
                TrainContent(
                    items, loggedKeys, doneSets, totalSets, hints,
                    draftOf = { key -> loggedByKey[key]?.asDraft() ?: drafts[key] ?: SetDraftState("", "") },
                    onDraft = { key, d -> drafts[key] = d },
                    onPanelY = { id, y -> panelY[id] = y },
                    onUnlog = { id, i ->
                        // Ao desfazer, os campos ficam com o que tinha sido registrado, para só corrigir.
                        loggedByKey[id to i]?.let { drafts[id to i] = it.asDraft() }
                        viewModel.unlogSet(id, i)
                    },
                    onLog = { id, i, d -> logDraft(id, i, d) }
                )
            }
            ScrollHints(scroll.canScrollBackward, scroll.canScrollForward, Modifier.matchParentSize())
        }
        TrainFooter(
            viewModel = viewModel,
            done = done,
            allDone = allDone,
            keepOn = keepOn,
            nextLabel = next?.let { (id, i) ->
                val name = items.firstOrNull { it.exercise.id == id }?.exercise?.name.orEmpty()
                tr("{0} · série {1}", tr(name), i)
            },
            onNext = {
                val target = next ?: return@TrainFooter
                if (logDraft(target.first, target.second, drafts[target] ?: SetDraftState("", ""))) {
                    // Rola até o exercício da série seguinte.
                    val after = pending.getOrNull(1) ?: return@TrainFooter
                    panelY[after.first]?.let { y -> scope.launch { scroll.animateScrollTo(y) } }
                }
            },
            onClose = onClose
        )
    }
}

/**
 * Rodapé fixo do modo treino. Linha de ferramentas (descanso, próxima série, tela acesa)
 * e linha de ações (concluir/desfazer e sair).
 */
@Composable
private fun TrainFooter(
    viewModel: ZenViewModel,
    done: Boolean,
    allDone: Boolean,
    keepOn: Boolean,
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
                ToolButton(
                    top = tr("PRÓXIMA SÉRIE"),
                    main = nextLabel ?: tr("Tudo feito!"),
                    onClick = onNext,
                    modifier = Modifier.weight(1.4f),
                    accent = Midori,
                    enabled = nextLabel != null
                )
                // Tela acesa: amarelo sólido quando ligada; rótulo curto (ON/OFF) para caber em qualquer idioma.
                ToolButton(
                    top = if (keepOn) "ON" else "OFF",
                    main = "",
                    icon = if (keepOn) Icons.Filled.LightMode else Icons.Outlined.DarkMode,
                    iconDescription = if (keepOn) tr("Tela acesa ligada") else tr("Tela acesa desligada"),
                    onClick = viewModel::toggleKeepScreenOn,
                    modifier = Modifier.width(56.dp),
                    accent = Ki,
                    filled = keepOn
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        accent = if (allDone) Midori else Aka, fill = true
                    )
                }
                ZenButton(tr("Sair"), onClose, Modifier.weight(1f), accent = p.card, fill = true)
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
            .drawBehind { drawRect(p.shadow, topLeft = Offset(3.dp.toPx(), 3.dp.toPx()), size = size) }
            .pressScale(interaction)
            .height(52.dp)
            .background(bg)
            .drawBehind {
                if (progress != null) drawRect(accent, size = size.copy(width = size.width * progress.coerceIn(0f, 1f)))
            }
            .border(CellBorder, p.border)
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
                Text(dayName.uppercase(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
            }
            KanjiBadge("体", color = Ki, size = 40.dp)
            Spacer(Modifier.width(10.dp))
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .size(40.dp)
                    .pressScale(interaction)
                    .background(p.bg)
                    .border(CellBorder, p.border)
                    .clickable(interaction, null, onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text("✕", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            }
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(p.border))
    }
}

@Composable
private fun TrainContent(
    items: List<WorkoutItemFull>,
    loggedKeys: Set<Pair<Long, Int>>,
    doneSets: Int,
    totalSets: Int,
    hints: Map<Long, String>,
    draftOf: (Pair<Long, Int>) -> SetDraftState,
    onDraft: (Pair<Long, Int>, SetDraftState) -> Unit,
    onPanelY: (Long, Int) -> Unit,
    onUnlog: (Long, Int) -> Unit,
    onLog: (Long, Int, SetDraftState) -> Unit
) {
    val parts = items.map { e -> (1..e.item.sets).count { (e.exercise.id to it) in loggedKeys }.toFloat() / e.item.sets.coerceAtLeast(1) }
    PowerGauge(doneSets, totalSets, parts)
    Spacer(Modifier.height(16.dp))
    items.forEachIndexed { index, entry ->
        ExercisePanel(
            entry = entry,
            entranceIndex = index + 1,
            hint = hints[entry.exercise.id],
            draftOf = { draftOf(entry.exercise.id to it) },
            isLogged = { (entry.exercise.id to it) in loggedKeys },
            onDraft = { i, d -> onDraft(entry.exercise.id to i, d) },
            onToggle = { i, d, wasLogged ->
                if (wasLogged) onUnlog(entry.exercise.id, i) else onLog(entry.exercise.id, i, d)
            },
            modifier = Modifier.onGloballyPositioned { onPanelY(entry.exercise.id, it.positionInParent().y.toInt()) }
        )
        if (index < items.lastIndex) Spacer(Modifier.height(14.dp))
    }
}

/**
 * Medidor de "energia" do treino, estilo barra de poder de mangá: um segmento por exercício,
 * listrado na diagonal quando cheio, com o kanji e a frase de ânimo mudando conforme o progresso.
 */
@Composable
private fun PowerGauge(done: Int, total: Int, parts: List<Float>) {
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
        Text("$percent%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth().height(14.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        parts.forEach { part ->
            val fill by animateFloatAsState(part, spring(stiffness = Spring.StiffnessMediumLow), label = "segment")
            Box(
                Modifier
                    .weight(1f)
                    .height(14.dp)
                    .background(p.bg)
                    .border(CellBorder, p.border)
                    .drawBehind {
                        if (fill <= 0f) return@drawBehind
                        val w = size.width * fill
                        drawRect(color, size = size.copy(width = w))
                        // Listras diagonais (hachura de mangá) sobre a parte cheia.
                        clipRect(right = w) {
                            val step = 6.dp.toPx()
                            var x = -size.height
                            while (x < size.width) {
                                drawLine(Sumi.copy(alpha = 0.18f), Offset(x, size.height), Offset(x + size.height, 0f), 2.dp.toPx())
                                x += step
                            }
                        }
                    }
            )
        }
    }
}

@Composable
private fun ExercisePanel(
    entry: WorkoutItemFull,
    entranceIndex: Int,
    hint: String?,
    draftOf: (Int) -> SetDraftState,
    isLogged: (Int) -> Boolean,
    onDraft: (Int, SetDraftState) -> Unit,
    onToggle: (Int, SetDraftState, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val group = entry.exercise.group
    val accent = group.color()
    val sets = entry.item.sets
    val doneCount = (1..sets).count(isLogged)
    val complete = doneCount == sets

    ZenCard(modifier, accent = accent, entranceIndex = entranceIndex) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KanjiBadge(group.kanji(), color = accent, size = 38.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tr(entry.exercise.name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(tr("{0} séries × {1}", sets, entry.item.reps), color = Zen.p.muted, style = MaterialTheme.typography.labelLarge)
            }
            Box(contentAlignment = Alignment.Center) {
                if (complete) HankoStamp() else SetCounter(doneCount, sets)
            }
        }
        hint?.let {
            Spacer(Modifier.height(8.dp))
            SpeechTag(it)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ColumnLabel(tr("Série"), Modifier.width(SideCell))
            ColumnLabel(tr("kg"), Modifier.weight(1f))
            ColumnLabel(tr("reps"), Modifier.weight(1f))
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
                onWeight = { onDraft(i, draft.copy(weight = it)) },
                onReps = { onDraft(i, draft.copy(reps = it)) },
                onToggle = { onToggle(i, draft, logged) }
            )
            if (i < sets) Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun SetCounter(done: Int, total: Int) {
    Text(
        "$done/$total",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.background(Zen.p.bg).border(CellBorder, Zen.p.border).padding(horizontal = 8.dp, vertical = 3.dp)
    )
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
        Text("完", color = Aka, fontWeight = FontWeight.Black, fontSize = 18.sp)
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
            modifier = Modifier.background(Ki).border(CellBorder, p.border).padding(horizontal = 6.dp, vertical = 3.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .padding(start = 4.dp)
                .background(p.bg)
                .border(CellBorder, p.border)
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
    onWeight: (String) -> Unit,
    onReps: (String) -> Unit,
    onToggle: () -> Unit
) {
    val p = Zen.p
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
            Modifier.size(SideCell, CellHeight).background(indexBg).border(CellBorder, p.border),
            contentAlignment = Alignment.Center
        ) {
            Text("$index", color = if (logged) Sumi else p.ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
        SetInput(draft.weight, onWeight, KeyboardType.Decimal, placeholder = "0", enabled = !logged, modifier = Modifier.weight(1f))
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
                .background(checkBg)
                .border(CellBorder, p.border)
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
        modifier = modifier.height(CellHeight).background(p.bg).border(CellBorder, if (enabled) p.border else p.muted.copy(alpha = 0.5f)),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth().padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                if (value.isEmpty()) Text(placeholder, color = p.muted, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                inner()
            }
        }
    )
}
