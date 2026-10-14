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
import com.zenkaigym.ui.theme.Zen

@Composable
fun SettingsScreen(viewModel: ZenViewModel) {
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()

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
        }
    }
}
