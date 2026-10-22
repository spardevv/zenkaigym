package com.zenkaigym.data.repo

import com.zenkaigym.domain.tr
import androidx.room.withTransaction
import com.zenkaigym.data.AppDatabase
import com.zenkaigym.data.ExerciseCatalog
import com.zenkaigym.data.FoodExamples
import com.zenkaigym.data.entity.Availability
import com.zenkaigym.data.entity.DietPlan
import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.data.entity.Meal
import com.zenkaigym.data.entity.MealCheck
import com.zenkaigym.data.entity.MealExtra
import com.zenkaigym.data.entity.MealItem
import com.zenkaigym.data.entity.MealType
import com.zenkaigym.data.entity.MuscleGroup
import com.zenkaigym.data.entity.PlanStatus
import com.zenkaigym.data.entity.UserProfile
import com.zenkaigym.data.entity.WeightEntry
import com.zenkaigym.data.entity.WorkoutDay
import com.zenkaigym.data.entity.SetLog
import com.zenkaigym.data.entity.WorkoutItem
import com.zenkaigym.data.entity.WorkoutLog
import com.zenkaigym.data.entity.WorkoutPlan
import com.zenkaigym.data.entity.WorkoutSwap
import com.zenkaigym.domain.CheatDay
import com.zenkaigym.domain.CheatMeal
import com.zenkaigym.domain.MealSchedule
import com.zenkaigym.domain.WeeklyDietPlanner
import com.zenkaigym.domain.WeeklySuggestion
import com.zenkaigym.domain.NutritionCalculator
import com.zenkaigym.domain.NutritionTarget
import com.zenkaigym.domain.SuggestionEngine
import com.zenkaigym.domain.TimeWindow
import com.zenkaigym.domain.WeekStart
import com.zenkaigym.domain.WorkoutPlanner
import com.zenkaigym.domain.WorkoutRequest
import com.zenkaigym.util.todayEpochDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ZenRepository(
    private val db: AppDatabase,
    private val engine: SuggestionEngine
) {
    /** Primeiro dia da semana (Ajustes); define a ordem dos dias ao montar a ficha. */
    var weekStart: WeekStart = WeekStart.MONDAY

    private val profileDao = db.profileDao()
    private val foodDao = db.foodDao()
    private val workoutDao = db.workoutDao()
    private val dietDao = db.dietDao()

    val profile = profileDao.observeProfile()
    val weights = profileDao.observeWeights()
    val availability = profileDao.observeAvailability()
    val foods = foodDao.observeFoods()
    val exercises = workoutDao.observeExercises()
    val mealSlots = dietDao.observeSlots()
    fun mealChecks(epochDay: Long) = dietDao.observeChecks(epochDay)
    val proposedWorkout = workoutDao.observeByStatus(PlanStatus.PROPOSED)
    val activeWorkout = workoutDao.observeByStatus(PlanStatus.ACTIVE)
    val proposedDiet = dietDao.observeByStatus(PlanStatus.PROPOSED)
    val activeDiet = dietDao.observeByStatus(PlanStatus.ACTIVE)

    /** Dados iniciais: exercícios, agenda de refeições padrão e (uma única vez) alimentos de exemplo. */
    suspend fun ensureSeed(seedFoods: Boolean = false) {
        if (workoutDao.exerciseCount() == 0) workoutDao.insertExercises(ExerciseCatalog.all())
        if (dietDao.getSlots().isEmpty()) dietDao.insertSlots(MealSchedule.build(4))
        if (seedFoods && foodDao.getFoods().isEmpty()) foodDao.insertAll(FoodExamples.all())
        // Mantém cerca de um ano de refeições marcadas: alimentam o gráfico de aderência da dieta.
        dietDao.pruneChecksBefore(todayEpochDay() - HISTORY_DAYS)
        // Trocas valem só no próprio dia; os extras ficam até o fim da semana para o saldo semanal da dieta.
        workoutDao.pruneSwapsBefore(todayEpochDay())
        dietDao.pruneExtrasBefore(weekStart.weekStartOf(todayEpochDay()))
    }

    // ---- Progresso (gráficos do perfil) ----

    fun volumePerDay(from: Long) = workoutDao.observeVolumePerDay(from)
    fun finishedWorkoutDays(from: Long) = workoutDao.observeFinishedDays(from)
    fun mealsDonePerDay(from: Long) = dietDao.observeChecksPerDay(from)

    /** Apaga tudo o que o usuário registrou e recria só os dados iniciais (exercícios, agenda e alimentos). */
    suspend fun wipeAll() {
        withContext(Dispatchers.IO) { db.clearAllTables() }
        ensureSeed(seedFoods = true)
    }

    private companion object {
        const val HISTORY_DAYS = 400L
    }

    // ---- Perfil, peso e disponibilidade ----

    suspend fun saveProfile(profile: UserProfile, availability: List<Availability>? = null) {
        db.withTransaction {
            val old = profileDao.getProfile()
            profileDao.upsertProfile(
                profile.copy(
                    targetKcal = old?.targetKcal,
                    targetProtein = old?.targetProtein,
                    targetCarbs = old?.targetCarbs,
                    targetFat = old?.targetFat
                )
            )
            if (old == null || old.weightKg != profile.weightKg) {
                profileDao.insertWeight(WeightEntry(epochDay = todayEpochDay(), weightKg = profile.weightKg))
            }
            if (availability != null) profileDao.upsertAvailability(availability)
        }
    }

    suspend fun saveAvailability(availability: List<Availability>) = profileDao.upsertAvailability(availability)

    suspend fun addWeight(weightKg: Double) {
        db.withTransaction {
            val current = profileDao.getProfile() ?: return@withTransaction
            profileDao.upsertProfile(current.copy(weightKg = weightKg))
            profileDao.insertWeight(WeightEntry(epochDay = todayEpochDay(), weightKg = weightKg))
        }
    }

    suspend fun acceptTarget(target: NutritionTarget) {
        val current = profileDao.getProfile() ?: return
        profileDao.upsertProfile(
            current.copy(
                targetKcal = target.kcal, targetProtein = target.protein,
                targetCarbs = target.carbs, targetFat = target.fat
            )
        )
    }

    suspend fun suggestTarget(profile: UserProfile, availability: List<Availability>): NutritionTarget =
        engine.nutritionTarget(profile, availability.count { it.available })

    // ---- Reponderação automática ----
    // Quando o corpo, o peso, os dias de treino ou a meta mudam, o app recalcula a meta (se o usuário
    // ainda usa a sugerida) e monta novas sugestões de treino e dieta para revisão.

    private suspend fun availableDays() = profileDao.getAvailability().filter { it.available }.map { it.dayOfWeek }.toSet()

    private suspend fun <T> reweigh(refreshWorkout: Boolean = true, refreshDiet: Boolean = true, change: suspend () -> T): T {
        val oldProfile = profileDao.getProfile()
        val oldDays = availableDays()
        val oldTarget = oldProfile?.let { engine.nutritionTarget(it, oldDays.size) }
        val result = change()
        val profile = profileDao.getProfile() ?: return result
        val days = availableDays()
        val target = engine.nutritionTarget(profile, days.size)
        val usesSuggested = oldTarget != null && profile.targetKcal == oldTarget.kcal && profile.targetProtein == oldTarget.protein
        if (usesSuggested && target != oldTarget) acceptTarget(target)
        if (refreshWorkout && days.isNotEmpty()) generateWorkoutProposal()
        if (refreshDiet) generateDietProposal()
        return result
    }

    suspend fun updateProfile(profile: UserProfile) = reweigh { saveProfile(profile) }

    suspend fun registerWeight(kg: Double) = reweigh { addWeight(kg) }

    /** Salva a agenda de treino; só remonta treino e dieta se os dias disponíveis mudaram. Devolve true se remontou. */
    suspend fun updateAvailability(list: List<Availability>): Boolean {
        // Mudou o dia ou o horário (a duração define quanto treino cabe): remonta.
        fun key(l: List<Availability>) = l.filter { it.available }.map { Triple(it.dayOfWeek, it.startTime, it.endTime) }.toSet()
        val before = key(profileDao.getAvailability())
        val after = key(list)
        if (before == after) {
            saveAvailability(list)
            return false
        }
        reweigh { saveAvailability(list) }
        return true
    }

    /** Meta escolhida pelo usuário: refaz só a dieta. */
    suspend fun updateTarget(target: NutritionTarget) = reweigh(refreshWorkout = false) { acceptTarget(target) }

    suspend fun updateMealCount(count: Int) {
        setMealCount(count)
        generateDietProposal()
    }

    // ---- Alimentos ----

    suspend fun saveFood(food: Food) {
        if (food.id == 0L) foodDao.insert(food) else foodDao.update(food)
    }

    suspend fun deleteFood(id: Long) = foodDao.delete(id)

    suspend fun deleteAllFoods() = foodDao.deleteAll()

    /** Apaga todos os alimentos e volta para a lista sugerida; a dieta é remontada com ela. */
    suspend fun resetFoodsToSuggestion() {
        db.withTransaction {
            foodDao.deleteAll()
            foodDao.insertAll(FoodExamples.all())
        }
        generateDietProposal()
    }

    suspend fun addFoodExamples() = foodDao.insertAll(FoodExamples.all())

    // ---- Treino ----

    /** Retorna uma mensagem de erro, ou null se a sugestão foi gerada. */
    suspend fun generateWorkoutProposal(): String? {
        val profile = profileDao.getProfile() ?: return tr("Preencha seu perfil primeiro.")
        val availability = profileDao.getAvailability().filter { it.available }
        if (availability.isEmpty()) return tr("Marque ao menos um dia disponível no perfil.")
        // Cada dia entra com os minutos da sua janela de horário (sem janela, a duração padrão).
        val minutes = availability.associate {
            it.dayOfWeek to (TimeWindow.fromStored(it.startTime, it.endTime)?.durationMinutes ?: WorkoutPlanner.DEFAULT_MINUTES)
        }
        val request = WorkoutRequest(
            days = minutes, goal = profile.goal, level = profile.level, cardio = profile.cardio,
            focus = profile.focus, equipment = profile.equipment, weekStart = weekStart
        )
        val planned = engine.workoutPlan(request, workoutDao.exercises())
        db.withTransaction {
            workoutDao.deleteByStatus(PlanStatus.PROPOSED)
            val planId = workoutDao.insertPlan(
                WorkoutPlan(status = PlanStatus.PROPOSED, splitName = planned.splitName, createdAt = System.currentTimeMillis())
            )
            planned.days.forEach { day ->
                val dayId = workoutDao.insertDay(WorkoutDay(planId = planId, dayOfWeek = day.dayOfWeek, name = day.name))
                day.items.forEachIndexed { index, item ->
                    workoutDao.insertItem(
                        WorkoutItem(dayId = dayId, exerciseId = item.exerciseId, sets = item.sets, reps = item.reps, position = index)
                    )
                }
            }
        }
        return null
    }

    suspend fun acceptWorkout(planId: Long) {
        db.withTransaction {
            workoutDao.deleteByStatus(PlanStatus.ACTIVE)
            workoutDao.setStatus(planId, PlanStatus.ACTIVE)
        }
    }

    suspend fun discardWorkoutProposal() = workoutDao.deleteByStatus(PlanStatus.PROPOSED)

    suspend fun removeWorkoutItem(id: Long) = workoutDao.deleteItem(id)

    suspend fun setWorkoutSets(id: Long, sets: Int) = workoutDao.updateSets(id, sets.coerceIn(1, 10))

    suspend fun replaceExercise(itemId: Long, exerciseId: Long) = workoutDao.updateExercise(itemId, exerciseId)

    suspend fun addWorkoutItem(dayId: Long, exerciseId: Long) {
        db.withTransaction {
            val position = workoutDao.lastPosition(dayId) + 1
            workoutDao.insertItem(WorkoutItem(dayId = dayId, exerciseId = exerciseId, sets = 3, reps = "10-12", position = position))
        }
    }

    /** Cadastra um exercício próprio do usuário e devolve o id. */
    suspend fun addExercise(name: String, group: MuscleGroup): Long =
        workoutDao.insertExercise(Exercise(name = name.trim(), group = group))

    /** Gera uma sugestão em silêncio quando o usuário ainda não tem nenhuma ficha. */
    suspend fun ensureWorkoutSuggestion() {
        if (workoutDao.planCount() == 0) generateWorkoutProposal()
    }

    // ---- Dieta ----

    /** Meta efetiva: a aceita pelo usuário ou, na falta dela, a sugerida. */
    private suspend fun effectiveTarget(profile: UserProfile): NutritionTarget {
        val kcal = profile.targetKcal
        val protein = profile.targetProtein
        if (kcal != null && protein != null) {
            val carbs = profile.targetCarbs ?: NutritionCalculator.fromKcalAndProtein(kcal, protein).carbs
            val fat = profile.targetFat ?: NutritionCalculator.fromKcalAndProtein(kcal, protein).fat
            return NutritionTarget(kcal, protein, carbs, fat)
        }
        return engine.nutritionTarget(profile, profileDao.getAvailability().count { it.available })
    }

    /** Como montar o cardápio da semana: sugerido pelo app ou em branco, com ou sem dia do lixo. */
    data class WeekRequest(val manual: Boolean = false, val cheat: CheatDay? = null)

    /** Dia do lixo do plano mais recente, para que remontar a dieta não o perca. */
    private suspend fun currentCheat(): CheatDay? {
        val plan = dietDao.latestPlan() ?: return null
        val day = plan.cheatDay ?: return null
        val meals = dietDao.cheatMeals(plan.id).filter { it.dayOfWeek == day }
        if (meals.isEmpty()) return null
        return CheatDay(day, meals.map { CheatMeal(it.type, it.cheatKcal ?: 0) })
    }

    /**
     * Monta o cardápio da semana (segunda a domingo). Sem [request], mantém o dia do lixo atual.
     * Retorna uma mensagem (erro ou aviso) ou null se tudo certo.
     */
    suspend fun generateDietProposal(request: WeekRequest? = null): String? {
        val profile = profileDao.getProfile() ?: return tr("Preencha seu perfil primeiro.")
        val foods = foodDao.getFoods()
        if (foods.isEmpty() && request?.manual != true) return tr("Cadastre seus alimentos primeiro.")
        val target = effectiveTarget(profile)
        val slots = dietDao.getSlots().ifEmpty { MealSchedule.build(4) }
        val types = slots.map { it.type }.distinct().sortedBy { it.ordinal }
        val cheat = if (request != null) request.cheat else currentCheat()
        val week: WeeklySuggestion = if (request?.manual == true) {
            WeeklyDietPlanner.blank(target, types, cheat)
        } else {
            val suggestion = engine.weeklyDietPlan(target, foods, types, cheat)
            if (suggestion.days.all { day -> day.meals.all { it.items.isEmpty() } && day.cheat.isEmpty() }) {
                return suggestion.warnings.firstOrNull() ?: tr("Não foi possível montar a dieta.")
            }
            suggestion
        }
        val cheatDay = week.days.firstOrNull { it.cheat.isNotEmpty() }?.dayOfWeek
        db.withTransaction {
            dietDao.deleteByStatus(PlanStatus.PROPOSED)
            val planId = dietDao.insertPlan(
                DietPlan(
                    status = PlanStatus.PROPOSED, kcal = target.kcal, protein = target.protein,
                    carbs = target.carbs, fat = target.fat, createdAt = System.currentTimeMillis(), cheatDay = cheatDay
                )
            )
            week.days.forEach { day ->
                types.forEachIndexed { index, type ->
                    val cheatMeal = day.cheat.firstOrNull { it.type == type }
                    val planned = day.meals.firstOrNull { it.type == type }
                    if (cheatMeal != null) {
                        dietDao.insertMeal(
                            Meal(planId = planId, name = type.pt, position = index, type = type, dayOfWeek = day.dayOfWeek, cheatKcal = cheatMeal.kcal)
                        )
                    } else if (planned != null) {
                        val mealId = dietDao.insertMeal(
                            Meal(planId = planId, name = planned.name, position = index, type = type, dayOfWeek = day.dayOfWeek)
                        )
                        planned.items.forEach { dietDao.insertItem(MealItem(mealId = mealId, foodId = it.foodId, grams = it.grams)) }
                    }
                }
            }
        }
        return week.warnings.firstOrNull()
    }

    suspend fun acceptDiet(planId: Long) {
        db.withTransaction {
            dietDao.deleteByStatus(PlanStatus.ACTIVE)
            dietDao.setStatus(planId, PlanStatus.ACTIVE)
        }
    }

    suspend fun discardDietProposal() = dietDao.deleteByStatus(PlanStatus.PROPOSED)

    suspend fun removeMealItem(id: Long) = dietDao.deleteItem(id)

    suspend fun setMealItemGrams(id: Long, grams: Int) = dietDao.updateGrams(id, grams.coerceIn(5, 1000))

    suspend fun replaceMealItemFood(itemId: Long, foodId: Long, grams: Int) =
        dietDao.updateFood(itemId, foodId, grams.coerceIn(5, 1000))

    suspend fun addMealItem(mealId: Long, foodId: Long, grams: Int) {
        dietDao.insertItem(MealItem(mealId = mealId, foodId = foodId, grams = grams.coerceIn(5, 1000)))
    }

    /** Cadastra um alimento e devolve o id (para já usá-lo numa troca). */
    suspend fun addFood(food: Food): Long = foodDao.insert(food)

    suspend fun ensureDietSuggestion() {
        if (dietDao.planCount() == 0) generateDietProposal()
    }

    // ---- Agenda de refeições e check diário ----

    suspend fun setMealCount(count: Int) {
        db.withTransaction {
            val rebuilt = MealSchedule.build(count, dietDao.getSlots())
            dietDao.clearSlots()
            dietDao.insertSlots(rebuilt)
        }
    }

    suspend fun setMealTime(type: MealType, time: String) = dietDao.updateSlotTime(type, time)

    // ---- Registro de treino ----

    fun setsLogged(epochDay: Long) = workoutDao.observeSets(epochDay)
    fun workoutLog(epochDay: Long) = workoutDao.observeWorkoutLog(epochDay)

    suspend fun logSet(epochDay: Long, exerciseId: Long, setIndex: Int, reps: Int, weightKg: Double) =
        workoutDao.upsertSet(SetLog(epochDay = epochDay, exerciseId = exerciseId, setIndex = setIndex, reps = reps, weightKg = weightKg))

    suspend fun unlogSet(epochDay: Long, exerciseId: Long, setIndex: Int) = workoutDao.deleteSet(epochDay, exerciseId, setIndex)

    suspend fun lastSets(exerciseId: Long, beforeDay: Long) = workoutDao.lastSets(exerciseId, beforeDay)
    suspend fun bestWeight(exerciseId: Long, beforeDay: Long) = workoutDao.bestWeight(exerciseId, beforeDay)

    suspend fun setWorkoutDone(epochDay: Long, done: Boolean) {
        if (done) workoutDao.upsertWorkoutLog(WorkoutLog(epochDay)) else workoutDao.deleteWorkoutLog(epochDay)
    }

    /** Treino do dia marcado como não realizado (ou a marca desfeita). */
    suspend fun setWorkoutSkipped(epochDay: Long, skipped: Boolean) {
        if (skipped) workoutDao.upsertWorkoutLog(WorkoutLog(epochDay, finished = false)) else workoutDao.deleteWorkoutLog(epochDay)
    }

    /** Treinos marcados (feitos ou não realizados) a partir de [from]. */
    fun workoutLogsFrom(from: Long) = workoutDao.observeWorkoutLogs(from)

    /** [kcal] são as calorias que a refeição teve de fato (já com o ajuste do dia). */
    suspend fun setMealDone(type: MealType, epochDay: Long, done: Boolean, kcal: Int? = null) {
        if (done) dietDao.upsertCheck(MealCheck(type, epochDay, skipped = false, kcal = kcal)) else dietDao.deleteCheck(type, epochDay)
    }

    suspend fun setMealSkipped(type: MealType, epochDay: Long, skipped: Boolean) {
        if (skipped) dietDao.upsertCheck(MealCheck(type, epochDay, skipped = true)) else dietDao.deleteCheck(type, epochDay)
    }

    /** Refeições marcadas e pratos extras de [from] em diante: base do saldo da semana. */
    fun checksFrom(from: Long) = dietDao.observeChecksFrom(from)
    fun extrasFrom(from: Long) = dietDao.observeExtrasFrom(from)

    // ---- Ajustes só do dia ----

    fun swaps(epochDay: Long) = workoutDao.observeSwaps(epochDay)

    /** Troca o exercício do item só em [epochDay]; voltar ao exercício da ficha desfaz a troca. */
    suspend fun swapExerciseForDay(epochDay: Long, itemId: Long, exerciseId: Long) {
        db.withTransaction {
            if (workoutDao.itemExercise(itemId) == exerciseId) workoutDao.deleteSwap(epochDay, itemId)
            else workoutDao.upsertSwap(WorkoutSwap(epochDay, itemId, exerciseId))
        }
    }

    fun extras(epochDay: Long) = dietDao.observeExtras(epochDay)

    suspend fun addExtra(extra: MealExtra) = dietDao.insertExtra(extra)

    suspend fun removeExtra(id: Long) = dietDao.deleteExtra(id)
}
