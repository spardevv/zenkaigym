package com.zenkaigym.domain

/** Preferência de aparência: segue o sistema por padrão, ou força claro/escuro. */
enum class ThemeMode(val pt: String) {
    SYSTEM("Sistema"),
    LIGHT("Claro"),
    DARK("Escuro");

    val label: String get() = tr(pt)

    /** Escuro de fato, dado o modo escuro atual do sistema. */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
