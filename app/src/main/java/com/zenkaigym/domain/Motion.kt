package com.zenkaigym.domain

/** Regras puras de movimento (atrasos, direção, progresso), separadas do Compose para serem testáveis. */
object Motion {
    const val STAGGER_STEP_MS = 50
    const val MAX_STAGGER_STEPS = 6

    /** Atraso de entrada do [index]-ésimo item de uma lista; limitado para listas longas não demorarem. */
    fun staggerDelayMs(index: Int): Int = index.coerceIn(0, MAX_STAGGER_STEPS) * STAGGER_STEP_MS

    /** 1 ao ir para uma aba à direita, -1 para a esquerda e 0 se igual ou desconhecida. */
    fun tabDirection(order: List<String>, from: String?, to: String?): Int {
        val a = order.indexOf(from)
        val b = order.indexOf(to)
        return if (from == null || to == null || a < 0 || b < 0) 0 else (b - a).coerceIn(-1, 1)
    }

    /** Quanto de cada segmento de uma linha já foi desenhado: os segmentos aparecem em sequência. */
    fun segmentFractions(progress: Float, segments: Int): List<Float> {
        val drawn = progress.coerceIn(0f, 1f) * segments
        return List(segments) { (drawn - it).coerceIn(0f, 1f) }
    }
}
