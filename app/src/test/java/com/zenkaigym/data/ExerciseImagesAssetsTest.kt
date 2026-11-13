package com.zenkaigym.data

import com.zenkaigym.data.entity.MuscleGroup
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Confere que todo exercício do catálogo (menos cardio) tem imagem, que os arquivos existem e que os créditos estão completos. */
class ExerciseImagesAssetsTest {
    private val dir = File("src/main/assets/exercises")
    private val json = File(dir, "credits.json").readText(Charsets.UTF_8)

    private fun values(key: String) = Regex("\"$key\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(json).map { it.groupValues[1] }.toList()

    @Test
    fun everyNonCardioExerciseHasAnImageFile() {
        val names = values("name").toSet()
        val files = values("file")
        val expected = ExerciseCatalog.entries.filter { it.group != MuscleGroup.CARDIO }.map { it.name }
        expected.forEach { assertTrue("sem imagem: $it", it in names) }
        assertEquals("nomes fora do catálogo", emptySet<String>(), names - ExerciseCatalog.entries.map { it.name }.toSet())
        files.forEach { assertTrue("arquivo ausente: $it", File(dir, it).isFile) }
    }

    @Test
    fun everyCreditHasAuthorAndCreativeCommonsLicense() {
        assertTrue(values("author").isNotEmpty() && values("author").none { it.isBlank() })
        assertTrue(values("license").all { it.startsWith("CC") })
        assertTrue(values("source").all { it.startsWith("https://") })
    }
}
