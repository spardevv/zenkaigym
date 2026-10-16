package com.zenkaigym.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zenkaigym.ui.nav.AppNav
import com.zenkaigym.ui.theme.ZenkaigymTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationUiTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var app: TestApp

    @Before fun setUp() {
        app = TestApp.create().withProfile()
        rule.setContent { ZenkaigymTheme(darkTheme = false) { AppNav(app.viewModel) } }
        rule.waitUntil(10_000) { app.viewModel.ready.value }
    }

    @After fun tearDown() = app.close()

    /** O rótulo da aba é o último nó com esse texto (o cabeçalho da tela vem antes na árvore). */
    private fun openTab(label: String, expectedSubtitle: String) {
        rule.onAllNodesWithText(label).onLast().performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTextCount(expectedSubtitle) > 0 }
        rule.onNodeWithText(expectedSubtitle).assertIsDisplayed()
    }

    @Test fun abreNoDojo() {
        rule.waitUntil(5_000) { rule.onAllNodesWithTextCount("Dojo") > 0 }
    }

    @Test fun cadaAbaMostraSuaTela() {
        openTab("Treino", "Dias, horários e ficha")
        openTab("Dieta", "Meta, horários e refeições")
        openTab("Perfil", "Seus dados")
        openTab("Ajustes", "Aparência, idioma e informações")
    }

    @Test fun dietaAbreMeusAlimentosEVolta() {
        openTab("Dieta", "Meta, horários e refeições")
        rule.onNodeWithText("Meus alimentos  →").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTextCount("Valores por 100 g · a dieta usa estes") > 0 }
        // Trocar de aba fecha a sub-tela de alimentos.
        openTab("Ajustes", "Aparência, idioma e informações")
    }
}
