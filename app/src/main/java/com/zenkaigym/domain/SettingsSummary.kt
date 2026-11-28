package com.zenkaigym.domain

/** Textos curtos que as linhas de Ajustes mostram à direita do título. */
object SettingsSummary {
    fun reminders(meals: Boolean, water: Boolean): String = when {
        meals && water -> tr("Refeições e água")
        meals -> tr("Refeições")
        water -> tr("Só água")
        else -> tr("Desligados")
    }

    fun languageCode(choice: AppLanguage): String = when (choice) {
        AppLanguage.PT -> "PT"
        AppLanguage.EN -> "EN"
        AppLanguage.JA -> "JA"
        AppLanguage.SYSTEM -> tr("Sistema")
    }
}
