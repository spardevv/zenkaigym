package com.zenkaigym.ui.screens

import com.zenkaigym.domain.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenkaigym.domain.AppLanguage
import com.zenkaigym.domain.ThemeMode
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.components.SectionTitle
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenCard
import com.zenkaigym.ui.components.ZenChip
import com.zenkaigym.ui.components.ZenHeader
import com.zenkaigym.ui.components.ZenPage
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.components.KanjiBadge
import com.zenkaigym.ui.components.ZenDialog
import com.zenkaigym.ui.components.ZenField
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import com.zenkaigym.ui.theme.Zen

@Composable
fun SettingsScreen(viewModel: ZenViewModel) {
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    var confirming by remember { mutableStateOf(false) }

    ZenPage {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ZenHeader(tr("Ajustes"), "設", accent = Ao, subtitle = tr("Aparência, idioma e informações"))

            ZenCard(accent = Ao, entranceIndex = 0) {
                SectionTitle(tr("Aparência"), accent = Ao)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach {
                        ZenChip(it.label, theme == it, { viewModel.setThemeMode(it) }, Modifier.weight(1f), accent = Ao)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (theme == ThemeMode.SYSTEM) tr("Segue o modo claro/escuro do seu celular.") else tr("Fixo em modo {0}.", theme.label.lowercase()),
                    color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
                )
            }

            ZenCard(accent = Ao, entranceIndex = 1) {
                SectionTitle(tr("Idioma"), accent = Ao)
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach {
                        ZenChip(
                            if (it == AppLanguage.SYSTEM) tr("Sistema") else it.nativeName,
                            language == it, { viewModel.setLanguage(it) }, Modifier.fillMaxWidth(), accent = Ao
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (language == AppLanguage.SYSTEM) tr("Segue o idioma do seu celular.") else tr("Idioma fixo: {0}.", language.nativeName),
                    color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
                )
            }

            ZenCard(accent = Ao, entranceIndex = 2) {
                SectionTitle(tr("Seus dados"), accent = Ao)
                Spacer(Modifier.height(6.dp))
                Text(
                    tr("Tudo fica salvo só neste aparelho, de forma local. Nada é enviado para a internet nem precisa de permissão de armazenamento."),
                    color = Zen.p.muted
                )
            }

            ZenButton(tr("Rever apresentação"), viewModel::replayIntro, accent = Zen.p.card, small = true)

            ZenCard(accent = Aka, entranceIndex = 3) {
                SectionTitle(tr("Zona de perigo"), accent = Aka)
                Spacer(Modifier.height(6.dp))
                Text(tr("Apaga perfil, pesos, fichas, dietas, alimentos cadastrados e todo o histórico de treinos e refeições."), color = Zen.p.muted)
                Spacer(Modifier.height(12.dp))
                ZenButton(tr("Apagar todos os dados"), { confirming = true }, accent = Aka, fill = true)
            }
        }
    }

    if (confirming) WipeDialog(onConfirm = { confirming = false; viewModel.wipeAllData() }, onDismiss = { confirming = false })
}

/**
 * Confirmação em duas etapas: o aviso explica o que se perde e o botão só libera
 * depois de digitar a palavra de confirmação, para não apagar tudo com um toque acidental.
 */
@Composable
private fun WipeDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val word = tr("APAGAR")
    var typed by remember { mutableStateOf("") }
    val ready = typed.trim().equals(word, ignoreCase = true)
    // Pequeno tremor no aviso ao abrir: chama atenção para a gravidade.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        listOf(8f, -8f, 5f, -5f, 0f).forEach { shake.animateTo(it, tween(55)) }
    }

    ZenDialog(
        tr("Apagar tudo?"), onDismiss, accent = Aka,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZenButton(tr("Apagar"), onConfirm, Modifier.weight(1f), accent = Aka, enabled = ready, fill = true)
                ZenButton(tr("Cancelar"), onDismiss, Modifier.weight(1f), accent = Zen.p.card, fill = true)
            }
        }
    ) {
        Row(Modifier.graphicsLayer { translationX = shake.value.dp.toPx() }, verticalAlignment = Alignment.CenterVertically) {
            KanjiBadge("危", color = Aka, size = 42.dp)
            Text(
                tr("Esta ação não pode ser desfeita."),
                Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(tr("Serão apagados: perfil e pesos, fichas de treino, séries registradas, dietas, alimentos e refeições marcadas. Tema e idioma continuam como estão."))
        Spacer(Modifier.height(14.dp))
        ZenField(tr("Digite {0} para confirmar", word), typed, { typed = it }, placeholder = word)
    }
}
