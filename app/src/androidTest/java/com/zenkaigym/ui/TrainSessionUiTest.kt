package com.zenkaigym.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zenkaigym.data.entity.SetLog
import com.zenkaigym.ui.screens.TrainSessionScreen
import com.zenkaigym.ui.theme.ZenkaigymTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrainSessionUiTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var app: TestApp

    @After fun tearDown() { app.close() }

    /** Abre o modo treino com todas as séries de trabalho registradas, menos a última. */
    private fun startWithOneSetLeft() {
        app = TestApp.create().withWorkoutEveryDay()
        // Sem isto o modo treino pede a permissão de notificação e o diálogo do sistema cobre o teste.
        app.viewModel.markRestPermissionAsked()
        // Ainda sem conteúdo composto: coleta o fluxo direto até a sessão de hoje existir.
        val day = runBlocking { withTimeout(10_000) { app.viewModel.todayWorkout.first { it.session != null } } }.session!!
        val epochDay = app.viewModel.clock.value.epochDay
        val all = day.items.sortedBy { it.item.position }.flatMap { e -> (1..e.item.sets).map { e.exercise to it } }
        runBlocking {
            // Histórico da semana passada: preenche as repetições do rascunho da última série.
            all.forEach { (ex, i) ->
                app.repo.logSet(SetLog(epochDay = epochDay - 7, exerciseId = ex.id, setIndex = i, reps = 10, weightKg = 20.0))
            }
            all.dropLast(1).forEach { (ex, i) ->
                app.repo.logSet(SetLog(epochDay = epochDay, exerciseId = ex.id, setIndex = i, reps = 10, weightKg = 20.0))
            }
        }
        rule.setContent { ZenkaigymTheme(darkTheme = false) { TrainSessionScreen(day, app.viewModel, onClose = {}) } }
        rule.waitForIdle()
    }

    @Test fun registrarUltimaSerieAbreAConclusaoEUndoVoltaAoTreino() {
        startWithOneSetLeft()
        rule.onNodeWithText("Registrar série", substring = true).performClick()
        rule.waitUntil(5_000) { app.viewModel.workoutDone.value }
        rule.onNodeWithText("Treino concluído").assertIsDisplayed()
        rule.onNodeWithText("Voltar ao Dojo".uppercase()).assertIsDisplayed()

        rule.onNodeWithText("Desfazer conclusão".uppercase()).performClick()
        rule.waitUntil(5_000) { !app.viewModel.workoutDone.value }
        rule.onNodeWithText("CONCLUIR TREINO").assertIsDisplayed()
    }
}
