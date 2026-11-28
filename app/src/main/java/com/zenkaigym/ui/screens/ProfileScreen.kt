package com.zenkaigym.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import com.zenkaigym.ui.components.ZenFixedHeader
import com.zenkaigym.ui.components.HeaderBar
import com.zenkaigym.domain.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenkaigym.data.entity.Goal
import com.zenkaigym.data.entity.Sex
import com.zenkaigym.data.entity.UserProfile
import com.zenkaigym.domain.ProfileValidation
import com.zenkaigym.domain.ProfileValidator
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.components.SectionTitle
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenCard
import com.zenkaigym.ui.components.ZenChip
import com.zenkaigym.ui.components.ZenField
import com.zenkaigym.ui.components.ZenPage
import com.zenkaigym.ui.components.currentWindowLayout
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Sakura
import com.zenkaigym.ui.theme.Zen
import com.zenkaigym.util.toDecimalOrNull
import androidx.compose.animation.AnimatedVisibility
import com.zenkaigym.ui.components.Conceal
import com.zenkaigym.ui.components.Reveal
import kotlin.math.abs

@Composable
fun ProfileScreen(viewModel: ZenViewModel, onBack: () -> Unit) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    // O formulário recomeça do perfil salvo sempre que ele muda (ao salvar ou ao registrar um peso novo).
    var height by rememberSaveable(profile) { mutableStateOf(profile?.heightCm?.toString() ?: "") }
    var weight by rememberSaveable(profile) { mutableStateOf(profile?.weightKg?.let { "%.1f".format(it) } ?: "") }
    var age by rememberSaveable(profile) { mutableStateOf(profile?.age?.toString() ?: "") }
    var sex by rememberSaveable(profile) { mutableStateOf(profile?.sex ?: Sex.MALE) }
    var goal by rememberSaveable(profile) { mutableStateOf(profile?.goal ?: Goal.MAINTAIN) }
    var prefs by remember(profile) {
        mutableStateOf(profile?.let { TrainingChoices(it.level, it.cardio, it.focus, it.equipment) } ?: TrainingChoices())
    }
    var error by remember(profile) { mutableStateOf<String?>(null) }

    // Sem perfil ainda, salvar é sempre possível; com perfil, só quando algo mudou.
    val changed = profile?.let { saved ->
        height.trim() != saved.heightCm.toString() ||
            weight.toDecimalOrNull()?.let { abs(it - saved.weightKg) >= 0.05 } != false ||
            age.trim() != saved.age.toString() ||
            sex != saved.sex ||
            goal != saved.goal ||
            prefs != TrainingChoices(saved.level, saved.cardio, saved.focus, saved.equipment)
    } ?: true

    fun reset() {
        profile?.let {
            height = it.heightCm.toString()
            weight = "%.1f".format(it.weightKg)
            age = it.age.toString()
            sex = it.sex
            goal = it.goal
            prefs = TrainingChoices(it.level, it.cardio, it.focus, it.equipment)
        }
        error = null
    }

    ZenPage {
      Column(Modifier.fillMaxSize()) {
        ZenFixedHeader(tr("Perfil"), "己", accent = Sakura, subtitle = tr("Seus dados"), back = { HeaderBar(onBack) })
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(start = currentWindowLayout.gutter, end = currentWindowLayout.gutter, top = 4.dp, bottom = currentWindowLayout.gutter),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ZenCard(accent = Sakura, entranceIndex = 0) {
                SectionTitle(tr("Corpo"), accent = Sakura)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ZenField(tr("Altura (cm)"), height, { height = it }, Modifier.weight(1f), KeyboardType.Number, imeAction = ImeAction.Next)
                    ZenField(tr("Peso (kg)"), weight, { weight = it }, Modifier.weight(1f), KeyboardType.Decimal, imeAction = ImeAction.Next)
                    ZenField(tr("Idade"), age, { age = it }, Modifier.weight(1f), KeyboardType.Number)
                }
                Spacer(Modifier.height(12.dp))
                Label(tr("Sexo"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Sex.entries.forEach { ZenChip(it.label, sex == it, { sex = it }, accent = Sakura) }
                }
                Spacer(Modifier.height(12.dp))
                Label(tr("Objetivo"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Goal.entries.forEach { ZenChip(it.label, goal == it, { goal = it }, accent = Sakura) }
                }
                Spacer(Modifier.height(16.dp))
                SectionTitle(tr("Treino"), accent = Sakura)
                Spacer(Modifier.height(10.dp))
                TrainingPrefs(prefs, { prefs = it }, accent = Sakura)
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
                                        UserProfile(
                                            heightCm = result.heightCm, weightKg = result.weightKg, age = result.age, sex = sex, goal = goal,
                                            level = prefs.level, cardio = prefs.cardio, focus = prefs.focus, equipment = prefs.equipment
                                        )
                                    )
                                }
                            }
                        }, Modifier.weight(1f), accent = Sakura, fill = true)
                        if (profile != null) ZenButton(tr("Desfazer"), ::reset, accent = Zen.p.card)
                    }
                }
            }
        }
      }
    }
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 4.dp))
}
