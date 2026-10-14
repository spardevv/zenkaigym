package com.zenkaigym

import android.os.Bundle
import androidx.activity.ComponentActivity
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
            ZenkaigymTheme(darkTheme = dark) {
                val ready by viewModel.ready.collectAsStateWithLifecycle()
                val showIntro by viewModel.showIntro.collectAsStateWithLifecycle()
                val showSetup by viewModel.showSetup.collectAsStateWithLifecycle()
                when {
                    !ready -> LoadingScreen()
                    showIntro -> IntroScreen(onFinish = viewModel::finishIntro)
                    showSetup -> SetupScreen(viewModel)
                    else -> AppNav(viewModel)
                }
            }
        }
    }

    private companion object {
        var freshProcess = true
    }
}
