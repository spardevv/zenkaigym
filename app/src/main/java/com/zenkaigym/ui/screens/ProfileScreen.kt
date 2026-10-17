package com.zenkaigym.ui.screens

import com.zenkaigym.domain.tr
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenkaigym.data.entity.Goal
import com.zenkaigym.data.entity.Sex
import com.zenkaigym.data.entity.UserProfile
import com.zenkaigym.data.entity.WeightEntry
import com.zenkaigym.domain.Motion
import com.zenkaigym.domain.ProfileValidation
import com.zenkaigym.domain.ProfileValidator
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.components.SectionTitle
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenCard
import com.zenkaigym.ui.components.ZenChip
import com.zenkaigym.ui.components.ZenField
import com.zenkaigym.ui.components.ZenHeader
import com.zenkaigym.ui.components.ZenPage
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Daidai
import com.zenkaigym.ui.theme.Zen
import com.zenkaigym.util.formatEpochDay
import com.zenkaigym.util.toDecimalOrNull
import androidx.compose.animation.AnimatedVisibility
import com.zenkaigym.ui.components.Conceal
import com.zenkaigym.ui.components.Reveal
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.zenkaigym.ui.components.KanjiBadge
import com.zenkaigym.ui.components.StatTile
import com.zenkaigym.ui.components.ZenBarChart
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Sumi

@Composable
fun ProfileScreen(viewModel: ZenViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val weights by viewModel.weights.collectAsStateWithLifecycle()

    // O formulário recomeça do perfil salvo sempre que ele muda (ao salvar ou ao registrar um peso novo).
    var height by remember(profile) { mutableStateOf(profile?.heightCm?.toString() ?: "") }
    var weight by remember(profile) { mutableStateOf(profile?.weightKg?.let { "%.1f".format(it) } ?: "") }
    var age by remember(profile) { mutableStateOf(profile?.age?.toString() ?: "") }
    var sex by remember(profile) { mutableStateOf(profile?.sex ?: Sex.MALE) }
    var goal by remember(profile) { mutableStateOf(profile?.goal ?: Goal.MAINTAIN) }
    var error by remember(profile) { mutableStateOf<String?>(null) }

    // Sem perfil ainda, salvar é sempre possível; com perfil, só quando algo mudou.
    val changed = profile?.let { saved ->
        height.trim() != saved.heightCm.toString() ||
            weight.toDecimalOrNull()?.let { abs(it - saved.weightKg) >= 0.05 } != false ||
            age.trim() != saved.age.toString() ||
            sex != saved.sex ||
            goal != saved.goal
    } ?: true

    fun reset() {
        profile?.let {
            height = it.heightCm.toString()
            weight = "%.1f".format(it.weightKg)
            age = it.age.toString()
            sex = it.sex
            goal = it.goal
        }
        error = null
    }

    ZenPage {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ZenHeader(tr("Perfil"), "己", accent = Daidai, subtitle = tr("Seus dados"))

            ZenCard(accent = Daidai, entranceIndex = 0) {
                SectionTitle(tr("Corpo"), accent = Daidai)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ZenField(tr("Altura (cm)"), height, { height = it }, Modifier.weight(1f), KeyboardType.Number)
                    ZenField(tr("Peso (kg)"), weight, { weight = it }, Modifier.weight(1f), KeyboardType.Decimal)
                    ZenField(tr("Idade"), age, { age = it }, Modifier.weight(1f), KeyboardType.Number)
                }
                Spacer(Modifier.height(12.dp))
                Label(tr("Sexo"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Sex.entries.forEach { ZenChip(it.label, sex == it, { sex = it }, accent = Daidai) }
                }
                Spacer(Modifier.height(12.dp))
                Label(tr("Objetivo"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Goal.entries.forEach { ZenChip(it.label, goal == it, { goal = it }, accent = Daidai) }
                }
                AnimatedVisibility(error != null, enter = Reveal, exit = Conceal) {
                    Text(error.orEmpty(), Modifier.padding(top = 10.dp), color = Aka, style = MaterialTheme.typography.labelLarge)
                }
                // Salvar (e desfazer) só aparecem quando há algo diferente do perfil salvo.
                AnimatedVisibility(changed, enter = Reveal, exit = Conceal) {
                    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ZenButton(tr("Salvar"), {
                            when (val result = ProfileValidator.validate(height, weight, age)) {
                                is ProfileValidation.Invalid -> error = result.message
                                is ProfileValidation.Valid -> {
                                    error = null
                                    viewModel.saveProfile(
                                        UserProfile(heightCm = result.heightCm, weightKg = result.weightKg, age = result.age, sex = sex, goal = goal)
                                    )
                                }
                            }
                        }, Modifier.weight(1f), accent = Daidai, fill = true)
                        if (profile != null) ZenButton(tr("Desfazer"), ::reset, accent = Zen.p.card)
                    }
                }
            }

            profile?.let {
                WeightCard(it, weights, viewModel::addWeight)
                ProgressCards(viewModel)
            }
        }
    }
}

/** Evolução do treino (volume e sessões por semana) e aderência da dieta (refeições feitas por dia). */
@Composable
private fun ProgressCards(viewModel: ZenViewModel) {
    val training by viewModel.trainingProgress.collectAsStateWithLifecycle()
    val diet by viewModel.dietProgress.collectAsStateWithLifecycle()
    val clock by viewModel.clock.collectAsStateWithLifecycle()

    ZenCard(accent = Ki, entranceIndex = 2) {
        SectionTitle(tr("Evolução do treino"), accent = Ki)
        Spacer(Modifier.height(10.dp))
        val t = training
        if (t == null || !t.hasData) {
            EmptyProgress("体", Ki, tr("Registre suas séries no modo treino para ver o volume de cada semana aqui."))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(tr("Treinos · 30 dias"), t.sessionsLast30, Modifier.weight(1f))
                StatTile(tr("Volume · semana"), t.weeklyVolume.last().roundToInt(), Modifier.weight(1f), format = ::volumeText)
            }
            t.volumeChange?.let { change ->
                Spacer(Modifier.height(8.dp))
                Text(
                    if (change >= 0) tr("▲ {0}% sobre a semana passada", change) else tr("▼ {0}% sobre a semana passada", -change),
                    color = Sumi,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(if (change >= 0) Midori else Daidai).padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            ZenBarChart(
                values = t.weeklyVolume.map { it.toFloat() },
                color = Ki,
                describe = { i ->
                    val back = t.weeklyVolume.size - 1 - i
                    val week = if (back == 0) tr("Esta semana") else tr("Há {0} sem.", back)
                    tr("{0} · {1} treinos · {2}", week, t.weeklySessions[i], volumeText(t.weeklyVolume[i].roundToInt()))
                },
                startLabel = tr("{0} semanas atrás", t.weeklyVolume.size - 1),
                endLabel = tr("Agora")
            )
            Spacer(Modifier.height(4.dp))
            Text(tr("Volume = carga × repetições. Toque numa barra para ver a semana."), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
        }
    }

    ZenCard(accent = Midori, entranceIndex = 3) {
        SectionTitle(tr("Constância na dieta"), accent = Midori)
        Spacer(Modifier.height(10.dp))
        val d = diet
        if (d == null || !d.hasData) {
            EmptyProgress("食", Midori, tr("Marque as refeições como feitas no Dojo para acompanhar sua constância."))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(tr("Média · 14 dias"), d.averagePercent, Modifier.weight(1f), format = { "$it%" })
                StatTile(
                    tr("Sequência"), d.streak, Modifier.weight(1f),
                    format = { tr("{0} dias", it) },
                    accent = if (d.streak >= 3) Midori else null
                )
            }
            Spacer(Modifier.height(12.dp))
            ZenBarChart(
                values = d.daily,
                color = Midori,
                maxValue = 1f,
                describe = { i ->
                    val day = clock.epochDay - (d.daily.size - 1 - i)
                    tr("{0} · {1}% das refeições", formatEpochDay(day), (d.daily[i] * 100).roundToInt())
                },
                startLabel = formatEpochDay(clock.epochDay - (d.daily.size - 1)),
                endLabel = tr("Hoje")
            )
        }
    }
}

@Composable
private fun EmptyProgress(kanji: String, accent: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        KanjiBadge(kanji, color = accent, size = 40.dp)
        Text(text, Modifier.padding(start = 12.dp), color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Volume legível: até 999 em kg, depois em toneladas. */
private fun volumeText(kg: Int): String = if (kg >= 1000) "%.1f t".format(kg / 1000f) else "$kg kg"

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
private fun WeightCard(profile: UserProfile, weights: List<WeightEntry>, onRegister: (Double) -> Unit) {
    var input by remember { mutableStateOf("") }
    ZenCard(accent = Daidai, entranceIndex = 1) {
        SectionTitle(tr("Evolução do peso"), accent = Daidai)
        Spacer(Modifier.height(8.dp))
        Text("%.1f kg".format(profile.weightKg), style = MaterialTheme.typography.displayMedium)
        if (weights.size >= 2) {
            val recent = weights.takeLast(12)
            Spacer(Modifier.height(10.dp))
            WeightChart(recent)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatEpochDay(recent.first().epochDay), style = MaterialTheme.typography.labelSmall, color = Zen.p.muted)
                Text(formatEpochDay(recent.last().epochDay), style = MaterialTheme.typography.labelSmall, color = Zen.p.muted)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ZenField(tr("Novo peso (kg)"), input, { input = it }, Modifier.weight(1f), KeyboardType.Decimal)
            ZenButton(tr("Registrar"), {
                val kg = input.toDecimalOrNull()
                if (kg != null && kg in 30.0..300.0) {
                    onRegister(kg)
                    input = ""
                }
            }, accent = Daidai)
        }
    }
}

@Composable
private fun WeightChart(entries: List<WeightEntry>) {
    val line = Daidai
    val progress = remember { Animatable(0f) }
    LaunchedEffect(entries.size) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }
    Canvas(Modifier.fillMaxWidth().height(80.dp)) {
        val min = entries.minOf { it.weightKg }
        val max = entries.maxOf { it.weightKg }
        val range = (max - min).coerceAtLeast(0.5)
        val pad = 8.dp.toPx()
        fun point(i: Int) = Offset(
            pad + (size.width - 2 * pad) * i / (entries.size - 1),
            pad + (size.height - 2 * pad) * (1f - ((entries[i].weightKg - min) / range).toFloat())
        )
        val fractions = Motion.segmentFractions(progress.value, entries.size - 1)
        drawCircle(line, 5.dp.toPx(), point(0))
        fractions.forEachIndexed { index, fraction ->
            if (fraction > 0f) {
                val start = point(index)
                val end = point(index + 1)
                drawLine(line, start, start + (end - start) * fraction, strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(line, 5.dp.toPx() * fraction, point(index + 1))
            }
        }
    }
}
