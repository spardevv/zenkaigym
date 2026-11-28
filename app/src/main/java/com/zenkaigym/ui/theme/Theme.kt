package com.zenkaigym.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.zenkaigym.ui.components.LocalMotionEnabled
import com.zenkaigym.ui.components.motionEnabled
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zenkaigym.domain.AppStyle
import com.zenkaigym.domain.WeekStart

val LocalZenPalette = staticCompositionLocalOf { LightPalette }

/**
 * Formas do estilo atual. Shōnen: retângulos de borda grossa e sombra sólida (quadro de mangá).
 * Kawaii: cantos arredondados, bordas finas e sombra curta e suave.
 * Isekai: painéis de interface de jogo — cantos chanfrados, linhas finas de neon e sombra de "holograma".
 */
data class ZenLook(
    val kawaii: Boolean,
    val corner: Dp,
    val border: Dp,
    val thinBorder: Dp,
    val shadow: Dp,
    val isekai: Boolean = false
) {
    /** Forma de cartões, botões e campos. */
    val shape: Shape = when {
        isekai -> CutCornerShape(corner)
        corner == 0.dp -> RectangleShape
        else -> RoundedCornerShape(corner)
    }

    /** Forma de peças pequenas (células, chips, etiquetas): cantos menores. */
    val smallShape: Shape = when {
        isekai -> CutCornerShape(corner / 2)
        corner == 0.dp -> RectangleShape
        else -> RoundedCornerShape(corner / 2)
    }

    /** Textos de destaque sem itálico (Kawaii e Isekai); só o Shōnen usa itálico. */
    val upright: Boolean get() = kawaii || isekai

    companion object {
        val SHONEN = ZenLook(kawaii = false, corner = 0.dp, border = 3.dp, thinBorder = 2.dp, shadow = 5.dp)
        val KAWAII = ZenLook(kawaii = true, corner = 18.dp, border = 2.dp, thinBorder = 1.5.dp, shadow = 3.dp)
        val ISEKAI = ZenLook(kawaii = false, corner = 14.dp, border = 1.5.dp, thinBorder = 1.dp, shadow = 3.dp, isekai = true)
    }
}

val LocalZenLook = staticCompositionLocalOf { ZenLook.SHONEN }

/** Primeiro dia da semana escolhido em Ajustes: ordem dos dias em treino, dieta e agenda. */
val LocalWeekStart = staticCompositionLocalOf { WeekStart.MONDAY }

object Zen {
    val p: ZenPalette
        @Composable @ReadOnlyComposable get() = LocalZenPalette.current

    val look: ZenLook
        @Composable @ReadOnlyComposable get() = LocalZenLook.current

    val week: WeekStart
        @Composable @ReadOnlyComposable get() = LocalWeekStart.current
}

/** Paleta de um estilo e modo (usada pelo tema e pelas prévias de estilo). */
fun paletteFor(style: AppStyle, dark: Boolean): ZenPalette = when (style) {
    AppStyle.KAWAII -> if (dark) KawaiiDarkPalette else KawaiiLightPalette
    AppStyle.ISEKAI -> if (dark) IsekaiDarkPalette else IsekaiLightPalette
    AppStyle.SHONEN -> if (dark) DarkPalette else LightPalette
}

/** Formas de um estilo. */
fun lookFor(style: AppStyle): ZenLook = when (style) {
    AppStyle.KAWAII -> ZenLook.KAWAII
    AppStyle.ISEKAI -> ZenLook.ISEKAI
    AppStyle.SHONEN -> ZenLook.SHONEN
}

@Composable
fun ZenkaigymTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    style: AppStyle = AppStyle.SHONEN,
    weekStart: WeekStart = WeekStart.MONDAY,
    content: @Composable () -> Unit
) {
    val palette = paletteFor(style, darkTheme)
    accentSet = when (style) {
        AppStyle.KAWAII -> if (darkTheme) AccentSet.KAWAII_DARK else AccentSet.KAWAII_LIGHT
        AppStyle.ISEKAI -> if (darkTheme) AccentSet.ISEKAI_DARK else AccentSet.ISEKAI_LIGHT
        AppStyle.SHONEN -> if (darkTheme) AccentSet.SHONEN_DARK else AccentSet.SHONEN_LIGHT
    }
    val scheme = if (darkTheme) {
        darkColorScheme(primary = Aka, onPrimary = Sumi, background = palette.bg, surface = palette.card, onBackground = palette.ink, onSurface = palette.ink)
    } else {
        lightColorScheme(primary = Aka, onPrimary = Sumi, background = palette.bg, surface = palette.card, onBackground = palette.ink, onSurface = palette.ink)
    }
    CompositionLocalProvider(
        LocalZenPalette provides palette,
        LocalZenLook provides lookFor(style),
        LocalWeekStart provides weekStart,
        LocalMotionEnabled provides motionEnabled(LocalContext.current),
        LocalContentColor provides palette.ink
    ) {
        val typography = when (style) {
            AppStyle.KAWAII -> KawaiiTypography
            AppStyle.ISEKAI -> IsekaiTypography
            AppStyle.SHONEN -> ZenTypography
        }
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
