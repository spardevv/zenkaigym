package com.zenkaigym.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenkaigym.data.ExerciseCatalog
import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.domain.tr
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.components.MiniButton
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenDialog
import com.zenkaigym.ui.components.ZenField
import com.zenkaigym.ui.components.ZenIcons
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Zen

/** Exercícios cadastrados pelo usuário (aba Exercícios do Treino): renomear ou excluir, com confirmação. */
@Composable
fun MyExercisesList(exercises: List<Exercise>, viewModel: ZenViewModel) {
    val catalogNames = remember { ExerciseCatalog.entries.map { it.name }.toSet() }
    val mine = remember(exercises) { exercises.filter { it.name !in catalogNames } }
    var renaming by remember { mutableStateOf<Exercise?>(null) }
    var deleting by remember { mutableStateOf<Exercise?>(null) }

    Column {
        if (mine.isEmpty()) {
            Text(tr("Você ainda não criou nenhum exercício. Crie um ao trocar ou adicionar exercícios na ficha."), color = Zen.p.muted)
        }
        mine.forEach { exercise ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(exercise.name, style = MaterialTheme.typography.bodyLarge)
                    Text(exercise.group.label, color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
                }
                MiniButton(ZenIcons.Edit, { renaming = exercise }, description = tr("Renomear"))
                Spacer(Modifier.width(4.dp))
                MiniButton(ZenIcons.Close, { deleting = exercise }, color = Aka)
            }
        }
    }

    renaming?.let { exercise ->
        var name by remember(exercise.id) { mutableStateOf(exercise.name) }
        ZenDialog(tr("Renomear exercício"), { renaming = null }, accent = Ki, footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZenButton(tr("Salvar"), { renaming = null; viewModel.renameExercise(exercise.id, name) }, Modifier.weight(1f), accent = Ki, enabled = name.isNotBlank(), fill = true)
                ZenButton(tr("Cancelar"), { renaming = null }, Modifier.weight(1f), accent = Zen.p.card, fill = true)
            }
        }) {
            ZenField(tr("Nome"), name, { name = it })
        }
    }

    deleting?.let { exercise ->
        ZenDialog(tr("Excluir exercício?"), { deleting = null }, accent = Aka, footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZenButton(tr("Excluir"), { deleting = null; viewModel.deleteExercise(exercise.id) }, Modifier.weight(1f), accent = Aka, fill = true)
                ZenButton(tr("Cancelar"), { deleting = null }, Modifier.weight(1f), accent = Zen.p.card, fill = true)
            }
        }) {
            Text(tr("{0} e todo o histórico de cargas dele serão apagados. Isso não pode ser desfeito.", exercise.name), color = Zen.p.muted)
        }
    }
}
