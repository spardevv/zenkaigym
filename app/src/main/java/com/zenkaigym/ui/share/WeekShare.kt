package com.zenkaigym.ui.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.zenkaigym.domain.ProgressStats
import com.zenkaigym.domain.tr
import com.zenkaigym.util.formatEpochDay
import java.io.File
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Cartão da semana para compartilhar (WhatsApp, Instagram…): um quadro de mangá com nível, patente e os
 * números da semana, desenhado direto num Bitmap (não depende do tema nem do tamanho da tela).
 * A imagem fica no cache do app e só sai do aparelho se o usuário escolher para onde mandar.
 */
object WeekShare {
    private const val W = 1080
    private const val H = 1350

    private const val PAPER = 0xFFFFFDF7.toInt()
    private const val INK = 0xFF17171A.toInt()
    private const val MUTED = 0xFF6B6B73.toInt()
    private const val AKA = 0xFFF25A52.toInt()
    private const val KI = 0xFFFFD23F.toInt()
    private const val MIDORI = 0xFF55D48A.toInt()
    private const val AO = 0xFF45C0EE.toInt()
    private const val DAIDAI = 0xFFFF9F43.toInt()

    fun share(context: Context, stats: ProgressStats) {
        try {
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(dir, "zenkaigym-semana.png")
            val bitmap = render(stats)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, tr("Minha semana no Zenkaigym 禅"))
                clipData = ClipData.newRawUri("", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, tr("Compartilhar resumo da semana")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Log.e("WeekShare", "Falha ao compartilhar o resumo", e)
            Toast.makeText(context, tr("Não foi possível gerar a imagem."), Toast.LENGTH_LONG).show()
        }
    }

    fun render(s: ProgressStats): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(PAPER)

        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = INK }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val black = Typeface.create("sans-serif-black", Typeface.NORMAL)

        // Linhas de velocidade saindo do canto do emblema (集中線), bem claras.
        fill.color = 0x14000000
        val cx = W * 0.78f
        val cy = 330f
        for (k in 0 until 48) {
            val a = Math.toRadians(k * 7.5)
            val r1 = 220f
            val r2 = 1400f
            val path = android.graphics.Path().apply {
                moveTo(cx + cos(a).toFloat() * r1, cy + sin(a).toFloat() * r1)
                lineTo(cx + cos(a + 0.025).toFloat() * r2, cy + sin(a + 0.025).toFloat() * r2)
                lineTo(cx + cos(a - 0.025).toFloat() * r2, cy + sin(a - 0.025).toFloat() * r2)
                close()
            }
            c.drawPath(path, fill)
        }

        // Moldura de quadro de mangá.
        stroke.strokeWidth = 14f
        c.drawRect(28f, 28f, W - 28f, H - 28f, stroke)

        // Faixa vermelha do título.
        fill.color = AKA
        c.drawRect(28f, 28f, W - 28f, 64f, fill)

        text.textSize = 44f
        text.color = INK
        c.drawText("ZENKAIGYM 禅", 80f, 150f, text)
        text.textSize = 34f
        text.color = MUTED
        val first = s.weekFirstDay
        c.drawText(tr("SEMANA {0} – {1}", formatEpochDay(first), formatEpochDay(first + 6)), 80f, 200f, text)

        // Emblema da patente.
        val badge = RectF(cx - 150f, cy - 150f, cx + 150f, cy + 150f)
        fill.color = DAIDAI
        c.drawRoundRect(RectF(badge.left + 14, badge.top + 14, badge.right + 14, badge.bottom + 14), 28f, 28f, Paint(fill).apply { color = INK })
        c.drawRoundRect(badge, 28f, 28f, fill)
        stroke.strokeWidth = 10f
        c.drawRoundRect(badge, 28f, 28f, stroke)
        text.color = INK
        text.typeface = black
        text.textSize = 200f
        text.textAlign = Paint.Align.CENTER
        c.drawText(s.level.rank.kanji, cx, cy + 70f, text)
        text.textAlign = Paint.Align.LEFT

        // Nível, patente e XP.
        text.textSize = 110f
        c.drawText(tr("NÍVEL {0}", s.level.level), 80f, 360f, text)
        text.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        text.textSize = 44f
        text.color = MUTED
        c.drawText(s.level.rank.label.uppercase(), 84f, 420f, text)
        // Barra de XP.
        val bar = RectF(80f, 450f, 700f, 486f)
        fill.color = 0xFFE8E4DA.toInt()
        c.drawRect(bar, fill)
        fill.color = DAIDAI
        c.drawRect(RectF(bar.left, bar.top, bar.left + bar.width() * s.level.fraction, bar.bottom), fill)
        stroke.strokeWidth = 5f
        c.drawRect(bar, stroke)

        // Números da semana em quadros.
        val w = s.thisWeek
        val tiles = listOf(
            Triple(tr("TREINOS"), if (s.plannedPerWeek > 0) "${w.sessions}/${s.plannedPerWeek}" else "${w.sessions}", KI),
            Triple(tr("SÉRIES"), "${w.sets}", AKA),
            Triple(tr("VOLUME"), volume(w.volumeKg), DAIDAI),
            Triple(tr("DIETA"), "${s.weekDietPercent}%", MIDORI),
            Triple(tr("RECORDES"), "${w.records}", KI),
            Triple(tr("CARDIO"), tr("{0} min", w.cardioMinutes), AO),
            Triple(tr("DESAFIOS"), "${s.challenges.count { it.completed }}/${s.challenges.size}", AKA),
            Triple(tr("SEQUÊNCIA"), tr("{0} sem.", s.weekStreak), MIDORI)
        )
        val cols = 2
        val gap = 24f
        val left = 80f
        val top = 560f
        val tileW = (W - 2 * left - gap) / cols
        val tileH = 130f
        tiles.forEachIndexed { i, (label, value, color) ->
            val x = left + (i % cols) * (tileW + gap)
            val y = top + (i / cols) * (tileH + gap)
            val r = RectF(x, y, x + tileW, y + tileH)
            fill.color = INK
            c.drawRect(RectF(r.left + 10, r.top + 10, r.right + 10, r.bottom + 10), fill)
            fill.color = PAPER
            c.drawRect(r, fill)
            fill.color = color
            c.drawRect(RectF(r.left, r.top, r.left + 18f, r.bottom), fill)
            stroke.strokeWidth = 6f
            c.drawRect(r, stroke)
            text.color = MUTED
            text.textSize = 30f
            text.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            c.drawText(label, r.left + 44f, r.top + 44f, text)
            text.color = INK
            text.textSize = 60f
            text.typeface = black
            c.drawText(value, r.left + 44f, r.top + 108f, text)
        }

        text.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        text.textSize = 32f
        text.color = MUTED
        text.textAlign = Paint.Align.CENTER
        c.drawText(tr("Feito com Zenkaigym · treino e dieta offline"), W / 2f, H - 70f, text)
        return bmp
    }

    private fun volume(kg: Double): String =
        if (kg >= 1000) "%.1f t".format(java.util.Locale.US, kg / 1000) else "${kg.roundToInt()} kg"
}
