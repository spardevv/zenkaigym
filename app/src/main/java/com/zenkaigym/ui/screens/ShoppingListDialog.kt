package com.zenkaigym.ui.screens

import android.content.Intent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.zenkaigym.data.entity.DietPlanFull
import com.zenkaigym.domain.ShoppingEntry
import com.zenkaigym.domain.ShoppingPlanner
import com.zenkaigym.domain.tr
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenChip
import com.zenkaigym.ui.components.ZenDialog
import com.zenkaigym.ui.components.accent
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Zen
import com.zenkaigym.util.dayOfWeekFromEpochDay
import com.zenkaigym.util.formatEpochDay
import com.zenkaigym.util.todayEpochDay

/** Lista de compras do cardápio para os próximos dias, com opção de compartilhar ou copiar o texto. */
@Composable
fun ShoppingListDialog(plan: DietPlanFull, onClose: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var days by remember { mutableIntStateOf(ShoppingPlanner.MONTH) }
    var copied by remember { mutableIntStateOf(0) }

    val today = remember { todayEpochDay() }
    val entries = remember(plan) {
        plan.meals.filter { it.meal.cheatKcal == null }
            .flatMap { m -> m.items.map { ShoppingEntry(m.meal.dayOfWeek, it.food, it.item.grams) } }
    }
    val list = ShoppingPlanner.build(entries, dayOfWeekFromEpochDay(today), days)
    val period = tr("De {0} a {1}", formatEpochDay(today), formatEpochDay(today + days - 1))
    val text = ShoppingPlanner.text(list, period)

    ZenDialog(tr("Lista de compras"), onClose, accent = Midori, footer = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenButton(tr("Compartilhar"), {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, tr("Lista de compras"))
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(send, tr("Compartilhar")))
            }, Modifier.weight(1f), accent = Midori, fill = true, small = true, enabled = !list.isEmpty)
            ZenButton(if (copied > 0) tr("Copiado ✓") else tr("Copiar"), {
                clipboard.setText(AnnotatedString(text))
                copied++
            }, Modifier.weight(1f), accent = Ao, fill = true, small = true, enabled = !list.isEmpty)
        }
        Spacer(Modifier.height(8.dp))
        ZenButton(tr("Fechar"), onClose, accent = Zen.p.card, fill = true, small = true)
    }) {
        Text(tr("PERÍODO"), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(ShoppingPlanner.WEEK to tr("Semana"), ShoppingPlanner.FORTNIGHT to tr("Quinzena"), ShoppingPlanner.MONTH to tr("Mês")).forEach { (d, label) ->
                ZenChip(label, days == d, { days = d; copied = 0 }, Modifier.weight(1f), accent = Midori, compact = true)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(tr("{0} · somando o cardápio de cada dia.", period), color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)

        if (list.isEmpty) {
            Spacer(Modifier.height(12.dp))
            Text(tr("O cardápio ainda não tem alimentos."), color = Zen.p.muted)
        }
        list.groups.forEach { (cat, items) ->
            Spacer(Modifier.height(12.dp))
            Text("${cat.kanji}  ${cat.label.uppercase()}", color = cat.accent(), style = MaterialTheme.typography.labelLarge)
            Column(Modifier.padding(top = 4.dp)) {
                items.forEach { item ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tr(item.food.name), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Text(ShoppingPlanner.quantity(item.food, item.grams), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
