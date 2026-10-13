package com.zenkaigym.data

import android.content.Context
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

    /** Configuração inicial (tema + perfil) já concluída. */
    var setupDone: Boolean
        get() = prefs.getBoolean("setup_done", false)
        set(value) = prefs.edit().putBoolean("setup_done", value).apply()
}
