package com.zenkaigym

import android.app.Application
import com.zenkaigym.data.AppDatabase
import com.zenkaigym.data.AppFlags
import com.zenkaigym.data.IntroPrefs
import com.zenkaigym.data.repo.ZenRepository
import com.zenkaigym.domain.IntroGate
import com.zenkaigym.domain.RuleBasedEngine

class ZenApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.create(this) }
    val repository: ZenRepository by lazy { ZenRepository(database, RuleBasedEngine()) }
    val appFlags: AppFlags by lazy { AppFlags(this) }
    val introGate: IntroGate by lazy { IntroGate(IntroPrefs(this)) }
}
