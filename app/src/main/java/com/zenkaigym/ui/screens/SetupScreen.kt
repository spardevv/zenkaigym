package com.zenkaigym.ui.screens

import com.zenkaigym.domain.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenkaigym.data.entity.CardioPref
import com.zenkaigym.data.entity.Goal
import com.zenkaigym.data.entity.Sex
import com.zenkaigym.data.entity.UserProfile
import com.zenkaigym.domain.ProfileValidation
import com.zenkaigym.domain.ProfileValidator
import com.zenkaigym.domain.ThemeMode
import com.zenkaigym.domain.AppStyle
import com.zenkaigym.ui.theme.Sakura
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.components.FeedbackHost
import com.zenkaigym.ui.components.SectionTitle
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenCard
import com.zenkaigym.ui.components.ZenChip
import com.zenkaigym.ui.components.ZenField
import com.zenkaigym.ui.components.ZenHeader
import com.zenkaigym.ui.components.ZenPage
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Zen

/** Primeira abertura (depois da apresentação): aparência e dados básicos; já monta treino e dieta. */
@Composable
fun SetupScreen(viewModel: ZenViewModel) {
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val style by viewModel.style.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf(Sex.MALE) }
    var goal by remember { mutableStateOf(Goal.MAINTAIN) }
    var prefs by remember { mutableStateOf(TrainingChoices(cardio = CardioPref.WARMUP)) }
    // Cardio sugerido pelo objetivo, até o usuário mexer nele.
    var cardioTouched by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ZenPage {
        Column(
            Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ZenHeader(tr("Vamos começar"), "始", subtitle = tr("Seu treino e sua dieta saem prontos em seguida"))

            ZenCard(accent = Ao, entranceIndex = 0) {
                SectionTitle(tr("Aparência"), accent = Ao)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach {
                        ZenChip(it.label, theme == it, { viewModel.setThemeMode(it) }, Modifier.weight(1f), accent = Ao)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppStyle.entries.forEach {
                        ZenChip(it.label, style == it, { viewModel.setStyle(it) }, Modifier.weight(1f), accent = Sakura)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(tr("Você pode mudar isso depois em Ajustes."), color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
            }

            ZenCard(accent = Aka, entranceIndex = 1) {
                SectionTitle(tr("Seus dados"))
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ZenField(tr("Altura (cm)"), height, { height = it }, Modifier.weight(1f), KeyboardType.Number)
                    ZenField(tr("Peso (kg)"), weight, { weight = it }, Modifier.weight(1f), KeyboardType.Decimal)
                    ZenField(tr("Idade"), age, { age = it }, Modifier.weight(1f), KeyboardType.Number)
                }
                Spacer(Modifier.height(12.dp))
                Label(tr("Sexo"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Sex.entries.forEach { ZenChip(it.label, sex == it, { sex = it }, accent = Ao) }
                }
                Spacer(Modifier.height(12.dp))
                Label(tr("Objetivo"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Goal.entries.forEach {
                        ZenChip(it.label, goal == it, {
                            goal = it
                            if (!cardioTouched) prefs = prefs.copy(cardio = if (it == Goal.LOSE) CardioPref.FINISHER else CardioPref.WARMUP)
                        })
                    }
                }
                Spacer(Modifier.height(16.dp))
                Label(tr("Como você treina"))
                TrainingPrefs(prefs, { cardioTouched = cardioTouched || it.cardio != prefs.cardio; prefs = it }, accent = Ki)
                Spacer(Modifier.height(6.dp))
                Text(tr("Tudo fica salvo só neste aparelho."), color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
                error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = Aka, style = MaterialTheme.typography.labelLarge)
                }
            }

            ZenButton(tr("Começar"), {
                when (val r = ProfileValidator.validate(height, weight, age)) {
                    is ProfileValidation.Invalid -> error = r.message
                    is ProfileValidation.Valid -> {
                        error = null
                        viewModel.finishSetup(UserProfile(
                                heightCm = r.heightCm, weightKg = r.weightKg, age = r.age, sex = sex, goal = goal,
                                level = prefs.level, cardio = prefs.cardio, focus = prefs.focus, equipment = prefs.equipment
                            ))
                    }
                }
            }, fill = true, enabled = busy == null)
        }
        Box(Modifier.statusBarsPadding()) { FeedbackHost(busy, null, Modifier.align(Alignment.TopCenter)) }
    }
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 4.dp))
}
