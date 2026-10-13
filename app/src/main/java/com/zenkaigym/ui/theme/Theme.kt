package com.zenkaigym.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalZenPalette = staticCompositionLocalOf { LightPalette }

object Zen {
    val p: ZenPalette
        @Composable @ReadOnlyComposable get() = LocalZenPalette.current
}

@Composable
fun ZenkaigymTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    val scheme = if (darkTheme) {
        darkColorScheme(primary = Aka, onPrimary = Sumi, background = palette.bg, surface = palette.card, onBackground = palette.ink, onSurface = palette.ink)
    } else {
        lightColorScheme(primary = Aka, onPrimary = Sumi, background = palette.bg, surface = palette.card, onBackground = palette.ink, onSurface = palette.ink)
    }
    CompositionLocalProvider(LocalZenPalette provides palette, LocalContentColor provides palette.ink) {
        MaterialTheme(colorScheme = scheme, typography = ZenTypography, content = content)
    }
}
