package com.zenkaigym.domain

/** Estilo visual do app: Shōnen (mangá de ação, o visual original) ou Kawaii (fofo, em tons pastel). */
enum class AppStyle(val pt: String) {
    SHONEN("Shōnen"),
    KAWAII("Kawaii");

    val label: String get() = tr(pt)

    companion object {
        fun fromName(name: String?): AppStyle = entries.firstOrNull { it.name == name } ?: SHONEN
    }
}
