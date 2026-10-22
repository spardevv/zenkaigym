package com.zenkaigym.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zenkaigym.data.entity.CardioPref
import com.zenkaigym.data.entity.Equipment
import com.zenkaigym.data.entity.Level
import com.zenkaigym.data.entity.TrainingFocus
import com.zenkaigym.domain.tr
import com.zenkaigym.ui.components.ZenChip
import com.zenkaigym.ui.theme.Zen

/** Escolhas que orientam a ficha: nível, cardio, foco muscular e equipamento. */
data class TrainingChoices(
    val level: Level = Level.INTERMEDIATE,
    val cardio: CardioPref = CardioPref.NONE,
    val focus: TrainingFocus = TrainingFocus.BALANCED,
    val equipment: Equipment = Equipment.GYM
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrainingPrefs(choices: TrainingChoices, onChange: (TrainingChoices) -> Unit, accent: Color) {
    @Composable
    fun <T> Group(title: String, options: List<T>, selected: T, label: (T) -> String, pick: (T) -> Unit) {
        Text(title.uppercase(), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { ZenChip(label(it), selected == it, { pick(it) }, accent = accent) }
        }
        Spacer(Modifier.height(12.dp))
    }
    Group(tr("Nível"), Level.entries, choices.level, { it.label }) { onChange(choices.copy(level = it)) }
    Group(tr("Cardio"), CardioPref.entries, choices.cardio, { it.label }) { onChange(choices.copy(cardio = it)) }
    Group(tr("Foco muscular"), TrainingFocus.entries, choices.focus, { it.label }) { onChange(choices.copy(focus = it)) }
    Group(tr("Equipamento"), Equipment.entries, choices.equipment, { it.label }) { onChange(choices.copy(equipment = it)) }
}
