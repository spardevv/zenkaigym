package com.zenkaigym.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenkaigym.domain.tr
import com.zenkaigym.ui.theme.Zen

/**
 * Cartão sanfona: o cabeçalho (título, resumo opcional e seta) abre e fecha o conteúdo.
 * Fechado, o conteúdo nem entra na composição. Sem movimento ([LocalMotionEnabled] desligado), abre sem animar.
 */
@Composable
fun AccordionCard(
    title: String,
    meta: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val state = if (expanded) tr("Aberto") else tr("Fechado")
    ZenCard(modifier, accent = accent) {
        Row(
            Modifier.fillMaxWidth().defaultMinSize(minHeight = MIN_TOUCH)
                .semantics(mergeDescendants = true) { stateDescription = state }
                .clickable(role = Role.Button, onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (meta != null) {
                    Text(meta, color = Zen.p.muted, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            ZenIcon(if (expanded) ZenIcons.ChevronUp else ZenIcons.ChevronDown, Modifier.padding(start = 8.dp), size = 22.dp)
        }
        if (LocalMotionEnabled.current) {
            AnimatedVisibility(
                expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) { Column(content = content) }
        } else if (expanded) {
            Column(content = content)
        }
    }
}
