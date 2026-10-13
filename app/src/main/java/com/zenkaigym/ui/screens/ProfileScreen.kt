package com.zenkaigym.ui.screens

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
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Zen
import com.zenkaigym.util.formatEpochDay
import com.zenkaigym.util.toDecimalOrNull

@Composable
fun ProfileScreen(viewModel: ZenViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val weights by viewModel.weights.collectAsStateWithLifecycle()

    // O formulário é inicializado uma vez, quando o perfil salvo existe.
    val hasProfile = profile != null
    var height by remember(hasProfile) { mutableStateOf(profile?.heightCm?.toString() ?: "") }
    var weight by remember(hasProfile) { mutableStateOf(profile?.weightKg?.let { "%.1f".format(it) } ?: "") }
    var age by remember(hasProfile) { mutableStateOf(profile?.age?.toString() ?: "") }
    var sex by remember(hasProfile) { mutableStateOf(profile?.sex ?: Sex.MALE) }
    var goal by remember(hasProfile) { mutableStateOf(profile?.goal ?: Goal.MAINTAIN) }
    var error by remember { mutableStateOf<String?>(null) }

    ZenPage {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ZenHeader("Perfil", "己", subtitle = "Seus dados")

            ZenCard(accent = Aka, entranceIndex = 0) {
                SectionTitle("Corpo")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ZenField("Altura (cm)", height, { height = it }, Modifier.weight(1f), KeyboardType.Number)
                    ZenField("Peso (kg)", weight, { weight = it }, Modifier.weight(1f), KeyboardType.Decimal)
                    ZenField("Idade", age, { age = it }, Modifier.weight(1f), KeyboardType.Number)
                }
                Spacer(Modifier.height(12.dp))
                Label("Sexo")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Sex.entries.forEach { ZenChip(it.label, sex == it, { sex = it }, accent = Ao) }
                }
                Spacer(Modifier.height(12.dp))
                Label("Objetivo")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Goal.entries.forEach { ZenChip(it.label, goal == it, { goal = it }) }
                }
                error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = Aka, style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(14.dp))
                ZenButton("Salvar", {
                    when (val result = ProfileValidator.validate(height, weight, age)) {
                        is ProfileValidation.Invalid -> error = result.message
                        is ProfileValidation.Valid -> {
                            error = null
                            viewModel.saveProfile(
                                UserProfile(heightCm = result.heightCm, weightKg = result.weightKg, age = result.age, sex = sex, goal = goal)
                            )
                        }
                    }
                }, fill = true)
            }

            profile?.let { WeightCard(it, weights, viewModel::addWeight) }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
private fun WeightCard(profile: UserProfile, weights: List<WeightEntry>, onRegister: (Double) -> Unit) {
    var input by remember { mutableStateOf("") }
    ZenCard(accent = Ao, entranceIndex = 1) {
        SectionTitle("Evolução do peso", accent = Ao)
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
            ZenField("Novo peso (kg)", input, { input = it }, Modifier.weight(1f), KeyboardType.Decimal)
            ZenButton("Registrar", {
                val kg = input.toDecimalOrNull()
                if (kg != null && kg in 30.0..300.0) {
                    onRegister(kg)
                    input = ""
                }
            })
        }
    }
}

@Composable
private fun WeightChart(entries: List<WeightEntry>) {
    val line = Aka
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
