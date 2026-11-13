package com.zenkaigym.ui.components

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import android.content.Context
import android.provider.Settings
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.zenkaigym.domain.Motion
import kotlinx.coroutines.delay

/**
 * Falso quando o usuário desligou as animações do sistema (escala 0, "remover animações").
 * Lido uma vez ao compor o tema; quem anima sem parar ou entra em cascata deve respeitar.
 */
val LocalMotionEnabled = staticCompositionLocalOf { true }

fun motionEnabled(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f

/**
 * Entrada suave (fade + subida) com atraso escalonado pelo [index]. Negativo = sem animação
 * (itens de listas roláveis, que não devem animar de novo ao reaparecer). Só os primeiros itens animam.
 */
@Composable
fun Modifier.zenEntrance(index: Int = 0): Modifier {
    val animate = index in 0..Motion.MAX_STAGGER_STEPS && LocalMotionEnabled.current
    val progress = remember { Animatable(if (animate) 0f else 1f) }
    if (animate) {
        LaunchedEffect(Unit) {
            delay(Motion.staggerDelayMs(index).toLong())
            progress.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
        }
    }
    return graphicsLayer {
        val value = progress.value
        if (value < 1f) {
            alpha = value
            translationY = (1f - value) * 20.dp.toPx()
        }
    }
}

/** Aparecer/sumir de trechos dentro de cartões (erros, botões condicionais): abre a altura e esmaece. */
val Reveal: EnterTransition = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(180, delayMillis = 40))
val Conceal: ExitTransition = fadeOut(tween(120)) + shrinkVertically(tween(220, easing = FastOutSlowInEasing))

/** Encolhe levemente enquanto pressionado e volta com um pequeno quique. */
@Composable
fun Modifier.pressScale(source: MutableInteractionSource, pressedScale: Float = 0.92f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) pressedScale else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press-scale"
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
