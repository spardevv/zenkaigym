package com.zenkaigym.ui.components

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zenkaigym.domain.Motion
import com.zenkaigym.ui.theme.Sumi
import com.zenkaigym.ui.theme.Zen

/**
 * Gráfico de barras no estilo do app: barras com contorno grosso, a mais recente hachurada.
 * As barras crescem em sequência ao aparecer e se ajustam suavemente quando os dados mudam.
 * Tocar numa barra a destaca e mostra [describe] dela acima do gráfico.
 */
@Composable
fun ZenBarChart(
    values: List<Float>,
    color: Color,
    describe: (Int) -> String,
    modifier: Modifier = Modifier,
    maxValue: Float? = null,
    height: Dp = 110.dp,
    startLabel: String? = null,
    endLabel: String? = null
) {
    val p = Zen.p
    // Por padrão, a barra destacada é a mais recente.
    var selected by remember(values.size) { mutableStateOf(values.lastIndex) }

    // Entrada: as barras crescem uma após a outra.
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }

    val top = (maxValue ?: values.maxOrNull() ?: 0f).coerceAtLeast(0.0001f)
    // Cada barra anima a própria altura quando o valor muda (ex.: série registrada agora).
    val heights = values.mapIndexed { i, v ->
        animateFloatAsState((v / top).coerceIn(0f, 1f), spring(stiffness = Spring.StiffnessMediumLow), label = "bar-$i").value
    }

    Column(modifier) {
        AnimatedContent(
            selected,
            transitionSpec = { (slideInVertically { it / 2 } + fadeIn(tween(160))) togetherWith (slideOutVertically { -it / 2 } + fadeOut(tween(120))) },
            label = "chart-caption"
        ) { index ->
            Text(
                if (index in values.indices) describe(index) else "",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(Zen.look.smallShape)
                    .background(p.bg)
                    .border(Zen.look.thinBorder, p.border, Zen.look.smallShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        // Leitores de tela não enxergam o desenho: recebem a descrição de todas as barras em texto.
        val summary = values.indices.joinToString(". ") { describe(it) }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .semantics { contentDescription = summary }
                .pointerInput(values.size) {
                    detectTapGestures { pos ->
                        if (values.isEmpty()) return@detectTapGestures
                        val slot = size.width / values.size.toFloat()
                        selected = (pos.x / slot).toInt().coerceIn(0, values.lastIndex)
                    }
                }
        ) {
            if (values.isEmpty()) return@Canvas
            val border = 2.dp.toPx()
            val gap = 4.dp.toPx()
            val slot = size.width / values.size
            val barWidth = slot - gap
            val fractions = Motion.segmentFractions(grow.value, values.size)
            // Linha de base.
            drawLine(p.border, Offset(0f, size.height - border / 2), Offset(size.width, size.height - border / 2), border)
            values.indices.forEach { i ->
                val h = (size.height - border) * heights[i] * fractions[i]
                val left = i * slot + gap / 2
                val isSelected = i == selected
                if (h <= 0.5f) return@forEach
                val topY = size.height - border - h
                drawRect(if (isSelected) color else color.copy(alpha = 0.55f).compositeOver(p.card), Offset(left, topY), Size(barWidth, h))
                if (isSelected) {
                    // Hachura diagonal na barra destacada.
                    clipRect(left, topY, left + barWidth, topY + h) {
                        val step = 6.dp.toPx()
                        var x = left - h
                        while (x < left + barWidth) {
                            drawLine(Sumi.copy(alpha = 0.2f), Offset(x, topY + h), Offset(x + h, topY), 2.dp.toPx())
                            x += step
                        }
                    }
                }
                drawRect(p.border, Offset(left, topY), Size(barWidth, h), style = Stroke(if (isSelected) border * 1.5f else border))
            }
        }
        if (startLabel != null || endLabel != null) {
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(startLabel.orEmpty(), color = p.muted, style = MaterialTheme.typography.labelSmall)
                Text(endLabel.orEmpty(), color = p.muted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Número de destaque que "conta" até o valor ao aparecer ou mudar. */
@Composable
fun StatTile(label: String, value: Int, modifier: Modifier = Modifier, format: (Int) -> String = { it.toString() }, accent: Color? = null) {
    val p = Zen.p
    val shown by animateIntAsState(value, tween(600, easing = FastOutSlowInEasing), label = "stat")
    Column(
        modifier
            .clip(Zen.look.smallShape)
            .background(accent ?: p.bg)
            .border(Zen.look.thinBorder, p.border, Zen.look.smallShape)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label.uppercase(), color = if (accent != null) Sumi else p.muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        Text(format(shown), color = if (accent != null) Sumi else p.ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, maxLines = 1)
    }
}
