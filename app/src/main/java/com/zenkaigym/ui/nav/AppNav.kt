package com.zenkaigym.ui.nav

import com.zenkaigym.domain.tr
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.Dp
import com.zenkaigym.ui.components.currentWindowLayout
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import com.zenkaigym.ui.components.FeedbackHost
import com.zenkaigym.ui.components.LocalMotionEnabled
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.screens.DietScreen
import com.zenkaigym.ui.screens.HomeScreen
import com.zenkaigym.ui.screens.LevelUpScene
import com.zenkaigym.ui.screens.MenuScreen
import com.zenkaigym.ui.screens.ProfileScreen
import com.zenkaigym.ui.screens.ProgressScreen
import com.zenkaigym.ui.screens.SettingsScreen
import com.zenkaigym.ui.screens.TrainSessionScreen
import com.zenkaigym.ui.screens.WorkoutScreen
import com.zenkaigym.util.dayOfWeekFromEpochDay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Daidai
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Sakura
import com.zenkaigym.ui.theme.Sumi
import com.zenkaigym.ui.theme.Zen

object Routes {
    const val HOME = "home"
    const val MENU = "menu"
    const val WORKOUT = "workout"
    const val DIET = "diet"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
}

private class Tab(val route: String, val kanji: String, val label: String, val color: Color)

// Entradas da coluna lateral (telas largas); no celular o caminho é o selo do Dojo e o menu.
private fun tabs() = listOf(
    Tab(Routes.HOME, "道", tr("Dojo"), Aka),
    Tab(Routes.WORKOUT, "体", tr("Treino"), Ki),
    Tab(Routes.DIET, "食", tr("Dieta"), Midori),
    Tab(Routes.PROGRESS, "進", tr("Evolução"), Daidai),
    Tab(Routes.PROFILE, "己", tr("Perfil"), Sakura),
    Tab(Routes.SETTINGS, "設", tr("Ajustes"), Ao)
)

/** Subtelas e o menu pertencem à entrada de onde são abertos: Menu ao Dojo. */
private fun parentTab(route: String): String = when (route) {
    Routes.MENU -> Routes.HOME
    else -> route
}

// Troca de tela: desliza um pouco para o lado da aba escolhida + fade (barato e dá noção de direção).
private fun tabEnter(direction: Int): EnterTransition =
    fadeIn(tween(200, delayMillis = 40)) + slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { it / 8 * direction }

private fun tabExit(direction: Int): ExitTransition =
    fadeOut(tween(120)) + slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { -it / 8 * direction }

@Composable
fun AppNav(viewModel: ZenViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: Routes.HOME
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val motion = LocalMotionEnabled.current
    var message by remember { mutableStateOf<String?>(null) }

    // Confirmações aparecem por alguns segundos; uma nova substitui a anterior.
    LaunchedEffect(viewModel) {
        viewModel.messages.collectLatest {
            message = it
            delay(3200)
            message = null
        }
    }

    // Sempre a partir do Dojo: o voltar de qualquer tela leva a ele, e cada tela abre do começo (sem salvar estado).
    fun go(route: String) {
        if (route == Routes.HOME) {
            navController.popBackStack(Routes.HOME, inclusive = false)
        } else {
            navController.navigate(route) {
                popUpTo(Routes.HOME)
                launchSingleTop = true
            }
        }
    }

    // Modo treino: tela cheia por cima de tudo enquanto houver treino hoje na ficha ativa.
    val trainingActive by viewModel.trainingActive.collectAsStateWithLifecycle()
    val assignment by viewModel.todayWorkout.collectAsStateWithLifecycle()
    val trainingDay = assignment.session

    // O nível é acompanhado o tempo todo: subir de nível em qualquer tela dispara a comemoração.
    val stats by viewModel.progressStats.collectAsStateWithLifecycle()
    val levelUp by viewModel.levelUp.collectAsStateWithLifecycle()
    LaunchedEffect(stats?.level?.level) { stats?.level?.level?.let(viewModel::observeLevel) }

    val layout = currentWindowLayout

    // Se a tela virar larga com o menu aberto, a coluna lateral assume o papel dele.
    LaunchedEffect(layout.useRail) {
        if (layout.useRail && navController.currentDestination?.route == Routes.MENU) go(Routes.HOME)
    }

    Box(Modifier.fillMaxSize().background(Zen.p.bg).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
      Row(Modifier.fillMaxSize()) {
        // Tablets e celulares deitados: as abas ficam numa coluna à esquerda, liberando a altura para o conteúdo.
        if (layout.useRail) SideRail(parentTab(current), layout.railWidth, onSelect = ::go)
        Column(Modifier.weight(1f).fillMaxHeight().background(Zen.p.bg).imePadding()) {
            Box(Modifier.weight(1f).statusBarsPadding()) {
                NavHost(
                    navController,
                    startDestination = Routes.HOME,
                    // Entrar desliza da direita; voltar (e a volta ao Dojo) desliza da esquerda.
                    enterTransition = { if (motion) tabEnter(if (targetState.destination.route == Routes.HOME) -1 else 1) else EnterTransition.None },
                    exitTransition = { if (motion) tabExit(if (targetState.destination.route == Routes.HOME) -1 else 1) else ExitTransition.None },
                    popEnterTransition = { if (motion) tabEnter(-1) else EnterTransition.None },
                    popExitTransition = { if (motion) tabExit(-1) else ExitTransition.None }
                ) {
                    composable(Routes.HOME) { HomeScreen(viewModel, onNavigate = ::go, showMenuSeal = !layout.useRail) }
                    composable(Routes.MENU) { MenuScreen(viewModel, onOpen = ::go, onClose = { go(Routes.HOME) }) }
                    composable(Routes.WORKOUT) { WorkoutScreen(viewModel, onBack = { go(Routes.HOME) }) }
                    composable(Routes.DIET) {
                        DietScreen(viewModel, onBack = { go(Routes.HOME) })
                    }
                    composable(Routes.PROFILE) {
                        ProfileScreen(viewModel, onBack = { go(Routes.HOME) })
                    }
                    composable(Routes.PROGRESS) { ProgressScreen(viewModel, onBack = { go(Routes.HOME) }) }
                    composable(Routes.SETTINGS) { SettingsScreen(viewModel, onBack = { go(Routes.HOME) }) }
                }
            }
        }
      }
        AnimatedVisibility(
            visible = trainingActive && trainingDay != null,
            enter = slideInVertically(tween(320)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(260)) { it } + fadeOut(tween(200))
        ) {
            // Guarda o último dia mostrado para a animação de saída não ficar sem conteúdo.
            var shown by remember { mutableStateOf(trainingDay) }
            if (trainingDay != null) shown = trainingDay
            shown?.let { TrainSessionScreen(it, viewModel, onClose = viewModel::stopTraining) }
        }
        // Subida de nível: comemora fora do modo treino (para não atrapalhar a série), por cima das telas.
        if (!trainingActive) {
            levelUp?.let { up -> LevelUpScene(up, onDismiss = viewModel::dismissLevelUp) }
        }
        // As confirmações ficam por cima de tudo, inclusive do modo treino.
        FeedbackHost(busy, message, Modifier.align(Alignment.TopCenter).statusBarsPadding())
    }
}

/** Uma entrada da coluna lateral (kanji + rótulo). */
@Composable
private fun TabItem(tab: Tab, selected: Boolean, modifier: Modifier, onSelect: (String) -> Unit) {
    val p = Zen.p
    val background by animateColorAsState(
        if (selected) tab.color else Color.Transparent, tween(220), label = "tab-bg"
    )
    val scale by animateFloatAsState(
        if (selected) 1.25f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "tab-scale"
    )
    Column(
        modifier
            .background(background)
            .semantics(mergeDescendants = true) { this.selected = selected }
            .clickable(role = Role.Tab) { onSelect(tab.route) }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Altura fixa para o kanji, para os rótulos ficarem alinhados.
        Box(Modifier.height(36.dp), contentAlignment = Alignment.Center) {
            Text(
                tab.kanji,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = if (selected) Sumi else p.ink
            )
        }
        Text(
            tab.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            color = if (selected) Sumi else p.muted
        )
    }
}

/** Abas em coluna, para tablets e celulares deitados. Rola se a altura não bastar. */
@Composable
private fun SideRail(current: String, width: Dp, onSelect: (String) -> Unit) {
    val p = Zen.p
    Row(Modifier.fillMaxHeight().background(p.card)) {
        Box(Modifier.width(width).fillMaxHeight().statusBarsPadding().navigationBarsPadding(), contentAlignment = Alignment.Center) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                tabs().forEach { tab ->
                    TabItem(tab, tab.route == current, Modifier.fillMaxWidth().padding(vertical = 2.dp), onSelect)
                }
            }
        }
        Box(Modifier.fillMaxHeight().width(3.dp).background(p.border))
    }
}
