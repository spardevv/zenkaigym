package com.zenkaigym.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenkaigym.data.entity.WorkoutPlanFull
import com.zenkaigym.domain.tr
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenChip
import com.zenkaigym.ui.components.ZenDialog
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Zen
import com.zenkaigym.util.WEEK_DAYS_SHORT

/**
 * Escolhe outra sessão da ficha para treinar hoje. O resto da semana segue a sequência do plano a partir
 * da escolhida; antes de confirmar, mostra como ficam os próximos dias.
 */
@Composable
fun ChangeWorkoutDialog(
    plan: WorkoutPlanFull,
    current: Long,
    swapped: Boolean,
    preview: (Long) -> List<String>,
    onConfirm: (Long) -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit
) {
    var picked by remember { mutableStateOf(current) }
    val sessions = plan.days.sortedBy { it.day.dayOfWeek }

    ZenDialog(tr("Trocar treino de hoje"), onDismiss, accent = Aka, footerReserve = if (swapped) 170 else 130, footer = {
        ZenButton(
            tr("Treinar este hoje"), { onConfirm(picked) }, accent = Aka, fill = true,
            enabled = picked != current
        )
        if (swapped) {
            Spacer(Modifier.height(8.dp))
            ZenButton(tr("Voltar ao plano original"), onRestore, accent = Zen.p.card, fill = true, small = true)
        }
        Spacer(Modifier.height(8.dp))
        ZenButton(tr("Fechar"), onDismiss, accent = Zen.p.card, fill = true, small = true)
    }) {
        Text(
            tr("Escolha o treino de hoje. Os outros dias seguem a sequência da ficha a partir dele."),
            color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(10.dp))
        sessions.forEach { s ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ZenChip(
                    tr(s.day.name) + " · " + WEEK_DAYS_SHORT[s.day.dayOfWeek - 1],
                    picked == s.day.id, { picked = s.day.id }, Modifier.weight(1f), accent = Aka, compact = true
                )
            }
            Spacer(Modifier.height(6.dp))
        }
        if (picked != current) {
            Spacer(Modifier.height(6.dp))
            Text(tr("PRÓXIMOS DIAS"), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
            val lines = preview(picked)
            if (lines.isEmpty()) {
                Text(tr("Sem mais treinos nesta semana."), color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
            }
            lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
