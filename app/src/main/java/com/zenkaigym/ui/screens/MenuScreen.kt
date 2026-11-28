package com.zenkaigym.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenkaigym.domain.tr
import com.zenkaigym.domain.windowsByDay
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.components.HeaderBar
import com.zenkaigym.ui.components.KanjiBadge
import com.zenkaigym.ui.components.SectionTitle
import com.zenkaigym.ui.components.ZenCard
import com.zenkaigym.ui.components.ZenFixedHeader
import com.zenkaigym.ui.components.ZenPage
import com.zenkaigym.ui.components.ZenProgressBar
import com.zenkaigym.ui.components.currentWindowLayout
import com.zenkaigym.ui.nav.Routes
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Daidai
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Sakura
import com.zenkaigym.ui.theme.Zen

/** Um destino do menu: rota, kanji, título, cor própria e o dado atual que o quadro mostra. */
private class MenuEntry(
    val route: String, val kanji: String, val title: String, val accent: Color, val detail: String,
    /** Fração de 0 a 1 para a barra de XP (só a Evolução). */
    val bar: Float? = null
)

/** Tamanho do quadro: alto (Treino, Dieta), largo (Evolução) ou baixo (Perfil, Ajustes). */
private enum class TileSize { TALL, WIDE, LOW }

/** Tela cheia "Para onde agora?": os destinos agrupados em Plano, Progresso e Você, cada um com seu dado atual. */
@Composable
fun MenuScreen(viewModel: ZenViewModel, onOpen: (String) -> Unit, onClose: () -> Unit) {
    val gutter = currentWindowLayout.gutter
    val workout by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val availability by viewModel.availability.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val suggested by viewModel.suggestedTarget.collectAsStateWithLifecycle()
    val slots by viewModel.mealSlots.collectAsStateWithLifecycle()
    val stats by viewModel.progressStats.collectAsStateWithLifecycle()
    val style by viewModel.style.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val workoutLine = workout?.let {
        tr(it.plan.splitName) + " · " + tr("{0} dias de treino", availability.windowsByDay().size)
    } ?: tr("Sem ficha ainda")
    val kcal = profile?.targetKcal ?: suggested?.kcal
    val dietLine = kcal?.let { tr("{0} kcal · {1} refeições", it, slots.size) } ?: tr("Sem meta ainda")
    val level = stats?.level
    val levelLine = level?.let { tr("Nv {0} · {1} / {2} XP", it.level, it.xpIntoLevel, it.xpForNext) } ?: "—"
    val profileLine = profile?.let { "%.1f kg".format(it.weightKg) + " · " + it.goal.label } ?: tr("Sem perfil ainda")

    ZenPage {
        Column(Modifier.fillMaxSize()) {
            ZenFixedHeader(
                tr("Menu"), "門", subtitle = tr("Para onde agora?"),
                back = { HeaderBar(onClose, backLabel = tr("Fechar")) }
            )
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(start = gutter, end = gutter, top = 4.dp, bottom = gutter),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionTitle(tr("Plano"))
                // Altura igual nos dois quadros da linha; com letra grande eles crescem juntos.
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MenuTile(MenuEntry(Routes.WORKOUT, "体", tr("Treino"), Ki, workoutLine), TileSize.TALL, onOpen, Modifier.weight(1f).fillMaxHeight())
                    MenuTile(MenuEntry(Routes.DIET, "食", tr("Dieta"), Midori, dietLine), TileSize.TALL, onOpen, Modifier.weight(1f).fillMaxHeight())
                }
                SectionTitle(tr("Progresso"))
                MenuTile(
                    MenuEntry(Routes.PROGRESS, "進", tr("Evolução"), Daidai, levelLine, bar = level?.fraction),
                    TileSize.WIDE, onOpen, Modifier.fillMaxWidth()
                )
                SectionTitle(tr("Você"))
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MenuTile(MenuEntry(Routes.PROFILE, "己", tr("Perfil"), Sakura, profileLine), TileSize.LOW, onOpen, Modifier.weight(1f).fillMaxHeight())
                    MenuTile(
                        MenuEntry(Routes.SETTINGS, "設", tr("Ajustes"), Ao, style.label + " · " + themeMode.label),
                        TileSize.LOW, onOpen, Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

/** Um quadro do menu. Os grandes levam o fundo decorativo do estilo; o toque segue o formato do cartão. */
@Composable
private fun MenuTile(entry: MenuEntry, size: TileSize, onOpen: (String) -> Unit, modifier: Modifier) {
    val detail: @Composable () -> Unit = {
        Text(entry.detail, style = MaterialTheme.typography.bodySmall, color = Zen.p.muted)
    }
    ZenCard(modifier, accent = entry.accent, onClick = { onOpen(entry.route) }, backdrop = size != TileSize.LOW, fillHeight = size != TileSize.WIDE) {
        when (size) {
            TileSize.TALL -> Column(Modifier.fillMaxHeight().heightIn(min = 110.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KanjiBadge(entry.kanji, color = entry.accent, size = 44.dp)
                Spacer(Modifier.weight(1f))
                Text(entry.title.uppercase(), style = MaterialTheme.typography.titleMedium)
                detail()
            }
            TileSize.WIDE -> Row(verticalAlignment = Alignment.CenterVertically) {
                KanjiBadge(entry.kanji, color = entry.accent, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(entry.title.uppercase(), style = MaterialTheme.typography.titleMedium)
                    detail()
                    entry.bar?.let { ZenProgressBar(it, entry.accent) }
                }
            }
            // Empilhado (kanji, título, dado): com letra grande o título não quebra no meio da palavra.
            TileSize.LOW -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KanjiBadge(entry.kanji, color = entry.accent, size = 36.dp)
                Text(entry.title.uppercase(), style = MaterialTheme.typography.titleMedium)
                detail()
            }
        }
    }
}
