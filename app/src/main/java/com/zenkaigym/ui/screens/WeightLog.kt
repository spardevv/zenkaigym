package com.zenkaigym.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zenkaigym.data.entity.WeightEntry
import com.zenkaigym.domain.tr
import com.zenkaigym.ui.components.ConfirmDialog
import com.zenkaigym.ui.components.SectionTitle
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenCard
import com.zenkaigym.ui.components.ZenDialog
import com.zenkaigym.ui.components.ZenField
import com.zenkaigym.ui.components.ZenIcon
import com.zenkaigym.ui.components.ZenIcons
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Daidai
import com.zenkaigym.ui.theme.Zen
import com.zenkaigym.util.formatEpochDay
import com.zenkaigym.util.toDecimalOrNull

/** Registro do peso (aba Peso da Evolução): campo novo, últimos registros e janelas de editar e remover. */
@Composable
fun WeightLogCard(
    weights: List<WeightEntry>,
    onRegister: (Double) -> Unit,
    onEdit: (Long, Double) -> Unit,
    onRemove: (Long) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<WeightEntry?>(null) }
    var removing by remember { mutableStateOf<WeightEntry?>(null) }
    ZenCard(accent = Daidai, entranceIndex = 2) {
        SectionTitle(tr("Registros de peso"), accent = Daidai)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ZenField(tr("Novo peso (kg)"), input, { input = it }, Modifier.weight(1f), KeyboardType.Decimal)
            ZenButton(tr("Registrar"), {
                val kg = input.toDecimalOrNull()
                if (kg != null && kg in 30.0..300.0) {
                    onRegister(kg)
                    input = ""
                }
            }, accent = Daidai)
        }
        if (weights.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            weights.takeLast(8).asReversed().forEach { entry ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { editing = entry },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(formatEpochDay(entry.epochDay), Modifier.weight(1f), color = Zen.p.muted)
                    Text("%.1f kg".format(entry.weightKg), style = MaterialTheme.typography.bodyLarge)
                    ZenIcon(ZenIcons.Edit, Modifier.padding(start = 8.dp), size = 16.dp, color = Zen.p.muted)
                }
            }
        }
    }

    editing?.let { entry ->
        var text by remember(entry.id) { mutableStateOf("%.1f".format(entry.weightKg)) }
        val kg = text.toDecimalOrNull()
        val valid = kg != null && kg in 30.0..300.0
        ZenDialog(
            tr("Editar peso"), { editing = null }, accent = Daidai,
            footer = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZenButton(tr("Salvar"), { editing = null; onEdit(entry.id, kg!!) }, Modifier.weight(1f), accent = Daidai, enabled = valid, fill = true)
                    if (weights.size > 1) {
                        ZenButton(tr("Remover"), { removing = entry }, Modifier.weight(1f), accent = Aka, fill = true)
                    }
                }
            }
        ) {
            Text(formatEpochDay(entry.epochDay), color = Zen.p.muted)
            Spacer(Modifier.height(8.dp))
            ZenField(tr("Peso (kg)"), text, { text = it }, keyboardType = KeyboardType.Decimal)
        }
    }

    removing?.let { entry ->
        ConfirmDialog(
            tr("Remover peso?"), tr("O registro de {0} kg será apagado.", "%.1f".format(entry.weightKg)), tr("Remover"),
            onConfirm = { removing = null; editing = null; onRemove(entry.id) },
            onDismiss = { removing = null }
        )
    }
}
