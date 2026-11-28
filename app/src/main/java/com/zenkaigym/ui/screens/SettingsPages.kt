package com.zenkaigym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenkaigym.domain.AppLanguage
import com.zenkaigym.domain.WeekStart
import com.zenkaigym.domain.tr
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.components.SectionTitle
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenCard
import com.zenkaigym.ui.components.ZenChip
import com.zenkaigym.ui.components.ZenDialog
import com.zenkaigym.ui.components.ZenIcon
import com.zenkaigym.ui.components.ZenIcons
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Sakura
import com.zenkaigym.ui.theme.Zen

/** Subpáginas de Ajustes. É estado interno da tela, não uma rota. */
enum class SettingsPage { TRAINING, REMINDERS, WEEK_LANGUAGE, BACKUP, ABOUT, DANGER }

/** Linha de Ajustes: título à esquerda, [value] e seta à direita. Sem [onClick] não é tocável (use [trailing] para um botão). */
@Composable
fun SettingsRow(
    title: String,
    value: String?,
    onClick: (() -> Unit)?,
    titleColor: Color = Zen.p.ink,
    trailing: (@Composable () -> Unit)? = null
) {
    val base = Modifier.fillMaxWidth().heightIn(min = 52.dp)
    Row(
        (if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, Modifier.weight(1f), color = titleColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (value != null) {
            Text(
                value, Modifier.padding(start = 8.dp), color = Zen.p.muted,
                style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (trailing != null) {
            Spacer(Modifier.padding(start = 8.dp))
            trailing()
        } else if (onClick != null) {
            Spacer(Modifier.padding(start = 6.dp))
            ZenIcon(ZenIcons.ChevronRight, color = Zen.p.muted)
        }
    }
}

/** Fio fino entre duas linhas do mesmo grupo. */
@Composable
fun SettingsDivider() {
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(Zen.p.border.copy(alpha = 0.18f)))
}

/** Modo treino e descanso entre séries. */
@Composable
fun TrainingPage(viewModel: ZenViewModel) {
    val keepOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val restPreset by viewModel.restPreset.collectAsStateWithLifecycle()

    ZenCard(accent = Ki, entranceIndex = 0) {
        SectionTitle(tr("Modo treino"), accent = Ki)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenChip(tr("Tela sempre acesa"), keepOn, { if (!keepOn) viewModel.toggleKeepScreenOn() }, Modifier.weight(1f), accent = Ki)
            ZenChip(tr("Normal"), !keepOn, { if (keepOn) viewModel.toggleKeepScreenOn() }, Modifier.weight(1f), accent = Ki)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (keepOn) tr("A tela do treino fica sempre ativa enquanto o modo treino estiver aberto, sem apagar no descanso.")
            else tr("A tela do treino apaga sozinha como as outras, conforme o tempo de tela do celular."),
            color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
        )
    }

    ZenCard(accent = Ki, entranceIndex = 1) {
        SectionTitle(tr("Descanso entre séries"), accent = Ki)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0, 60, 90, 120, 180).forEach { seconds ->
                ZenChip(
                    if (seconds == 0) tr("Auto") else seconds.toString(),
                    restPreset == seconds, { viewModel.setRestPreset(seconds) }, Modifier.weight(1f), accent = Ki, compact = true
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (restPreset == 0) tr("Automático: o descanso muda conforme as repetições da série (de 60 a 120 segundos).")
            else tr("Descanso fixo de {0} segundos depois de cada série.", restPreset),
            color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** Lembretes das refeições e do treino, e da água. */
@Composable
fun RemindersPage(viewModel: ZenViewModel, onReminders: (Boolean) -> Unit, onWaterReminders: (Boolean) -> Unit) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val waterReminders by viewModel.waterReminders.collectAsStateWithLifecycle()

    ZenCard(accent = Sakura, entranceIndex = 0) {
        SectionTitle(tr("Lembretes"), accent = Sakura)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenChip(tr("Ligados"), reminders, { if (!reminders) onReminders(true) }, Modifier.weight(1f), accent = Sakura)
            ZenChip(tr("Desligados"), !reminders, { if (reminders) onReminders(false) }, Modifier.weight(1f), accent = Sakura)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            tr("Avisos nos horários da sua agenda de refeições e de treino. Funciona sem internet, direto no aparelho."),
            color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        Text(tr("ÁGUA"), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenChip(tr("Ligados"), waterReminders, { if (!waterReminders) onWaterReminders(true) }, Modifier.weight(1f), accent = Ao)
            ZenChip(tr("Desligados"), !waterReminders, { if (waterReminders) onWaterReminders(false) }, Modifier.weight(1f), accent = Ao)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            tr("Um copo às 10h, 13h, 16h e 19h. Não avisa quando a meta do dia já foi batida."),
            color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** Primeiro dia da semana e idioma do app. */
@Composable
fun WeekLanguagePage(viewModel: ZenViewModel) {
    val weekStart by viewModel.weekStart.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()

    ZenCard(accent = Midori, entranceIndex = 0) {
        SectionTitle(tr("Semana"), accent = Midori)
        Spacer(Modifier.height(10.dp))
        Text(tr("A SEMANA COMEÇA NA"), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WeekStart.entries.forEach {
                ZenChip(it.label, weekStart == it, { viewModel.setWeekStart(it) }, Modifier.weight(1f), accent = Midori)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            tr("Define a ordem dos dias no treino, na dieta e na agenda, e onde cada semana começa nos gráficos de progresso."),
            color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
        )
    }

    ZenCard(accent = Ao, entranceIndex = 1) {
        SectionTitle(tr("Idioma"), accent = Ao)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppLanguage.entries.forEach {
                ZenChip(
                    if (it == AppLanguage.SYSTEM) tr("Sistema") else it.nativeName,
                    language == it, { viewModel.setLanguage(it) }, Modifier.fillMaxWidth(), accent = Ao
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (language == AppLanguage.SYSTEM) tr("Segue o idioma do seu celular.") else tr("Idioma fixo: {0}.", language.nativeName),
            color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** Exportar e importar. Os seletores de arquivo moram em [SettingsScreen] e chegam como funções. */
@Composable
fun BackupPage(onExport: () -> Unit, onImport: () -> Unit) {
    ZenCard(accent = Ao, entranceIndex = 0) {
        SectionTitle(tr("Seus dados"), accent = Ao)
        Spacer(Modifier.height(6.dp))
        Text(
            tr("Tudo fica salvo só neste aparelho, de forma local. Nada é enviado para a internet nem precisa de permissão de armazenamento."),
            color = Zen.p.muted
        )
        Spacer(Modifier.height(10.dp))
        Text(
            tr("Exporte tudo num arquivo .zip (perfil, fichas, dieta, treinos, peso, água e evolução) e importe esse mesmo arquivo para restaurar ou trocar de celular. Dentro dele há também planilhas CSV para abrir no Excel. O arquivo só vai para onde você escolher."),
            color = Zen.p.muted
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenButton(tr("Exportar dados"), onExport, Modifier.weight(1f), accent = Ao, fill = true)
            ZenButton(tr("Importar dados"), onImport, Modifier.weight(1f), accent = Zen.p.card, fill = true)
        }
    }
}

/** Aviso de saúde, privacidade, créditos, versão e o botão de rever a apresentação. */
@Composable
fun AboutPage(onReplayIntro: () -> Unit) {
    val context = LocalContext.current
    ZenCard(accent = Ao, entranceIndex = 0) {
        SectionTitle(tr("Aviso de saúde"), accent = Ao)
        Spacer(Modifier.height(6.dp))
        Text(
            tr("O Zenkaigym é uma ferramenta de apoio e não substitui médico, nutricionista ou educador físico. Metas de calorias, cardápios e fichas de treino são sugestões gerais. Antes de mudar a dieta ou começar a treinar, consulte um profissional de saúde, principalmente se tiver alguma condição médica, estiver grávida ou for menor de idade. Pare o exercício se sentir dor ou mal-estar."),
            color = Zen.p.muted
        )
        Spacer(Modifier.height(10.dp))
        SectionTitle(tr("Privacidade"), accent = Ao)
        Spacer(Modifier.height(6.dp))
        Text(
            tr("Não há conta, anúncios, rastreamento nem coleta de dados. Seus dados não saem do aparelho e não vão para backup na nuvem. Ao apagar os dados ou desinstalar o app, tudo é perdido."),
            color = Zen.p.muted
        )
        Spacer(Modifier.height(10.dp))
        var showCredits by remember { mutableStateOf(false) }
        ZenButton(tr("Créditos das imagens"), { showCredits = true }, accent = Zen.p.card, small = true)
        if (showCredits) ImageCreditsDialog { showCredits = false }
        Spacer(Modifier.height(10.dp))
        Text(tr("Zenkaigym versão {0}", versionName(context)), color = Zen.p.muted, style = MaterialTheme.typography.labelLarge)
    }

    ZenButton(tr("Rever apresentação"), onReplayIntro, accent = Zen.p.card, small = true)
}

/** Apagar tudo: o botão só abre a confirmação em [SettingsScreen]. */
@Composable
fun DangerPage(onWipe: () -> Unit) {
    ZenCard(accent = Aka, entranceIndex = 0) {
        SectionTitle(tr("Zona de perigo"), accent = Aka)
        Spacer(Modifier.height(6.dp))
        Text(tr("Apaga perfil, pesos, fichas, dietas, alimentos cadastrados e todo o histórico de treinos e refeições."), color = Zen.p.muted)
        Spacer(Modifier.height(12.dp))
        ZenButton(tr("Apagar todos os dados"), onWipe, accent = Aka, fill = true)
    }
}

/** Atribuição exigida pelas licenças Creative Commons das imagens (wger.de). */
@Composable
private fun ImageCreditsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val diagram = remember { com.zenkaigym.data.ExerciseImages.diagramCredit(context) }
    ZenDialog(
        tr("Créditos das imagens"), onDismiss, accent = Ao,
        footer = { ZenButton(tr("Fechar"), onDismiss, Modifier.fillMaxWidth(), accent = Zen.p.card, fill = true) }
    ) {
        Text(
            tr("O diagrama de músculos dos exercícios vem do wger.de e é licenciado pelo autor sob Creative Commons (CC-BY-SA). Foi redimensionado e colorido por exercício; continua sob a mesma licença."),
            color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
        )
        if (diagram != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                tr("Diagrama de músculos") + ": ${diagram.title} — ${diagram.author} · ${diagram.license}\n${diagram.source}",
                color = Zen.p.muted, style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
