package com.zenkaigym.data

import android.content.Context
import com.zenkaigym.domain.AppLanguage
import com.zenkaigym.domain.IntroStore
import com.zenkaigym.domain.ThemeMode

class IntroPrefs(context: Context) : IntroStore {
    private val prefs = context.getSharedPreferences("zenkaigym_intro", Context.MODE_PRIVATE)

    override fun isSeen() = prefs.getBoolean(KEY_SEEN, false)
    override fun setSeen() = prefs.edit().putBoolean(KEY_SEEN, true).apply()

    private companion object {
        const val KEY_SEEN = "intro_seen"
    }
}

/** Marcas de primeira execução (ex.: alimentos iniciais já inseridos). */
class AppFlags(context: Context) {
    private val prefs = context.getSharedPreferences("zenkaigym_flags", Context.MODE_PRIVATE)

    var foodsSeeded: Boolean
        get() = prefs.getBoolean("foods_seeded", false)
        set(value) = prefs.edit().putBoolean("foods_seeded", value).apply()

    var themeMode: ThemeMode
        get() = ThemeMode.fromName(prefs.getString("theme_mode", null))
        set(value) = prefs.edit().putString("theme_mode", value.name).apply()

    /** Idioma escolhido em Ajustes; padrão: seguir o sistema. */
    var language: AppLanguage
        get() = AppLanguage.fromName(prefs.getString("language", null))
        set(value) = prefs.edit().putString("language", value.name).apply()

    /** Dia (epochDay) com o modo treino aberto; -1 = nenhum. Faz o app reabrir direto no treino em andamento. */
    var trainingDay: Long
        get() = prefs.getLong("training_day", -1L)
        set(value) = prefs.edit().putLong("training_day", value).apply()

    /** Manter a tela acesa no modo treino. */
    var keepScreenOn: Boolean
        get() = prefs.getBoolean("keep_screen_on", true)
        set(value) = prefs.edit().putBoolean("keep_screen_on", value).apply()

    /** Configuração inicial (tema + perfil) já concluída. */
    var setupDone: Boolean
        get() = prefs.getBoolean("setup_done", false)
        set(value) = prefs.edit().putBoolean("setup_done", value).apply()
}
