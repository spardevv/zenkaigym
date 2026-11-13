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
import com.zenkaigym.domain.tr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Lembretes locais de refeição e treino. Cada aviso é um alarme que, ao tocar, mostra a notificação
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

    /** Cancela tudo e agenda o próximo disparo de cada aviso da agenda atual (nada, se os lembretes estiverem desligados). */
    suspend fun rescheduleAll(context: Context) {
        val app = context.applicationContext as ZenApp
        val alarms = context.getSystemService(AlarmManager::class.java)
        Reminder.allIds.forEach { alarms.cancel(alarmIntent(context, it)) }
        if (!app.appFlags.remindersEnabled) return

        val slots = app.database.dietDao().observeSlots().first()
        val availability = app.database.profileDao().getAvailability()
        val now = Calendar.getInstance()
        Reminders.build(slots, availability).forEach { reminder ->
            val meal = (reminder as? Reminder.Meal)?.type
            // Inexato de propósito: não exige a permissão de alarmes exatos e o atraso de poucos minutos não importa.
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, Reminders.nextTrigger(reminder, now), alarmIntent(context, reminder.id, meal))
        }
    }

    internal fun show(context: Context, id: Int, meal: MealType?) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        createChannel(context)
        val text = if (meal != null) tr("{0}: hora de comer!", meal.label) else tr("Hora do treino! Abra o modo treino e bora.")
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

    internal fun extras(intent: Intent): Pair<Int, MealType?> =
        intent.getIntExtra(EXTRA_ID, -1) to intent.getStringExtra(EXTRA_MEAL)?.let { name -> MealType.entries.firstOrNull { it.name == name } }
}

/** Disparo de um aviso: mostra a notificação e agenda os próximos. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as ZenApp
        I18n.select(app.appFlags.language)
        val (id, meal) = ReminderScheduler.extras(intent)
        if (id >= 0 && app.appFlags.remindersEnabled) ReminderScheduler.show(context, id, meal)
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
