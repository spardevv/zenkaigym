package com.zenkaigym.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zenkaigym.ui.screens.SetupScreen
import com.zenkaigym.ui.theme.ZenkaigymTheme
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetupUiTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var app: TestApp

    @Before fun setUp() {
        app = TestApp.create()
        rule.setContent { ZenkaigymTheme(darkTheme = false) { SetupScreen(app.viewModel) } }
    }

    @After fun tearDown() = app.close()

    private fun fill(height: String, weight: String, age: String) {
        val fields = rule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput(height)
        fields[1].performTextInput(weight)
        fields[2].performTextInput(age)
    }

    @Test fun camposVaziosMostramErroDeAltura() {
        rule.onNodeWithText("Continuar").performClick()
        rule.onNodeWithText("Altura inválida (100–250 cm).").assertIsDisplayed()
        assertTrue(app.viewModel.showSetup.value)
    }

    @Test fun pesoInvalidoMostraErroDePeso() {
        fill("175", "5", "30")
        rule.onNodeWithText("Continuar").performClick()
        rule.onNodeWithText("Peso inválido (30–300 kg).").assertIsDisplayed()
        assertTrue(app.viewModel.showSetup.value)
    }

    @Test fun dadosValidosAvancamParaOTreino() {
        fill("175", "72,5", "30")
        rule.onNodeWithText("Continuar").performClick()
        rule.onNodeWithText("Dias e horários").assertIsDisplayed()
        // Ainda não terminou: o Dojo só abre depois de aceitar treino e dieta.
        assertTrue(app.viewModel.showSetup.value)
    }

    @Test fun montarOTreinoSalvaOPerfilEPreparaASugestao() {
        fill("175", "72,5", "30")
        rule.onNodeWithText("Continuar").performClick()
        rule.onNodeWithText("Montar meu treino").performClick()
        rule.waitUntil(10_000) { runBlocking { app.repo.profile.first() } != null }
        val profile = runBlocking { app.repo.profile.first() }
        assertEquals(175, profile?.heightCm)
        assertEquals(72.5, profile?.weightKg ?: 0.0, 0.0)
        assertTrue(app.viewModel.showSetup.value)
    }
}
