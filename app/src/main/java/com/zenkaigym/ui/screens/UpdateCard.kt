package com.zenkaigym.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zenkaigym.domain.LatestRelease
import com.zenkaigym.domain.UpdateCheck
import com.zenkaigym.domain.tr
import com.zenkaigym.ui.components.SectionTitle
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenCard
import com.zenkaigym.ui.components.ZenIcons
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Zen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object Error : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: LatestRelease) : UpdateState
}

/** Verifica a última versão do APK no GitHub (só ao toque) e leva ao download, que abre no navegador. */
@Composable
fun UpdateCard(entranceIndex: Int = 0) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val current = remember { versionName(context) }
    var state by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }

    fun check() {
        state = UpdateState.Checking
        scope.launch {
            val latest = withContext(Dispatchers.IO) { UpdateCheck.fetchLatest() }
            state = when {
                latest == null -> UpdateState.Error
                UpdateCheck.isNewer(latest.version, current) -> UpdateState.Available(latest)
                else -> UpdateState.UpToDate
            }
        }
    }

    ZenCard(accent = Midori, entranceIndex = entranceIndex) {
        SectionTitle(tr("Atualização"), accent = Midori)
        Spacer(Modifier.height(6.dp))
        Text(
            when (val s = state) {
                UpdateState.Idle -> tr("Zenkaigym versão {0}", current)
                UpdateState.Checking -> tr("Verificando…")
                UpdateState.Error -> tr("Não foi possível verificar. Confira a conexão.")
                UpdateState.UpToDate -> tr("Você já está na última versão ({0}).", current)
                is UpdateState.Available -> tr("Nova versão {0} disponível (você tem {1}).", s.release.version, current)
            },
            color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ZenButton(
                tr("Verificar atualização"), ::check, accent = Zen.p.card, small = true,
                enabled = state != UpdateState.Checking
            )
            (state as? UpdateState.Available)?.let { available ->
                ZenButton(
                    tr("Baixar"), { openUrl(context, available.release.downloadUrl) },
                    accent = Midori, small = true, trailing = ZenIcons.ArrowRight
                )
            }
        }
    }
}

private fun versionName(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull() ?: "?"

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
