package com.zenkaigym.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

val Washi = Color(0xFFF3F0E8)     // papel
val Stone = Color(0xFF7A7A7A)     // cinza
val Ink2 = Color(0xFF202226)

/** Conjunto de acentos em uso; definido pelo ZenkaigymTheme conforme o estilo e o modo claro/escuro. */
enum class AccentSet { SHONEN_LIGHT, SHONEN_DARK, KAWAII_LIGHT, KAWAII_DARK, ISEKAI_LIGHT, ISEKAI_DARK }

internal var accentSet by mutableStateOf(AccentSet.SHONEN_LIGHT)

private fun pick(
    shonenLight: Long, shonenDark: Long, kawaiiLight: Long, kawaiiDark: Long, isekaiLight: Long, isekaiDark: Long
): Color = Color(
    when (accentSet) {
        AccentSet.SHONEN_LIGHT -> shonenLight
        AccentSet.SHONEN_DARK -> shonenDark
        AccentSet.KAWAII_LIGHT -> kawaiiLight
        AccentSet.KAWAII_DARK -> kawaiiDark
        AccentSet.ISEKAI_LIGHT -> isekaiLight
        AccentSet.ISEKAI_DARK -> isekaiDark
    }
)

/** Tinta usada sobre os acentos (texto de botões e selos): quase preta no Shōnen, ameixa no Kawaii, azul-noite no Isekai. Nunca #000. */
val Sumi: Color get() = pick(0xFF17171A, 0xFF17171A, 0xFF4A3552, 0xFF3A2748, 0xFF06101E, 0xFF06101E)

// Acentos. Shōnen: levemente saturados no claro, pastéis no escuro.
// Kawaii: rosa, lilás, roxo e azul-bebê em tons pastel (um pouco mais claros no escuro).
// Isekai: neons de interface de jogo — ciano de "sistema", rosa-carmim de dano, ouro, verde de HP e violeta de mana.
// Os seis valores de Aka, na ordem de AccentSet: lidos por Aka e por previewAccent.
private val AkaHex = longArrayOf(0xFFF25A52, 0xFFFF8F8F, 0xFFF7A8C4, 0xFFF9B8D0, 0xFFE0264A, 0xFFFF4D6D)
val Aka: Color get() = pick(AkaHex[0], AkaHex[1], AkaHex[2], AkaHex[3], AkaHex[4], AkaHex[5])       // vermelho · rosa · carmim

/** Cor de destaque (Aka) de um estilo e modo, sem tocar no [accentSet] global: serve às prévias de estilo. */
fun previewAccent(style: com.zenkaigym.domain.AppStyle, dark: Boolean): Color = Color(
    AkaHex[when (style) {
        com.zenkaigym.domain.AppStyle.SHONEN -> 0
        com.zenkaigym.domain.AppStyle.KAWAII -> 2
        com.zenkaigym.domain.AppStyle.ISEKAI -> 4
    } + if (dark) 1 else 0]
)
val Ki: Color get() = pick(0xFFF8CC4A, 0xFFFFDB80, 0xFFD9C3F2, 0xFFE2D0F7, 0xFFF2A900, 0xFFFFC857)        // amarelo · lilás · ouro
val Ao: Color get() = pick(0xFF45C0EE, 0xFF84D4F0, 0xFFA9D6F5, 0xFFB8DEF8, 0xFF0891D1, 0xFF2FD8FF)        // azul · azul-bebê · ciano
val Daidai: Color get() = pick(0xFFF59A40, 0xFFFFA860, 0xFFF9C6D9, 0xFFFAD0E0, 0xFFE56A12, 0xFFFF9248)    // laranja · rosa-pêssego
val Sakura: Color get() = pick(0xFFE888B0, 0xFFF2A6C4, 0xFFC4A5E8, 0xFFCDB2EE, 0xFF7C4DDB, 0xFFB388FF)    // rosa · roxo pastel · violeta
val Midori: Color get() = pick(0xFF55D48A, 0xFF88E2AE, 0xFFA8E3E8, 0xFFB5E8EC, 0xFF12B76A, 0xFF3DF2A0)    // verde · azul-menta · verde HP

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
    ink = Color(0xFF17171A),
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

// Kawaii claro: creme rosado, cartões brancos, tinta ameixa e bordas lilás.
val KawaiiLightPalette = ZenPalette(
    bg = Color(0xFFFFF5FA),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF4A3552),
    muted = Color(0xFF7A6585),
    border = Color(0xFFC9A8E0),
    shadow = Color(0xFFEBD9F4)
)
// Kawaii escuro: roxo-noite, cartões lavanda escura e texto rosado claro.
val KawaiiDarkPalette = ZenPalette(
    bg = Color(0xFF2A2238),
    card = Color(0xFF3A3050),
    ink = Color(0xFFF6ECFA),
    muted = Color(0xFFC7B6D6),
    border = Color(0xFFB79AD6),
    shadow = Color(0xFF1C1626)
)

// Isekai escuro (o visual de referência): azul-noite, painéis de vidro escuro, linhas ciano e sombra de "holograma".
val IsekaiDarkPalette = ZenPalette(
    bg = Color(0xFF070B16),
    card = Color(0xFF0F1830),
    ink = Color(0xFFE6F1FF),
    muted = Color(0xFF8CA3C7),
    border = Color(0xFF2BB8E6),
    shadow = Color(0xFF0B4A66)
)
// Isekai claro: tela "de gelo" (azul-gelo, painéis brancos, tinta azul-marinho e linhas azuis).
val IsekaiLightPalette = ZenPalette(
    bg = Color(0xFFE9F1FA),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF0B1B33),
    muted = Color(0xFF4F6A8F),
    border = Color(0xFF1B84C4),
    shadow = Color(0xFFA9CCEA)
)
