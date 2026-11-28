package com.zenkaigym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenkaigym.domain.LevelInfo
import com.zenkaigym.domain.WeekStripDay
import com.zenkaigym.domain.tr
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Sumi
import com.zenkaigym.ui.theme.Zen
import com.zenkaigym.util.WEEK_DAYS_SHORT

/**
 * Faixa da semana do Dojo, sem cartão: treinos da semana, nível e sete quadradinhos (feito = Ki, hoje =
 * borda grossa, planejado e perdido = borda tracejada). A faixa inteira é um botão que abre a Evolução.
 */
@Composable
fun DojoWeekStrip(days: List<WeekStripDay>, sessions: Int, planned: Int, level: LevelInfo?, onClick: () -> Unit) {
    val p = Zen.p
    val look = Zen.look
    val description = tr("Abrir evolução")
    Column(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                tr("Semana · {0}/{1} treinos", sessions, planned),
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (level != null) {
                Text(
                    tr("Nv {0} · {1} / {2} XP", level.level, level.xpIntoLevel, level.xpForNext),
                    style = MaterialTheme.typography.labelMedium, color = p.muted, maxLines = 1
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEach { day ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    val base = Modifier.fillMaxWidth().height(28.dp)
                    val box = when {
                        // Feito (fundo Ki) e hoje (borda grossa) são independentes: a borda vem depois do fundo.
                        day.done -> base.background(Ki, look.smallShape).then(if (day.today) Modifier.border(look.border, p.border, look.smallShape) else Modifier)
                        day.today -> base.border(look.border, p.border, look.smallShape)
                        day.missed -> base.drawBehind {
                            // Tracejado seguindo o contorno do estilo (reto, arredondado ou chanfrado).
                            val width = look.thinBorder.toPx()
                            drawOutline(
                                look.smallShape.createOutline(size, layoutDirection, this),
                                p.muted,
                                style = Stroke(width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(width * 3, width * 2)))
                            )
                        }
                        else -> base.border(look.thinBorder, p.muted, look.smallShape)
                    }
                    Box(box, contentAlignment = Alignment.Center) {
                        if (day.done) Text("✓", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Sumi)
                    }
                    Text(
                        WEEK_DAYS_SHORT[day.dayOfWeek - 1].take(1),
                        Modifier.padding(top = 2.dp),
                        style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                        color = if (day.today) p.ink else p.muted, fontWeight = if (day.today) FontWeight.Bold else null
                    )
                }
            }
        }
    }
}
