package com.zenkaigym.ui.screens

import com.zenkaigym.ui.components.ZenIcon
import com.zenkaigym.ui.components.ZenIcons
import androidx.compose.foundation.layout.Box
import com.zenkaigym.ui.components.ZenFixedHeader
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.domain.DoneSet
import com.zenkaigym.domain.ExerciseProgress
import com.zenkaigym.domain.SessionSummary
import com.zenkaigym.domain.Cardio
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.domain.tr
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.components.KanjiBadge
import com.zenkaigym.ui.components.ScrollHints
import com.zenkaigym.ui.components.SectionTitle
import com.zenkaigym.ui.components.StatPill
import com.zenkaigym.ui.components.ZenBarChart
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenCard
import com.zenkaigym.ui.components.ZenHeader
import com.zenkaigym.ui.components.ZenPage
import com.zenkaigym.ui.components.currentWindowLayout
import com.zenkaigym.ui.theme.Daidai
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Zen
import com.zenkaigym.util.formatEpochDay

/** Últimas sessões desenhadas no gráfico; mais que isso fica ilegível numa tela de celular. */
private const val CHART_SESSIONS = 12

/** Exercícios já registrados no modo treino e, ao tocar num deles, a progressão ao longo do tempo. */
@Composable
fun HistoryScreen(viewModel: ZenViewModel, onBack: () -> Unit) {
    val exercises by viewModel.loggedExercises.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selected = exercises.firstOrNull { it.id == selectedId }

    // Voltar do sistema fecha primeiro o detalhe e só depois a tela.
    BackHandler(enabled = selected != null) { selectedId = null }

    ZenPage {
        if (selected == null) {
            ExerciseList(exercises, onBack, onSelect = { selectedId = it.id })
        } else {
            ExerciseDetail(selected, viewModel, onBack = { selectedId = null })
        }
    }
}

@Composable
private fun ExerciseList(exercises: List<Exercise>, onBack: () -> Unit, onSelect: (Exercise) -> Unit) {
    val listState = rememberLazyListState()
    Column(Modifier.fillMaxSize()) {
    ZenFixedHeader(
        tr("Histórico"), "歴", accent = Ki, subtitle = tr("Progressão por exercício"),
        back = { ZenButton(tr("Voltar ao perfil"), onBack, accent = Zen.p.card, small = true, leading = ZenIcons.ArrowLeft) }
    )
    Box(Modifier.weight(1f)) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = currentWindowLayout.gutter, end = currentWindowLayout.gutter, top = 4.dp, bottom = currentWindowLayout.gutter),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (exercises.isEmpty()) {
            item(key = "empty") {
                ZenCard(entranceIndex = 1) {
                    Text(tr("Nada registrado ainda."), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(tr("Registre séries no modo treino e a evolução de cada exercício aparece aqui."), color = Zen.p.muted)
                }
            }
        }
        items(exercises, key = { it.id }) { exercise ->
            ZenCard(
                Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) { onSelect(exercise) },
                entranceIndex = -1
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    KanjiBadge("体", color = Ki, size = 40.dp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(tr(exercise.name), style = MaterialTheme.typography.bodyLarge)
                        Text(exercise.group.label, color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
                    }
                    ZenIcon(ZenIcons.ChevronRight, size = 20.dp, color = Zen.p.muted)
                }
            }
        }
    }
    ScrollHints(listState.canScrollBackward, listState.canScrollForward, Modifier.fillMaxSize())
    }
    }
}

@Composable
private fun ExerciseDetail(exercise: Exercise, viewModel: ZenViewModel, onBack: () -> Unit) {
    val logs by remember(exercise.id) { viewModel.setsOf(exercise.id) }.collectAsStateWithLifecycle(emptyList())
    val progress = remember(logs) { ExerciseProgress.from(logs) }
    val byLoad = progress.byLoad
    val listState = rememberLazyListState()

    fun value(v: Double) = if (byLoad) "${kgLabel(v)} kg" else tr("{0} reps", v.toInt())

    Column(Modifier.fillMaxSize()) {
    ZenFixedHeader(
        tr(exercise.name), "歴", accent = Ki, subtitle = exercise.group.label,
        back = { ZenButton(tr("Voltar à lista"), onBack, accent = Zen.p.card, small = true, leading = ZenIcons.ArrowLeft) }
    )
    Box(Modifier.weight(1f)) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = currentWindowLayout.gutter, end = currentWindowLayout.gutter, top = 4.dp, bottom = currentWindowLayout.gutter),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (progress.sessions.isNotEmpty()) {
            item(key = "stats") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill(tr("Recorde"), value(progress.best), Modifier.weight(1f), color = Ki)
                    StatPill(tr("Sessões"), progress.sessions.size.toString(), Modifier.weight(1f), color = Midori)
                    StatPill(
                        tr("Variação"),
                        progress.change?.let { (if (it > 0) "+" else "") + value(it) } ?: "—",
                        Modifier.weight(1f), color = Daidai
                    )
                }
            }
            item(key = "chart") {
                val recent = progress.sessions.takeLast(CHART_SESSIONS)
                val trend = progress.trend.takeLast(CHART_SESSIONS)
                ZenCard(accent = Ki, entranceIndex = 1) {
                    SectionTitle(if (byLoad) tr("Carga máxima por sessão") else tr("Repetições por sessão"), accent = Ki)
                    Spacer(Modifier.height(10.dp))
                    ZenBarChart(
                        values = trend.map { it.toFloat() },
                        color = Ki,
                        describe = { i ->
                            if (byLoad) tr("{0} kg em {1}", kgLabel(trend[i]), formatEpochDay(recent[i].epochDay))
                            else tr("{0} reps em {1}", trend[i].toInt(), formatEpochDay(recent[i].epochDay))
                        },
                        startLabel = formatEpochDay(recent.first().epochDay),
                        endLabel = formatEpochDay(recent.last().epochDay)
                    )
                }
            }
            // Força estimada: junta carga e repetições num número só, então dá para comparar 5 × 100 kg com 10 × 85 kg.
            if (byLoad) item(key = "one-rep-max") {
                val recent = progress.sessions.takeLast(CHART_SESSIONS)
                val values = progress.oneRepMaxTrend.takeLast(CHART_SESSIONS)
                ZenCard(accent = Aka, entranceIndex = 2) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle(tr("1RM estimado"), Modifier.weight(1f), accent = Aka)
                        Text(tr("melhor: {0} kg", kgLabel(roundKg(progress.bestOneRepMax))), style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.height(10.dp))
                    ZenBarChart(
                        values = values.map { it.toFloat() },
                        color = Aka,
                        describe = { i -> tr("{0} kg em {1}", kgLabel(roundKg(values[i])), formatEpochDay(recent[i].epochDay)) },
                        startLabel = formatEpochDay(recent.first().epochDay),
                        endLabel = formatEpochDay(recent.last().epochDay)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tr("Carga máxima para 1 repetição, estimada pela fórmula de Epley (carga × (1 + reps ÷ 30)). Não é um teste: só mostra a tendência de força."),
                        color = Zen.p.muted, style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            item(key = "sessions-title") { SectionTitle(tr("Últimas sessões"), accent = Midori) }
            items(progress.sessions.asReversed(), key = { it.epochDay }) { session ->
                ZenCard(entranceIndex = -1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatEpochDay(session.epochDay), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        if (session.oneRepMax > 0) {
                            Text(tr("1RM ≈ {0} kg", kgLabel(roundKg(session.oneRepMax))), color = Zen.p.muted, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(setsLabel(session.sets), color = Zen.p.muted)
                    cardioLabel(session)?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(it, color = Zen.p.muted, style = MaterialTheme.typography.labelLarge)
                    }
                    session.notes.forEach { note ->
                        Spacer(Modifier.height(2.dp))
                        Text("✎ $note", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
    ScrollHints(listState.canScrollBackward, listState.canScrollForward, Modifier.fillMaxSize())
    }
    }
}

private fun kgLabel(kg: Double): String = if (kg % 1.0 == 0.0) kg.toLong().toString() else kg.toString()

/** Uma casa decimal basta para o 1RM estimado. */
private fun roundKg(kg: Double): Double = Math.round(kg * 10) / 10.0

/** "10 × 40 kg (RIR 2)  ·  8 × 42.5 kg". */
private fun setsLabel(sets: List<DoneSet>): String =
    sets.joinToString("  ·  ") {
        val base = if (it.weightKg > 0) "${it.reps} × ${kgLabel(it.weightKg)} kg" else tr("{0} reps", it.reps)
        base + (it.rir?.let { r -> " (RIR $r)" } ?: "")
    }

/** Cardio: distância, ritmo e calorias somadas da sessão (os minutos ficam nas "reps"). */
private fun cardioLabel(session: SessionSummary): String? {
    if (session.distanceKm == null && session.kcal == null) return null
    return listOfNotNull(
        session.distanceKm?.let { tr("{0} km", kgLabel(roundKg(it))) },
        Cardio.paceLabel(session.totalReps, session.distanceKm),
        session.kcal?.let { tr("≈ {0} kcal", it) }
    ).joinToString(" · ")
}
