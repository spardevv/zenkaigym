package com.zenkaigym.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import com.zenkaigym.domain.IntroFlow
import com.zenkaigym.domain.IntroPages
import com.zenkaigym.ui.components.KanjiBadge
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenPage
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Zen

private val accents = listOf(Aka, Ao, Ki, Midori, Aka)

@Composable
fun IntroScreen(onFinish: () -> Unit) {
    val pages = IntroPages.all
    var flow by remember { mutableStateOf(IntroFlow(pages.size)) }
    var forward by remember { mutableStateOf(true) }
    var dragged by remember { mutableStateOf(0f) }

    fun go(next: Boolean) {
        forward = next
        flow = if (next) flow.next() else flow.back()
    }

    BackHandler(enabled = !flow.isFirst) { go(false) }

    ZenPage {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragged = 0f },
                        onDragEnd = {
                            if (dragged < -80f) go(true) else if (dragged > 80f) go(false)
                        }
                    ) { _, amount -> dragged += amount }
                }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!flow.isLast) ZenButton("Pular", onFinish, accent = Zen.p.card, small = true)
            }

            AnimatedContent(
                targetState = flow.index,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                transitionSpec = {
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally(tween(300)) { it * dir } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(300)) { -it * dir } + fadeOut(tween(200)))
                },
                label = "intro-page"
            ) { index ->
                val page = pages[index]
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val breathing = rememberInfiniteTransition(label = "breathing")
                    val pulse by breathing.animateFloat(
                        1f, 1.05f,
                        infiniteRepeatable(tween(1600), RepeatMode.Reverse),
                        label = "pulse"
                    )
                    KanjiBadge(
                        page.kanji, color = accents[index % accents.size], size = 168.dp,
                        modifier = Modifier.graphicsLayer {
                            scaleX = pulse
                            scaleY = pulse
                        }
                    )
                    Spacer(Modifier.height(32.dp))
                    Text(
                        page.title.uppercase(),
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.width(72.dp).height(6.dp).background(accents[index % accents.size]))
                    Spacer(Modifier.height(20.dp))
                    Text(
                        page.body,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Dots(count = pages.size, current = flow.index)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!flow.isFirst) ZenButton("Voltar", { go(false) }, accent = Zen.p.card)
                ZenButton(
                    if (flow.isLast) "Começar" else "Próximo",
                    { if (flow.isLast) onFinish() else go(true) },
                    modifier = Modifier.weight(1f),
                    accent = accents[flow.index % accents.size],
                    fill = true
                )
            }
        }
    }
}

@Composable
private fun Dots(count: Int, current: Int) {
    val p = Zen.p
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { i ->
            Box(
                Modifier
                    .width(if (i == current) 28.dp else 14.dp)
                    .height(14.dp)
                    .background(if (i == current) accents[i % accents.size] else Color.Transparent)
                    .border(3.dp, p.border)
            )
        }
    }
}
