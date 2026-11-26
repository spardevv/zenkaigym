package com.zenkaigym.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zenkaigym.MainActivity
import com.zenkaigym.R
import com.zenkaigym.ZenApp
import com.zenkaigym.data.entity.MealType
import com.zenkaigym.domain.I18n
import com.zenkaigym.domain.Reminder
import com.zenkaigym.domain.Reminders
import com.zenkaigym.domain.Water
import com.zenkaigym.domain.tr
import com.zenkaigym.util.todayEpochDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Lembretes locais de refeição, treino e água. Cada aviso é um alarme que, ao tocar, mostra a notificação
 * e agenda os próximos. Os horários vêm da agenda do app; nada sai do aparelho.
 */
object ReminderScheduler {
    private const val CHANNEL_ID = "reminders"
    private const val EXTRA_ID = "reminder_id"
    private const val EXTRA_MEAL = "reminder_meal"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, tr("Lembretes"), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun alarmIntent(context: Context, id: Int, meal: MealType? = null): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_ID, id)
        meal?.let { intent.putExtra(EXTRA_MEAL, it.name) }
        return PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Cancela tudo e agenda o próximo disparo de cada aviso ligado (nada, se todos os lembretes estiverem desligados). */
    suspend fun rescheduleAll(context: Context) {
        val app = context.applicationContext as ZenApp
        val alarms = context.getSystemService(AlarmManager::class.java)
        Reminder.allIds.forEach { alarms.cancel(alarmIntent(context, it)) }
        val flags = app.appFlags
        if (!flags.remindersEnabled && !flags.waterReminders) return

        val slots = app.database.dietDao().observeSlots().first()
        val availability = app.database.profileDao().getAvailability()
        val now = Calendar.getInstance()
        Reminders.build(slots, availability, meals = flags.remindersEnabled, water = flags.waterReminders).forEach { reminder ->
            val meal = (reminder as? Reminder.Meal)?.type
            // Inexato de propósito: não exige a permissão de alarmes exatos e o atraso de poucos minutos não importa.
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, Reminders.nextTrigger(reminder, now), alarmIntent(context, reminder.id, meal))
        }
    }

    internal fun show(context: Context, id: Int, text: String) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        createChannel(context)
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Zenkaigym")
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        // Permissão negada em tempo de execução vira SecurityException em alguns aparelhos: melhor perder o aviso que travar.
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    /**
     * Texto do aviso [id], ou null se não deve aparecer: lembrete desligado, ou água com a meta do dia já batida.
     * Lê o banco, então roda fora da thread principal.
     */
    internal suspend fun textFor(app: ZenApp, id: Int, meal: MealType?): String? {
        val flags = app.appFlags
        return when {
            id >= Reminder.WATER_BASE -> {
                if (!flags.waterReminders) return null
                val day = todayEpochDay()
                val drank = app.database.dietDao().getWater(day)?.ml ?: 0
                val goal = Water.goalMl(app.database.profileDao().getProfile()?.weightKg)
                if (drank >= goal) null else tr("Hora de um copo de água! Hoje: {0} de {1}.", Water.label(drank), Water.label(goal))
            }
            !flags.remindersEnabled -> null
            meal != null -> tr("{0}: hora de comer!", meal.label)
            else -> tr("Hora do treino! Abra o modo treino e bora.")
        }
    }

    internal fun extras(intent: Intent): Pair<Int, MealType?> =
        intent.getIntExtra(EXTRA_ID, -1) to intent.getStringExtra(EXTRA_MEAL)?.let { name -> MealType.entries.firstOrNull { it.name == name } }
}

/** Disparo de um aviso: mostra a notificação (se ainda faz sentido) e agenda os próximos. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as ZenApp
        I18n.select(app.appFlags.language)
        val (id, meal) = ReminderScheduler.extras(intent)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (id >= 0) runCatching { ReminderScheduler.textFor(app, id, meal) }.getOrNull()?.let { ReminderScheduler.show(context, id, it) }
                ReminderScheduler.rescheduleAll(context)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Alarmes se perdem ao reiniciar o aparelho: agenda de novo. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderScheduler.rescheduleAll(context)
            } finally {
                pending.finish()
            }
        }
    }
}
