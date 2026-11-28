package com.zenkaigym.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import com.zenkaigym.ui.components.ZenFixedHeader
import com.zenkaigym.ui.components.HeaderBar
import com.zenkaigym.notify.ReminderScheduler
import androidx.core.content.ContextCompat
import android.os.Build
import android.content.pm.PackageManager
import android.Manifest
import com.zenkaigym.domain.tr
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.zenkaigym.ZenApp
import com.zenkaigym.data.Backup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.getValue
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import com.zenkaigym.domain.SettingsSummary
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenkaigym.domain.AppLanguage
import com.zenkaigym.domain.AppStyle
import com.zenkaigym.domain.WeekStart
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Sakura
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.domain.ThemeMode
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.components.SectionTitle
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenCard
import com.zenkaigym.ui.components.ZenChip
import com.zenkaigym.ui.components.ZenHeader
import com.zenkaigym.ui.components.ZenPage
import com.zenkaigym.ui.components.currentWindowLayout
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.components.KanjiBadge
import com.zenkaigym.ui.components.ZenDialog
import com.zenkaigym.ui.components.ZenField
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import com.zenkaigym.ui.theme.Zen

@Composable
fun SettingsScreen(viewModel: ZenViewModel, onBack: () -> Unit = {}) {
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val style by viewModel.style.collectAsStateWithLifecycle()
    val keepOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val restPreset by viewModel.restPreset.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val waterReminders by viewModel.waterReminders.collectAsStateWithLifecycle()
    val weekStart by viewModel.weekStart.collectAsStateWithLifecycle()
    var confirming by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    val app = context.applicationContext as ZenApp

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) viewModel.exportData(context, uri)
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            Toast.makeText(context, tr("Sem a permissão de notificações os avisos não aparecem. Libere nas configurações do celular."), Toast.LENGTH_LONG).show()
        }
    }
    fun changeReminders(on: Boolean) {
        viewModel.setReminders(on)
        scope.launch(Dispatchers.IO) { ReminderScheduler.rescheduleAll(context) }
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (on && needsPermission) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    fun changeWaterReminders(on: Boolean) {
        viewModel.setWaterReminders(on)
        scope.launch(Dispatchers.IO) { ReminderScheduler.rescheduleAll(context) }
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (on && needsPermission) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri
    }

    var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    BackHandler(enabled = page != null) { page = null }

    ZenPage {
      Column(Modifier.fillMaxSize()) {
        val open = page
        if (open == null) {
            ZenFixedHeader(tr("Ajustes"), "設", accent = Ao, subtitle = tr("Aparência, estilo, treino, semana, idioma e informações"), back = { HeaderBar(onBack) })
        } else {
            val title = when (open) {
                SettingsPage.TRAINING -> tr("Modo treino")
                SettingsPage.REMINDERS -> tr("Lembretes")
                SettingsPage.WEEK_LANGUAGE -> tr("Semana e idioma")
                SettingsPage.BACKUP -> tr("Backup")
                SettingsPage.ABOUT -> tr("Sobre")
                SettingsPage.DANGER -> tr("Apagar todos os dados")
            }
            ZenFixedHeader(title, "設", accent = Ao, back = { HeaderBar({ page = null }, backLabel = tr("Ajustes")) })
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(start = currentWindowLayout.gutter, end = currentWindowLayout.gutter, top = 4.dp, bottom = currentWindowLayout.gutter),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (open) {
                null -> {
                    StyleCard(
                        style = style, theme = theme,
                        dark = theme.isDark(isSystemInDarkTheme()),
                        onStyle = { viewModel.setStyle(it) }, onTheme = { viewModel.setThemeMode(it) }
                    )

                    ZenCard(accent = Ki, entranceIndex = 1) {
                        SectionTitle(tr("Treino e lembretes"), accent = Ki)
                        Spacer(Modifier.height(4.dp))
                        SettingsRow(
                            tr("Modo treino"),
                            listOf(
                                if (keepOn) tr("Tela acesa") else tr("Normal"),
                                if (restPreset == 0) tr("Auto") else tr("{0} s", restPreset)
                            ).joinToString(" · "),
                            { page = SettingsPage.TRAINING }
                        )
                        SettingsDivider()
                        SettingsRow(tr("Lembretes"), SettingsSummary.reminders(reminders, waterReminders), { page = SettingsPage.REMINDERS })
                        SettingsDivider()
                        SettingsRow(
                            tr("Semana e idioma"), "${weekStart.label} · ${SettingsSummary.languageCode(language)}",
                            { page = SettingsPage.WEEK_LANGUAGE }
                        )
                    }

                    ZenCard(accent = Ao, entranceIndex = 2) {
                        SectionTitle(tr("Dados e app"), accent = Ao)
                        Spacer(Modifier.height(4.dp))
                        SettingsRow(tr("Backup"), tr("Exportar · Importar"), { page = SettingsPage.BACKUP })
                        SettingsDivider()
                        UpdateRow()
                        SettingsDivider()
                        SettingsRow(tr("Sobre"), null, { page = SettingsPage.ABOUT })
                        SettingsDivider()
                        SettingsRow(tr("Apagar todos os dados"), null, { page = SettingsPage.DANGER }, titleColor = Aka)
                    }
                }
                SettingsPage.TRAINING -> TrainingPage(viewModel)
                SettingsPage.REMINDERS -> RemindersPage(viewModel, ::changeReminders, ::changeWaterReminders)
                SettingsPage.WEEK_LANGUAGE -> WeekLanguagePage(viewModel)
                SettingsPage.BACKUP -> BackupPage(
                    onExport = { exportLauncher.launch(Backup.suggestedName()) },
                    onImport = { importLauncher.launch(arrayOf("*/*")) }
                )
                SettingsPage.ABOUT -> AboutPage(onReplayIntro = viewModel::replayIntro)
                SettingsPage.DANGER -> DangerPage(onWipe = { confirming = true })
            }
        }
      }
    }

    pendingImport?.let { uri ->
        ImportDialog(
            onConfirm = {
                pendingImport = null
                viewModel.importData(context, uri)
            },
            onDismiss = { pendingImport = null }
        )
    }

    if (confirming) WipeDialog(onConfirm = { confirming = false; viewModel.wipeAllData() }, onDismiss = { confirming = false })
}

/** Importar troca tudo o que existe no aparelho pelo conteúdo do backup. */
@Composable
private fun ImportDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ZenDialog(
        tr("Importar dados?"), onDismiss, accent = Aka,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZenButton(tr("Importar"), onConfirm, Modifier.weight(1f), accent = Aka, fill = true)
                ZenButton(tr("Cancelar"), onDismiss, Modifier.weight(1f), accent = Zen.p.card, fill = true)
            }
        }
    ) {
        Text(tr("Tudo o que está neste aparelho será substituído pelo conteúdo do backup."), color = Zen.p.muted)
    }
}

/**
 * Confirmação em duas etapas: o aviso explica o que se perde e o botão só libera
 * depois de digitar a palavra de confirmação, para não apagar tudo com um toque acidental.
 */
@Composable
private fun WipeDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val word = tr("APAGAR")
    var typed by remember { mutableStateOf("") }
    val ready = typed.trim().equals(word, ignoreCase = true)
    // Pequeno tremor no aviso ao abrir: chama atenção para a gravidade.
    val shake = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        listOf(8f, -8f, 5f, -5f, 0f).forEach { shake.animateTo(it, tween(55)) }
    }

    ZenDialog(
        tr("Apagar tudo?"), onDismiss, accent = Aka,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZenButton(tr("Apagar"), onConfirm, Modifier.weight(1f), accent = Aka, enabled = ready, fill = true)
                ZenButton(tr("Cancelar"), onDismiss, Modifier.weight(1f), accent = Zen.p.card, fill = true)
            }
        }
    ) {
        Row(Modifier.graphicsLayer { translationX = shake.value.dp.toPx() }, verticalAlignment = Alignment.CenterVertically) {
            KanjiBadge("危", color = Aka, size = 42.dp)
            Text(
                tr("Esta ação não pode ser desfeita."),
                Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(tr("Serão apagados: perfil e pesos, fichas de treino, séries registradas, dietas, alimentos e refeições marcadas. Tema, estilo, semana e idioma continuam como estão."))
        Spacer(Modifier.height(14.dp))
        ZenField(tr("Digite {0} para confirmar", word), typed, { typed = it }, placeholder = word)
    }
}
