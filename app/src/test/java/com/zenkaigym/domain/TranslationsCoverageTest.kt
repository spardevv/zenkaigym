package com.zenkaigym.domain

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Garante que todo texto literal passado a tr("…") no código tenha tradução em Translations.kt. */
class TranslationsCoverageTest {
    private val literal = Regex("""\btr\("([^"\\]*)"""")

    @Test
    fun todoTextoComTrTemTraducao() {
        val root = File("src/main/java") // os testes unitários rodam a partir do módulo app
        assertTrue("pasta de código não encontrada: ${root.absolutePath}", root.isDirectory)
        val missing = root.walk().filter { it.extension == "kt" && it.name != "Translations.kt" }
            .flatMap { f -> literal.findAll(f.readText()).map { f.name to it.groupValues[1] } }
            .filter { (_, pt) -> pt !in Translations.en }
            .map { (file, pt) -> "$file: \"$pt\"" }.distinct().toList()
        assertTrue("Textos sem tradução:\n" + missing.joinToString("\n"), missing.isEmpty())
    }
}
