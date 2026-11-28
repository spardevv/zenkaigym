package com.zenkaigym.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import com.zenkaigym.domain.MealSchedule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zenkaigym.data.entity.SetLog
import com.zenkaigym.ui.screens.HomeScreen
import com.zenkaigym.ui.theme.ZenkaigymTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DojoUiTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var app: TestApp

    @After fun tearDown() { app.close() }

    private fun start() {
        app = TestApp.create().withWorkoutEveryDay()
        rule.setContent { ZenkaigymTheme(darkTheme = false) { HomeScreen(app.viewModel, onNavigate = {}, showMenuSeal = true) } }
        rule.waitUntil(10_000) { app.viewModel.todayWorkout.value.session != null }
    }

    private fun startWithDiet() {
        app = TestApp.create().withDiet()
        rule.setContent { ZenkaigymTheme(darkTheme = false) { HomeScreen(app.viewModel, onNavigate = {}, showMenuSeal = true) } }
        rule.waitUntil(10_000) { rule.onAllNodesWithTextCount("FEITO") > 0 }
    }

    private fun openMenu() { rule.onNodeWithContentDescription("Mais opções do treino").performClick() }

    @Test fun marcarComoFeitoPeloMenuDoCartao() {
        start()
        openMenu()
        rule.onNodeWithText("Marcar como feito").performClick()
        rule.waitUntil(5_000) { app.viewModel.workoutDone.value }
        rule.onNodeWithText("TREINO FEITO").assertIsDisplayed()
    }

    @Test fun desfazerConclusaoPeloMenuDoCartao() {
        start()
        openMenu()
        rule.onNodeWithText("Marcar como feito").performClick()
        rule.waitUntil(5_000) { app.viewModel.workoutDone.value }
        openMenu()
        rule.onNodeWithText("Desfazer conclusão").performClick()
        rule.waitUntil(5_000) { !app.viewModel.workoutDone.value }
        rule.onNodeWithText("TREINAR AGORA").assertIsDisplayed()
    }

    @Test fun naoRealizadoPeloMenuDoCartao() {
        start()
        openMenu()
        rule.onNodeWithText("Não realizado").performClick()
        rule.waitUntil(5_000) { app.viewModel.workoutSkipped.value }
        rule.onNodeWithText("NÃO REALIZADO").assertIsDisplayed()
        rule.onNodeWithText("DESFAZER").assertIsDisplayed()
    }

    @Test fun seriesRegistradasMostramEmAndamento() {
        start()
        val first = app.viewModel.todayWorkout.value.session!!.items.minByOrNull { it.item.position }!!
        runBlocking {
            app.repo.logSet(SetLog(epochDay = app.viewModel.clock.value.epochDay, exerciseId = first.exercise.id, setIndex = 1, reps = 10, weightKg = 20.0))
        }
        rule.waitUntil(5_000) { rule.onAllNodesWithTextCount("Em andamento", substring = true) > 0 }
        rule.onNodeWithText("Em andamento", substring = true).assertIsDisplayed()
        rule.onNodeWithText("CONTINUAR").assertIsDisplayed()
    }

    @Test fun naoRealizeiPeloMenuDaRefeicao() {
        startWithDiet()
        rule.onNodeWithContentDescription("Mais opções da refeição").performClick()
        rule.onNodeWithText("Não realizei").performClick()
        rule.waitUntil(5_000) { app.viewModel.mealsSkipped.value.isNotEmpty() }
        // O cartão avança para a próxima refeição: volta à marcada pelo chip do horário (o último nó com o texto).
        val type = app.viewModel.mealsSkipped.value.first()
        val time = MealSchedule.timeOf(type, app.viewModel.mealSlots.value).format()
        rule.onAllNodesWithText(time).onLast().performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTextCount("NÃO REALIZADA", substring = true) > 0 }
        rule.waitForIdle()
        rule.onAllNodesWithText("NÃO REALIZADA", substring = true)[0].assertIsDisplayed()
    }

    @Test fun feitoContinuaAUmToque() {
        startWithDiet()
        rule.onAllNodesWithText("FEITO")[0].performClick()
        rule.waitUntil(5_000) { app.viewModel.mealsDone.value.isNotEmpty() }
    }
}
