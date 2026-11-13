package com.zenkaigym.domain

import com.zenkaigym.data.entity.Availability
import com.zenkaigym.data.entity.MealSlot
import com.zenkaigym.data.entity.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RemindersTest {
    /** Quarta-feira, 2026-01-07, às [hour]:[minute]. */
    private fun wednesday(hour: Int, minute: Int) = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.JANUARY, 7, hour, minute, 0)
    }

    private fun at(base: Calendar, daysLater: Int, hour: Int, minute: Int) = (base.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, daysLater)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }.timeInMillis

    @Test
    fun refeicaoAindaHojeDisparaHoje() {
        val now = wednesday(10, 0)
        val next = Reminders.nextTrigger(Reminder.Meal(MealType.LUNCH, TimeOfDay(12, 30)), now)
        assertEquals(at(now, 0, 12, 30), next)
    }

    @Test
    fun refeicaoQueJaPassouVaiParaAmanha() {
        val now = wednesday(13, 0)
        val next = Reminders.nextTrigger(Reminder.Meal(MealType.LUNCH, TimeOfDay(12, 30)), now)
        assertEquals(at(now, 1, 12, 30), next)
    }

    @Test
    fun horarioIgualAoAgoraVaiParaOProximoDia() {
        val now = wednesday(12, 30)
        val next = Reminders.nextTrigger(Reminder.Meal(MealType.LUNCH, TimeOfDay(12, 30)), now)
        assertEquals(at(now, 1, 12, 30), next)
    }

    @Test
    fun treinoEsperaOProximoDiaDaSemanaCerto() {
        val now = wednesday(10, 0)
        // Sexta = 5: dois dias depois da quarta.
        assertEquals(at(now, 2, 18, 0), Reminders.nextTrigger(Reminder.Workout(5, TimeOfDay(18, 0)), now))
        // Quarta = 3 mas o horário já passou: só na quarta seguinte.
        assertEquals(at(now, 7, 8, 0), Reminders.nextTrigger(Reminder.Workout(3, TimeOfDay(8, 0)), now))
    }

    @Test
    fun montaAvisosSoComHorarioValidoEDiasDisponiveis() {
        val slots = listOf(MealSlot(MealType.BREAKFAST, 0, "07:00"), MealSlot(MealType.LUNCH, 1, "lixo"))
        val availability = listOf(
            Availability(1, true, "18:00", "19:00"),
            Availability(2, true, null, null),
            Availability(3, false, "18:00", "19:00")
        )
        val built = Reminders.build(slots, availability)
        assertEquals(listOf(Reminder.Meal(MealType.BREAKFAST, TimeOfDay(7, 0)), Reminder.Workout(1, TimeOfDay(18, 0))), built)
    }

    @Test
    fun idsSaoUnicos() {
        assertEquals(Reminder.allIds.size, Reminder.allIds.toSet().size)
        assertTrue(Reminder.Meal(MealType.SUPPER, TimeOfDay(22, 0)).id in Reminder.allIds)
        assertTrue(Reminder.Workout(7, TimeOfDay(6, 0)).id in Reminder.allIds)
    }
}
