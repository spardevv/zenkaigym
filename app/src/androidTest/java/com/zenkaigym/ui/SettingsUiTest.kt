package com.zenkaigym.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zenkaigym.domain.AppLanguage
import com.zenkaigym.domain.I18n
import com.zenkaigym.domain.Lang
import com.zenkaigym.domain.ThemeMode
import com.zenkaigym.ui.screens.SettingsScreen
import com.zenkaigym.ui.theme.ZenkaigymTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsUiTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var app: TestApp

    @Before fun setUp() {
        app = TestApp.create()
        rule.setContent { ZenkaigymTheme(darkTheme = false) { SettingsScreen(app.viewModel) } }
    }

    @After fun tearDown() = app.close()

    @Test fun mostraOsAjustesNoIdiomaPadrao() {
        rule.onNodeWithText("Aparência, idioma e informações").assertIsDisplayed()
        rule.onNodeWithText("Segue o modo claro/escuro do seu celular.").assertIsDisplayed()
    }

    @Test fun escolherTemaEscuroAtualizaEstadoEPreferencia() {
        rule.onNodeWithText("Escuro").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Fixo em modo escuro.").assertIsDisplayed()
        assertEquals(ThemeMode.DARK, app.viewModel.themeMode.value)
        assertEquals(ThemeMode.DARK, app.flags.themeMode)
    }

    @Test fun trocarParaInglesTraduzAsTelasNaHora() {
        rule.onNodeWithText("English (US)").performClick()
        rule.waitForIdle()
        assertEquals(Lang.EN, I18n.lang)
        rule.onNodeWithText("Language fixed: English (US).").assertIsDisplayed()
        assertEquals(AppLanguage.EN, app.flags.language)
    }

    @Test fun trocarParaJapones() {
        rule.onNodeWithText("日本語").performClick()
        rule.waitForIdle()
        assertEquals(Lang.JA, I18n.lang)
        rule.onNodeWithText("設定").assertIsDisplayed()
    }

    @Test fun reverApresentacaoReabreAIntro() {
        app.viewModel.finishIntro()
        rule.waitForIdle()
        assertEquals(false, app.viewModel.showIntro.value)
        rule.onNodeWithText("Rever apresentação").performClick()
        rule.waitForIdle()
        assertEquals(true, app.viewModel.showIntro.value)
    }
}
