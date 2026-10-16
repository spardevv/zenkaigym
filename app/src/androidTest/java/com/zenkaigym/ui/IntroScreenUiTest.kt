package com.zenkaigym.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zenkaigym.domain.AppLanguage
import com.zenkaigym.domain.I18n
import com.zenkaigym.ui.screens.IntroScreen
import com.zenkaigym.ui.theme.ZenkaigymTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntroScreenUiTest {
    @get:Rule val rule = createComposeRule()
    private var finished = 0

    @Before fun setUp() {
        I18n.select(AppLanguage.PT, "pt")
        finished = 0
        rule.setContent { ZenkaigymTheme(darkTheme = false) { IntroScreen(onFinish = { finished++ }) } }
    }

    @After fun tearDown() = I18n.select(AppLanguage.PT, "pt")

    private fun click(text: String) {
        rule.onNodeWithText(text).performClick()
        rule.waitForIdle()
    }

    @Test fun comecaNaPrimeiraPaginaSemBotaoVoltar() {
        rule.onNodeWithText("BEM-VINDO AO ZENKAIGYM").assertIsDisplayed()
        rule.onNodeWithText("Voltar").assertDoesNotExist()
        rule.onNodeWithText("Pular").assertIsDisplayed()
    }

    @Test fun proximoEVoltarNavegamEntrePaginas() {
        click("Próximo")
        rule.waitUntil(5_000) { rule.onAllNodesWithTextCount("CONTE QUEM VOCÊ É") > 0 }
        rule.onNodeWithText("Voltar").assertIsDisplayed()
        click("Voltar")
        rule.waitUntil(5_000) { rule.onAllNodesWithTextCount("BEM-VINDO AO ZENKAIGYM") > 0 }
        rule.onNodeWithText("Voltar").assertDoesNotExist()
    }

    @Test fun pularConcluiAApresentacao() {
        click("Pular")
        assertEquals(1, finished)
    }

    @Test fun naUltimaPaginaOBotaoViraComecarEPularSome() {
        repeat(4) {
            click("Próximo")
            rule.waitForIdle()
        }
        rule.waitUntil(5_000) { rule.onAllNodesWithTextCount("Começar") > 0 }
        rule.onNodeWithText("Pular").assertDoesNotExist()
        click("Começar")
        assertEquals(1, finished)
    }
}
