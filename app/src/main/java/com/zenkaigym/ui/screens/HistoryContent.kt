package com.zenkaigym.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.domain.Cardio
import com.zenkaigym.domain.DoneSet
import com.zenkaigym.domain.ExerciseProgress
import com.zenkaigym.domain.SessionSummary
import com.zenkaigym.domain.tr
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.components.HeaderBar
import com.zenkaigym.ui.components.KanjiBadge
import com.zenkaigym.ui.components.SectionTitle
import com.zenkaigym.ui.components.StatPill
import com.zenkaigym.ui.components.ZenBarChart
import com.zenkaigym.ui.components.ZenCard
import com.zenkaigym.ui.components.ZenIcon
import com.zenkaigym.ui.components.ZenIcons
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Daidai
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Zen
import com.zenkaigym.util.formatEpochDay

/** Últimas sessões desenhadas no gráfico; mais que isso fica ilegível numa tela de celular. */
private const val CHART_SESSIONS = 12

/**
 * Exercícios já registrados no modo treino. Vive dentro da aba Treino da Evolução, que já rola na vertical:
 * por isso é uma [Column] comum, sem lista preguiçosa nem página própria.
 */
@Composable
fun HistoryList(exercises: List<Exercise>, onSelect: (Exercise) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (exercises.isEmpty()) {
            ZenCard(entranceIndex = 1) {
                Text(tr("Nada registrado ainda."), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(tr("Registre séries no modo treino e a evolução de cada exercício aparece aqui."), color = Zen.p.muted)
            }
        }
        exercises.forEach { exercise ->
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
}

/** Progressão de um exercício ao longo do tempo: recorde, gráficos e as últimas sessões. */
@Composable
fun HistoryDetail(exercise: Exercise, viewModel: ZenViewModel, onBack: () -> Unit) {
    val logs by remember(exercise.id) { viewModel.setsOf(exercise.id) }.collectAsStateWithLifecycle(emptyList())
    val progress = remember(logs) { ExerciseProgress.from(logs) }
    val byLoad = progress.byLoad

    fun value(v: Double) = if (byLoad) "${kgLabel(v)} kg" else tr("{0} reps", v.toInt())

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HeaderBar(onBack, backLabel = tr("Treino"))
        Column {
            Text(tr(exercise.name), style = MaterialTheme.typography.headlineSmall)
            Text(exercise.group.label, color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
        }
    if (progress.sessions.isNotEmpty()) {
        run {
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
        run {
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
        if (byLoad) run {
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
        SectionTitle(tr("Últimas sessões"), accent = Midori)
        progress.sessions.asReversed().forEach { session ->
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
