package com.zenkaigym.ui

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.zenkaigym.data.AppDatabase
import com.zenkaigym.data.AppFlags
import com.zenkaigym.data.IntroPrefs
import com.zenkaigym.data.entity.Availability
import com.zenkaigym.data.entity.Goal
import com.zenkaigym.data.entity.SetLog
import com.zenkaigym.data.entity.Sex
import com.zenkaigym.data.entity.UserProfile
import com.zenkaigym.data.repo.ZenRepository
import com.zenkaigym.domain.AppLanguage
import com.zenkaigym.domain.I18n
import com.zenkaigym.domain.IntroGate
import com.zenkaigym.domain.RuleBasedEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Ambiente isolado para testes de interface: banco em memória, preferências zeradas e idioma português.
 * Cria o [ZenViewModel] real, então os testes exercitam a tela junto com a lógica e o banco.
 */
class TestApp private constructor(private val context: Context) {
    val db: AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    val repo = ZenRepository(db, RuleBasedEngine())
    val flags = AppFlags(context)
    val viewModel: ZenViewModel

    init {
        runBlocking { repo.ensureSeed() }
        viewModel = ZenViewModel(repo, IntroGate(IntroPrefs(context)), flags)
    }

    /** Perfil salvo + alimentos de exemplo, como depois do primeiro uso. */
    fun withProfile(): TestApp = apply {
        runBlocking {
            repo.saveProfile(
                UserProfile(heightCm = 178, weightKg = 82.5, age = 30, sex = Sex.MALE, goal = Goal.MAINTAIN),
                (1..7).map { Availability(it, it in setOf(1, 3, 5), "18:00", "19:00") }
            )
            repo.addFoodExamples()
        }
        flags.setupDone = true
    }

    /** Perfil com treino ativo nos 7 dias da semana: assim há sessão hoje, qualquer que seja o dia. */
    fun withWorkoutEveryDay(): TestApp = withProfile().apply {
        runBlocking {
            repo.saveAvailability((1..7).map { Availability(it, true, "18:00", "19:00") })
            repo.generateWorkoutProposal()
            repo.acceptWorkout(repo.proposedWorkout.first()!!.plan.id)
        }
    }

    /** Perfil com dieta ativa (a sugestão aceita): assim o Dojo mostra as refeições do dia. */
    fun withDiet(): TestApp = withProfile().apply {
        runBlocking {
            repo.ensureDietSuggestion()
            val proposal = checkNotNull(repo.proposedDiet.first()) { "ensureDietSuggestion não gerou uma dieta" }
            repo.acceptDiet(proposal.plan.id)
        }
    }

    /** Treino ativo + uma série de trabalho registrada hoje no primeiro exercício. Devolve o nome dele. */
    fun withLoggedSet(): TestApp = withWorkoutEveryDay().apply {
        runBlocking {
            val first = repo.exercises.first().first()
            loggedExerciseName = first.name
            repo.logSet(SetLog(epochDay = viewModel.clock.value.epochDay, exerciseId = first.id, setIndex = 1, reps = 10, weightKg = 20.0))
        }
    }

    /** Nome do exercício registrado por [withLoggedSet]. */
    var loggedExerciseName: String = ""
        private set

    fun close() {
        db.close()
        I18n.select(AppLanguage.PT, "pt")
    }

    companion object {
        /** Cria o ambiente na thread principal (o ViewModel precisa dela) com as preferências limpas. */
        fun create(): TestApp {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            listOf("zenkaigym_intro", "zenkaigym_flags").forEach {
                context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
            }
            I18n.select(AppLanguage.PT, "pt")
            var app: TestApp? = null
            InstrumentationRegistry.getInstrumentation().runOnMainSync { app = TestApp(context) }
            return app!!
        }
    }
}
