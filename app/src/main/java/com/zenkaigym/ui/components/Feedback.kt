package com.zenkaigym.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Zen

/**
 * Aviso flutuante no topo: mostra "carregando" enquanto algo é salvo/recalculado ([busy]) e,
 * ao terminar, a confirmação ([message]) com um check que "salta".
 */
@Composable
fun FeedbackHost(busy: String?, message: String?, modifier: Modifier = Modifier) {
    val p = Zen.p
    // Mantém o último texto durante a animação de saída.
    val lastText = remember { arrayOf("") }
    val text = busy ?: message
    if (text != null) lastText[0] = text

    AnimatedVisibility(
        visible = text != null,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        enter = slideInVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) { -it } + fadeIn(tween(120)),
        exit = slideOutVertically(tween(200)) { -it } + fadeOut(tween(160))
    ) {
        Column(
            Modifier
                .drawBehind { drawRect(p.shadow, topLeft = Offset(4.dp.toPx(), 4.dp.toPx()), size = size) }
                .background(p.card)
                .border(3.dp, p.border)
        ) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (busy != null) Spinner() else CheckPop()
                Text(
                    lastText[0],
                    Modifier.weight(1f).padding(start = 12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (busy != null) ProgressBar()
        }
    }
}

/** Quadrado que gira enquanto trabalha. */
@Composable
private fun Spinner() {
    val t = rememberInfiniteTransition(label = "spinner")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "angle")
    Box(
        Modifier
            .size(22.dp)
            .graphicsLayer { rotationZ = angle }
            .background(Ki)
            .border(3.dp, Zen.p.border)
    )
}

/** Check verde que aparece com um pequeno salto. */
@Composable
private fun CheckPop() {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium)) }
    Box(
        Modifier
            .height(26.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .background(Midori)
            .border(3.dp, Zen.p.border)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("✓", color = com.zenkaigym.ui.theme.Sumi, style = MaterialTheme.typography.labelLarge)
    }
}

/** Barra indeterminada na base do aviso. */
@Composable
private fun ProgressBar() {
    val t = rememberInfiniteTransition(label = "bar")
    val x by t.animateFloat(-0.4f, 1f, infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart), label = "x")
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clipToBounds()
            .drawBehind {
                val w = size.width * 0.4f
                drawRect(Aka, topLeft = Offset(size.width * x, 0f), size = androidx.compose.ui.geometry.Size(w, size.height))
            }
    )
}
