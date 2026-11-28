package com.zenkaigym.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zenkaigym.domain.tr
import com.zenkaigym.ui.theme.Zen

/**
 * Barra de cima das telas internas: botão "← Dojo" (ou outro [backLabel]) à esquerda e [actions] à direita.
 * Entra no parâmetro `back` do [ZenFixedHeader].
 */
@Composable
fun HeaderBar(
    onBack: () -> Unit,
    backLabel: String = tr("Dojo"),
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ZenButton(backLabel, onBack, accent = Zen.p.card, small = true, leading = ZenIcons.ArrowLeft)
        Spacer(Modifier.weight(1f))
        actions()
    }
}

/** Botão só com o ícone de três pontos; a [description] serve ao leitor de tela. */
@Composable
fun MoreButton(onClick: () -> Unit, description: String = tr("Mais opções")) {
    ZenButton("", onClick, accent = Zen.p.card, small = true, leading = ZenIcons.More, modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description })
}
