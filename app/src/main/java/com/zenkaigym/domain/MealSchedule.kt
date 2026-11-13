package com.zenkaigym.domain

import com.zenkaigym.data.entity.MealSlot
import com.zenkaigym.data.entity.MealType

/** Regras puras da agenda de refeições do dia (quantidade, horários, próxima refeição). */
object MealSchedule {
    const val MIN_MEALS = 3
    const val MAX_MEALS = 6

    private val byCount: Map<Int, List<MealType>> = mapOf(
        3 to listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER),
        4 to listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.SNACK_PM, MealType.DINNER),
        5 to listOf(MealType.BREAKFAST, MealType.SNACK_AM, MealType.LUNCH, MealType.SNACK_PM, MealType.DINNER),
        6 to listOf(MealType.BREAKFAST, MealType.SNACK_AM, MealType.LUNCH, MealType.SNACK_PM, MealType.DINNER, MealType.SUPPER)
    )

    fun typesFor(count: Int): List<MealType> = byCount.getValue(count.coerceIn(MIN_MEALS, MAX_MEALS))

    /** Agenda padrão para [count] refeições, mantendo os horários que o usuário já escolheu. */
    fun build(count: Int, current: List<MealSlot> = emptyList()): List<MealSlot> {
        val known = current.associate { it.type to it.time }
        return typesFor(count).mapIndexed { index, type ->
            MealSlot(type, index, known[type] ?: type.defaultTime)
        }
    }

    /** Horário de uma refeição segundo a agenda; sem linha na agenda usa o horário padrão do tipo. */
    fun timeOf(type: MealType, slots: List<MealSlot>): TimeOfDay =
        TimeOfDay.parse(slots.firstOrNull { it.type == type }?.time) ?: TimeOfDay.parse(type.defaultTime)!!

    /** Quanto tempo depois do horário uma refeição ainda conta como "a atual". */
    const val GRACE_MINUTES = 90

    /**
     * Próxima refeição a fazer: a primeira não feita cujo horário ainda não passou há mais de
     * [GRACE_MINUTES]. Null quando não resta nenhuma para hoje.
     */
    fun next(types: List<MealType>, slots: List<MealSlot>, done: Set<MealType>, nowMinutes: Int): MealType? =
        types.filter { it !in done }
            .sortedBy { timeOf(it, slots).totalMinutes }
            .firstOrNull { timeOf(it, slots).totalMinutes >= nowMinutes - GRACE_MINUTES }
}
