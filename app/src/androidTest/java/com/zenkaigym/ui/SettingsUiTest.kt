package com.zenkaigym.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zenkaigym.domain.AppLanguage
import com.zenkaigym.domain.AppStyle
import com.zenkaigym.domain.WeekStart
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
        rule.onNodeWithText("Estilo, treino, dados e app").assertIsDisplayed()
        rule.onNodeWithText("Segue o modo claro/escuro do seu celular.").assertIsDisplayed()
    }

    @Test fun escolherTemaEscuroAtualizaEstadoEPreferencia() {
        rule.onNodeWithText("Escuro").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Fixo em modo escuro.").assertIsDisplayed()
        assertEquals(ThemeMode.DARK, app.viewModel.themeMode.value)
        assertEquals(ThemeMode.DARK, app.flags.themeMode)
    }

    @Test fun escolherEstiloKawaiiAtualizaEstadoEPreferencia() {
        rule.onNodeWithText("Kawaii").performClick()
        rule.waitForIdle()
        assertEquals(AppStyle.KAWAII, app.viewModel.style.value)
        assertEquals(AppStyle.KAWAII, app.flags.style)
        rule.onNodeWithText("Shōnen").performClick()
        rule.waitForIdle()
        assertEquals(AppStyle.SHONEN, app.flags.style)
    }

    /** Abre uma linha da página principal (pode estar abaixo da dobra). */
    private fun abrirLinha(titulo: String) {
        rule.onNodeWithText(titulo).performScrollTo().performClick()
        rule.waitForIdle()
    }

    @Test fun escolherSemanaComecandoNoDomingo() {
        abrirLinha("Semana e idioma")
        rule.onNodeWithText("Domingo").performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(WeekStart.SUNDAY, app.viewModel.weekStart.value)
        assertEquals(WeekStart.SUNDAY, app.flags.weekStart)
    }

    @Test fun trocarParaInglesTraduzAsTelasNaHora() {
        abrirLinha("Semana e idioma")
        rule.onNodeWithText("English (US)").performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(Lang.EN, I18n.lang)
        rule.onNodeWithText("Language fixed: English (US).").performScrollTo().assertIsDisplayed()
        assertEquals(AppLanguage.EN, app.flags.language)
    }

    @Test fun trocarParaJapones() {
        abrirLinha("Semana e idioma")
        rule.onNodeWithText("日本語").performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(Lang.JA, I18n.lang)
        // O botão de voltar da subpágina leva o nome da tela anterior.
        rule.onNodeWithText("設定").assertIsDisplayed()
    }

    @Test fun reverApresentacaoReabreAIntro() {
        app.viewModel.finishIntro()
        rule.waitForIdle()
        assertEquals(false, app.viewModel.showIntro.value)
        abrirLinha("Sobre")
        // ZenButton mostra o rótulo em maiúsculas.
        rule.onNodeWithText("Rever apresentação", ignoreCase = true).performScrollTo().performClick()
        rule.waitForIdle()
        assertEquals(true, app.viewModel.showIntro.value)
    }

    @Test fun mostraAsSeteLinhas() {
        // Os títulos de grupo saem em maiúsculas (SectionTitle).
        rule.onNodeWithText("Treino e lembretes", ignoreCase = true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Dados e app", ignoreCase = true).performScrollTo().assertIsDisplayed()
        listOf("Modo treino", "Lembretes", "Semana e idioma", "Backup", "Atualização", "Sobre", "Apagar todos os dados").forEach {
            rule.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
    }

    @Test fun voltarFechaASubpagina() {
        abrirLinha("Backup")
        rule.onNodeWithText("Treino e lembretes", ignoreCase = true).assertDoesNotExist()
        Espresso.pressBack()
        rule.waitForIdle()
        rule.onNodeWithText("Treino e lembretes", ignoreCase = true).assertIsDisplayed()
    }

    @Test fun atualizacaoTemVerificarNaLinha() {
        // O botão fica na própria linha, sem abrir subpágina; ZenButton escreve em maiúsculas.
        rule.onNodeWithText("Verificar", ignoreCase = true).performScrollTo().assertIsDisplayed()
    }
}
