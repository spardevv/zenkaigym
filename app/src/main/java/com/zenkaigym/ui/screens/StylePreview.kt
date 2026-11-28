package com.zenkaigym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenkaigym.domain.AppStyle
import com.zenkaigym.domain.ThemeMode
import com.zenkaigym.domain.tr
import com.zenkaigym.ui.components.SectionTitle
import com.zenkaigym.ui.components.StyleBackdrop
import com.zenkaigym.ui.components.ZenCard
import com.zenkaigym.ui.components.ZenChip
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.LocalZenLook
import com.zenkaigym.ui.theme.LocalZenPalette
import com.zenkaigym.ui.theme.Sakura
import com.zenkaigym.ui.theme.Zen
import com.zenkaigym.ui.theme.lookFor
import com.zenkaigym.ui.theme.paletteFor
import com.zenkaigym.ui.theme.previewAccent

/**
 * Cartão "Estilo" de Ajustes: três miniaturas ao vivo (Shōnen, Kawaii, Isekai) e os chips Claro/Escuro/Sistema.
 * As miniaturas só trocam paleta e formas locais; nunca o tema inteiro, para o resto do app só mudar ao tocar.
 */
@Composable
fun StyleCard(
    style: AppStyle,
    theme: ThemeMode,
    dark: Boolean,
    onStyle: (AppStyle) -> Unit,
    onTheme: (ThemeMode) -> Unit
) {
    ZenCard(accent = Sakura, entranceIndex = 0) {
        SectionTitle(tr("Estilo"), accent = Sakura)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppStyle.entries.forEach {
                StyleMiniature(it, selected = style == it, dark = dark, onClick = { onStyle(it) }, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            when (style) {
                AppStyle.SHONEN -> tr("Mangá de ação: bordas grossas, cores vivas e linhas de velocidade.")
                AppStyle.KAWAII -> tr("Fofo: tons pastel de rosa, lilás, roxo e azul-bebê, cantos redondos e brilhos.")
                AppStyle.ISEKAI -> tr("Interface de jogo: azul-noite com neon, cantos chanfrados e grade holográfica.")
            },
            color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM).forEach {
                ZenChip(it.label, theme == it, { onTheme(it) }, Modifier.weight(1f), accent = Ao)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (theme == ThemeMode.SYSTEM) tr("Segue o modo claro/escuro do seu celular.") else tr("Fixo em modo {0}.", theme.label.lowercase()),
            color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** Uma miniatura: fundo do estilo, faixa de destaque, mini cartão e o nome. Sem [com.zenkaigym.ui.theme.ZenkaigymTheme]. */
@Composable
private fun StyleMiniature(style: AppStyle, selected: Boolean, dark: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = paletteFor(style, dark)
    val look = lookFor(style)
    CompositionLocalProvider(LocalZenPalette provides palette, LocalZenLook provides look) {
        val accent = previewAccent(style, dark)
        val outline = if (selected) look.border else look.thinBorder
        Box(
            modifier
                .clip(look.shape)
                .background(palette.bg)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .border(outline, if (selected) palette.border else palette.border.copy(alpha = 0.45f), look.shape)
        ) {
            StyleBackdrop(Modifier.matchParentSize(), fadeTo = palette.bg)
            Column(Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(8.dp).clip(look.smallShape).background(accent))
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().height(34.dp).clip(look.smallShape).background(palette.card)
                        .border(look.thinBorder, palette.border, look.smallShape)
                ) {
                    Box(Modifier.padding(6.dp).fillMaxWidth(0.6f).height(5.dp).clip(look.smallShape).background(palette.muted))
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                    Text(style.label, color = palette.ink, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}
