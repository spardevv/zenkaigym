package com.zenkaigym.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
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
 */
data class ZenLook(
    val kawaii: Boolean,
    val corner: Dp,
    val border: Dp,
    val thinBorder: Dp,
    val shadow: Dp
) {
    /** Forma de cartões, botões e campos. */
    val shape: Shape = if (corner == 0.dp) RectangleShape else RoundedCornerShape(corner)

    /** Forma de peças pequenas (células, chips, etiquetas): cantos menores. */
    val smallShape: Shape = if (corner == 0.dp) RectangleShape else RoundedCornerShape(corner / 2)

    companion object {
        val SHONEN = ZenLook(kawaii = false, corner = 0.dp, border = 3.dp, thinBorder = 2.dp, shadow = 5.dp)
        val KAWAII = ZenLook(kawaii = true, corner = 18.dp, border = 2.dp, thinBorder = 1.5.dp, shadow = 3.dp)
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

@Composable
fun ZenkaigymTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    style: AppStyle = AppStyle.SHONEN,
    weekStart: WeekStart = WeekStart.MONDAY,
    content: @Composable () -> Unit
) {
    val kawaii = style == AppStyle.KAWAII
    val palette = when {
        kawaii && darkTheme -> KawaiiDarkPalette
        kawaii -> KawaiiLightPalette
        darkTheme -> DarkPalette
        else -> LightPalette
    }
    accentSet = when {
        kawaii && darkTheme -> AccentSet.KAWAII_DARK
        kawaii -> AccentSet.KAWAII_LIGHT
        darkTheme -> AccentSet.SHONEN_DARK
        else -> AccentSet.SHONEN_LIGHT
    }
    val scheme = if (darkTheme) {
        darkColorScheme(primary = Aka, onPrimary = Sumi, background = palette.bg, surface = palette.card, onBackground = palette.ink, onSurface = palette.ink)
    } else {
        lightColorScheme(primary = Aka, onPrimary = Sumi, background = palette.bg, surface = palette.card, onBackground = palette.ink, onSurface = palette.ink)
    }
    CompositionLocalProvider(
        LocalZenPalette provides palette,
        LocalZenLook provides if (kawaii) ZenLook.KAWAII else ZenLook.SHONEN,
        LocalWeekStart provides weekStart,
        LocalMotionEnabled provides motionEnabled(LocalContext.current),
        LocalContentColor provides palette.ink
    ) {
        MaterialTheme(colorScheme = scheme, typography = if (kawaii) KawaiiTypography else ZenTypography, content = content)
    }
}
