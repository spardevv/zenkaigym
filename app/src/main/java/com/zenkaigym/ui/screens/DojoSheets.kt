package com.zenkaigym.ui.screens

import androidx.compose.runtime.Composable
import com.zenkaigym.domain.DojoWorkoutState
import com.zenkaigym.domain.tr
import com.zenkaigym.ui.components.SheetOption
import com.zenkaigym.ui.components.ZenSheet
import com.zenkaigym.ui.theme.Aka

/** Folha do ⋯ do treino de hoje: as ações secundárias de cada estado do cartão. */
@Composable
fun WorkoutSheet(
    state: DojoWorkoutState,
    title: String,
    subtitle: String?,
    onDismiss: () -> Unit,
    onMarkDone: () -> Unit,
    onUndoDone: () -> Unit,
    onSkip: () -> Unit,
    onChange: () -> Unit
) {
    ZenSheet(onDismiss, accent = Aka, title = title, subtitle = subtitle) {
        when (state) {
            DojoWorkoutState.PENDING, DojoWorkoutState.IN_PROGRESS -> {
                SheetOption(tr("Marcar como feito"), tr("Conta o treino de hoje sem registrar séries."), onMarkDone)
                SheetOption(tr("Não realizado"), tr("A sessão passa para o próximo dia de treino."), onSkip)
                SheetOption(tr("Trocar o treino de hoje"), tr("Escolhe outra sessão da semana só para hoje."), onChange)
            }
            DojoWorkoutState.DONE ->
                SheetOption(tr("Desfazer conclusão"), tr("O treino de hoje volta a ficar pendente."), onUndoDone)
            DojoWorkoutState.SKIPPED ->
                SheetOption(tr("Trocar o treino de hoje"), tr("Escolhe outra sessão da semana só para hoje."), onChange)
            else -> Unit
        }
    }
}

/** Folha do ⋯ da refeição: prato fora do cardápio, não realizei e atalho para a Dieta. */
@Composable
fun MealSheet(
    title: String,
    subtitle: String,
    canSkip: Boolean,
    onDismiss: () -> Unit,
    onOther: () -> Unit,
    onSkip: () -> Unit,
    onMenu: () -> Unit
) {
    ZenSheet(onDismiss, accent = Aka, title = title, subtitle = subtitle) {
        SheetOption(tr("Comi outra coisa"), tr("Registra um prato da sua lista ou um prato livre, em kcal."), onOther)
        if (canSkip) SheetOption(tr("Não realizei"), tr("As próximas refeições do dia se ajustam."), onSkip)
        SheetOption(tr("Ver no cardápio"), tr("Abre a Dieta no dia de hoje."), onMenu)
    }
}
