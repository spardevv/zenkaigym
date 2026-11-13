package com.zenkaigym.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Idiomas de fato disponíveis no app. */
enum class Lang { PT, EN, JA }

/** Escolha do usuário em Ajustes: seguir o sistema ou fixar um idioma. */
enum class AppLanguage(val nativeName: String) {
    SYSTEM(""),
    PT("Português (Brasil)"),
    EN("English (US)"),
    JA("日本語");

    companion object {
        fun fromName(name: String?): AppLanguage = entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/**
 * Idioma atual. É um estado do Compose: qualquer texto montado com [tr] durante a composição
 * é refeito sozinho quando o idioma muda. Os textos do app são escritos em português e usados como chave.
 */
object I18n {
    private var current by mutableStateOf(Lang.PT)

    val lang: Lang get() = current

    /** Sistema em português ou japonês usa o respectivo idioma; qualquer outro cai em inglês. */
    fun resolve(choice: AppLanguage, systemLanguage: String): Lang = when (choice) {
        AppLanguage.PT -> Lang.PT
        AppLanguage.EN -> Lang.EN
        AppLanguage.JA -> Lang.JA
        AppLanguage.SYSTEM -> when (systemLanguage.lowercase().take(2)) {
            "pt" -> Lang.PT
            "ja" -> Lang.JA
            else -> Lang.EN
        }
    }

    fun select(choice: AppLanguage, systemLanguage: String = java.util.Locale.getDefault().language) {
        current = resolve(choice, systemLanguage)
    }
}

private val placeholder = Regex("""\{(\d+)\}""")

/**
 * Traduz um texto escrito em português. Valores variáveis entram como {0}, {1}... no texto.
 * Sem tradução cadastrada, devolve o próprio texto em português.
 */
fun tr(pt: String, vararg args: Any?): String {
    val text = when (I18n.lang) {
        Lang.PT -> pt
        Lang.EN -> Translations.en[pt] ?: pt
        Lang.JA -> Translations.ja[pt] ?: pt
    }
    if (args.isEmpty()) return text
    return placeholder.replace(text) { m -> args.getOrNull(m.groupValues[1].toInt())?.toString() ?: m.value }
}
