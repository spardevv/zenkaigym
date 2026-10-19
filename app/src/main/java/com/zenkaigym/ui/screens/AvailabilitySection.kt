package com.zenkaigym.ui.screens

import com.zenkaigym.domain.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenkaigym.data.entity.Availability
import com.zenkaigym.domain.TimeWindow
import com.zenkaigym.domain.WeeklySchedule
import com.zenkaigym.domain.availabilityOf
import com.zenkaigym.ui.components.SectionTitle
import com.zenkaigym.ui.components.TimeChip
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenCard
import com.zenkaigym.ui.components.ZenChip
import com.zenkaigym.ui.components.ZenTimePickerDialog
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Daidai
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Zen
import com.zenkaigym.util.WEEK_DAYS_SHORT
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onEach

private data class PickerTarget(val day: Int, val editingStart: Boolean)

/**
 * Dias de treino com início e fim por dia. Salva sozinho, logo depois de a pessoa parar de mexer
 * (e ao sair da tela), então não precisa de botão de salvar.
 */
@OptIn(FlowPreview::class)
@Composable
fun AvailabilitySection(
    windows: SnapshotStateMap<Int, TimeWindow>,
    onSave: (List<Availability>) -> Unit,
    entranceIndex: Int = 0
) {
    var picker by remember { mutableStateOf<PickerTarget?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var dirty by remember { mutableStateOf(false) }
    // Dias recém-marcados herdam o último horário escolhido, para marcar vários rapidamente.
    var lastWindow by remember { mutableStateOf(TimeWindow.DEFAULT) }
    val save by rememberUpdatedState {
        dirty = false
        onSave((1..7).map { availabilityOf(it, windows[it]) })
    }

    LaunchedEffect(windows) {
        snapshotFlow { windows.toMap() }
            .drop(1)
            .onEach { dirty = true }
            .debounce(900)
            .collect { save() }
    }
    DisposableEffect(windows) { onDispose { if (dirty) save() } }

    ZenCard(accent = Ki, entranceIndex = entranceIndex) {
        SectionTitle(tr("Dias e horários"), accent = Ki)
        Spacer(Modifier.height(4.dp))
        Text(tr("Marque os dias em que pode treinar e o horário de cada um. Salva sozinho."), color = Zen.p.muted)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Zen.week.days.forEach { day ->
                ZenChip(
                    WEEK_DAYS_SHORT[day - 1], windows.containsKey(day),
                    { if (windows.containsKey(day)) windows.remove(day) else windows[day] = lastWindow },
                    Modifier.weight(1f), accent = Ki, compact = true
                )
            }
        }

        val week = Zen.week
        val days = windows.keys.sortedBy { week.position(it) }
        if (days.isEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(tr("Nenhum dia marcado ainda."), color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
        }
        days.forEach { day ->
            val window = windows.getValue(day)
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    WEEK_DAYS_SHORT[day - 1].uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.width(52.dp)
                )
                TimeChip(window.start.format(), { picker = PickerTarget(day, true) }, Modifier.weight(1f), accent = Ki.copy(alpha = 0.5f))
                Text("→", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 8.dp))
                TimeChip(window.end.format(), { picker = PickerTarget(day, false) }, Modifier.weight(1f), accent = Daidai.copy(alpha = 0.5f))
            }
        }
        if (days.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tr("{0} {1} · {2}/semana", days.size, if (days.size == 1) tr("dia") else tr("dias"), WeeklySchedule.totalLabel(windows.toMap())),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                if (days.size > 1) {
                    ZenButton(tr("Copiar 1º p/ todos"), { windows.putAll(WeeklySchedule.copyToAll(windows.toMap(), days.first())) }, accent = Zen.p.card, small = true)
                }
            }
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Aka, style = MaterialTheme.typography.labelLarge)
        }
    }

    picker?.let { target ->
        val window = windows[target.day]
        if (window == null) {
            picker = null
        } else {
            ZenTimePickerDialog(
                title = (if (target.editingStart) tr("Início") else tr("Fim")) + " · " + WEEK_DAYS_SHORT[target.day - 1],
                initial = if (target.editingStart) window.start else window.end,
                onDismiss = { picker = null },
                onConfirm = { time ->
                    val updated = if (target.editingStart) window.withStart(time) else window.withEnd(time)
                    if (updated == null) {
                        error = tr("Horário inválido: o fim deve ser ao menos {0} min depois do início.", TimeWindow.MIN_DURATION_MINUTES)
                    } else {
                        error = null
                        windows[target.day] = updated
                        lastWindow = updated
                    }
                    picker = null
                }
            )
        }
    }
}
