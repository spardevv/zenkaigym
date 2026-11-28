package com.zenkaigym.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenkaigym.ui.theme.Zen
import kotlinx.coroutines.launch

/** Fecha a folha aberta (com animação, se houver) e depois executa o que for passado. */
private val LocalSheetClose = compositionLocalOf<(() -> Unit) -> Unit> { { it() } }

/**
 * Folha de baixo do app: ações secundárias de um cartão. Só os cantos de cima seguem o estilo
 * (retos no Shōnen, arredondados no Kawaii, chanfrados no Isekai); uma faixa de [accent] marca o topo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenSheet(
    onDismiss: () -> Unit,
    accent: Color,
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val look = Zen.look
    val p = Zen.p
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val motion = LocalMotionEnabled.current
    val shape: Shape = when {
        look.isekai -> CutCornerShape(topStart = look.corner, topEnd = look.corner, bottomEnd = 0.dp, bottomStart = 0.dp)
        look.corner == 0.dp -> RectangleShape
        else -> RoundedCornerShape(topStart = look.corner, topEnd = look.corner)
    }
    val close: (() -> Unit) -> Unit = { after ->
        scope.launch {
            if (motion) state.hide()
            onDismiss()
            after()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = shape,
        containerColor = p.card,
        contentColor = p.ink,
        dragHandle = null
    ) {
        CompositionLocalProvider(LocalSheetClose provides close) {
            Column(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)) {
                Box(Modifier.fillMaxWidth().height(6.dp).background(accent))
                Box(
                    Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp, bottom = 6.dp)
                        .width(36.dp).height(4.dp).clip(CircleShape).background(p.muted)
                )
                Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Text(title.uppercase(), style = MaterialTheme.typography.titleMedium)
                    if (subtitle != null) Text(subtitle, color = p.muted, style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 16.dp), content = content)
            }
        }
    }
}

/** Linha de ação da [ZenSheet]: título e uma frase; tocar fecha a folha e executa a ação. */
@Composable
fun SheetOption(title: String, description: String, onClick: () -> Unit, enabled: Boolean = true) {
    val close = LocalSheetClose.current
    Column(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clickable(enabled = enabled, role = Role.Button) { close(onClick) }
            .semantics { if (!enabled) disabled() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (enabled) Zen.p.ink else Zen.p.muted)
        Text(description, style = MaterialTheme.typography.bodySmall, color = Zen.p.muted)
    }
}
