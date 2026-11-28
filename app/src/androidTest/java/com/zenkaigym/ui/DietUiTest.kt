package com.zenkaigym.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zenkaigym.ui.screens.DietScreen
import com.zenkaigym.ui.theme.ZenkaigymTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DietUiTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var app: TestApp

    @After fun tearDown() { app.close() }

    private fun start() {
        app = TestApp.create().withDiet()
        rule.setContent { ZenkaigymTheme(darkTheme = false) { DietScreen(app.viewModel, onBack = {}) } }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("+ ADICIONAR").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun estado(valor: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, valor)

    @Test fun abasDaDieta() {
        start()
        listOf("Cardápio", "Meta", "Alimentos").forEach { rule.onNodeWithText(it).assertExists() }
    }

    @Test fun carrinhoAbreListaDeCompras() {
        start()
        rule.onNodeWithContentDescription("Lista de compras").performClick()
        // O título da janela é maiúsculo; o cartão do cabeçalho só tem a descrição.
        rule.waitUntil(5_000) { rule.onAllNodesWithText("LISTA DE COMPRAS").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("LISTA DE COMPRAS").assertIsDisplayed()
    }

    @Test fun novoCardapioFicaNoMenuDoCabecalho() {
        start()
        rule.onNodeWithContentDescription("Mais opções").performClick()
        rule.onNodeWithText("Novo cardápio…").assertIsDisplayed()
    }

    @Test fun alimentosVirouAba() {
        start()
        rule.onNodeWithText("Alimentos").performClick()
        rule.onNodeWithText("+ NOVO ALIMENTO").assertIsDisplayed()
    }

    /** Estado ("Aberto"/"Fechado") de cada cabeçalho de refeição, na ordem da tela. */
    private fun estados() = rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription))
        .fetchSemanticsNodes().map { it.config[SemanticsProperties.StateDescription] }

    @Test fun soUmaRefeicaoAbertaPorVez() {
        start()
        val antes = estados()
        assertEquals(1, antes.count { it == "Aberto" })
        // Abrir outra refeição fecha a que estava aberta: continua uma só, mas em outro lugar.
        rule.onAllNodes(estado("Fechado")).onFirst().performClick()
        rule.waitForIdle()
        val depois = estados()
        assertEquals(1, depois.count { it == "Aberto" })
        assertNotEquals(antes, depois)
    }
}
