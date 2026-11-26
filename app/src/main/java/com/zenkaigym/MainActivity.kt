package com.zenkaigym

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import com.zenkaigym.notify.ReminderScheduler
import com.zenkaigym.notify.RestTimer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.zenkaigym.domain.tr
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zenkaigym.domain.I18n
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.nav.AppNav
import com.zenkaigym.ui.screens.IntroScreen
import com.zenkaigym.ui.screens.LoadingScreen
import com.zenkaigym.ui.screens.SetupScreen
import com.zenkaigym.ui.theme.ZenkaigymTheme

class MainActivity : ComponentActivity() {
    private val viewModel: ZenViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = application as ZenApp
                ZenViewModel(app.repository, app.introGate, app.appFlags)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Processo novo (inclusive após o sistema matá-lo): descarta o estado salvo para abrir sempre no Dojo.
        // Rotação e troca de tema no mesmo processo continuam restaurando normalmente.
        super.onCreate(if (freshProcess) null else savedInstanceState)
        freshProcess = false
        enableEdgeToEdge()
        I18n.select((application as ZenApp).appFlags.language)
        setContent {
            val mode by viewModel.themeMode.collectAsStateWithLifecycle()
            val dark = mode.isDark(isSystemInDarkTheme())
            // As barras do sistema acompanham o tema escolhido, não só o do sistema.
            DisposableEffect(dark) {
                val bars = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                onDispose { }
            }
            val style by viewModel.style.collectAsStateWithLifecycle()
            val weekStart by viewModel.weekStart.collectAsStateWithLifecycle()
            ZenkaigymTheme(darkTheme = dark, style = style, weekStart = weekStart) {
                val ready by viewModel.ready.collectAsStateWithLifecycle()
                val showIntro by viewModel.showIntro.collectAsStateWithLifecycle()
                val showSetup by viewModel.showSetup.collectAsStateWithLifecycle()
                val importing by viewModel.importing.collectAsStateWithLifecycle()
                // Durante a importação o botão voltar não fecha o app: a tela fica até os dados chegarem.
                BackHandler(enabled = importing) { }
                Box(Modifier.fillMaxSize()) {
                    when {
                        !ready -> LoadingScreen()
                        showIntro -> IntroScreen(onFinish = viewModel::finishIntro)
                        showSetup -> SetupScreen(viewModel)
                        else -> AppNav(viewModel)
                    }
                    // Por cima das telas (que continuam montadas e recebem os dados novos), engolindo os toques.
                    if (importing) {
                        Box(
                            Modifier.fillMaxSize().pointerInput(Unit) {
                                awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
                            }
                        ) { LoadingScreen(tr("Importando seus dados…")) }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        RestTimer.setForeground(true)
    }

    override fun onStop() {
        super.onStop()
        // Fora da tela, o descanso em andamento passa para a notificação com cronômetro.
        RestTimer.setForeground(false)
        // Horários de refeição e treino podem ter mudado: os alarmes acompanham a agenda atual.
        val app = application as ZenApp
        if (app.appFlags.remindersEnabled || app.appFlags.waterReminders) {
            CoroutineScope(Dispatchers.IO).launch { ReminderScheduler.rescheduleAll(app) }
        }
    }

    private companion object {
        var freshProcess = true
    }
}
