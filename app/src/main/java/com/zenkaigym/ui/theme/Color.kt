package com.zenkaigym.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

val Sumi = Color(0xFF17171A)      // tinta (quase preta, nunca #000)
val Washi = Color(0xFFF3F0E8)     // papel
val Stone = Color(0xFF7A7A7A)     // cinza
val Ink2 = Color(0xFF202226)

/** Definido pelo ZenkaigymTheme; os acentos abaixo trocam para tons pastel no escuro. */
internal var accentsDark by mutableStateOf(false)

// Acentos: levemente saturados no claro, pastéis no escuro
val Aka: Color get() = if (accentsDark) Color(0xFFFF8F8F) else Color(0xFFF25A52)       // vermelho
val Ki: Color get() = if (accentsDark) Color(0xFFFFDB80) else Color(0xFFF8CC4A)        // amarelo
val Ao: Color get() = if (accentsDark) Color(0xFF84D4F0) else Color(0xFF45C0EE)        // azul
val Daidai: Color get() = if (accentsDark) Color(0xFFFFA860) else Color(0xFFF59A40)    // laranja
val Sakura: Color get() = if (accentsDark) Color(0xFFF2A6C4) else Color(0xFFE888B0)    // rosa
val Midori: Color get() = if (accentsDark) Color(0xFF88E2AE) else Color(0xFF55D48A)    // verde

/** Cores semânticas do tema (claro = papel de mangá, escuro = sumi). */
data class ZenPalette(
    val bg: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val border: Color,
    val shadow: Color
)

// Claro: bordas e sombras em grafite suave (não preto) para reduzir o contraste seco.
val LightPalette = ZenPalette(
    bg = Washi,
    card = Color(0xFFFCFBF7),
    ink = Sumi,
    muted = Color(0xFF6E6E6A),
    border = Color(0xFF3A3A40),
    shadow = Color(0xFF77757D)
)
// Escuro: fundo cinza grafite (não preto), cartões um tom acima, texto off-white e bordas suaves.
val DarkPalette = ZenPalette(
    bg = Color(0xFF26282E),
    card = Color(0xFF32353C),
    ink = Color(0xFFECE9E2),
    muted = Color(0xFFB8B5AE),
    border = Color(0xFF9DA1AD),
    shadow = Color(0xFF17181C)
)
