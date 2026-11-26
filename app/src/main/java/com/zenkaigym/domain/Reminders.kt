package com.zenkaigym.domain

import com.zenkaigym.data.entity.Availability
import com.zenkaigym.data.entity.MealSlot
import com.zenkaigym.data.entity.MealType
import com.zenkaigym.util.dayOfWeekFromCalendar
import java.util.Calendar

/** Um aviso diário: refeição (todo dia) ou treino (num dia da semana, com horário próprio). */
sealed class Reminder {
    abstract val id: Int
    abstract val time: TimeOfDay

    /** Dias em que vale (1 = segunda ... 7 = domingo); null = todos. */
    abstract val days: Set<Int>?

    data class Meal(val type: MealType, override val time: TimeOfDay) : Reminder() {
        override val id get() = MEAL_BASE + type.ordinal
        override val days: Set<Int>? get() = null
    }

    data class Workout(val dayOfWeek: Int, override val time: TimeOfDay) : Reminder() {
        override val id get() = WORKOUT_BASE + dayOfWeek
        override val days: Set<Int> get() = setOf(dayOfWeek)
    }

    /** Copo de água: [index] = posição na lista de horários de água (todo dia). */
    data class Water(val index: Int, override val time: TimeOfDay) : Reminder() {
        override val id get() = WATER_BASE + index
        override val days: Set<Int>? get() = null
    }

    companion object {
        const val MEAL_BASE = 1
        const val WORKOUT_BASE = 100
        const val WATER_BASE = 200
        const val MAX_WATER = 8

        /** Todos os ids possíveis, para cancelar os alarmes antigos antes de agendar de novo. */
        val allIds: List<Int> =
            MealType.entries.map { MEAL_BASE + it.ordinal } + (1..7).map { WORKOUT_BASE + it } + (0 until MAX_WATER).map { WATER_BASE + it }
    }
}

object Reminders {
    /** Horários fixos do lembrete de água, espalhados pelo dia. */
    val WATER_TIMES = listOf(TimeOfDay(10, 0), TimeOfDay(13, 0), TimeOfDay(16, 0), TimeOfDay(19, 0))

    /**
     * Avisos a partir da agenda de refeições e dos dias de treino com horário definido
     * ([meals]: lembretes gerais ligados) e, com [water], os de água.
     */
    fun build(slots: List<MealSlot>, availability: List<Availability>, meals: Boolean = true, water: Boolean = false): List<Reminder> {
        val mealReminders = if (!meals) emptyList() else slots.mapNotNull { slot -> TimeOfDay.parse(slot.time)?.let { Reminder.Meal(slot.type, it) } }
        val workouts = if (!meals) emptyList() else availability.filter { it.available }
            .mapNotNull { a -> TimeOfDay.parse(a.startTime)?.let { Reminder.Workout(a.dayOfWeek, it) } }
        val waterReminders = if (!water) emptyList() else WATER_TIMES.mapIndexed { i, t -> Reminder.Water(i, t) }
        return mealReminders + workouts + waterReminders
    }

    /** Próximo disparo (em ms) depois de [now]; sempre no futuro, nunca no mesmo minuto. */
    fun nextTrigger(reminder: Reminder, now: Calendar): Long {
        val days = reminder.days
        for (offset in 0..7) {
            val candidate = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, reminder.time.hour)
                set(Calendar.MINUTE, reminder.time.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (candidate.timeInMillis <= now.timeInMillis) continue
            if (days == null || dayOfWeekFromCalendar(candidate.get(Calendar.DAY_OF_WEEK)) in days) return candidate.timeInMillis
        }
        error("Sem próximo horário para $reminder")
    }
}
