package com.zenkaigym.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ícones do app, desenhados num traço só (grade de 24, linha de 2,4 com pontas redondas) para ficarem
 * iguais em qualquer aparelho. Antes eram símbolos de texto (✓ → ✕ ⇄ ✎…), que mudam de desenho conforme
 * a fonte do sistema e, em alguns aparelhos, viram emoji colorido e ignoram a cor do tema.
 */
object ZenIcons {
    // Ações
    val Check = stroke("Check", "M4.5 12.5 L9.5 17.5 L19.5 6.5")
    val Close = stroke("Close", "M6 6 L18 18 M18 6 L6 18")
    val Minus = stroke("Minus", "M5 12 H19")
    val Plus = stroke("Plus", "M12 5 V19 M5 12 H19")
    val Swap = stroke("Swap", "M4 8 H19.5 M16 4.5 L19.5 8 L16 11.5 M20 16 H4.5 M8 12.5 L4.5 16 L8 19.5")
    val Edit = stroke("Edit", "M4.5 19.5 L5.5 15 L15.5 5 a2.1 2.1 0 0 1 3 0 l0.5 0.5 a2.1 2.1 0 0 1 0 3 L9 18.5 Z M13.5 7 L17 10.5")
    val Undo = stroke("Undo", "M4 4.5 V9 H8.5 M4.4 9 A8 8 0 1 1 4 12.5")
    val Timer = stroke("Timer", "M5 13.5 a7 7 0 1 0 14 0 a7 7 0 1 0 -14 0 M12 13.5 V9.5 M9.5 3 H14.5")

    // Direções
    val ArrowRight = stroke("ArrowRight", "M4 12 H20 M13.5 5.5 L20 12 L13.5 18.5")
    val ArrowLeft = stroke("ArrowLeft", "M20 12 H4 M10.5 5.5 L4 12 L10.5 18.5")
    val ChevronUp = stroke("ChevronUp", "M5.5 15 L12 8.5 L18.5 15")
    val ChevronDown = stroke("ChevronDown", "M5.5 9 L12 15.5 L18.5 9")
    val ChevronLeft = stroke("ChevronLeft", "M15 5.5 L8.5 12 L15 18.5")
    val ChevronRight = stroke("ChevronRight", "M9 5.5 L15.5 12 L9 18.5")

    // Preenchidos
    val Star = fill("Star", "M12 3 L14.35 9.26 L21.03 9.56 L15.8 13.74 L17.58 20.19 L12 16.5 L6.42 20.19 L8.2 13.74 L2.97 9.56 L9.65 9.26 Z")
    val Play = fill("Play", "M8 5.5 L18.5 12 L8 18.5 Z")
    val TrendUp = fill("TrendUp", "M12 6 L19 17 H5 Z")
    val TrendDown = fill("TrendDown", "M12 18 L19 7 H5 Z")

    // Abas: haltere, tigela com hashi, portal torii, pessoa e controles deslizantes.
    val Workout = stroke("Workout", "M10 12 H14 M6.6 6.5 h2.6 v11 h-2.6 Z M14.8 6.5 h2.6 v11 h-2.6 Z M3 9.5 V14.5 M21 9.5 V14.5")
    val Diet = stroke("Diet", "M3.5 12 H20.5 A8.5 8.5 0 0 1 3.5 12 Z M8 8.5 L18.5 3 M12 9 L20.5 6")
    val Dojo = stroke("Dojo", "M2.5 5.5 C7 7.5 17 7.5 21.5 5.5 M5 11 H19 M7.5 7.5 V20.5 M16.5 7.5 V20.5 M12 7 V11")
    val Profile = stroke(
        "Profile",
        "M8.3 8 a3.7 3.7 0 1 0 7.4 0 a3.7 3.7 0 1 0 -7.4 0 M4.5 20.5 C4.5 16.5 8 14.5 12 14.5 C16 14.5 19.5 16.5 19.5 20.5"
    )
    val Settings = stroke(
        "Settings",
        "M4 7.5 H12.4 M17.6 7.5 H20 M12.4 7.5 a2.6 2.6 0 1 0 5.2 0 a2.6 2.6 0 1 0 -5.2 0 " +
            "M4 16.5 H6.4 M11.6 16.5 H20 M6.4 16.5 a2.6 2.6 0 1 0 5.2 0 a2.6 2.6 0 1 0 -5.2 0"
    )

    private fun stroke(name: String, path: String): ImageVector =
        ImageVector.Builder("Zen$name", 24.dp, 24.dp, 24f, 24f).addPath(
            pathData = addPathNodes(path),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.4f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ).build()

    /** Formas cheias (estrela, triângulos): o contorno fino arredonda as pontas para combinar com o traço. */
    private fun fill(name: String, path: String): ImageVector =
        ImageVector.Builder("Zen$name", 24.dp, 24.dp, 24f, 24f).addPath(
            pathData = addPathNodes(path),
            fill = SolidColor(Color.Black),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.6f,
            strokeLineJoin = StrokeJoin.Round
        ).build()
}

/**
 * Ícone do app. Sem [description] é só enfeite (o leitor de tela ignora); a cor padrão é a do texto ao redor.
 */
@Composable
fun ZenIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    color: Color = LocalContentColor.current,
    description: String? = null
) {
    Icon(icon, description, modifier.size(size), tint = color)
}

/** Texto com um ícone antes ([leading]) ou depois ([trailing]), alinhados pelo centro e na mesma cor. */
@Composable
fun IconText(
    text: String,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    trailing: ImageVector? = null,
    color: Color = LocalContentColor.current,
    style: TextStyle = LocalTextStyle.current,
    iconSize: Dp = 16.dp
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (leading != null) {
            ZenIcon(leading, size = iconSize, color = color)
            Spacer(Modifier.width(5.dp))
        }
        Text(text, Modifier.weight(1f, fill = false), color = color, style = style)
        if (trailing != null) {
            Spacer(Modifier.width(5.dp))
            ZenIcon(trailing, size = iconSize, color = color)
        }
    }
}
