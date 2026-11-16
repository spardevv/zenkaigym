package com.zenkaigym.domain

/**
 * Estilo visual do app: Shōnen (mangá de ação, o visual original), Kawaii (fofo, em tons pastel)
 * ou Isekai (interface de jogo: janelas de status com neon, cantos chanfrados e grade holográfica).
 */
enum class AppStyle(val pt: String) {
    SHONEN("Shōnen"),
    KAWAII("Kawaii"),
    ISEKAI("Isekai");

    val label: String get() = tr(pt)

    companion object {
        fun fromName(name: String?): AppStyle = entries.firstOrNull { it.name == name } ?: SHONEN
    }
}
