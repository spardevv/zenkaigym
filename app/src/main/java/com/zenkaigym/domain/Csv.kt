package com.zenkaigym.domain

import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.data.entity.MealCheck
import com.zenkaigym.data.entity.SetLog
import com.zenkaigym.data.entity.WaterLog
import com.zenkaigym.data.entity.WeightEntry
import java.util.Locale

/**
 * Exportação em CSV para planilhas: vírgula como separador, ponto nos decimais e datas ISO (aaaa-mm-dd),
 * que Excel, Google Planilhas e LibreOffice entendem em qualquer idioma. Os cabeçalhos seguem o idioma do app.
 */
object Csv {
    private const val DAY_MS = 86_400_000L

    /** Campo com vírgula, aspas ou quebra de linha vai entre aspas (aspas internas dobradas). */
    fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

    fun row(values: List<Any?>): String = values.joinToString(",") { v ->
        when (v) {
            null -> ""
            is Double -> number(v)
            is Boolean -> if (v) "1" else "0"
            else -> escape(v.toString())
        }
    }

    fun number(v: Double): String {
        val text = String.format(Locale.US, "%.2f", v).trimEnd('0').trimEnd('.')
        return if (text == "-0") "0" else text
    }

    /** 20773 -> "2026-11-16". */
    fun date(epochDay: Long): String {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        return format.format(java.util.Date(epochDay * DAY_MS))
    }

    private fun table(header: List<String>, rows: List<List<Any?>>): String =
        (listOf(row(header)) + rows.map(::row)).joinToString("\r\n", postfix = "\r\n")

    fun sets(sets: List<SetLog>, exercises: List<Exercise>): String {
        val byId = exercises.associateBy { it.id }
        val rows = sets.sortedWith(compareBy({ it.epochDay }, { it.exerciseId }, { it.setIndex })).map { s ->
            val ex = byId[s.exerciseId]
            listOf(
                date(s.epochDay), ex?.let { tr(it.name) } ?: s.exerciseId.toString(), ex?.group?.label,
                if (s.warmup) -s.setIndex else s.setIndex, s.warmup, s.weightKg, s.reps,
                s.rir, s.rir?.let(Progression::rpe), s.distanceKm, s.kcal,
                Progression.oneRepMax(s.reps, s.weightKg).takeIf { it > 0 && !s.warmup }, s.note
            )
        }
        return table(
            listOf(
                tr("data"), tr("exercício"), tr("grupo"), tr("série"), tr("aquecimento"), tr("kg"), tr("reps / min"),
                "RIR", "RPE", tr("km"), tr("kcal"), tr("1RM estimado"), tr("nota")
            ),
            rows
        )
    }

    fun weights(weights: List<WeightEntry>): String =
        table(listOf(tr("data"), tr("peso (kg)")), weights.sortedBy { it.epochDay }.map { listOf(date(it.epochDay), it.weightKg) })

    fun meals(checks: List<MealCheck>): String =
        table(
            listOf(tr("data"), tr("refeição"), tr("situação"), tr("kcal")),
            checks.sortedWith(compareBy({ it.epochDay }, { it.type.ordinal })).map {
                listOf(date(it.epochDay), it.type.label, if (it.skipped) tr("não realizada") else tr("feita"), it.kcal)
            }
        )

    fun water(logs: List<WaterLog>): String =
        table(listOf(tr("data"), tr("água (ml)")), logs.sortedBy { it.epochDay }.map { listOf(date(it.epochDay), it.ml) })
}
