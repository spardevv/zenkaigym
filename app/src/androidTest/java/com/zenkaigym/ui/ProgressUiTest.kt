package com.zenkaigym.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zenkaigym.ui.screens.ProgressScreen
import com.zenkaigym.ui.theme.ZenkaigymTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressUiTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var app: TestApp
    private var backs = 0

    @After fun tearDown() { app.close() }

    /** [alturaDp] limita a altura da tela: com pouco espaço o conteúdo da aba sempre precisa rolar. */
    private fun start(alturaDp: Int? = null) {
        app = TestApp.create().withLoggedSet()
        rule.setContent {
            ZenkaigymTheme(darkTheme = false) {
                Box(if (alturaDp != null) Modifier.height(alturaDp.dp) else Modifier) { ProgressScreen(app.viewModel, onBack = { backs++ }) }
            }
        }
        rule.waitUntil(10_000) { rule.onAllNodesWithTextCount("Resumo") > 0 }
    }

    /** O chip da aba é o primeiro "Treino" da árvore; o resto são cartões dentro das abas. */
    private fun openTrainingTab() {
        rule.onAllNodesWithText("Treino").onFirst().performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTextCount(app.loggedExerciseName) > 0 }
    }

    private fun openHistoryDetail() {
        openTrainingTab()
        rule.onNodeWithText(app.loggedExerciseName).performScrollTo().performClick()
        rule.waitForIdle()
    }

    @Test fun abasTemRotuloEscrito() {
        start()
        listOf("Resumo", "Treino", "Dieta", "Peso", "Conquistas").forEach {
            rule.onAllNodesWithText(it).onFirst().assertIsDisplayed()
        }
    }

    @Test fun historicoAbreDentroDaAbaTreino() {
        start()
        openHistoryDetail()
        rule.onNodeWithText("Últimas sessões", ignoreCase = true).performScrollTo().assertIsDisplayed()
    }

    @Test fun voltarFechaDetalheDoHistorico() {
        start()
        openHistoryDetail()
        rule.onNodeWithText("Últimas sessões", ignoreCase = true).assertExists()
        Espresso.pressBack()
        rule.waitForIdle()
        rule.onNodeWithText("Últimas sessões", ignoreCase = true).assertDoesNotExist()
        rule.onNodeWithText(app.loggedExerciseName).performScrollTo().assertIsDisplayed()
        assertEquals("o voltar só fechou o detalhe, a tela continua na Evolução", 0, backs)
    }

    /** Posição da rolagem vertical do conteúdo da aba (a única área rolável da tela). */
    private fun rolagem(): Float {
        val node = rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)).fetchSemanticsNode()
        return node.config[SemanticsProperties.VerticalScrollAxisRange].value()
    }

    @Test fun detalheAbreNoTopoMesmoComAListaLongeNoFim() {
        start(alturaDp = 300)
        openTrainingTab()
        rule.onNodeWithText(app.loggedExerciseName).performScrollTo()
        rule.waitForIdle()
        assertTrue("a lista fica longe do topo", rolagem() > 0f)
        rule.onNodeWithText(app.loggedExerciseName).performClick()
        rule.waitForIdle()
        assertEquals("o detalhe abre no topo", 0f, rolagem(), 0.5f)
    }

    private fun openWeightTab() {
        rule.onAllNodesWithText("Peso").onFirst().performClick()
        rule.waitForIdle()
    }

    @Test fun registrarPesoNaAbaPeso() {
        start()
        openWeightTab()
        rule.onNode(hasSetTextAction()).performTextInput("80,5")
        rule.onNodeWithText("REGISTRAR").performScrollTo().performClick()
        rule.waitUntil(10_000) { app.viewModel.weights.value.any { it.weightKg == 80.5 } }
    }

    @Test fun textoVazioApontaParaAPropriaAba() {
        start()
        openWeightTab()
        rule.onNodeWithText("Registre seu peso aqui ao menos duas vezes para ver a evolução.").assertIsDisplayed()
    }
}
