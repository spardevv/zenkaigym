package com.zenkaigym.ui.screens

import com.zenkaigym.domain.tr
import android.content.Intent
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
fun SettingsScreen(viewModel: ZenViewModel) {
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val style by viewModel.style.collectAsStateWithLifecycle()
    val keepOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val weekStart by viewModel.weekStart.collectAsStateWithLifecycle()
    var confirming by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    val app = context.applicationContext as ZenApp

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { Backup.export(context, app.database, uri) }.isSuccess }
            Toast.makeText(context, if (ok) tr("Backup salvo!") else tr("Não foi possível salvar o backup."), Toast.LENGTH_LONG).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri
    }

    ZenPage {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ZenHeader(tr("Ajustes"), "設", accent = Ao, subtitle = tr("Aparência, estilo, treino, semana, idioma e informações"))

            ZenCard(accent = Ao, entranceIndex = 0) {
                SectionTitle(tr("Aparência"), accent = Ao)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach {
                        ZenChip(it.label, theme == it, { viewModel.setThemeMode(it) }, Modifier.weight(1f), accent = Ao)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (theme == ThemeMode.SYSTEM) tr("Segue o modo claro/escuro do seu celular.") else tr("Fixo em modo {0}.", theme.label.lowercase()),
                    color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
                )
            }

            ZenCard(accent = Sakura, entranceIndex = 1) {
                SectionTitle(tr("Estilo"), accent = Sakura)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppStyle.entries.forEach {
                        ZenChip(it.label, style == it, { viewModel.setStyle(it) }, Modifier.weight(1f), accent = Sakura)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (style == AppStyle.SHONEN) tr("Mangá de ação: bordas grossas, cores vivas e linhas de velocidade.")
                    else tr("Fofo: tons pastel de rosa, lilás, roxo e azul-bebê, cantos redondos e brilhos."),
                    color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
                )
            }

            ZenCard(accent = Ki, entranceIndex = 2) {
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

            ZenCard(accent = Midori, entranceIndex = 2) {
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

            ZenCard(accent = Ao, entranceIndex = 2) {
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

            ZenCard(accent = Ao, entranceIndex = 3) {
                SectionTitle(tr("Seus dados"), accent = Ao)
                Spacer(Modifier.height(6.dp))
                Text(
                    tr("Tudo fica salvo só neste aparelho, de forma local. Nada é enviado para a internet nem precisa de permissão de armazenamento."),
                    color = Zen.p.muted
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    tr("Para trocar de celular ou guardar uma cópia, exporte seus dados para um arquivo e importe no outro aparelho. O arquivo só vai para onde você escolher."),
                    color = Zen.p.muted
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ZenButton(tr("Exportar dados"), { exportLauncher.launch(Backup.suggestedName()) }, Modifier.weight(1f), accent = Ao, fill = true)
                    ZenButton(tr("Importar dados"), { importLauncher.launch(arrayOf("*/*")) }, Modifier.weight(1f), accent = Zen.p.card, fill = true)
                }
            }

            ZenCard(accent = Ao, entranceIndex = 4) {
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
                val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
                Text(tr("Zenkaigym versão {0}", version), color = Zen.p.muted, style = MaterialTheme.typography.labelLarge)
            }

            ZenButton(tr("Rever apresentação"), viewModel::replayIntro, accent = Zen.p.card, small = true)

            ZenCard(accent = Aka, entranceIndex = 5) {
                SectionTitle(tr("Zona de perigo"), accent = Aka)
                Spacer(Modifier.height(6.dp))
                Text(tr("Apaga perfil, pesos, fichas, dietas, alimentos cadastrados e todo o histórico de treinos e refeições."), color = Zen.p.muted)
                Spacer(Modifier.height(12.dp))
                ZenButton(tr("Apagar todos os dados"), { confirming = true }, accent = Aka, fill = true)
            }
        }
    }

    pendingImport?.let { uri ->
        ImportDialog(
            onConfirm = {
                pendingImport = null
                scope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { Backup.import(context, app.database, uri) }.getOrDefault(Backup.ImportResult.INVALID) }
                    when (result) {
                        Backup.ImportResult.OK -> {
                            Toast.makeText(context, tr("Dados importados! Reiniciando…"), Toast.LENGTH_LONG).show()
                            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            if (launch != null) context.startActivity(Intent.makeRestartActivityTask(launch.component))
                            Runtime.getRuntime().exit(0)
                        }
                        Backup.ImportResult.TOO_NEW ->
                            Toast.makeText(context, tr("Esse backup é de uma versão mais nova do app. Atualize o Zenkaigym."), Toast.LENGTH_LONG).show()
                        Backup.ImportResult.INVALID ->
                            Toast.makeText(context, tr("Esse arquivo não é um backup do Zenkaigym."), Toast.LENGTH_LONG).show()
                    }
                }
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
        Text(tr("Tudo o que está neste aparelho será substituído pelo conteúdo do backup. O app vai reiniciar."), color = Zen.p.muted)
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
