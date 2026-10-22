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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.screens.DietScreen
import com.zenkaigym.ui.screens.FoodsScreen
import com.zenkaigym.ui.screens.HomeScreen
import com.zenkaigym.ui.screens.ProfileScreen
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
import com.zenkaigym.domain.Motion
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Daidai
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Sumi
import com.zenkaigym.ui.theme.Zen

object Routes {
    const val HOME = "home"
    const val WORKOUT = "workout"
    const val DIET = "diet"
    const val FOODS = "foods"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
}

private class Tab(val route: String, val kanji: String, val label: String, val color: Color, val hero: Boolean = false)

// O Dojo (home) fica sempre no centro e com destaque maior.
private fun tabs() = listOf(
    Tab(Routes.WORKOUT, "体", tr("Treino"), Ki),
    Tab(Routes.DIET, "食", tr("Dieta"), Midori),
    Tab(Routes.HOME, "道", tr("Dojo"), Aka, hero = true),
    Tab(Routes.PROFILE, "己", tr("Perfil"), Daidai),
    Tab(Routes.SETTINGS, "設", tr("Ajustes"), Ao)
)

/** Ordem das abas na barra, para saber de que lado a próxima tela entra. */
private val TabOrder = listOf(Routes.WORKOUT, Routes.DIET, Routes.HOME, Routes.PROFILE, Routes.SETTINGS)

/** 1 = entra pela direita, -1 = pela esquerda. "Meus alimentos" se comporta como uma subtela à direita da Dieta. */
private fun navDirection(from: String?, to: String?): Int = when {
    to == Routes.FOODS -> 1
    from == Routes.FOODS && to == Routes.DIET -> -1
    else -> Motion.tabDirection(
        TabOrder,
        from?.let { if (it == Routes.FOODS) Routes.DIET else it },
        to?.let { if (it == Routes.FOODS) Routes.DIET else it }
    )
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
    var message by remember { mutableStateOf<String?>(null) }

    // Confirmações aparecem por alguns segundos; uma nova substitui a anterior.
    LaunchedEffect(viewModel) {
        viewModel.messages.collectLatest {
            message = it
            delay(3200)
            message = null
        }
    }

    fun go(route: String) {
        // "Meus alimentos" é uma subtela da Dieta: ao trocar de aba ela sai da pilha, e a Dieta volta limpa.
        if (navController.currentDestination?.route == Routes.FOODS) {
            navController.popBackStack(Routes.FOODS, inclusive = true)
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Modo treino: tela cheia por cima de tudo enquanto houver treino hoje na ficha ativa.
    val trainingActive by viewModel.trainingActive.collectAsStateWithLifecycle()
    val assignment by viewModel.todayWorkout.collectAsStateWithLifecycle()
    val trainingDay = assignment.session

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(Zen.p.bg).imePadding()) {
            Box(Modifier.weight(1f).statusBarsPadding()) {
                NavHost(
                    navController,
                    startDestination = Routes.HOME,
                    enterTransition = { tabEnter(navDirection(initialState.destination.route, targetState.destination.route)) },
                    exitTransition = { tabExit(navDirection(initialState.destination.route, targetState.destination.route)) },
                    popEnterTransition = { tabEnter(navDirection(initialState.destination.route, targetState.destination.route)) },
                    popExitTransition = { tabExit(navDirection(initialState.destination.route, targetState.destination.route)) }
                ) {
                    composable(Routes.HOME) { HomeScreen(viewModel, onNavigate = ::go) }
                    composable(Routes.WORKOUT) { WorkoutScreen(viewModel) }
                    composable(Routes.DIET) { DietScreen(viewModel, onOpenFoods = { navController.navigate(Routes.FOODS) { launchSingleTop = true } }) }
                    composable(Routes.FOODS) { FoodsScreen(viewModel, onBack = { navController.popBackStack() }) }
                    composable(Routes.PROFILE) { ProfileScreen(viewModel) }
                    composable(Routes.SETTINGS) { SettingsScreen(viewModel) }
                }
            }
            // Alimentos é aberto pela Dieta, então a aba Dieta segue destacada.
            BottomBar(if (current == Routes.FOODS) Routes.DIET else current, onSelect = ::go)
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
        // As confirmações ficam por cima de tudo, inclusive do modo treino.
        FeedbackHost(busy, message, Modifier.align(Alignment.TopCenter).statusBarsPadding())
    }
}

@Composable
private fun BottomBar(current: String, onSelect: (String) -> Unit) {
    val p = Zen.p
    Column(Modifier.background(p.card).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(p.border))
        Row(Modifier.fillMaxWidth()) {
            tabs().forEach { tab ->
                val selected = tab.route == current
                val background by animateColorAsState(
                    if (selected) tab.color else Color.Transparent, tween(220), label = "tab-bg"
                )
                val scale by animateFloatAsState(
                    if (selected) 1.25f else 1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "tab-scale"
                )
                Column(
                    Modifier
                        .weight(if (tab.hero) 1.25f else 1f)
                        .background(background)
                        .clickable { onSelect(tab.route) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        tab.kanji,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                        fontSize = if (tab.hero) 28.sp else 22.sp,
                        fontWeight = FontWeight.Black,
                        color = if (selected) Sumi else if (tab.hero) tab.color else p.ink
                    )
                    Text(
                        tab.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (tab.hero) FontWeight.Black else null,
                        color = if (selected) Sumi else if (tab.hero) tab.color else p.muted
                    )
                }
            }
        }
    }
}
