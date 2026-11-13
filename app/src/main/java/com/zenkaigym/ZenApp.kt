package com.zenkaigym

import android.app.Application
import com.zenkaigym.data.AppDatabase
import com.zenkaigym.data.AppFlags
import com.zenkaigym.data.IntroPrefs
import com.zenkaigym.data.repo.ZenRepository
import com.zenkaigym.domain.I18n
import com.zenkaigym.domain.IntroGate
import com.zenkaigym.notify.ReminderScheduler
import com.zenkaigym.domain.RuleBasedEngine

class ZenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        I18n.select(appFlags.language)
        ReminderScheduler.createChannel(this)
    }

    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val repository: ZenRepository by lazy { ZenRepository(database, RuleBasedEngine()) }
    val appFlags: AppFlags by lazy { AppFlags(this) }
    val introGate: IntroGate by lazy { IntroGate(IntroPrefs(this)) }
}
