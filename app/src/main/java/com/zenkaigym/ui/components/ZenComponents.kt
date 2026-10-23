package com.zenkaigym.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Sakura
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Sumi
import com.zenkaigym.ui.theme.Zen
import kotlin.math.cos
import kotlin.math.sin

private val BorderWidth = 3.dp

/** Cartão estilo mangá: borda grossa, sombra sólida deslocada e faixa de destaque opcional. */
@Composable
fun ZenCard(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    entranceIndex: Int = 0,
    content: @Composable ColumnScope.() -> Unit
) {
    val p = Zen.p
    val look = Zen.look
    Column(
        modifier
            .zenEntrance(entranceIndex)
            .padding(end = look.shadow, bottom = look.shadow)
            .zenShadow(look.shadow, p.shadow, look.corner)
            .clip(look.shape)
            .background(p.card)
            .border(look.border, p.border, look.shape)
    ) {
        if (accent != null) {
            if (look.kawaii) {
                // Kawaii: faixa de cor mais fina e sem linha dura embaixo.
                Box(Modifier.fillMaxWidth().height(8.dp).background(accent))
            } else {
                Box(Modifier.fillMaxWidth().height(10.dp).background(accent))
                Box(Modifier.fillMaxWidth().height(BorderWidth).background(p.border))
            }
        }
        Column(Modifier.fillMaxWidth().padding(14.dp), content = content)
    }
}

@Composable
fun ZenButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Aka,
    enabled: Boolean = true,
    fill: Boolean = false,
    small: Boolean = false
) {
    val p = Zen.p
    val look = Zen.look
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Afunda até a sombra ao pressionar. O valor é lido só no layout (offset com lambda),
    // então a animação não recompõe o botão a cada quadro.
    val press = animateDpAsState(
        if (pressed) 4.dp else 0.dp,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    // Habilitar/desabilitar troca a cor suavemente (ex.: botão que libera ao preencher um campo).
    val background by animateColorAsState(if (enabled) accent else p.muted, tween(180), label = "button-bg")
    Box(
        modifier
            .padding(end = 4.dp, bottom = 4.dp)
            .zenShadow(4.dp, p.shadow, look.corner)
    ) {
        Box(
            Modifier
                .offset {
                    val o = press.value.roundToPx()
                    IntOffset(o, o)
                }
                .then(if (fill) Modifier.fillMaxWidth() else Modifier)
                .clip(look.shape)
                .background(background)
                .border(look.border, p.border, look.shape)
                .defaultMinSize(minHeight = MIN_TOUCH)
                .clickable(interaction, null, enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = if (small) 10.dp else 16.dp, vertical = if (small) 7.dp else 12.dp),
            contentAlignment = Alignment.Center
        ) {
            FitLabel(
                text.uppercase(), if (enabled && (accent == p.card || accent == p.bg)) p.ink else Sumi,
                baseSp = if (small) 12f else 14f, letterSpacing = if (small) 0.5.sp else 1.sp
            )
        }
    }
}

/**
 * Rótulo de botão/chip numa linha só: se não couber (idioma mais longo, tela estreita, botões lado a lado),
 * a fonte encolhe até um mínimo, para que todos os botões de uma fileira fiquem com a mesma altura.
 */
@Composable
private fun FitLabel(text: String, color: Color, baseSp: Float, letterSpacing: TextUnit) {
    var size by remember(text, baseSp) { mutableFloatStateOf(baseSp) }
    Text(
        text, color = color, style = MaterialTheme.typography.labelLarge,
        fontSize = size.sp, letterSpacing = if (size < baseSp) 0.sp else letterSpacing,
        maxLines = 1, softWrap = false, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis,
        onTextLayout = { if (it.hasVisualOverflow && size > MIN_LABEL_SP) size = (size - 1f).coerceAtLeast(MIN_LABEL_SP) }
    )
}

private const val MIN_LABEL_SP = 9f

/** Tamanho mínimo recomendado para alvos de toque (acessibilidade). */
internal val MIN_TOUCH = 48.dp

@Composable
fun ZenChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Ki,
    compact: Boolean = false
) {
    val p = Zen.p
    val interaction = remember { MutableInteractionSource() }
    val background by animateColorAsState(if (selected) accent else p.card, tween(200), label = "chip-bg")
    val content by animateColorAsState(if (selected) Sumi else p.ink, tween(200), label = "chip-fg")
    val look = Zen.look
    Box(
        modifier
            .pressScale(interaction)
            .clip(look.shape)
            .background(background)
            .border(look.border, p.border, look.shape)
            .defaultMinSize(minHeight = MIN_TOUCH)
            .semantics { this.selected = selected }
            .clickable(interaction, null, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (compact) 0.dp else 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        FitLabel(text, content, baseSp = if (compact) 12f else 14f, letterSpacing = if (compact) 0.sp else 1.sp)
    }
}

@Composable
fun ZenField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String = ""
) {
    val p = Zen.p
    Column(modifier) {
        Text(label.uppercase(), color = p.muted, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = p.ink),
            cursorBrush = SolidColor(Aka),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier
                .fillMaxWidth()
                .clip(Zen.look.smallShape)
                .background(p.bg)
                .border(Zen.look.border, p.border, Zen.look.smallShape)
                .padding(12.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(placeholder, color = p.muted, style = MaterialTheme.typography.bodyLarge)
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
fun KanjiBadge(kanji: String, modifier: Modifier = Modifier, color: Color = Aka, size: Dp = 46.dp) {
    val p = Zen.p
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(Zen.look.border, p.border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(kanji, color = Sumi, fontWeight = FontWeight.Black, fontSize = (size.value * 0.5f).sp)
        // Kawaii: um brilho no canto do selo.
        if (Zen.look.kawaii) {
            Text(
                "✦", color = p.card, fontSize = (size.value * 0.24f).sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = size * 0.1f, end = size * 0.14f)
            )
        }
    }
}

@Composable
fun ZenHeader(title: String, kanji: String, modifier: Modifier = Modifier, subtitle: String? = null, accent: Color = Aka) {
    val p = Zen.p
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title.uppercase(), style = MaterialTheme.typography.displayMedium)
            Box(Modifier.width(72.dp).height(6.dp).clip(Zen.look.smallShape).background(accent))
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, color = p.muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        KanjiBadge(kanji, color = accent, size = 56.dp)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, accent: Color = Aka) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (Zen.look.kawaii) Box(Modifier.size(14.dp).clip(CircleShape).background(accent))
        else Box(Modifier.width(8.dp).height(22.dp).background(accent))
        Spacer(Modifier.width(8.dp))
        Text(text.uppercase(), style = MaterialTheme.typography.headlineSmall)
    }
}

/** Linhas de velocidade irradiando do canto superior direito. */
@Composable
fun SpeedLines(modifier: Modifier = Modifier) {
    val color = Zen.p.muted.copy(alpha = 0.18f)
    Canvas(modifier.fillMaxSize()) {
        val origin = Offset(size.width, 0f)
        val lines = 26
        for (i in 0 until lines) {
            val angle = Math.PI / 2 + Math.PI / 2 * i / (lines - 1)
            val dx = cos(angle).toFloat()
            val dy = sin(angle).toFloat()
            val start = size.width * (0.35f + (i % 4) * 0.08f)
            val end = size.width * (0.9f + (i % 3) * 0.25f)
            drawLine(
                color,
                Offset(origin.x + dx * start, origin.y + dy * start),
                Offset(origin.x + dx * end, origin.y + dy * end),
                strokeWidth = if (i % 3 == 0) 3f else 1.5f
            )
        }
    }
}

/** Kawaii: brilhos, corações e bolinhas no topo da página, no lugar das linhas de velocidade. */
@Composable
fun Sparkles(modifier: Modifier = Modifier) {
    val pink = Aka.copy(alpha = 0.4f)
    val lilac = Sakura.copy(alpha = 0.4f)
    val blue = Ao.copy(alpha = 0.45f)
    Canvas(modifier.fillMaxSize()) {
        // Posições fixas (frações da área) para o desenho não "pular" entre telas.
        val marks = listOf(
            Triple(0.86f, 0.10f, 0), Triple(0.70f, 0.26f, 1), Triple(0.93f, 0.40f, 2), Triple(0.58f, 0.08f, 2),
            Triple(0.78f, 0.55f, 0), Triple(0.47f, 0.32f, 1), Triple(0.96f, 0.70f, 1), Triple(0.64f, 0.78f, 2),
            Triple(0.36f, 0.12f, 0), Triple(0.88f, 0.88f, 2)
        )
        marks.forEachIndexed { i, (fx, fy, kind) ->
            val c = Offset(size.width * fx, size.height * fy)
            val r = (if (i % 3 == 0) 9f else 6f) * density
            when (kind) {
                0 -> drawHeart(c, r, pink)
                1 -> drawSparkle(c, r, lilac)
                else -> drawCircle(blue, r * 0.6f, c)
            }
        }
    }
}

/** Coração simples (dois círculos e um triângulo) centrado em [c]. */
fun DrawScope.drawHeart(c: Offset, r: Float, color: Color) {
    drawCircle(color, r * 0.55f, Offset(c.x - r * 0.45f, c.y - r * 0.2f))
    drawCircle(color, r * 0.55f, Offset(c.x + r * 0.45f, c.y - r * 0.2f))
    val path = Path().apply {
        moveTo(c.x - r * 0.98f, c.y - r * 0.02f)
        lineTo(c.x + r * 0.98f, c.y - r * 0.02f)
        lineTo(c.x, c.y + r * 0.95f)
        close()
    }
    drawPath(path, color)
}

/** Estrelinha de quatro pontas (brilho) centrada em [c]. */
fun DrawScope.drawSparkle(c: Offset, r: Float, color: Color) {
    val k = r * 0.28f
    val path = Path().apply {
        moveTo(c.x, c.y - r)
        lineTo(c.x + k, c.y - k)
        lineTo(c.x + r, c.y)
        lineTo(c.x + k, c.y + k)
        lineTo(c.x, c.y + r)
        lineTo(c.x - k, c.y + k)
        lineTo(c.x - r, c.y)
        lineTo(c.x - k, c.y - k)
        close()
    }
    drawPath(path, color)
}

/** Sombra deslocada do estilo: retângulo sólido no Shōnen; no Kawaii, arredondada com a curva do cartão. */
fun Modifier.zenShadow(offset: Dp, color: Color, corner: Dp): Modifier = drawBehind {
    val o = offset.toPx()
    if (corner == 0.dp) drawRect(color, topLeft = Offset(o, o), size = size)
    else drawRoundRect(color, topLeft = Offset(o, o), size = size, cornerRadius = CornerRadius(corner.toPx()))
}

@Composable
fun ZenPage(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Zen.p.bg)) {
        if (Zen.look.kawaii) Sparkles(Modifier.height(260.dp).fillMaxWidth())
        else SpeedLines(Modifier.height(260.dp).fillMaxWidth())
        content()
    }
}

@Composable
fun StatPill(label: String, value: String, modifier: Modifier = Modifier, color: Color = Ki) {
    val p = Zen.p
    Column(
        modifier
            .clip(Zen.look.smallShape)
            .background(color)
            .border(Zen.look.border, p.border, Zen.look.smallShape)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Uma linha só: o valor encolhe e o rótulo usa fonte menor, para caber em telas estreitas.
        Text(value, color = Sumi, style = MaterialTheme.typography.titleMedium, fontSize = 16.sp, maxLines = 1, softWrap = false)
        Text(
            label.uppercase(), color = Sumi, style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp, letterSpacing = 0.sp, maxLines = 1, softWrap = false
        )
    }
}
