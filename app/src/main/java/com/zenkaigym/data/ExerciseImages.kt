package com.zenkaigym.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONObject

/** Crédito do diagrama de músculos, igual para todos os exercícios. */
data class DiagramCredit(val title: String, val author: String, val license: String, val licenseUrl: String, val source: String)

/** Imagem de um exercício: o diagrama de músculos em [file]. */
data class ExerciseImage(val name: String, val file: String)

/**
 * Imagens dos exercícios, embutidas no app (assets/exercises) a partir do banco aberto wger.de: todo exercício
 * tem o mesmo diagrama de músculos (frente e costas, principal em vermelho e auxiliares em laranja).
 * Funciona sem internet. Exercícios criados pelo usuário simplesmente não mostram imagem.
 */
object ExerciseImages {
    private const val DIR = "exercises"

    private class Data(val diagram: DiagramCredit?, val items: Map<String, ExerciseImage>)

    @Volatile private var data: Data? = null

    private fun parse(json: String): Data {
        val root = JSONObject(json)
        val d = root.optJSONObject("diagram")?.let {
            DiagramCredit(
                it.getString("title"), it.getString("author"), it.getString("license"),
                it.getString("licenseUrl"), it.getString("source")
            )
        }
        val array = root.getJSONArray("items")
        val items = (0 until array.length()).map { array.getJSONObject(it) }.associate { o ->
            o.getString("name") to ExerciseImage(o.getString("name"), o.getString("file"))
        }
        return Data(d, items)
    }

    private fun data(context: Context): Data = data ?: synchronized(this) {
        data ?: runCatching {
            parse(context.assets.open("$DIR/credits.json").bufferedReader(Charsets.UTF_8).use { it.readText() })
        }.getOrDefault(Data(null, emptyMap())).also { data = it }
    }

    fun imageFor(context: Context, exerciseName: String): ExerciseImage? = data(context).items[exerciseName]

    fun diagramCredit(context: Context): DiagramCredit? = data(context).diagram

    /** Poucos bitmaps em memória (o pager recompõe a mesma imagem ao deslizar); evita decodificar o WebP toda vez. */
    private val cache = object : android.util.LruCache<String, Bitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun load(context: Context, exerciseName: String): Bitmap? {
        val image = imageFor(context, exerciseName) ?: return null
        cache.get(image.file)?.let { return it }
        return runCatching {
            context.assets.open("$DIR/${image.file}").use { BitmapFactory.decodeStream(it) }
        }.getOrNull()?.also { cache.put(image.file, it) }
    }
}
