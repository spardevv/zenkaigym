package com.zenkaigym.ui

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zenkaigym.ui.nav.AppNav
import com.zenkaigym.ui.theme.ZenkaigymTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationUiTest {
    @get:Rule val rule = createComposeRule()
    private var app: TestApp? = null

    @After fun tearDown() { app?.close() }

    /** Cria o ambiente e o app; com [wide] simula uma tela larga (coluna lateral no lugar do selo do menu). */
    private fun start(wide: Boolean = false, setup: TestApp.() -> TestApp = { withProfile() }) {
        val a = TestApp.create().setup()
        app = a
        rule.setContent {
            ZenkaigymTheme(darkTheme = false) {
                if (wide) {
                    val config = Configuration(LocalConfiguration.current).apply { screenWidthDp = 900; screenHeightDp = 700 }
                    CompositionLocalProvider(LocalConfiguration provides config) { AppNav(a.viewModel) }
                } else {
                    AppNav(a.viewModel)
                }
            }
        }
        rule.waitUntil(10_000) { a.viewModel.ready.value }
    }

    /** Espera a transição terminar: durante ela as duas telas existem e o mesmo texto pode aparecer duas vezes. */
    private fun waitForText(text: String) {
        rule.waitUntil(5_000) { rule.onAllNodesWithTextCount(text) > 0 }
        rule.waitForIdle()
    }

    private fun openMenu() {
        rule.onNodeWithContentDescription("Abrir menu").performClick()
        waitForText("Para onde agora?")
    }

    @Test fun verNoCardapioAbreADieta() {
        start(setup = { withDiet() })
        rule.waitUntil(10_000) { rule.onAllNodesWithTextCount("FEITO") > 0 }
        rule.onNodeWithContentDescription("Mais opções da refeição").performClick()
        rule.onNodeWithText("Ver no cardápio").performClick()
        waitForText("Meta, horários e refeições")
    }

    @Test fun faixaDaSemanaAbreAEvolucao() {
        start()
        rule.onNodeWithContentDescription("Abrir evolução").performClick()
        waitForText("Treino, dieta, peso e conquistas")
    }

    @Test fun abreNoDojo() { start(); rule.onNodeWithContentDescription("Abrir menu").assertIsDisplayed() }

    @Test fun menuAbreCadaTela() {
        start()
        listOf(
            "TREINO" to "Dias, horários e ficha", "DIETA" to "Meta, horários e refeições",
            "EVOLUÇÃO" to "Treino, dieta, peso e conquistas", "PERFIL" to "Seus dados",
            "AJUSTES" to "Aparência, estilo, treino, semana, idioma e informações"
        ).forEach { (tile, subtitle) ->
            openMenu()
            rule.onNodeWithText(tile).performClick()
            waitForText(subtitle)
            rule.onNodeWithText("DOJO").performClick()
            rule.waitUntil(5_000) { rule.onAllNodes(hasContentDescription("Abrir menu")).fetchSemanticsNodes().isNotEmpty() }
            rule.waitForIdle()
            rule.onNodeWithContentDescription("Abrir menu").assertIsDisplayed()
        }
    }

    @Test fun menuSemDadosMostraMarcadores() {
        start(setup = { this })
        openMenu()
        rule.onNodeWithText("Sem ficha ainda").assertIsDisplayed()
        rule.onNodeWithText("Sem perfil ainda").assertIsDisplayed()
        rule.onNodeWithText("PERFIL").performClick()
        waitForText("Seus dados")
    }

    @Test fun voltarDeQualquerTelaLevaAoDojo() {
        start(); openMenu(); rule.onNodeWithText("TREINO").performClick(); waitForText("Dias, horários e ficha")
        Espresso.pressBack()
        rule.waitUntil(5_000) { rule.onAllNodesWithTextCount("Dias, horários e ficha") == 0 }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Abrir menu").assertIsDisplayed()
        assertEquals(0, rule.onAllNodesWithTextCount("Para onde agora?"))
    }

    @Test fun fecharMenuVoltaAoDojo() {
        start(); openMenu()
        rule.onNodeWithText("FECHAR").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTextCount("Para onde agora?") == 0 }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Abrir menu").assertIsDisplayed()
    }

    @Test fun dietaReabreNoCardapio() {
        // A aba escolhida não sobrevive à saída da tela: ao voltar à Dieta, abre de novo no Cardápio.
        start(); openMenu(); rule.onNodeWithText("DIETA").performClick(); waitForText("Meta, horários e refeições")
        rule.onNodeWithText("Alimentos").performClick()
        rule.onNodeWithText("Alimentos").assertIsSelected()
        Espresso.pressBack()
        rule.waitUntil(5_000) { rule.onAllNodesWithTextCount("Meta, horários e refeições") == 0 }
        rule.waitForIdle()
        openMenu(); rule.onNodeWithText("DIETA").performClick(); waitForText("Meta, horários e refeições")
        rule.onNodeWithText("Cardápio").assertIsSelected()
    }

    @Test fun telaLargaMostraColunaSemSelo() {
        start(wide = true)
        assertEquals(0, rule.onAllNodes(hasContentDescription("Abrir menu")).fetchSemanticsNodes().size)
        rule.onNodeWithText("TREINO").performClick(); waitForText("Dias, horários e ficha")
        Espresso.pressBack()
        rule.waitUntil(5_000) { rule.onAllNodesWithTextCount("Dias, horários e ficha") == 0 }
        assertEquals(0, rule.onAllNodesWithTextCount("Dias, horários e ficha"))
    }
}
