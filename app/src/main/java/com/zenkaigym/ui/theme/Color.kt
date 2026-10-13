package com.zenkaigym.ui.theme

import androidx.compose.ui.graphics.Color

val Sumi = Color(0xFF17171A)      // tinta (quase preta, nunca #000)
val Washi = Color(0xFFF3F0E8)     // papel
val Stone = Color(0xFF7A7A7A)     // cinza
val Ink2 = Color(0xFF202226)

// Acentos vibrantes
val Aka = Color(0xFFFF2D2D)       // vermelho
val Ki = Color(0xFFFFD400)        // amarelo
val Ao = Color(0xFF00C2FF)        // ciano
val Midori = Color(0xFF2BE37A)    // verde

/** Cores semânticas do tema (claro = papel de mangá, escuro = sumi). */
data class ZenPalette(
    val bg: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val border: Color,
    val shadow: Color
)

val LightPalette = ZenPalette(bg = Washi, card = Color(0xFFFCFBF7), ink = Sumi, muted = Color(0xFF6E6E6A), border = Sumi, shadow = Sumi)
// Escuro sem preto/branco puros: fundo grafite, cartões um tom acima, texto off-white e bordas suaves.
val DarkPalette = ZenPalette(
    bg = Color(0xFF131417),
    card = Ink2,
    ink = Color(0xFFE9E6DF),
    muted = Color(0xFFB0ADA5),
    border = Color(0xFFCFCBC1),
    shadow = Color(0xFF3A3C42)
)
