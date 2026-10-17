package com.zenkaigym.ui.screens

import com.zenkaigym.domain.tr
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.fillMaxSize
import com.zenkaigym.ui.components.ScrollHints
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.FoodCategory
import com.zenkaigym.domain.Portions
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.components.FoodFormDialog
import com.zenkaigym.ui.components.KanjiBadge
import com.zenkaigym.ui.components.SectionTitle
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenCard
import com.zenkaigym.ui.components.ZenDialog
import com.zenkaigym.ui.components.ZenHeader
import com.zenkaigym.ui.components.ZenPage
import com.zenkaigym.ui.components.accent
import com.zenkaigym.ui.components.fmt
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Sakura
import com.zenkaigym.ui.theme.Zen

private enum class FoodsAction { RESET, CLEAR }

/** Seus alimentos, agrupados por categoria. Toque num alimento para editar ou excluir. */
@Composable
fun FoodsScreen(viewModel: ZenViewModel, onBack: () -> Unit) {
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Food?>(null) }
    var confirm by remember { mutableStateOf<FoodsAction?>(null) }
    val grouped = remember(foods) { FoodCategory.entries.map { cat -> cat to foods.filter { it.category == cat } }.filter { it.second.isNotEmpty() } }

    ZenPage {
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ZenButton(tr("←  Voltar à dieta"), onBack, accent = Zen.p.card, small = true)
                    ZenHeader(tr("Alimentos"), "米", accent = Midori, subtitle = tr("Valores por 100 g · a dieta usa estes"))
                    ZenButton(tr("+ Novo alimento"), { creating = true }, accent = Ao, fill = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ZenButton(tr("Refazer sugestão"), { confirm = FoodsAction.RESET }, Modifier.weight(1f), accent = Sakura, fill = true, small = true)
                        if (foods.isNotEmpty()) {
                            ZenButton(tr("Remover tudo"), { confirm = FoodsAction.CLEAR }, Modifier.weight(1f), accent = Aka, fill = true, small = true)
                        }
                    }
                }
            }
            if (foods.isEmpty()) {
                item(key = "empty") {
                    ZenCard(entranceIndex = 1) {
                        Text(tr("Nenhum alimento cadastrado."), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(tr("Cadastre os seus ou comece com uma lista pronta (dá para editar depois)."), color = Zen.p.muted)
                        Spacer(Modifier.height(10.dp))
                        ZenButton(tr("Adicionar lista pronta"), viewModel::addFoodExamples, accent = Ao)
                    }
                }
            }
            grouped.forEach { (cat, items) ->
                item(key = "title-${cat.name}") { SectionTitle(cat.label, accent = cat.accent(), modifier = Modifier.padding(top = 4.dp)) }
                items(items, key = { it.id }) { food ->
                    // Alimento removido/adicionado: os vizinhos deslizam para ocupar o espaço.
                    ZenCard(Modifier.animateItem().clickable { editing = food }, entranceIndex = -1) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            KanjiBadge(cat.kanji, color = cat.accent(), size = 40.dp)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(tr(food.name), style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    tr("{0} kcal · P {1} · C {2} · G {3}", food.kcalPer100.fmt(), food.proteinPer100.fmt(), food.carbsPer100.fmt(), food.fatPer100.fmt()),
                                    color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
                                )
                                Portions.unitGrams(food)?.let {
                                    Text(tr("1 {0} = {1} g", tr(food.unitLabel.orEmpty()), it.fmt()), color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Text("✎", style = MaterialTheme.typography.titleMedium, color = Zen.p.muted)
                        }
                    }
                }
            }
        }
        ScrollHints(listState.canScrollBackward, listState.canScrollForward, Modifier.fillMaxSize())
    }

    confirm?.let { action ->
        val reset = action == FoodsAction.RESET
        ZenDialog(if (reset) tr("Refazer sugestão?") else tr("Remover tudo?"), { confirm = null }, accent = if (reset) Sakura else Aka) {
            Text(
                if (reset) tr("Todos os seus alimentos serão apagados e trocados pela lista sugerida. A dieta será remontada.")
                else tr("Todos os alimentos serão apagados e as dietas perdem seus itens. Você pode cadastrar de novo ou refazer a sugestão."),
                color = Zen.p.muted
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZenButton(if (reset) tr("Refazer") else tr("Remover"), {
                    if (reset) viewModel.resetFoods() else viewModel.deleteAllFoods()
                    confirm = null
                }, Modifier.weight(1f), accent = if (reset) Sakura else Aka, fill = true)
                ZenButton(tr("Cancelar"), { confirm = null }, accent = Zen.p.card)
            }
        }
    }

    if (creating) {
        FoodFormDialog(
            initial = null,
            onDismiss = { creating = false },
            onSave = { viewModel.saveFood(it); creating = false }
        )
    }
    editing?.let { food ->
        FoodFormDialog(
            initial = food,
            onDismiss = { editing = null },
            onSave = { viewModel.saveFood(it); editing = null },
            onDelete = { viewModel.deleteFood(food.id); editing = null }
        )
    }
}
