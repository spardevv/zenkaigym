package com.zenkaigym.domain

import com.zenkaigym.data.entity.MuscleGroup

/** Kanji de cada grupo muscular: dá identidade a painéis, chips e ao cartão do treino. */
fun MuscleGroup.kanji(): String = when (this) {
    MuscleGroup.CHEST -> "胸"
    MuscleGroup.LATS, MuscleGroup.MID_BACK, MuscleGroup.TRAPS, MuscleGroup.LOWER_BACK -> "背"
    MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.ADDUCTORS, MuscleGroup.CALVES -> "脚"
    MuscleGroup.SHOULDERS_FRONT, MuscleGroup.SHOULDERS_SIDE, MuscleGroup.SHOULDERS_REAR -> "肩"
    MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.FOREARMS -> "腕"
    MuscleGroup.ABS, MuscleGroup.OBLIQUES -> "腹"
    MuscleGroup.CARDIO -> "走"
}

/** Primeiro grupo de cada kanji, na ordem recebida (grupos do mesmo kanji viram um só). */
fun List<MuscleGroup>.distinctByKanji(): List<MuscleGroup> = distinctBy { it.kanji() }
