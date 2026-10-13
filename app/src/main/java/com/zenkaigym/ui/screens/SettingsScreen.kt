package com.zenkaigym.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.zenkaigym.domain.ThemeMode
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.components.SectionTitle
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenCard
import com.zenkaigym.ui.components.ZenChip
import com.zenkaigym.ui.components.ZenHeader
import com.zenkaigym.ui.components.ZenPage
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Zen

@Composable
fun SettingsScreen(viewModel: ZenViewModel) {
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()

    ZenPage {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ZenHeader("Ajustes", "設", accent = Ao, subtitle = "Aparência e informações")

            ZenCard(accent = Ao, entranceIndex = 0) {
                SectionTitle("Aparência", accent = Ao)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach {
                        ZenChip(it.label, theme == it, { viewModel.setThemeMode(it) }, Modifier.weight(1f), accent = Ao)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (theme == ThemeMode.SYSTEM) "Segue o modo claro/escuro do seu celular." else "Fixo em modo ${theme.label.lowercase()}.",
                    color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
                )
            }

            ZenCard(accent = Ki, entranceIndex = 1) {
                SectionTitle("Seus dados", accent = Ki)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tudo fica salvo só neste aparelho, de forma local. Nada é enviado para a internet nem precisa de permissão de armazenamento.",
                    color = Zen.p.muted
                )
            }

            ZenButton("Rever apresentação", viewModel::replayIntro, accent = Zen.p.card, small = true)
        }
    }
}
