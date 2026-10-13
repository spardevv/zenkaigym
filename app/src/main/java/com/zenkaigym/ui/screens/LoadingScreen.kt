package com.zenkaigym.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.zenkaigym.ui.components.KanjiBadge
import com.zenkaigym.ui.components.ZenPage
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Zen

/** Mostrada só na abertura a frio, enquanto o banco e as telas principais carregam. */
@Composable
fun LoadingScreen() {
    val p = Zen.p
    val transition = rememberInfiniteTransition(label = "loading")
    val pulse by transition.animateFloat(1f, 1.08f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse")
    val slide by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "bar")

    ZenPage {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            KanjiBadge("禅", size = 140.dp, modifier = Modifier.graphicsLayer { scaleX = pulse; scaleY = pulse })
            Spacer(Modifier.height(28.dp))
            Text("ZENKAIGYM", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(20.dp))
            Box(Modifier.width(180.dp).height(14.dp).border(3.dp, p.border)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.35f)
                        .height(8.dp)
                        .graphicsLayer { translationX = slide * 180.dp.toPx() * 0.65f }
                        .background(Aka)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text("Preparando o dojo…", color = p.muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}
