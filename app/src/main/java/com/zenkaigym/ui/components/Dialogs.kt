package com.zenkaigym.ui.components

import androidx.compose.ui.draw.clip
import com.zenkaigym.domain.tr
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.FoodCategory
import com.zenkaigym.domain.FoodValidation
import com.zenkaigym.domain.FoodValidator
import com.zenkaigym.domain.Portions
import com.zenkaigym.domain.TimeOfDay
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Daidai
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Sumi
import com.zenkaigym.ui.theme.Zen

/**
 * Janela no estilo do app; o conteúdo rola quando é maior que a tela.
 * [footer], quando informado, fica fixo embaixo (fora da rolagem) — bom para os botões de ação.
 */
@Composable
fun ZenDialog(
    title: String,
    onDismiss: () -> Unit,
    accent: Color = Aka,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    // Usa quase toda a altura disponível, para só rolar quando o conteúdo realmente não cabe.
    val maxBody = (LocalConfiguration.current.screenHeightDp - if (footer != null) 270 else 190).dp
    val scroll = rememberScrollState()
    // Entrada: a janela "salta" de um pouco menor, com leve quique, em vez de surgir de uma vez.
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ZenCard(
            Modifier
                .padding(horizontal = 28.dp, vertical = 24.dp)
                .widthIn(max = 380.dp)
                .graphicsLayer {
                    val v = enter.value
                    alpha = v.coerceIn(0f, 1f)
                    scaleX = 0.9f + 0.1f * v
                    scaleY = 0.9f + 0.1f * v
                    translationY = (1f - v) * 24.dp.toPx()
                },
            accent = accent,
            entranceIndex = -1
        ) {
            // Mudanças de altura do conteúdo (erros, trechos que aparecem) acontecem com suavidade.
            Column(Modifier.padding(6.dp).animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))) {
                SectionTitle(title, accent = accent)
                Spacer(Modifier.height(14.dp))
                Box(Modifier.heightIn(max = maxBody)) {
                    Column(Modifier.verticalScroll(scroll).padding(end = 2.dp, bottom = 4.dp), content = content)
                    ScrollHints(scroll.canScrollBackward, scroll.canScrollForward, Modifier.matchParentSize())
                }
                if (footer != null) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp).height(2.dp).background(Zen.p.border))
                    footer()
                }
            }
        }
    }
}

fun FoodCategory.accent(): Color = when (this) {
    FoodCategory.PROTEIN -> Aka
    FoodCategory.CARB -> Ki
    FoodCategory.VEGETABLE -> Midori
    FoodCategory.SALAD -> Ao
    FoodCategory.FRUIT -> Daidai
}

fun FoodCategory.shortLabel() = if (this == FoodCategory.CARB) tr("Carbo") else label

fun Double.fmt() = if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)

/** Caixa de horário tocável (HH:mm). */
@Composable
fun TimeChip(time: String, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Color = Ki) {
    val p = Zen.p
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .pressScale(interaction, 0.94f)
            .clip(Zen.look.smallShape)
            .background(accent)
            .border(Zen.look.border, p.border, Zen.look.smallShape)
            .clickable(interaction, null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(time, color = Sumi, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}

/** Relógio de 24 h com alternância para digitação. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenTimePickerDialog(
    title: String,
    initial: TimeOfDay,
    onDismiss: () -> Unit,
    onConfirm: (TimeOfDay) -> Unit
) {
    val p = Zen.p
    val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
    var keyboard by remember { mutableStateOf(false) }
    val colors = TimePickerDefaults.colors(
        containerColor = p.card,
        clockDialColor = p.bg,
        clockDialSelectedContentColor = Sumi,
        clockDialUnselectedContentColor = p.ink,
        selectorColor = Aka,
        timeSelectorSelectedContainerColor = Ki,
        timeSelectorUnselectedContainerColor = p.bg,
        timeSelectorSelectedContentColor = Sumi,
        timeSelectorUnselectedContentColor = p.ink
    )
    ZenDialog(title, onDismiss) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (keyboard) TimeInput(state, colors = colors) else TimePicker(state, colors = colors)
        }
        Spacer(Modifier.height(8.dp))
        ZenChip(if (keyboard) tr("Relógio") else tr("Teclado"), false, { keyboard = !keyboard }, accent = Ao)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ZenButton(tr("Cancelar"), onDismiss, accent = p.card)
            ZenButton(tr("OK"), { onConfirm(TimeOfDay(state.hour, state.minute)) }, modifier = Modifier.weight(1f), fill = true)
        }
    }
}

/** Ajuste de quantidade: [−] valor [+]. */
@Composable
fun Stepper(label: String, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniButton("−", onMinus)
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
        MiniButton("+", onPlus)
    }
}

/** Linha tocável de uma lista de escolhas dentro de uma janela. */
@Composable
fun PickRow(title: String, subtitle: String?, trailing: String?, accent: Color, onClick: () -> Unit) {
    val p = Zen.p
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(Zen.look.smallShape)
            .background(p.bg)
            .border(Zen.look.thinBorder, p.border, Zen.look.smallShape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(6.dp).height(34.dp).background(accent))
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, color = p.muted, style = MaterialTheme.typography.bodyMedium)
        }
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Cadastro/edição de alimento (valores por 100 g). [fixedCategory] trava a categoria
 * (ex.: ao cadastrar um substituto da mesma categoria).
 */
@Composable
fun FoodFormDialog(
    initial: Food?,
    fixedCategory: FoodCategory? = null,
    onDismiss: () -> Unit,
    onSave: (Food) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(fixedCategory ?: initial?.category ?: FoodCategory.PROTEIN) }
    var kcal by remember { mutableStateOf(initial?.kcalPer100?.fmt() ?: "") }
    var protein by remember { mutableStateOf(initial?.proteinPer100?.fmt() ?: "") }
    var carbs by remember { mutableStateOf(initial?.carbsPer100?.fmt() ?: "") }
    var fat by remember { mutableStateOf(initial?.fatPer100?.fmt() ?: "") }
    var unitLabel by remember { mutableStateOf(initial?.unitLabel ?: "") }
    var unitGrams by remember { mutableStateOf(initial?.unitGrams?.fmt() ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    ZenDialog(if (initial == null) tr("Novo alimento") else tr("Editar alimento"), onDismiss, accent = category.accent()) {
        ZenField(tr("Nome"), name, { name = it }, placeholder = tr("ex.: Peito de frango"))
        if (fixedCategory == null) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FoodCategory.entries.forEach {
                    ZenChip(it.shortLabel(), category == it, { category = it }, Modifier.weight(1f), accent = it.accent(), compact = true)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(tr("VALORES POR 100 G"), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ZenField(tr("Kcal"), kcal, { kcal = it }, Modifier.weight(1f), KeyboardType.Decimal)
            ZenField(tr("Prot."), protein, { protein = it }, Modifier.weight(1f), KeyboardType.Decimal)
            ZenField(tr("Carbo"), carbs, { carbs = it }, Modifier.weight(1f), KeyboardType.Decimal)
            ZenField(tr("Gord."), fat, { fat = it }, Modifier.weight(1f), KeyboardType.Decimal)
        }
        Spacer(Modifier.height(10.dp))
        Text(tr("MEDIDA CASEIRA (OPCIONAL)"), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
        Text(tr("Para ovo, pão, fruta e afins: o app mostra em unidades e em gramas."), color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ZenField(tr("Unidade"), unitLabel, { unitLabel = it }, Modifier.weight(1f), placeholder = tr("ex.: ovo, fatia"))
            ZenField(tr("Peso (g)"), unitGrams, { unitGrams = it }, Modifier.weight(1f), KeyboardType.Decimal, placeholder = tr("ex.: 50"))
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Aka, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenButton(tr("Salvar"), {
                val unit = Portions.parseUnit(unitLabel, unitGrams)
                when (val r = FoodValidator.validate(name, kcal, protein, carbs, fat)) {
                    is FoodValidation.Invalid -> error = r.message
                    is FoodValidation.Valid -> when (unit) {
                        is Portions.UnitInput.Invalid -> error = unit.message
                        is Portions.UnitInput.Valid -> onSave(
                            Food(initial?.id ?: 0L, r.name, category, r.kcal, r.protein, r.carbs, r.fat, unit.label, unit.grams)
                        )
                    }
                }
            }, modifier = Modifier.weight(1f), accent = category.accent(), fill = true)
            ZenButton(tr("Cancelar"), onDismiss, accent = Zen.p.card)
        }
        if (onDelete != null) {
            Spacer(Modifier.height(6.dp))
            ZenButton(tr("Excluir alimento"), onDelete, accent = Aka, small = true)
        }
    }
}
