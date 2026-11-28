package com.zenkaigym.domain

import com.zenkaigym.data.entity.MuscleGroup
import com.zenkaigym.data.entity.MuscleGroup.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MuscleKanjiTest {
    @Test fun gruposDoMesmoKanjiAparecemUmaVezNaOrdem() {
        val row = listOf(QUADS, CHEST, GLUTES, LATS, CHEST, CARDIO).distinctByKanji()
        assertEquals(listOf("脚", "胸", "背", "走"), row.map { it.kanji() })
    }

    @Test fun todoGrupoTemUmDosSeteKanji() {
        MuscleGroup.entries.forEach { assertTrue(it.name, it.kanji() in setOf("胸", "背", "脚", "肩", "腕", "腹", "走")) }
    }
}
