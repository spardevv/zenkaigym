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
import com.zenkaigym.ui.screens.WorkoutScreen
import com.zenkaigym.ui.theme.ZenkaigymTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutUiTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var app: TestApp

    @After fun tearDown() { app.close() }

    private fun start() {
        app = TestApp.create().withWorkoutEveryDay()
        rule.setContent { ZenkaigymTheme(darkTheme = false) { WorkoutScreen(app.viewModel, onBack = {}) } }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("+ ADICIONAR").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun adicionar() = rule.onAllNodesWithText("+ ADICIONAR").fetchSemanticsNodes().size

    @Test fun abasDoTreino() {
        start()
        listOf("Ficha", "Agenda", "Exercícios").forEach { rule.onNodeWithText(it).assertExists() }
    }

    @Test fun soUmDiaAbertoPorVez() {
        start()
        // O dia de hoje (ou o primeiro) abre sozinho.
        assertEquals(1, adicionar())
        val fechados = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Fechado")
        rule.onAllNodes(fechados).onFirst().performClick()
        rule.waitForIdle()
        assertEquals(1, adicionar())
    }

    @Test fun novaSugestaoFicaNoMenuDoCabecalho() {
        start()
        rule.onNodeWithContentDescription("Mais opções").performClick()
        rule.onNodeWithText("Nova sugestão").assertIsDisplayed()
    }

    @Test fun agendaMostraOsDias() {
        start()
        rule.onNodeWithText("Agenda").performClick()
        rule.onNodeWithText("DIAS E HORÁRIOS").assertExists()
    }
}
