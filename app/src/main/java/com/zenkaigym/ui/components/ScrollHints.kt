package com.zenkaigym.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.zenkaigym.ui.theme.Zen

/**
 * Setas que avisam que há mais conteúdo para rolar: para cima quando dá para subir e para baixo quando dá para descer.
 * Não captam toque; ficam por cima da área rolável.
 */
@Composable
fun ScrollHints(canUp: Boolean, canDown: Boolean, modifier: Modifier = Modifier) {
    Box(modifier) {
        Hint(canUp, ZenIcons.ChevronUp, Alignment.TopCenter, direction = -1f)
        Hint(canDown, ZenIcons.ChevronDown, Alignment.BottomCenter, direction = 1f)
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.Hint(visible: Boolean, symbol: ImageVector, align: Alignment, direction: Float) {
    val p = Zen.p
    AnimatedVisibility(visible, Modifier.align(align), enter = fadeIn(tween(150)), exit = fadeOut(tween(150))) {
        // A animação infinita só existe enquanto a seta está visível: escondida, não gasta quadros.
        val bounce by rememberInfiniteTransition(label = "hint").animateFloat(
            0f, 3f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "bounce"
        )
        Box(
            Modifier
                .padding(6.dp)
                .graphicsLayer { translationY = bounce * direction * density }
                .clip(Zen.look.smallShape)
                .background(p.card)
                .border(Zen.look.thinBorder, p.border, Zen.look.smallShape)
                .padding(horizontal = 14.dp, vertical = 2.dp)
        ) {
            ZenIcon(symbol, size = 16.dp)
        }
    }
}
