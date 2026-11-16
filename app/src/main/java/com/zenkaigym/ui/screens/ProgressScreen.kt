package com.zenkaigym.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import com.zenkaigym.ui.components.IconText
import com.zenkaigym.ui.components.ZenIcon
import com.zenkaigym.ui.components.ZenIcons
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenkaigym.data.entity.WeightEntry
import com.zenkaigym.domain.Achievement
import com.zenkaigym.domain.LevelInfo
import com.zenkaigym.domain.ProgressStats
import com.zenkaigym.domain.XpRules
import com.zenkaigym.domain.tr
import com.zenkaigym.ui.ZenViewModel
import com.zenkaigym.ui.components.KanjiBadge
import com.zenkaigym.ui.components.ScrollHints
import com.zenkaigym.ui.components.SectionTitle
import com.zenkaigym.ui.components.StatTile
import com.zenkaigym.ui.components.ZenBarChart
import com.zenkaigym.ui.components.ZenButton
import com.zenkaigym.ui.components.ZenCard
import com.zenkaigym.ui.components.ZenChip
import com.zenkaigym.ui.components.ZenFixedHeader
import com.zenkaigym.ui.components.ZenPage
import com.zenkaigym.ui.components.ZenProgressBar
import com.zenkaigym.ui.components.currentWindowLayout
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Daidai
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Sakura
import com.zenkaigym.ui.theme.Sumi
import com.zenkaigym.ui.theme.Zen
import com.zenkaigym.util.WEEK_DAYS_SHORT
import com.zenkaigym.util.formatEpochDay
import com.zenkaigym.util.todayDayOfWeek
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class ProgressTab(val pt: String, val icon: ImageVector) {
    SUMMARY("Resumo", ZenIcons.Summary),
    TRAINING("Treino", ZenIcons.Workout),
    DIET("Dieta", ZenIcons.Diet),
    WEIGHT("Peso", ZenIcons.Weight),
    ACHIEVEMENTS("Conquistas", ZenIcons.Trophy)
}

/** Cada aba usa a cor do assunto, a mesma das telas de Treino, Dieta e Conquistas. */
@Composable
private fun ProgressTab.accent(): Color = when (this) {
    ProgressTab.SUMMARY -> Daidai
    ProgressTab.TRAINING -> Ki
    ProgressTab.DIET -> Midori
    ProgressTab.WEIGHT -> Ao
    ProgressTab.ACHIEVEMENTS -> Sakura
}

/**
 * Faixa de abas com rolagem horizontal: a aba escolhida sempre fica à vista (ao abrir pelo botão
 * "Ver conquistas", por exemplo) e um esmaecido na borda mostra que há mais abas para o lado.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProgressTabs(selected: Int, onSelect: (Int) -> Unit, gutter: Dp) {
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(scroll).padding(horizontal = gutter),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ProgressTab.entries.forEachIndexed { i, t ->
                val requester = remember { BringIntoViewRequester() }
                LaunchedEffect(selected) { if (i == selected) requester.bringIntoView() }
                ZenChip(
                    tr(t.pt), i == selected, { onSelect(i) }, Modifier.bringIntoViewRequester(requester),
                    accent = t.accent(), mark = t.icon
                )
            }
        }
        // Esmaecidos nas bordas. A camada acompanha o tamanho da faixa (matchParentSize): um fillMaxHeight solto
        // aqui ocuparia a tela toda e deixaria o conteúdo da aba com altura zero.
        val bg = Zen.p.bg
        Box(Modifier.matchParentSize()) {
            if (scroll.canScrollBackward) {
                Box(
                    Modifier.align(Alignment.CenterStart).fillMaxHeight().width(gutter + 14.dp)
                        .background(Brush.horizontalGradient(listOf(bg, Color.Transparent)))
                )
            }
            if (scroll.canScrollForward) {
                Box(
                    Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(gutter + 22.dp)
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, bg)))
                )
            }
        }
    }
}

private val AchievementColors = listOf(Ki, Aka, Ao, Midori, Daidai, Sakura)

/**
 * Evolução: um painel de jogador leve — nível e XP, metas e sequências da semana, e gráficos de treino, dieta e peso.
 * Tudo é calculado do que o usuário já registra; nada aqui exige uma ação extra.
 */
@Composable
fun ProgressScreen(viewModel: ZenViewModel, onBack: () -> Unit) {
    val stats by viewModel.progressStats.collectAsStateWithLifecycle()
    val weights by viewModel.weights.collectAsStateWithLifecycle()
    val clock by viewModel.clock.collectAsStateWithLifecycle()
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tab = ProgressTab.entries[tabIndex.coerceIn(0, ProgressTab.entries.lastIndex)]
    val gutter = currentWindowLayout.gutter

    ZenPage {
        Column(Modifier.fillMaxSize()) {
            ZenFixedHeader(
                tr("Evolução"), "進", accent = Daidai, subtitle = tr("Treino, dieta, peso e conquistas"),
                back = { ZenButton(tr("Voltar ao perfil"), onBack, accent = Zen.p.card, small = true, leading = ZenIcons.ArrowLeft) }
            )
            ProgressTabs(tabIndex, { tabIndex = it }, gutter)
            Spacer(Modifier.height(10.dp))
            val scroll = rememberScrollState()
            LaunchedEffect(tab) { scroll.scrollTo(0) }
            Box(Modifier.weight(1f)) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(scroll)
                        .padding(start = gutter, end = gutter, top = 4.dp, bottom = gutter),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val s = stats
                    if (s == null) {
                        Text(tr("Carregando…"), color = Zen.p.muted)
                    } else when (tab) {
                        ProgressTab.SUMMARY -> SummaryTab(s, onSeeAchievements = { tabIndex = ProgressTab.ACHIEVEMENTS.ordinal })
                        ProgressTab.TRAINING -> TrainingTab(s)
                        ProgressTab.DIET -> DietTab(s, clock.epochDay)
                        ProgressTab.WEIGHT -> WeightTab(s, weights)
                        ProgressTab.ACHIEVEMENTS -> AchievementsTab(s)
                    }
                }
                ScrollHints(scroll.canScrollBackward, scroll.canScrollForward, Modifier.matchParentSize())
            }
        }
    }
}

// ---- Nível ----

/** Nível, patente e barra de XP. Com [onClick], o cartão inteiro é um atalho (usado no Perfil). */
@Composable
internal fun LevelCard(level: LevelInfo, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val p = Zen.p
    ZenCard(
        modifier.then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        accent = Daidai, entranceIndex = 0
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KanjiBadge(level.rank.kanji, color = Daidai, size = 56.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(tr("NÍVEL {0}", level.level), style = MaterialTheme.typography.headlineSmall)
                Text(level.rank.label.uppercase(), color = p.muted, style = MaterialTheme.typography.labelLarge)
            }
            Text("${level.xp} XP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(12.dp))
        ZenProgressBar(level.fraction, Daidai, height = 14.dp)
        Spacer(Modifier.height(4.dp))
        Text(
            tr("{0} / {1} XP para o nível {2}", level.xpIntoLevel, level.xpForNext, level.level + 1),
            color = p.muted, style = MaterialTheme.typography.labelSmall
        )
    }
}

// ---- Resumo ----

@Composable
private fun SummaryTab(s: ProgressStats, onSeeAchievements: () -> Unit) {
    LevelCard(s.level)
    WeekCard(s)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(tr("Semanas na meta"), s.weekStreak, Modifier.weight(1f), accent = if (s.weekStreak >= 2) Ki else null)
        StatTile(tr("Dias de dieta"), s.dietStreak, Modifier.weight(1f), accent = if (s.dietStreak >= 3) Midori else null)
        StatTile(tr("Recordes"), s.records, Modifier.weight(1f))
    }

    val next = s.achievements.filter { !it.unlocked }.sortedByDescending { it.fraction }.take(3)
    ZenCard(accent = Sakura, entranceIndex = 2) {
        SectionTitle(tr("Próximas conquistas"), accent = Sakura)
        Spacer(Modifier.height(10.dp))
        if (next.isEmpty()) {
            Text(tr("Todas as conquistas desbloqueadas. Lenda!"), color = Zen.p.muted)
        } else {
            next.forEachIndexed { i, a ->
                AchievementRow(a, AchievementColors[s.achievements.indexOf(a) % AchievementColors.size])
                if (i < next.lastIndex) Spacer(Modifier.height(12.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        ZenButton(
            tr("Ver conquistas ({0}/{1})", s.unlockedCount, s.achievements.size),
            onSeeAchievements, accent = Sakura, fill = true, small = true, trailing = ZenIcons.ArrowRight
        )
    }

    Text(
        tr(
            "XP: treino +{0} · série +{1} · refeição +{2} · recorde +{3} · peso +{4}",
            XpRules.SESSION, XpRules.SET, XpRules.MEAL, XpRules.RECORD, XpRules.WEIGHT
        ),
        color = Zen.p.muted, style = MaterialTheme.typography.labelSmall
    )
}

/** Meta da semana: treinos feitos x planejados, os sete dias e a dieta até agora. */
@Composable
private fun WeekCard(s: ProgressStats) {
    val p = Zen.p
    val look = Zen.look
    ZenCard(accent = Ki, entranceIndex = 1) {
        SectionTitle(tr("Esta semana"), accent = Ki)
        Spacer(Modifier.height(10.dp))
        if (s.plannedPerWeek > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Treinos"), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Text("${s.weekSessions} / ${s.plannedPerWeek}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(4.dp))
            ZenProgressBar(s.weekSessions.toFloat() / s.plannedPerWeek, Ki)
        } else {
            Text(tr("{0} treinos nesta semana", s.weekSessions), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
        val todayDow = todayDayOfWeek()
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Zen.week.days.forEachIndexed { i, dow ->
                val done = s.weekDots.getOrElse(i) { false }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(look.smallShape)
                            .background(if (done) Ki else p.bg)
                            .border(if (dow == todayDow) look.border else look.thinBorder, p.border, look.smallShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (done) ZenIcon(ZenIcons.Check, size = 18.dp, color = Sumi)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(WEEK_DAYS_SHORT[dow - 1], color = if (dow == todayDow) p.ink else p.muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Dieta"), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            Text("${s.weekDietPercent}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(4.dp))
        ZenProgressBar(s.weekDietPercent / 100f, Midori)
    }
}

// ---- Treino ----

@Composable
private fun TrainingTab(s: ProgressStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(tr("Treinos"), s.totalSessions, Modifier.weight(1f))
        StatTile(tr("Séries"), s.totalSets, Modifier.weight(1f))
        StatTile(tr("Volume"), s.totalVolumeKg.roundToInt(), Modifier.weight(1f), format = ::volumeText)
    }
    if (!s.hasTrainingData) {
        ZenCard(accent = Ki, entranceIndex = 1) { EmptyProgress("体", Ki, tr("Registre suas séries no modo treino para ver a evolução aqui.")) }
        return
    }

    ZenCard(accent = Ki, entranceIndex = 1) {
        SectionTitle(tr("Treinos por semana"), accent = Ki)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(tr("Média / semana"), (s.avgSessionsPerWeek * 10).roundToInt(), Modifier.weight(1f), format = { "%.1f".format(it / 10f) })
            StatTile(tr("Melhor semana"), s.bestWeekSessions, Modifier.weight(1f))
            StatTile(tr("Melhor sequência"), s.bestWeekStreak, Modifier.weight(1f), format = { tr("{0} sem.", it) })
        }
        Spacer(Modifier.height(12.dp))
        val top = maxOf(s.plannedPerWeek, s.sessionsPerWeek.maxOrNull() ?: 0, 1).toFloat()
        ZenBarChart(
            values = s.sessionsPerWeek.map { it.toFloat() },
            color = Ki,
            maxValue = top,
            describe = { i ->
                val back = s.sessionsPerWeek.size - 1 - i
                val week = if (back == 0) tr("Esta semana") else tr("Há {0} sem.", back)
                tr("{0} · {1} treinos", week, s.sessionsPerWeek[i])
            },
            startLabel = tr("{0} semanas atrás", s.sessionsPerWeek.size - 1),
            endLabel = tr("Agora")
        )
        if (s.plannedPerWeek > 0) {
            Spacer(Modifier.height(4.dp))
            Text(tr("Meta: {0} treinos por semana.", s.plannedPerWeek), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
        }
    }

    ZenCard(accent = Aka, entranceIndex = 2) {
        SectionTitle(tr("Volume por semana"), accent = Aka)
        Spacer(Modifier.height(10.dp))
        val change = if (s.volumePerWeek.size >= 2 && s.volumePerWeek[s.volumePerWeek.size - 2] > 0.0) {
            ((s.volumePerWeek.last() - s.volumePerWeek[s.volumePerWeek.size - 2]) / s.volumePerWeek[s.volumePerWeek.size - 2] * 100).roundToInt()
        } else null
        change?.let {
            IconText(
                if (it >= 0) tr("+{0}% sobre a semana passada", it) else tr("−{0}% sobre a semana passada", -it),
                Modifier.background(if (it >= 0) Midori else Daidai).padding(horizontal = 8.dp, vertical = 3.dp),
                leading = if (it >= 0) ZenIcons.TrendUp else ZenIcons.TrendDown,
                color = Sumi, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), iconSize = 12.dp
            )
            Spacer(Modifier.height(10.dp))
        }
        ZenBarChart(
            values = s.volumePerWeek.map { it.toFloat() },
            color = Aka,
            describe = { i ->
                val back = s.volumePerWeek.size - 1 - i
                val week = if (back == 0) tr("Esta semana") else tr("Há {0} sem.", back)
                tr("{0} · {1}", week, volumeText(s.volumePerWeek[i].roundToInt()))
            },
            startLabel = tr("{0} semanas atrás", s.volumePerWeek.size - 1),
            endLabel = tr("Agora")
        )
        Spacer(Modifier.height(4.dp))
        Text(tr("Volume = carga × repetições. Toque numa barra para ver a semana."), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
    }

    ZenCard(accent = Ao, entranceIndex = 3) {
        SectionTitle(tr("Músculos · 30 dias"), accent = Ao)
        Spacer(Modifier.height(10.dp))
        if (s.groups.isEmpty()) {
            Text(tr("Nenhuma série nos últimos 30 dias."), color = Zen.p.muted)
        } else {
            val top = s.groups.first().sets.toFloat()
            s.groups.take(6).forEachIndexed { i, g ->
                if (i > 0) Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(g.group.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(tr("{0} séries", g.sets), color = Zen.p.muted, style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(3.dp))
                ZenProgressBar(g.sets / top, Ao, height = 8.dp)
            }
        }
    }

    ZenCard(accent = Ki, entranceIndex = 4) {
        SectionTitle(tr("Recordes recentes"), accent = Ki)
        Spacer(Modifier.height(8.dp))
        if (s.recentRecords.isEmpty()) {
            Text(tr("Quando você superar uma carga, o recorde aparece aqui."), color = Zen.p.muted)
        } else {
            s.recentRecords.forEach { r ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr(r.name), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatEpochDay(r.epochDay), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        "${kgText(r.weightKg)} kg",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black
                    )
                    Text(
                        "  +${kgText(r.weightKg - r.previousKg)}",
                        color = Sumi, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.background(Midori).padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ---- Dieta ----

@Composable
private fun DietTab(s: ProgressStats, today: Long) {
    if (!s.hasDietData) {
        ZenCard(accent = Midori, entranceIndex = 0) {
            EmptyProgress("食", Midori, tr("Marque as refeições como feitas no Dojo para acompanhar sua constância."))
        }
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(tr("Média · 14 dias"), s.dietAveragePercent, Modifier.weight(1f), format = { "$it%" })
        StatTile(tr("Sequência"), s.dietStreak, Modifier.weight(1f), format = { tr("{0} dias", it) }, accent = if (s.dietStreak >= 3) Midori else null)
        StatTile(tr("Melhor"), s.bestDietStreak, Modifier.weight(1f), format = { tr("{0} dias", it) })
    }

    ZenCard(accent = Midori, entranceIndex = 1) {
        SectionTitle(tr("Constância por semana"), accent = Midori)
        Spacer(Modifier.height(10.dp))
        ZenBarChart(
            values = s.dietWeekly,
            color = Midori,
            maxValue = 1f,
            describe = { i ->
                val back = s.dietWeekly.size - 1 - i
                val week = if (back == 0) tr("Esta semana") else tr("Há {0} sem.", back)
                tr("{0} · {1}% das refeições", week, (s.dietWeekly[i] * 100).roundToInt())
            },
            startLabel = tr("{0} semanas atrás", s.dietWeekly.size - 1),
            endLabel = tr("Agora")
        )
    }

    ZenCard(accent = Ao, entranceIndex = 2) {
        SectionTitle(tr("Últimos 14 dias"), accent = Ao)
        Spacer(Modifier.height(10.dp))
        ZenBarChart(
            values = s.dietDaily,
            color = Ao,
            maxValue = 1f,
            describe = { i ->
                val day = today - (s.dietDaily.size - 1 - i)
                tr("{0} · {1}% das refeições", formatEpochDay(day), (s.dietDaily[i] * 100).roundToInt())
            },
            startLabel = formatEpochDay(today - (s.dietDaily.size - 1)),
            endLabel = tr("Hoje")
        )
        Spacer(Modifier.height(4.dp))
        Text(tr("{0} refeições registradas no total.", s.mealsDone), color = Zen.p.muted, style = MaterialTheme.typography.labelSmall)
    }
}

// ---- Peso ----

@Composable
private fun WeightTab(s: ProgressStats, weights: List<WeightEntry>) {
    val w = s.weight
    if (w == null || w.entries < 2) {
        ZenCard(accent = Daidai, entranceIndex = 0) {
            SectionTitle(tr("Evolução do peso"), accent = Daidai)
            Spacer(Modifier.height(10.dp))
            if (w != null) Text("%.1f kg".format(w.current), style = MaterialTheme.typography.displayMedium)
            EmptyProgress("秤", Daidai, tr("Registre seu peso no Perfil ao menos duas vezes para ver a evolução."))
        }
        return
    }
    fun tenths(v: Double) = (v * 10).roundToInt()
    fun signed(tenth: Int) = (if (tenth > 0) "+" else if (tenth < 0) "−" else "") + "%.1f".format(abs(tenth) / 10f)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(tr("Atual"), tenths(w.current), Modifier.weight(1f), format = { "%.1f kg".format(it / 10f) })
        StatTile(tr("Desde o início"), tenths(w.change), Modifier.weight(1f), format = { signed(it) + " kg" })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(tr("Últimos 30 dias"), tenths(w.change30 ?: 0.0), Modifier.weight(1f), format = { if (w.change30 == null) "—" else signed(it) + " kg" })
        StatTile(tr("Por semana"), tenths(w.perWeek ?: 0.0), Modifier.weight(1f), format = { if (w.perWeek == null) "—" else signed(it) + " kg" })
    }

    ZenCard(accent = Daidai, entranceIndex = 1) {
        SectionTitle(tr("Evolução do peso"), accent = Daidai)
        Spacer(Modifier.height(8.dp))
        w.onTrack?.let { onTrack ->
            Text(
                if (onTrack) tr("No rumo do seu objetivo") else tr("Fora do rumo do objetivo por enquanto"),
                color = Sumi, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                modifier = Modifier.background(if (onTrack) Midori else Daidai).padding(horizontal = 8.dp, vertical = 3.dp)
            )
            Spacer(Modifier.height(10.dp))
        }
        WeightTrendChart(weights.takeLast(24), Daidai)
        Spacer(Modifier.height(6.dp))
        Text(
            tr("Menor {0} kg · Maior {1} kg · {2} registros", "%.1f".format(w.min), "%.1f".format(w.max), w.entries),
            color = Zen.p.muted, style = MaterialTheme.typography.labelSmall
        )
    }
}

/** Linha do peso ao longo do tempo (pelo dia de cada registro), com área sob a curva e a faixa mínima–máxima. */
@Composable
private fun WeightTrendChart(entries: List<WeightEntry>, color: Color) {
    val p = Zen.p
    val min = entries.minOf { it.weightKg }
    val max = entries.maxOf { it.weightKg }
    val summary = tr("Evolução do peso: de {0} kg para {1} kg", "%.1f".format(entries.first().weightKg), "%.1f".format(entries.last().weightKg))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("%.1f kg".format(max), color = p.muted, style = MaterialTheme.typography.labelSmall)
    }
    Canvas(Modifier.fillMaxWidth().height(130.dp).semantics { contentDescription = summary }) {
        val padX = 10.dp.toPx()
        val padY = 10.dp.toPx()
        val range = (max - min).coerceAtLeast(0.5)
        val firstDay = entries.first().epochDay
        val span = (entries.last().epochDay - firstDay).coerceAtLeast(1)
        // Com poucos dias distintos (ex.: dois registros no mesmo dia) o espaçamento por data vira uma linha reta
        // na vertical; nesse caso os pontos ficam igualmente espaçados.
        val byPosition = entries.map { it.epochDay }.distinct().size < 3
        fun x(e: WeightEntry) = padX + (size.width - 2 * padX) *
            if (byPosition) entries.indexOf(e).toFloat() / (entries.size - 1).coerceAtLeast(1)
            else (e.epochDay - firstDay).toFloat() / span
        fun y(e: WeightEntry) = padY + (size.height - 2 * padY) * (1f - ((e.weightKg - min) / range).toFloat())
        // Linhas-guia (máximo, meio e mínimo).
        listOf(padY, size.height / 2, size.height - padY).forEach {
            drawLine(p.border.copy(alpha = 0.25f), Offset(padX, it), Offset(size.width - padX, it), strokeWidth = 1.dp.toPx())
        }
        val line = Path().apply { entries.forEachIndexed { i, e -> if (i == 0) moveTo(x(e), y(e)) else lineTo(x(e), y(e)) } }
        val area = Path().apply {
            addPath(line)
            lineTo(x(entries.last()), size.height - padY)
            lineTo(x(entries.first()), size.height - padY)
            close()
        }
        drawPath(area, color.copy(alpha = 0.18f))
        drawPath(line, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        entries.forEachIndexed { i, e ->
            drawCircle(color, if (i == entries.lastIndex) 6.dp.toPx() else 3.5.dp.toPx(), Offset(x(e), y(e)))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("%.1f kg".format(min), color = p.muted, style = MaterialTheme.typography.labelSmall)
        Text(
            "${formatEpochDay(entries.first().epochDay)} → ${formatEpochDay(entries.last().epochDay)}",
            color = p.muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End
        )
    }
}

// ---- Conquistas ----

@Composable
private fun AchievementsTab(s: ProgressStats) {
    ZenCard(accent = Sakura, entranceIndex = 0) {
        SectionTitle(tr("Conquistas"), accent = Sakura)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("{0} de {1} desbloqueadas", s.unlockedCount, s.achievements.size), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(4.dp))
        ZenProgressBar(s.unlockedCount.toFloat() / s.achievements.size.coerceAtLeast(1), Sakura)
    }
    ZenCard(accent = Ki, entranceIndex = 1) {
        // Desbloqueadas primeiro; dentro de cada grupo, a ordem original.
        val indexed = s.achievements.withIndex().sortedBy { if (it.value.unlocked) 0 else 1 }
        indexed.forEachIndexed { pos, (index, a) ->
            if (pos > 0) Spacer(Modifier.height(14.dp))
            AchievementRow(a, AchievementColors[index % AchievementColors.size])
        }
    }
}

@Composable
private fun AchievementRow(a: Achievement, accent: Color) {
    val p = Zen.p
    Row(verticalAlignment = Alignment.Top) {
        KanjiBadge(a.kanji, color = if (a.unlocked) accent else p.muted.copy(alpha = 0.55f), size = 44.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(tr(a.title), style = MaterialTheme.typography.titleMedium, color = if (a.unlocked) p.ink else p.muted)
            Text(tr(a.description), color = p.muted, style = MaterialTheme.typography.bodyMedium)
            if (!a.unlocked) {
                Spacer(Modifier.height(6.dp))
                ZenProgressBar(a.fraction, accent, height = 8.dp)
                Spacer(Modifier.height(2.dp))
                Text(a.progressText, color = p.muted, style = MaterialTheme.typography.labelSmall)
            }
        }
        if (a.unlocked) ZenIcon(ZenIcons.Check, Modifier.padding(start = 8.dp), size = 20.dp, color = Midori)
    }
}

// ---- Auxiliares ----

@Composable
private fun EmptyProgress(kanji: String, accent: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        KanjiBadge(kanji, color = accent, size = 40.dp)
        Text(text, Modifier.padding(start = 12.dp), color = Zen.p.muted, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Volume legível: até 999 em kg, depois em toneladas. */
private fun volumeText(kg: Int): String = if (kg >= 1000) "%.1f t".format(kg / 1000f) else "$kg kg"

private fun kgText(kg: Double): String = if (kg % 1.0 == 0.0) kg.toLong().toString() else "%.1f".format(kg)
