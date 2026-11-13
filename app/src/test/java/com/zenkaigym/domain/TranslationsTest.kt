package com.zenkaigym.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Garante que nenhum texto fique sem tradução ou com marcadores {0}, {1}... perdidos. */
class TranslationsTest {
    private val placeholder = Regex("""\{\d+\}""")

    @Test
    fun inglesEJaponesTemAsMesmasChaves() {
        assertTrue(Translations.en.isNotEmpty())
        assertEquals(Translations.en.keys, Translations.ja.keys)
    }

    @Test
    fun nenhumaTraducaoEstaVazia() {
        listOf("en" to Translations.en, "ja" to Translations.ja).forEach { (name, map) ->
            map.forEach { (pt, text) -> assertTrue("[$name] tradução vazia para '$pt'", text.isNotBlank()) }
        }
    }

    @Test
    fun marcadoresSaoPreservadosNasTraducoes() {
        listOf("en" to Translations.en, "ja" to Translations.ja).forEach { (name, map) ->
            map.forEach { (pt, text) ->
                assertEquals(
                    "[$name] marcadores diferentes em '$pt'",
                    placeholder.findAll(pt).map { it.value }.toSortedSet(),
                    placeholder.findAll(text).map { it.value }.toSortedSet()
                )
            }
        }
    }
}
