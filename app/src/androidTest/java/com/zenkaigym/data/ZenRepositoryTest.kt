package com.zenkaigym.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zenkaigym.data.entity.Availability
import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.FoodCategory
import com.zenkaigym.data.entity.Goal
import com.zenkaigym.data.entity.Sex
import com.zenkaigym.data.entity.UserProfile
import com.zenkaigym.data.repo.ZenRepository
import com.zenkaigym.domain.NutritionTarget
import com.zenkaigym.domain.RuleBasedEngine
import com.zenkaigym.domain.TimeOfDay
import com.zenkaigym.domain.TimeWindow
import com.zenkaigym.domain.availabilityOf
import com.zenkaigym.domain.windowsByDay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZenRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: ZenRepository

    private val profile = UserProfile(heightCm = 178, weightKg = 82.5, age = 30, sex = Sex.MALE, goal = Goal.MAINTAIN)
    private val threeDays = (1..7).map { Availability(it, it in setOf(1, 3, 5)) }

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = ZenRepository(db, RuleBasedEngine())
        runBlocking { repo.ensureSeed() }
    }

    @After
    fun tearDown() = db.close()

    private fun saveBasics() = runBlocking {
        repo.saveProfile(profile, threeDays)
        repo.addFoodExamples()
    }

    // ---- Perfil e peso ----

    @Test
    fun seed_isIdempotent() = runBlocking {
        val before = db.workoutDao().exerciseCount()
        repo.ensureSeed()
        assertTrue(before > 0)
        assertEquals(before, db.workoutDao().exerciseCount())
    }

    @Test
    fun saveProfile_storesProfileAvailabilityAndFirstWeight() = runBlocking {
        repo.saveProfile(profile, threeDays)
        assertEquals(profile, repo.profile.first())
        assertEquals(listOf(1, 3, 5), repo.availability.first().filter { it.available }.map { it.dayOfWeek })
        assertEquals(listOf(82.5), repo.weights.first().map { it.weightKg })
    }

    @Test
    fun saveProfile_keepsAcceptedTargets_andDoesNotDuplicateUnchangedWeight() = runBlocking {
        repo.saveProfile(profile, threeDays)
        repo.acceptTarget(NutritionTarget(2780, 132, 390, 77))
        repo.saveProfile(profile.copy(age = 31), threeDays)
        val saved = repo.profile.first()!!
        assertEquals(31, saved.age)
        assertEquals(2780, saved.targetKcal)
        assertEquals(132, saved.targetProtein)
        assertEquals(1, repo.weights.first().size)
    }

    @Test
    fun addWeight_updatesProfileAndHistory() = runBlocking {
        repo.saveProfile(profile, threeDays)
        repo.addWeight(81.0)
        assertEquals(81.0, repo.profile.first()!!.weightKg, 0.0)
        assertEquals(listOf(82.5, 81.0), repo.weights.first().map { it.weightKg })
    }

    @Test
    fun addWeight_withoutProfile_doesNothing() = runBlocking {
        repo.addWeight(70.0)
        assertTrue(repo.weights.first().isEmpty())
    }

    // ---- Treino ----

    @Test
    fun generateWorkout_requiresProfileAndAvailability() = runBlocking {
        assertNotNull(repo.generateWorkoutProposal())
        repo.saveProfile(profile, (1..7).map { Availability(it, false) })
        assertNotNull(repo.generateWorkoutProposal())
        assertNull(repo.proposedWorkout.first())
    }

    @Test
    fun workout_proposalThenAccept_becomesActive() = runBlocking {
        saveBasics()
        assertNull(repo.generateWorkoutProposal())
        val proposal = repo.proposedWorkout.first()!!
        assertEquals(listOf(1, 3, 5), proposal.days.map { it.day.dayOfWeek }.sorted())
        assertTrue(proposal.days.all { it.items.isNotEmpty() })

        repo.acceptWorkout(proposal.plan.id)
        assertNull(repo.proposedWorkout.first())
        assertEquals(proposal.plan.id, repo.activeWorkout.first()!!.plan.id)
    }

    @Test
    fun workout_acceptingNewPlanReplacesActiveOne() = runBlocking {
        saveBasics()
        repo.generateWorkoutProposal()
        val first = repo.proposedWorkout.first()!!.plan.id
        repo.acceptWorkout(first)

        repo.generateWorkoutProposal()
        val second = repo.proposedWorkout.first()!!.plan.id
        repo.acceptWorkout(second)

        assertEquals(second, repo.activeWorkout.first()!!.plan.id)
        // O plano antigo (e seus dias/itens) foi removido em cascata.
        assertEquals(1, db.query("SELECT COUNT(*) FROM workout_plan", null).use { it.moveToFirst(); it.getInt(0) })
    }

    @Test
    fun workout_generatingAgainReplacesPendingProposal_andDiscardRemovesIt() = runBlocking {
        saveBasics()
        repo.generateWorkoutProposal()
        repo.generateWorkoutProposal()
        assertEquals(1, db.query("SELECT COUNT(*) FROM workout_plan", null).use { it.moveToFirst(); it.getInt(0) })
        repo.discardWorkoutProposal()
        assertNull(repo.proposedWorkout.first())
    }

    @Test
    fun workout_editing_clampsSets_andRemovesItems() = runBlocking {
        saveBasics()
        repo.generateWorkoutProposal()
        val item = repo.proposedWorkout.first()!!.days.first().items.first().item

        repo.setWorkoutSets(item.id, 99)
        assertEquals(10, repo.proposedWorkout.first()!!.days.first().items.first { it.item.id == item.id }.item.sets)
        repo.setWorkoutSets(item.id, 0)
        assertEquals(1, repo.proposedWorkout.first()!!.days.first().items.first { it.item.id == item.id }.item.sets)

        repo.removeWorkoutItem(item.id)
        assertTrue(repo.proposedWorkout.first()!!.days.first().items.none { it.item.id == item.id })
    }

    // ---- Dieta ----

    @Test
    fun generateDiet_requiresProfileAndFoods() = runBlocking {
        assertNotNull(repo.generateDietProposal())
        repo.saveProfile(profile, threeDays)
        assertNotNull(repo.generateDietProposal())
        assertNull(repo.proposedDiet.first())
    }

    @Test
    fun diet_usesSuggestedTarget_untilUserAcceptsOne() = runBlocking {
        saveBasics()
        repo.generateDietProposal()
        val suggested = repo.suggestTarget(profile, threeDays)
        assertEquals(suggested.kcal, repo.proposedDiet.first()!!.plan.kcal)

        repo.acceptTarget(NutritionTarget(2200, 150, 250, 61))
        repo.generateDietProposal()
        val plan = repo.proposedDiet.first()!!.plan
        assertEquals(2200, plan.kcal)
        assertEquals(150, plan.protein)
    }

    @Test
    fun diet_proposalThenAccept_replacesActive() = runBlocking {
        saveBasics()
        repo.generateDietProposal()
        val first = repo.proposedDiet.first()!!.plan.id
        repo.acceptDiet(first)
        assertEquals(first, repo.activeDiet.first()!!.plan.id)

        repo.generateDietProposal()
        val second = repo.proposedDiet.first()!!.plan.id
        repo.acceptDiet(second)
        assertEquals(second, repo.activeDiet.first()!!.plan.id)
        assertNull(repo.proposedDiet.first())
    }

    @Test
    fun diet_editing_clampsGrams_andRemovesItems() = runBlocking {
        saveBasics()
        repo.generateDietProposal()
        val item = repo.proposedDiet.first()!!.meals.first().items.first().item

        repo.setMealItemGrams(item.id, 1)
        assertEquals(5, repo.proposedDiet.first()!!.meals.first().items.first { it.item.id == item.id }.item.grams)
        repo.setMealItemGrams(item.id, 5000)
        assertEquals(1000, repo.proposedDiet.first()!!.meals.first().items.first { it.item.id == item.id }.item.grams)

        repo.removeMealItem(item.id)
        assertTrue(repo.proposedDiet.first()!!.meals.first().items.none { it.item.id == item.id })
    }

    @Test
    fun deletingFood_removesItFromExistingPlans() = runBlocking {
        saveBasics()
        repo.generateDietProposal()
        val foodId = repo.proposedDiet.first()!!.meals.first().items.first().food.id

        repo.deleteFood(foodId)

        val remaining = repo.proposedDiet.first()!!.meals.flatMap { it.items }
        assertTrue(remaining.none { it.food.id == foodId })
        assertTrue(repo.foods.first().none { it.id == foodId })
    }

    // ---- Disponibilidade ----

    @Test
    fun availability_persistsStartAndEndTimesPerDay() = runBlocking {
        val week = (1..7).map {
            availabilityOf(
                it,
                when (it) {
                    1 -> TimeWindow(TimeOfDay(6, 30), TimeOfDay(7, 45))
                    4 -> TimeWindow(TimeOfDay(19, 0), TimeOfDay(20, 30))
                    else -> null
                }
            )
        }
        repo.saveProfile(profile, week)

        val windows = repo.availability.first().windowsByDay()
        assertEquals(setOf(1, 4), windows.keys)
        assertEquals("06:30 – 07:45", windows.getValue(1).format())
        assertEquals("19:00 – 20:30", windows.getValue(4).format())
    }

    @Test
    fun availability_updatingADayReplacesItsWindow() = runBlocking {
        repo.saveProfile(profile, listOf(availabilityOf(2, TimeWindow(TimeOfDay(8, 0), TimeOfDay(9, 0)))))
        repo.saveProfile(profile, listOf(availabilityOf(2, TimeWindow(TimeOfDay(18, 0), TimeOfDay(19, 30)))))
        assertEquals("18:00 – 19:30", repo.availability.first().windowsByDay().getValue(2).format())
        assertEquals(1, repo.availability.first().size)
    }

    // ---- Alimentos ----

    @Test
    fun saveFood_insertsThenUpdates() = runBlocking {
        repo.saveFood(Food(name = "Tapioca", category = FoodCategory.CARB, kcalPer100 = 240.0, proteinPer100 = 0.0, carbsPer100 = 60.0, fatPer100 = 0.0))
        val saved = repo.foods.first().single()
        repo.saveFood(saved.copy(name = "Tapioca seca"))
        assertEquals(listOf("Tapioca seca"), repo.foods.first().map { it.name })
    }

    // ---- Ajustes só do dia ----

    @Test
    fun swap_valeSoNoDia_eVoltarAoOriginalDesfaz() = runBlocking {
        saveBasics()
        repo.generateWorkoutProposal()
        val item = repo.proposedWorkout.first()!!.days.first().items.first()
        val other = repo.exercises.first().first { it.group == item.exercise.group && it.id != item.exercise.id }

        repo.swapExerciseForDay(100, item.item.id, other.id)
        assertEquals(listOf(other.id), repo.swaps(100).first().map { it.exerciseId })
        assertTrue(repo.swaps(101).first().isEmpty())
        // A ficha continua com o exercício original.
        assertEquals(item.exercise.id, repo.proposedWorkout.first()!!.days.first().items.first { it.item.id == item.item.id }.exercise.id)

        repo.swapExerciseForDay(100, item.item.id, item.exercise.id)
        assertTrue(repo.swaps(100).first().isEmpty())
    }

    @Test
    fun extras_ficamNoDia_eSaoRemovidos() = runBlocking {
        val id = repo.addExtra(
            com.zenkaigym.data.entity.MealExtra(epochDay = 100, type = com.zenkaigym.data.entity.MealType.LUNCH, name = "Pizza", kcal = 800, protein = 30)
        )
        assertEquals(listOf("Pizza"), repo.extras(100).first().map { it.name })
        assertTrue(repo.extras(101).first().isEmpty())
        repo.removeExtra(id)
        assertTrue(repo.extras(100).first().isEmpty())
    }
}
