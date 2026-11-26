package com.zenkaigym.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenkaigym.domain.LevelUp
import com.zenkaigym.domain.tr
import com.zenkaigym.ui.components.LocalMotionEnabled
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Daidai
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Sumi
import com.zenkaigym.ui.theme.Zen
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cena de subida de nível, como a página de transformação de um mangá: linhas de velocidade girando atrás,
 * o kanji da patente caindo com quique e o novo nível. Subir de patente ganha título próprio.
 * Com animações desligadas no sistema, aparece parada. Toque em qualquer lugar (ou voltar) fecha.
 */
@Composable
fun LevelUpScene(up: LevelUp, onDismiss: () -> Unit) {
    val motion = LocalMotionEnabled.current
    val accent = if (up.rankUp) Daidai else Ki
    val drop = remember { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(up) {
        if (motion) drop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessLow))
    }
    val spin by if (motion) {
        rememberInfiniteTransition(label = "levelup-spin").animateFloat(
            0f, 360f, infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart), label = "levelup-angle"
        )
    } else remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    BackHandler(onBack = onDismiss)

    Box(
        Modifier
            .fillMaxSize()
            .background(Sumi.copy(alpha = 0.94f))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        // 集中線: cunhas finas saindo do centro, alternando a cor do destaque e o branco.
        Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = spin }) {
            val c = Offset(size.width / 2, size.height / 2)
            val far = size.maxDimension
            for (k in 0 until 40) {
                val a = Math.toRadians(k * 9.0)
                val w = 0.035
                val near = size.minDimension * 0.22f
                val path = Path().apply {
                    moveTo(c.x + cos(a).toFloat() * near, c.y + sin(a).toFloat() * near)
                    lineTo(c.x + cos(a + w).toFloat() * far, c.y + sin(a + w).toFloat() * far)
                    lineTo(c.x + cos(a - w).toFloat() * far, c.y + sin(a - w).toFloat() * far)
                    close()
                }
                drawPath(path, if (k % 2 == 0) accent.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.18f))
            }
        }

        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(24.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                if (up.rankUp) tr("NOVA PATENTE!") else tr("SUBIU DE NÍVEL!"),
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                fontStyle = if (Zen.look.upright) FontStyle.Normal else FontStyle.Italic,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = drop.value.coerceIn(0f, 1f) }
            )
            Spacer(Modifier.height(20.dp))
            // Emblema: cai de cima, gira um pouco e assenta com quique.
            Box(
                Modifier
                    .graphicsLayer {
                        val v = drop.value
                        translationY = (1f - v) * -600f
                        rotationZ = (1f - v) * -25f
                        val s = 0.6f + 0.4f * v
                        scaleX = s
                        scaleY = s
                    }
                    .size(160.dp)
                    .clip(Zen.look.shape)
                    .background(accent)
                    .border(5.dp, Sumi, Zen.look.shape),
                contentAlignment = Alignment.Center
            ) {
                Text(up.toRank.kanji, color = Sumi, fontSize = 96.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                tr("NÍVEL {0}", up.toLevel),
                color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center
            )
            Text(
                if (up.rankUp) tr("{0} → {1}", up.fromRank.label.uppercase(), up.toRank.label.uppercase())
                else up.toRank.label.uppercase(),
                color = accent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                if (up.rankUp) tr("Cada treino, refeição e recorde te trouxe até aqui. A próxima patente te espera.")
                else tr("Mais XP acumulado. Continue assim!"),
                color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            ZenButton(tr("Continuar"), onDismiss, accent = Aka, fill = true)
        }
    }
}
