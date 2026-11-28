package com.zenkaigym.ui.components

import androidx.compose.ui.graphics.Color
import com.zenkaigym.data.entity.MuscleGroup
import com.zenkaigym.ui.theme.Aka
import com.zenkaigym.ui.theme.Ao
import com.zenkaigym.ui.theme.Daidai
import com.zenkaigym.ui.theme.Ki
import com.zenkaigym.ui.theme.Midori
import com.zenkaigym.ui.theme.Sakura

/** Cor de cada grupo muscular (o kanji vem do domínio): dá identidade a cada painel do treino. */
fun MuscleGroup.color(): Color = when (this) {
    MuscleGroup.CHEST -> Aka
    MuscleGroup.LATS, MuscleGroup.MID_BACK, MuscleGroup.TRAPS, MuscleGroup.LOWER_BACK -> Ao
    MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.ADDUCTORS, MuscleGroup.CALVES -> Ki
    MuscleGroup.SHOULDERS_FRONT, MuscleGroup.SHOULDERS_SIDE, MuscleGroup.SHOULDERS_REAR -> Daidai
    MuscleGroup.BICEPS, MuscleGroup.TRICEPS, MuscleGroup.FOREARMS -> Sakura
    MuscleGroup.ABS, MuscleGroup.OBLIQUES, MuscleGroup.CARDIO -> Midori
}
