package com.zenkaigym.ui

import com.zenkaigym.domain.tr
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenkaigym.data.AppFlags
import com.zenkaigym.data.entity.Availability
import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.MealCheck
import com.zenkaigym.data.entity.MealExtra
import com.zenkaigym.data.entity.MealType
import com.zenkaigym.data.entity.MuscleGroup
import com.zenkaigym.data.entity.SetLog
import com.zenkaigym.data.entity.WorkoutLog
import com.zenkaigym.domain.DietProgress
import com.zenkaigym.domain.DoneSet
import com.zenkaigym.domain.Stats
import com.zenkaigym.domain.TrainingProgress
import com.zenkaigym.domain.Progression
import com.zenkaigym.data.entity.UserProfile
import com.zenkaigym.data.repo.ZenRepository
import com.zenkaigym.domain.AppLanguage
import com.zenkaigym.domain.AppStyle
import com.zenkaigym.domain.CheatDay
import com.zenkaigym.domain.DietPlanner
import com.zenkaigym.domain.I18n
import com.zenkaigym.domain.IntroGate
import com.zenkaigym.domain.NutritionCalculator
import com.zenkaigym.domain.NutritionTarget
import com.zenkaigym.domain.ThemeMode
import com.zenkaigym.domain.WeekStart
import com.zenkaigym.domain.WorkoutWeek
import com.zenkaigym.util.Clock
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** O que se sabe de um exercício antes de hoje. */
data class ExerciseHistory(val last: List<DoneSet>, val bestKg: Double)

@OptIn(ExperimentalCoroutinesApi::class)
class ZenViewModel(
    private val repo: ZenRepository,
    private val introGate: IntroGate,
    private val flags: AppFlags
) : ViewModel() {
    private val _showIntro = MutableStateFlow(introGate.shouldShow())
    val showIntro: StateFlow<Boolean> = _showIntro

    fun finishIntro() {
        introGate.markSeen()
        _showIntro.value = false
    }

    fun replayIntro() {
        _showIntro.value = true
    }

    // ---- Tema e configuração inicial ----

    private val _themeMode = MutableStateFlow(flags.themeMode)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    fun setThemeMode(mode: ThemeMode) {
        flags.themeMode = mode
        _themeMode.value = mode
    }

    private val _style = MutableStateFlow(flags.style)
    val style: StateFlow<AppStyle> = _style

    fun setStyle(style: AppStyle) {
        flags.style = style
        _style.value = style
    }

    private val _weekStart = MutableStateFlow(flags.weekStart.also { repo.weekStart = it })
    val weekStart: StateFlow<WeekStart> = _weekStart

    fun setWeekStart(start: WeekStart) {
        flags.weekStart = start
        repo.weekStart = start
        _weekStart.value = start
    }

    private val _language = MutableStateFlow(flags.language)
    val language: StateFlow<AppLanguage> = _language

    fun setLanguage(choice: AppLanguage) {
        flags.language = choice
        _language.value = choice
        I18n.select(choice)
    }

    private val _showSetup = MutableStateFlow(!flags.setupDone)
    val showSetup: StateFlow<Boolean> = _showSetup

    /** Passo do treino na configuração inicial: salva perfil e agenda e monta as sugestões de treino e dieta. */
    fun saveSetupProfile(profile: UserProfile, availability: List<Availability>) = launchSafe {
        working(tr("Montando seu treino e sua dieta…")) { repo.updateProfile(profile, availability) }
    }

    /** Último passo: ativa a dieta revisada e abre o Dojo com treino e dieta já definidos. */
    fun acceptDietAndFinish(planId: Long) = launchSafe {
        repo.acceptDiet(planId)
        flags.setupDone = true
        _showSetup.value = false
        toast(tr("Tudo pronto! Seu treino e sua dieta já estão ativos."))
    }

    // ---- Carregamento inicial ----
    // Os fluxos principais ficam ativos enquanto o processo vive (o banco é pequeno), então a troca de
    // abas é instantânea. `ready` só vira true quando todos já entregaram o primeiro valor: a tela de
    // carregamento aparece apenas na abertura a frio, nunca ao voltar do segundo plano.

    private val pending = MutableStateFlow(0)

    private fun <T> Flow<T>.eager(initial: T): StateFlow<T> {
        pending.update { it + 1 }
        var first = true
        return onEach {
            if (first) {
                first = false
                pending.update { n -> n - 1 }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, initial)
    }

    val profile = repo.profile.eager(null)
    val weights = repo.weights.eager(emptyList())
    val availability = repo.availability.eager(emptyList())
    val foods = repo.foods.eager(emptyList())
    val exercises = repo.exercises.eager(emptyList())
    val mealSlots = repo.mealSlots.eager(emptyList())
    val proposedWorkout = repo.proposedWorkout.eager(null)
    val activeWorkout = repo.activeWorkout.eager(null)
    val proposedDiet = repo.proposedDiet.eager(null)
    val activeDiet = repo.activeDiet.eager(null)

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    /** Dia e hora atuais, renovados a cada 30 s enquanto alguma tela os observa. */
    val clock: StateFlow<Clock> = flow {
        while (true) {
            emit(Clock.now())
            delay(30_000)
        }
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Clock.now())

    /** Refeições marcadas hoje (feitas ou não realizadas). */
    val checksToday: StateFlow<List<MealCheck>> = clock.map { it.epochDay }.distinctUntilChanged()
        .flatMapLatest { repo.mealChecks(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Refeições já marcadas como feitas hoje. */
    val mealsDone: StateFlow<Set<MealType>> = checksToday
        .map { list -> list.filter { !it.skipped }.map { it.type }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Refeições marcadas como não realizadas hoje. */
    val mealsSkipped: StateFlow<Set<MealType>> = checksToday
        .map { list -> list.filter { it.skipped }.map { it.type }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Calorias gravadas nas refeições feitas hoje (já com o ajuste do dia). */
    val mealsRecorded: StateFlow<Map<MealType, Int>> = checksToday
        .map { list -> list.filter { !it.skipped && it.kcal != null }.associate { it.type to it.kcal!! } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Séries já registradas hoje. */
    val setsToday: StateFlow<List<SetLog>> = clock.map { it.epochDay }.distinctUntilChanged()
        .flatMapLatest { repo.setsLogged(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Exercícios trocados só para hoje (a ficha continua igual). */
    val swapsToday = clock.map { it.epochDay }.distinctUntilChanged()
        .flatMapLatest { repo.swaps(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Pratos comidos fora do cardápio hoje. */
    val extrasToday: StateFlow<List<MealExtra>> = clock.map { it.epochDay }.distinctUntilChanged()
        .flatMapLatest { repo.extras(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Treino de hoje marcado como não realizado. */
    val workoutSkipped: StateFlow<Boolean> = clock.map { it.epochDay }.distinctUntilChanged()
        .flatMapLatest { repo.workoutLog(it) }
        .map { it?.finished == false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Treino de hoje marcado como concluído. */
    val workoutDone: StateFlow<Boolean> = clock.map { it.epochDay }.distinctUntilChanged()
        .flatMapLatest { repo.workoutLog(it) }
        .map { it?.finished == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // ---- Modo treino (tela cheia) ----
    // Fica salvo em AppFlags: ao desbloquear o celular ou reabrir o app, o treino de hoje continua na tela.

    private val trainingDay = MutableStateFlow(flags.trainingDay)

    /** Modo treino aberto hoje (vale só no mesmo dia: no dia seguinte o app volta ao normal). */
    val trainingActive: StateFlow<Boolean> = combine(trainingDay, clock) { day, now -> day == now.epochDay }
        .stateIn(viewModelScope, SharingStarted.Eagerly, flags.trainingDay == Clock.now().epochDay)

    fun startTraining() {
        val today = clock.value.epochDay
        flags.trainingDay = today
        trainingDay.value = today
    }

    fun stopTraining() {
        flags.trainingDay = -1L
        trainingDay.value = -1L
        stopRest()
    }

    private val _keepScreenOn = MutableStateFlow(flags.keepScreenOn)
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn

    fun toggleKeepScreenOn() {
        val value = !_keepScreenOn.value
        flags.keepScreenOn = value
        _keepScreenOn.value = value
    }

    /** Descanso em andamento: instante (System.currentTimeMillis) em que termina e a duração total. */
    data class Rest(val endsAt: Long, val totalMs: Long)

    private val _rest = MutableStateFlow<Rest?>(null)
    val rest: StateFlow<Rest?> = _rest

    fun startRest(seconds: Int) {
        val total = seconds * 1000L
        _rest.value = Rest(System.currentTimeMillis() + total, total)
    }

    fun stopRest() {
        _rest.value = null
    }

    // ---- Progresso (gráficos do perfil) ----

    private val today = clock.map { it.epochDay }.distinctUntilChanged()

    val trainingProgress: StateFlow<TrainingProgress?> = combine(today, _weekStart) { day, start -> day to start }.flatMapLatest { (day, start) ->
        // Vai até 30 dias atrás também, para o total de treinos do mês.
        val from = minOf(Stats.firstDay(day, start), day - 29)
        combine(repo.volumePerDay(from), repo.finishedWorkoutDays(from)) { volume, finished ->
            Stats.training(
                volumePerDay = volume.associate { it.epochDay to it.volume },
                sessionDays = volume.filter { it.sets > 0 }.map { it.epochDay }.toSet() + finished,
                today = day,
                weekStart = start
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ---- Semana corrente (reorganiza o treino e o saldo da dieta) ----

    private val weekFrom = combine(today, _weekStart) { day, start -> start.weekStartOf(day) }.distinctUntilChanged()

    /** Treinos marcados (feitos ou não realizados) desde o início da semana. */
    val weekWorkoutLogs: StateFlow<List<WorkoutLog>> = weekFrom.flatMapLatest { repo.workoutLogsFrom(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Qual sessão da ficha cai hoje, já reorganizada pelos treinos não realizados da semana. */
    val todayWorkout: StateFlow<WorkoutWeek.Assignment> =
        combine(activeWorkout, clock, _weekStart, weekWorkoutLogs) { plan, now, start, logs ->
            WorkoutWeek.assign(plan?.days.orEmpty(), now.epochDay, start, logs)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, WorkoutWeek.Assignment.REST)

    val weekChecks: StateFlow<List<MealCheck>> = weekFrom.flatMapLatest { repo.checksFrom(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weekExtras: StateFlow<List<MealExtra>> = weekFrom.flatMapLatest { repo.extrasFrom(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dietProgress: StateFlow<DietProgress?> = today.flatMapLatest { day ->
        combine(repo.mealsDonePerDay(day - Stats.DIET_DAYS), repo.mealSlots) { done, slots ->
            Stats.diet(done.associate { it.epochDay to it.count }, slots.size, day)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ---- Apagar todos os dados ----

    /** Apaga perfil, fichas, dietas, alimentos e histórico; mantém só tema e idioma. Volta à configuração inicial. */
    fun wipeAllData() = launchSafe {
        stopTraining()
        working(tr("Apagando seus dados…")) { repo.wipeAll() }
        flags.foodsSeeded = true // a lista inicial de alimentos já foi recriada
        flags.setupDone = false
        _showSetup.value = true
        toast(tr("Todos os dados foram apagados."))
    }

    /** Meta sugerida a partir do perfil e dos dias de treino salvos. */
    val suggestedTarget: StateFlow<NutritionTarget?> =
        combine(repo.profile, repo.availability) { profile, availability ->
            profile?.let { repo.suggestTarget(it, availability) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages

    /** Texto do que está sendo salvo/recalculado agora; null quando ocioso. Alimenta o indicador de carregamento. */
    private val _busy = MutableStateFlow<String?>(null)
    val busy: StateFlow<String?> = _busy

    /** Falha numa operação em segundo plano vira aviso na tela em vez de derrubar o app. */
    private val failureHandler = CoroutineExceptionHandler { _, error ->
        Log.e("ZenViewModel", "Falha em operação em segundo plano", error)
        _busy.value = null
        toast(tr("Algo deu errado. Tente de novo."))
    }

    private fun launchSafe(block: suspend CoroutineScope.() -> Unit): Job =
        viewModelScope.launch(failureHandler, block = block)

    init {
        launchSafe {
            try {
                val started = System.currentTimeMillis()
                repo.ensureSeed(seedFoods = !flags.foodsSeeded)
                flags.foodsSeeded = true
                pending.first { it == 0 }
                // Quem já tinha perfil antes desta versão não precisa refazer a configuração inicial.
                if (profile.value != null && !flags.setupDone) {
                    flags.setupDone = true
                    _showSetup.value = false
                }
                val remaining = MIN_LOADING_MS - (System.currentTimeMillis() - started)
                if (remaining > 0) delay(remaining)
            } finally {
                _ready.value = true // mesmo com falha, não deixa o app preso na tela de carregamento
            }
        }
    }

    private fun toast(text: String?) {
        if (text != null) _messages.tryEmit(text)
    }


    /** Mostra o carregamento por um tempo mínimo, para o usuário perceber que algo aconteceu. */
    private suspend fun <T> working(label: String, block: suspend () -> T): T {
        val started = System.currentTimeMillis()
        _busy.value = label
        try {
            return block()
        } finally {
            val remaining = MIN_BUSY_MS - (System.currentTimeMillis() - started)
            if (remaining > 0) delay(remaining)
            _busy.value = null
        }
    }

    // ---- Perfil, peso e metas ----

    fun saveProfile(profile: UserProfile) = launchSafe {
        working(tr("Atualizando dieta e treino…")) { repo.updateProfile(profile) }
        toast(tr("Perfil salvo! Dieta e treino foram reponderados — revise as sugestões."))
    }

    fun saveAvailability(list: List<Availability>) = launchSafe {
        val changed = working(tr("Salvando agenda…")) { repo.updateAvailability(list) }
        toast(if (changed) tr("Agenda salva! Treino e dieta reponderados — revise as sugestões.") else tr("Agenda salva."))
    }

    fun addWeight(kg: Double) = launchSafe {
        working(tr("Registrando peso…")) { repo.registerWeight(kg) }
        toast(tr("Peso registrado! Dieta e treino reponderados."))
    }

    fun acceptTarget(target: NutritionTarget) = launchSafe {
        working(tr("Aplicando meta…")) { repo.updateTarget(target) }
        toast(tr("Meta salva! A dieta foi ajustada a ela."))
    }

    fun acceptCustomTarget(kcal: Int, protein: Int) =
        acceptTarget(NutritionCalculator.fromKcalAndProtein(kcal, protein))

    // ---- Alimentos ----

    fun saveFood(food: Food) = launchSafe {
        working(tr("Salvando alimento…")) { repo.saveFood(food) }
        toast(tr("Alimento salvo."))
    }
    fun deleteFood(id: Long) = launchSafe {
        working(tr("Removendo alimento…")) { repo.deleteFood(id) }
        toast(tr("Alimento removido."))
    }
    fun deleteAllFoods() = launchSafe {
        working(tr("Removendo alimentos…")) { repo.deleteAllFoods() }
        toast(tr("Todos os alimentos foram removidos."))
    }
    fun resetFoods() = launchSafe {
        working(tr("Refazendo lista e dieta…")) { repo.resetFoodsToSuggestion() }
        toast(tr("Lista sugerida restaurada! Revise a nova dieta."))
    }
    fun addFoodExamples() = launchSafe {
        working(tr("Adicionando alimentos…")) { repo.addFoodExamples() }
        toast(tr("Lista pronta adicionada."))
    }

    // ---- Treino ----

    fun generateWorkout() = launchSafe {
        toast(working(tr("Montando treino…")) { repo.generateWorkoutProposal() } ?: tr("Nova sugestão de treino pronta."))
    }
    fun ensureWorkoutSuggestion() = launchSafe { repo.ensureWorkoutSuggestion() }
    fun acceptWorkout(planId: Long) = launchSafe {
        repo.acceptWorkout(planId)
        toast(tr("Ficha de treino ativada!"))
    }
    fun discardWorkout() = launchSafe { repo.discardWorkoutProposal() }
    fun removeWorkoutItem(id: Long) = launchSafe { repo.removeWorkoutItem(id) }
    fun setWorkoutSets(id: Long, sets: Int) = launchSafe { repo.setWorkoutSets(id, sets) }
    fun replaceExercise(itemId: Long, exercise: Exercise) = launchSafe { repo.replaceExercise(itemId, exercise.id) }
    fun addWorkoutItem(dayId: Long, exercise: Exercise) = launchSafe { repo.addWorkoutItem(dayId, exercise.id) }

    /** Última vez e melhor carga anteriores a hoje, para pré-preencher e sugerir a progressão. */
    suspend fun history(exerciseId: Long): ExerciseHistory {
        val today = clock.value.epochDay
        return ExerciseHistory(
            last = repo.lastSets(exerciseId, today).map { DoneSet(it.reps, it.weightKg) },
            bestKg = repo.bestWeight(exerciseId, today)
        )
    }

    fun logSet(exerciseId: Long, setIndex: Int, reps: Int, weightKg: Double) = launchSafe {
        val today = clock.value.epochDay
        val best = repo.bestWeight(exerciseId, today)
        repo.logSet(today, exerciseId, setIndex, reps, weightKg)
        if (Progression.isPersonalRecord(weightKg, best) && best > 0) toast(tr("Novo recorde! {0} kg", weightKg))
    }

    fun unlogSet(exerciseId: Long, setIndex: Int) = launchSafe {
        repo.unlogSet(clock.value.epochDay, exerciseId, setIndex)
    }

    fun setWorkoutDone(done: Boolean) = launchSafe {
        repo.setWorkoutDone(clock.value.epochDay, done)
        if (done) {
            stopTraining() // também zera o descanso
            toast(tr("Treino concluído! Bom trabalho."))
        }
    }

    /** Marca o treino de hoje como não realizado: a sessão passa para o próximo dia de treino. */
    fun setWorkoutSkipped(skipped: Boolean) = launchSafe {
        repo.setWorkoutSkipped(clock.value.epochDay, skipped)
        if (skipped) toast(tr("Anotado! A semana de treino foi reorganizada."))
    }

    fun createExerciseAndReplace(name: String, group: MuscleGroup, itemId: Long) = launchSafe {
        repo.replaceExercise(itemId, repo.addExercise(name, group))
    }

    /** Troca o exercício só no treino de hoje; escolher o da ficha desfaz a troca. */
    fun swapExerciseToday(itemId: Long, exercise: Exercise) = launchSafe {
        repo.swapExerciseForDay(clock.value.epochDay, itemId, exercise.id)
        toast(tr("Trocado só por hoje: {0}.", tr(exercise.name)))
    }

    fun createExerciseAndSwapToday(name: String, group: MuscleGroup, itemId: Long) = launchSafe {
        repo.swapExerciseForDay(clock.value.epochDay, itemId, repo.addExercise(name, group))
        toast(tr("Trocado só por hoje: {0}.", name))
    }

    fun createExerciseAndAdd(name: String, group: MuscleGroup, dayId: Long) = launchSafe {
        repo.addWorkoutItem(dayId, repo.addExercise(name, group))
    }

    // ---- Dieta ----

    fun generateDiet() = launchSafe {
        toast(working(tr("Montando dieta…")) { repo.generateDietProposal() } ?: tr("Nova sugestão de dieta pronta."))
    }
    /** Cria o cardápio da semana, sugerido ou em branco, com o dia do lixo escolhido (se houver). */
    fun createWeek(manual: Boolean, cheat: CheatDay?) = launchSafe {
        val label = if (manual) tr("Preparando cardápio…") else tr("Montando cardápio da semana…")
        val error = working(label) { repo.generateDietProposal(ZenRepository.WeekRequest(manual, cheat)) }
        toast(error ?: if (manual) tr("Cardápio em branco pronto — adicione os alimentos de cada dia.") else tr("Cardápio da semana pronto — revise e aceite."))
    }
    fun ensureDietSuggestion() = launchSafe { repo.ensureDietSuggestion() }
    fun acceptDiet(planId: Long) = launchSafe {
        repo.acceptDiet(planId)
        toast(tr("Dieta ativada!"))
    }
    fun discardDiet() = launchSafe { repo.discardDietProposal() }
    fun removeMealItem(id: Long) = launchSafe { repo.removeMealItem(id) }
    fun setMealItemGrams(id: Long, grams: Int) = launchSafe { repo.setMealItemGrams(id, grams) }

    /** Troca o alimento mantendo o equivalente em proteína (ou calorias) do item atual. */
    fun replaceFood(itemId: Long, old: Food, oldGrams: Int, new: Food) = launchSafe {
        repo.replaceMealItemFood(itemId, new.id, DietPlanner.equivalentGrams(old, oldGrams, new))
    }

    fun addFoodToMeal(mealId: Long, food: Food) = launchSafe {
        repo.addMealItem(mealId, food.id, DietPlanner.defaultGrams(food))
    }

    fun createFoodAndReplace(food: Food, itemId: Long, old: Food, oldGrams: Int) = launchSafe {
        val id = repo.addFood(food)
        repo.replaceMealItemFood(itemId, id, DietPlanner.equivalentGrams(old, oldGrams, food))
    }

    fun createFoodAndAdd(food: Food, mealId: Long) = launchSafe {
        val id = repo.addFood(food)
        repo.addMealItem(mealId, id, DietPlanner.defaultGrams(food))
    }

    // ---- Pratos extras do dia ----

    /** Registra um alimento da lista comido na refeição [type] hoje. */
    fun addFoodExtra(type: MealType, food: Food, grams: Int) = launchSafe {
        repo.addExtra(
            MealExtra(
                epochDay = clock.value.epochDay, type = type, foodId = food.id, grams = grams, name = food.name,
                kcal = (food.kcalPer100 * grams / 100).roundToInt(), protein = (food.proteinPer100 * grams / 100).roundToInt()
            )
        )
        toast(tr("Anotado! As próximas refeições de hoje foram ajustadas."))
    }

    /** Registra um prato livre (nome e valores estimados) comido na refeição [type] hoje. */
    fun addFreeExtra(type: MealType, name: String, kcal: Int, protein: Int) = launchSafe {
        repo.addExtra(MealExtra(epochDay = clock.value.epochDay, type = type, name = name.trim(), kcal = kcal, protein = protein))
        toast(tr("Anotado! As próximas refeições de hoje foram ajustadas."))
    }

    fun removeExtra(id: Long) = launchSafe { repo.removeExtra(id) }

    // ---- Agenda de refeições ----

    fun setMealCount(count: Int) = launchSafe {
        working(tr("Ajustando refeições…")) { repo.updateMealCount(count) }
        toast(tr("{0} refeições por dia. Dieta reponderada — revise a sugestão.", count))
    }
    fun setMealTime(type: MealType, time: String) = launchSafe {
        repo.setMealTime(type, time)
        toast(tr("Horário de {0} salvo: {1}.", type.label.lowercase(), time))
    }
    /** Marca a refeição como feita; [kcal] são as calorias que ela teve de fato (com o ajuste do dia). */
    fun setMealDone(type: MealType, done: Boolean, kcal: Int? = null) = launchSafe {
        repo.setMealDone(type, clock.value.epochDay, done, kcal)
    }

    fun setMealSkipped(type: MealType, skipped: Boolean) = launchSafe {
        repo.setMealSkipped(type, clock.value.epochDay, skipped)
        if (skipped) toast(tr("Anotado! O resto do dia e da semana foi reajustado."))
    }

    private companion object {
        const val MIN_LOADING_MS = 600L
        const val MIN_BUSY_MS = 450L
    }
}
