package com.zenkaigym.ui.components

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.zenkaigym.domain.tr
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Sumi
import com.zenkaigym.ui.theme.Zen

/** Botão quadrado pequeno (menos, mais, remover, renomear) usado para editar itens das fichas. */
@Composable
fun MiniButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Transparent,
    description: String = when (icon) {
        ZenIcons.Minus -> tr("Diminuir")
        ZenIcons.Plus -> tr("Aumentar")
        ZenIcons.Close -> tr("Remover")
        else -> icon.name
    }
) {
    val p = Zen.p
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .pressScale(interaction, 0.82f)
            .minimumInteractiveComponentSize()
            .size(34.dp)
            .clip(Zen.look.smallShape)
            .background(if (color == Color.Transparent) p.card else color)
            .border(Zen.look.border, p.border, Zen.look.smallShape)
            .semantics { contentDescription = description }
            .clickable(interaction, null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        ZenIcon(icon, size = 16.dp, color = if (color == Color.Transparent) p.ink else Sumi)
    }
}

/** Par menos/mais seguido do botão de remover. */
@Composable
fun EditControls(onMinus: () -> Unit, onPlus: () -> Unit, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MiniButton(ZenIcons.Minus, onMinus)
        Spacer(Modifier.width(4.dp))
        MiniButton(ZenIcons.Plus, onPlus)
        Spacer(Modifier.width(4.dp))
        MiniButton(ZenIcons.Close, onRemove, color = Aka)
    }
}
