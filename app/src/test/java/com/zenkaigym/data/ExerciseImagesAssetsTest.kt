package com.zenkaigym.data

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Confere que cada crédito aponta para um exercício do catálogo e para uma imagem que existe nos assets. */
class ExerciseImagesAssetsTest {
    private val dir = File("src/main/assets/exercises")
    private val json = File(dir, "credits.json").readText(Charsets.UTF_8)

    private fun values(key: String) = Regex("\"$key\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(json).map { it.groupValues[1] }.toList()

    @Test
    fun everyCreditMatchesACatalogExerciseAndAnExistingFile() {
        val names = values("name")
        val files = values("file")
        assertTrue(names.isNotEmpty() && names.size == files.size)
        val catalog = ExerciseCatalog.entries.map { it.name }.toSet()
        names.forEach { assertTrue("sem exercício no catálogo: $it", it in catalog) }
        files.forEach { assertTrue("arquivo ausente: $it", File(dir, it).isFile) }
    }

    @Test
    fun everyCreditHasAuthorAndCreativeCommonsLicense() {
        val n = values("name").size
        assertTrue(values("author").size == n && values("author").none { it.isBlank() })
        assertTrue(values("license").size == n && values("license").all { it.startsWith("CC") })
        assertTrue(values("source").all { it.startsWith("https://wger.de/") })
    }
}
