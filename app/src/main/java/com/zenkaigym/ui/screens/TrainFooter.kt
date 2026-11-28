package com.zenkaigym.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenkaigym.domain.TrainPrimary
import com.zenkaigym.domain.tr
import com.zenkaigym.notify.RestTimer
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenIcon
import com.zenkaigym.ui.components.ZenIcons
import com.zenkaigym.ui.components.ZenProgressBar
import com.zenkaigym.ui.components.pressScale
import com.zenkaigym.ui.components.zenShadow
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Sumi
import com.zenkaigym.ui.theme.Zen
import kotlinx.coroutines.delay

/** Próxima série de trabalho a registrar: número, exercício e o que está preenchido ("40 kg × 12"). */
data class NextSet(val setIndex: Int, val exerciseName: String, val detail: String)

/**
 * Rodapé único do modo treino. Sem descanso: cronômetro manual e o botão principal
 * ([primary]). Durante o descanso o rodapé inteiro vira o cronômetro.
 * [restBlocked] (treino concluído ou tudo registrado) desativa o descanso manual.
 */
@Composable
fun TrainFooter(
    viewModel: ZenViewModel,
    primary: TrainPrimary,
    next: NextSet?,
    restBlocked: Boolean,
    onLogNext: () -> Unit,
    onFinish: () -> Unit,
    onSummary: () -> Unit
) {
    val p = Zen.p
    val rest by viewModel.rest.collectAsStateWithLifecycle()
    val restPreset by viewModel.restPreset.collectAsStateWithLifecycle()
    val manualRest = restPreset.takeIf { it > 0 } ?: 90

    // Relógio do descanso: só atualiza o visor. O fim (vibração e aviso) fica com o RestTimer,
    // que continua contando com o app em segundo plano.
    val now by produceState(System.currentTimeMillis(), rest) {
        while (rest != null) {
            value = System.currentTimeMillis()
            delay(200)
        }
    }

    Column(Modifier.background(p.card)) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(p.border))
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            val r = rest
            if (r != null) {
                val remainingMs = (r.endsAt - now).coerceAtLeast(0)
                RestPanel(
                    label = next?.let { tr("Descanso · depois: série {0}", it.setIndex) } ?: tr("Descanso"),
                    time = "%d:%02d".format(remainingMs / 60_000, remainingMs / 1000 % 60),
                    fraction = if (r.totalMs > 0) 1f - remainingMs.toFloat() / r.totalMs else 1f,
                    onAdd = { viewModel.addRest() },
                    onSkip = { viewModel.stopRest() }
                )
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolButton(
                        onClick = { viewModel.startRest(manualRest) },
                        modifier = Modifier.semantics { contentDescription = tr("Descanso manual") },
                        accent = Ao,
                        enabled = !restBlocked,
                        icon = ZenIcons.Timer
                    )
                    when (primary) {
                        TrainPrimary.LOG_NEXT -> ToolButton(
                            onClick = onLogNext,
                            modifier = Modifier.weight(1f),
                            accent = Aka,
                            enabled = next != null,
                            filled = true,
                            main = tr("Registrar série {0}", next?.setIndex ?: 0),
                            leading = ZenIcons.Play,
                            sub = next?.let { "${it.exerciseName} · ${it.detail}" }
                        )
                        TrainPrimary.FINISH -> ZenButton(tr("Concluir treino"), onFinish, Modifier.weight(1f), accent = Midori, fill = true)
                        TrainPrimary.SUMMARY -> ZenButton(tr("Ver resumo"), onSummary, Modifier.weight(1f), accent = Midori, fill = true)
                    }
                }
            }
        }
    }
}

/** Rodapé durante o descanso: legenda, tempo grande, barra e os dois botões. */
@Composable
private fun RestPanel(label: String, time: String, fraction: Float, onAdd: () -> Unit, onSkip: () -> Unit) {
    val p = Zen.p
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = p.muted, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(time, color = p.ink, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))
        ZenProgressBar(fraction, Ao, height = 10.dp)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenButton(tr("+{0} s", RestTimer.ADD_SECONDS), onAdd, Modifier.weight(1f), accent = Ao, fill = true)
            ZenButton(tr("Pular"), onSkip, Modifier.weight(1f), accent = p.card, fill = true)
        }
    }
}

/**
 * Botão de ferramenta do rodapé, com sombra de mangá. Com [icon] é um botão só de ícone;
 * com [main] (e [sub] embaixo, em letra menor) é o botão largo da ação principal.
 */
@Composable
private fun ToolButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color,
    filled: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    leading: ImageVector? = null,
    main: String? = null,
    sub: String? = null
) {
    val p = Zen.p
    val look = Zen.look
    val interaction = remember { MutableInteractionSource() }
    // Cores opacas (sem transparência): misturar o acento com o fundo deixava o botão com tom barrento.
    val bg by animateColorAsState(if (filled) accent else p.bg, tween(200), label = "tool-bg")
    val mainColor = when {
        filled -> Sumi
        enabled -> p.ink
        else -> p.muted.copy(alpha = 0.5f)
    }
    Box(
        modifier
            .padding(end = 3.dp, bottom = 3.dp)
            .zenShadow(3.dp, p.shadow, look.smallShape)
            .pressScale(interaction)
            .heightIn(min = 52.dp)
            .clip(look.smallShape)
            .background(bg)
            .border(look.thinBorder, p.border, look.smallShape)
            .clickable(interaction, null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            // Ícone vetorial em vez de símbolo de texto: símbolos como ☀ viram emoji colorido e ignoram a cor.
            ZenIcon(icon, Modifier.size(22.dp), color = mainColor)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (leading != null) {
                        ZenIcon(leading, Modifier.size(16.dp), color = mainColor)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        main.orEmpty(), color = mainColor, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (sub != null) {
                    Text(
                        sub, color = mainColor, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
