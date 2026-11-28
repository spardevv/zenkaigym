package com.zenkaigym.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zenkaigym.domain.DaySummary
import com.zenkaigym.domain.LevelInfo
import com.zenkaigym.domain.tr
import com.zenkaigym.ui.components.SectionTitle
import com.zenkaigym.ui.components.StatTile
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenPage
import com.zenkaigym.ui.components.ZenProgressBar
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Daidai
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Zen
import kotlin.math.roundToInt

/**
 * Tela de conclusão do treino: carimbo, números do dia, XP e recordes.
 * [summary] e [level] chegam de fluxos que podem demorar uma emissão: enquanto nulos mostram "—".
 * O voltar do sistema equivale a [onBackToDojo].
 */
@Composable
fun TrainConclusion(
    dayName: String,
    summary: DaySummary?,
    level: LevelInfo?,
    onBackToDojo: () -> Unit,
    onUndo: () -> Unit
) {
    BackHandler(onBack = onBackToDojo)
    val p = Zen.p
    ZenPage {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HankoStamp(size = 96.dp)
            Spacer(Modifier.height(16.dp))
            Text(tr("Treino concluído"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(dayName, color = p.muted, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(tr("Séries"), summary?.sets ?: 0, Modifier.weight(1f), format = { if (summary == null) "—" else it.toString() })
                StatTile(tr("Volume"), summary?.volumeKg?.roundToInt() ?: 0, Modifier.weight(1f), format = { if (summary == null) "—" else volumeText(it) })
                StatTile(tr("Recordes"), summary?.records?.size ?: 0, Modifier.weight(1f), format = { if (summary == null) "—" else it.toString() })
            }
            Spacer(Modifier.height(20.dp))

            Text(
                summary?.let { tr("+{0} XP", it.xp) } ?: "—",
                color = p.ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(6.dp))
            ZenProgressBar(level?.fraction ?: 0f, Daidai)
            Spacer(Modifier.height(4.dp))
            Text(
                level?.let { tr("{0} / {1} XP para o nível {2}", it.xpIntoLevel, it.xpForNext, it.level + 1) } ?: "—",
                color = p.muted, style = MaterialTheme.typography.labelLarge
            )

            val records = summary?.records.orEmpty()
            if (records.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                SectionTitle(tr("Recordes de hoje"), Modifier.fillMaxWidth(), accent = Aka)
                Spacer(Modifier.height(8.dp))
                records.forEach { r ->
                    Text(
                        "${tr(r.name)} · ${kgLabel(r.previousKg)} → ${kgLabel(r.weightKg)} kg",
                        Modifier.fillMaxWidth().padding(vertical = 4.dp), fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            ZenButton(tr("Voltar ao Dojo"), onBackToDojo, Modifier.fillMaxWidth(), accent = Midori, fill = true)
            Spacer(Modifier.height(8.dp))
            ZenButton(tr("Desfazer conclusão"), onUndo, Modifier.fillMaxWidth(), accent = p.card, fill = true, small = true)
        }
    }
}

private fun kgLabel(kg: Double): String = if (kg % 1.0 == 0.0) kg.toLong().toString() else "%.1f".format(kg)
