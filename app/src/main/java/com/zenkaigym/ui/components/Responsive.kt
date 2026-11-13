package com.zenkaigym.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Classes de largura da janela, na mesma divisão do Material: compacta (celulares em pé),
 * média (celulares dobráveis abertos, tablets pequenos em pé) e expandida (tablets, paisagem larga).
 */
enum class WidthClass { COMPACT, MEDIUM, EXPANDED }

/** Medidas da janela atual. Acompanha rotação, dobra e modo multijanela, pois vem da configuração. */
@Immutable
data class WindowLayout(val widthDp: Int, val heightDp: Int) {
    val widthClass: WidthClass = when {
        widthDp >= 840 -> WidthClass.EXPANDED
        widthDp >= 600 -> WidthClass.MEDIUM
        else -> WidthClass.COMPACT
    }

    /** Celular deitado: pouca altura, então a barra de abas sai de baixo e vira uma coluna lateral. */
    val shortHeight: Boolean = heightDp < 480

    /** Abas em coluna lateral em vez de barra inferior. */
    val useRail: Boolean = widthClass != WidthClass.COMPACT || shortHeight

    /** Telas muito estreitas (< 360 dp): textos e espaços apertam um pouco. */
    val narrow: Boolean = widthDp < 360

    /** Largura máxima do conteúdo; além dela ele fica centralizado. */
    val contentMaxWidth: Dp = when (widthClass) {
        WidthClass.COMPACT -> 640.dp
        WidthClass.MEDIUM -> 720.dp
        WidthClass.EXPANDED -> 960.dp
    }

    /** Dois cartões lado a lado onde a tela tem espaço para isso. */
    val twoColumns: Boolean = widthClass == WidthClass.EXPANDED

    /** Margem lateral das telas. */
    val gutter: Dp = when {
        narrow -> 12.dp
        widthClass == WidthClass.COMPACT -> 16.dp
        else -> 24.dp
    }

    /** Largura da barra lateral de abas. */
    val railWidth: Dp = if (widthClass == WidthClass.COMPACT) 72.dp else 88.dp

    /** Largura máxima das janelas de diálogo. */
    val dialogMaxWidth: Dp = if (widthClass == WidthClass.COMPACT) 380.dp else 480.dp

    /** Folga vertical que um diálogo reserva para título, rodapé e margens. */
    val dialogChrome: Int = if (shortHeight) 130 else 190
}

val currentWindowLayout: WindowLayout
    @Composable @ReadOnlyComposable
    get() = LocalConfiguration.current.let { WindowLayout(it.screenWidthDp, it.screenHeightDp) }
