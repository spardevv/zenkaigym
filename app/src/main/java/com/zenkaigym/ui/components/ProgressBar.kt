package com.zenkaigym.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zenkaigym.ui.theme.Zen

/**
 * Barra de progresso no estilo do app (contorno do estilo atual, preenchimento animado).
 * [fraction] vai de 0 a 1.
 */
@Composable
fun ZenProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 12.dp) {
    val p = Zen.p
    val look = Zen.look
    val shown by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(600, easing = FastOutSlowInEasing), label = "progress-bar")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(look.smallShape)
            .background(p.bg)
            .border(look.thinBorder, p.border, look.smallShape)
    ) {
        if (shown > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(shown)
                    .background(color)
            )
        }
    }
}
