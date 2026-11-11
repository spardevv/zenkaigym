package com.zenkaigym.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray

/** Crédito de uma imagem de exercício (wger.de, licenças Creative Commons). */
data class ImageCredit(
    val name: String,
    val file: String,
    val title: String,
    val author: String,
    val license: String,
    val licenseUrl: String,
    val source: String
)

/**
 * Ilustrações dos exercícios, embutidas no app (assets/exercises) e vindas do banco aberto wger.de.
 * Cada imagem tem licença e autor próprios, guardados em credits.json e mostrados na tela e em Ajustes.
 * Funciona sem internet. Exercícios sem imagem (e os criados pelo usuário) simplesmente não mostram nada.
 */
object ExerciseImages {
    private const val DIR = "exercises"

    @Volatile private var credits: Map<String, ImageCredit>? = null

    private fun parse(json: String): Map<String, ImageCredit> {
        val array = JSONArray(json)
        return (0 until array.length()).map { array.getJSONObject(it) }.associate { o ->
            o.getString("name") to ImageCredit(
                name = o.getString("name"), file = o.getString("file"), title = o.getString("title"),
                author = o.getString("author"), license = o.getString("license"),
                licenseUrl = o.getString("licenseUrl"), source = o.getString("source")
            )
        }
    }

    private fun all(context: Context): Map<String, ImageCredit> = credits ?: synchronized(this) {
        credits ?: runCatching {
            parse(context.assets.open("$DIR/credits.json").bufferedReader(Charsets.UTF_8).use { it.readText() })
        }.getOrDefault(emptyMap()).also { credits = it }
    }

    fun creditFor(context: Context, exerciseName: String): ImageCredit? = all(context)[exerciseName]

    /** Todos os créditos, em ordem alfabética, para a tela de Ajustes. */
    fun allCredits(context: Context): List<ImageCredit> = all(context).values.sortedBy { it.name }

    fun load(context: Context, exerciseName: String): Bitmap? {
        val credit = creditFor(context, exerciseName) ?: return null
        return runCatching {
            context.assets.open("$DIR/${credit.file}").use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }
}
