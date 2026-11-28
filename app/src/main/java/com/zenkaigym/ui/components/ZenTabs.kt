package com.zenkaigym.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zenkaigym.ui.theme.Zen

/** Uma aba da faixa: rótulo (já traduzido), cor do assunto e ícone opcional. */
data class ZenTab(val label: String, val accent: Color, val icon: ImageVector? = null)

/**
 * Faixa de abas com rolagem horizontal: a aba escolhida sempre fica à vista (ao abrir pelo botão
 * "Ver conquistas", por exemplo) e um esmaecido na borda mostra que há mais abas para o lado.
 * Ocupa a largura toda e usa a margem lateral da tela (gutter) como respiro nas pontas.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZenTabs(tabs: List<ZenTab>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val gutter = currentWindowLayout.gutter
    val scroll = rememberScrollState()
    Box(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(scroll).padding(horizontal = gutter),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEachIndexed { i, t ->
                val requester = remember { BringIntoViewRequester() }
                LaunchedEffect(selected) { if (i == selected) requester.bringIntoView() }
                ZenChip(
                    t.label, i == selected, { onSelect(i) }, Modifier.bringIntoViewRequester(requester),
                    accent = t.accent, mark = t.icon
                )
            }
        }
        // Esmaecidos nas bordas. A camada acompanha o tamanho da faixa (matchParentSize): um fillMaxHeight solto
        // aqui ocuparia a tela toda e deixaria o conteúdo da aba com altura zero.
        val bg = Zen.p.bg
        Box(Modifier.matchParentSize()) {
            if (scroll.canScrollBackward) {
                Box(
                    Modifier.align(Alignment.CenterStart).fillMaxHeight().width(gutter + 14.dp)
                        .background(Brush.horizontalGradient(listOf(bg, Color.Transparent)))
                )
            }
            if (scroll.canScrollForward) {
                Box(
                    Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(gutter + 22.dp)
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, bg)))
                )
            }
        }
    }
}
