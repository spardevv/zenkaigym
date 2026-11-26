package com.zenkaigym.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.room.InvalidationTracker
import com.zenkaigym.MainActivity
import com.zenkaigym.R
import com.zenkaigym.ZenApp
import com.zenkaigym.data.entity.MealCheck
import com.zenkaigym.data.entity.MealType
import com.zenkaigym.data.entity.PlanStatus
import com.zenkaigym.domain.I18n
import com.zenkaigym.domain.MealSchedule
import com.zenkaigym.domain.Water
import com.zenkaigym.domain.WorkoutWeek
import com.zenkaigym.domain.tr
import com.zenkaigym.util.Clock
import com.zenkaigym.util.WEEK_DAYS_SHORT
import com.zenkaigym.util.dayOfWeekFromEpochDay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Widget do Dojo na tela inicial: treino de hoje, próxima refeição (com "Feito") e água do dia (com "+250 ml").
 * Lê o mesmo banco do app e se atualiza sozinho quando algo muda (e a cada 30 min, para virar o dia).
 */
class DojoWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                refresh(context)
            } finally {
                pending.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WATER, ACTION_MEAL_DONE -> {
                val pending = goAsync()
                scope.launch {
                    try {
                        val app = context.applicationContext as ZenApp
                        if (intent.action == ACTION_WATER) addWater(app) else markNextMeal(app)
                        refresh(context)
                    } catch (e: Exception) {
                        Log.e("DojoWidget", "Falha na ação do widget", e)
                    } finally {
                        pending.finish()
                    }
                }
            }
            else -> super.onReceive(context, intent)
        }
    }

    /** O que o widget mostra, já em texto. */
    private class Content(
        val day: String,
        val workout: String,
        val mealLabel: String,
        val meal: String,
        val mealType: MealType?,
        val waterMl: Int,
        val waterGoal: Int
    )

    companion object {
        const val ACTION_WATER = "com.zenkaigym.widget.WATER"
        const val ACTION_MEAL_DONE = "com.zenkaigym.widget.MEAL_DONE"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private var pendingRefresh: Job? = null

        /** Tabelas que mudam o que o widget mostra. */
        private val WATCHED = arrayOf("meal_check", "water_log", "workout_log", "set_log", "diet_plan", "meal", "workout_plan", "meal_slot", "user_profile")

        /** Liga o widget às mudanças do banco: qualquer escrita nessas tabelas redesenha (agrupando rajadas). */
        fun watch(app: ZenApp) {
            runCatching {
                app.database.invalidationTracker.addObserver(object : InvalidationTracker.Observer(WATCHED) {
                    override fun onInvalidated(tables: Set<String>) = requestRefresh(app)
                })
            }
        }

        fun requestRefresh(context: Context) {
            val app = context.applicationContext
            if (ids(app).isEmpty()) return
            pendingRefresh?.cancel()
            pendingRefresh = scope.launch {
                delay(400)
                runCatching { refresh(app) }.onFailure { Log.e("DojoWidget", "Falha ao atualizar o widget", it) }
            }
        }

        private fun ids(context: Context): IntArray =
            AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, DojoWidget::class.java))

        suspend fun refresh(context: Context) {
            val ids = ids(context)
            if (ids.isEmpty()) return
            val app = context.applicationContext as ZenApp
            I18n.select(app.appFlags.language)
            val views = render(context, load(app))
            AppWidgetManager.getInstance(context).updateAppWidget(ids, views)
        }

        private suspend fun load(app: ZenApp): Content {
            val db = app.database
            val flags = app.appFlags
            val now = Clock.now()
            val today = now.epochDay
            val weekStart = flags.weekStart
            val weekFrom = weekStart.weekStartOf(today)

            // Treino: a mesma reorganização da semana do Dojo (não realizados e troca manual).
            val plan = db.workoutDao().observeByStatus(PlanStatus.ACTIVE).first()
            val logs = db.workoutDao().observeWorkoutLogs(weekFrom).first()
            val override = if (flags.swapWeek == weekFrom && flags.swapDayId >= 0) WorkoutWeek.Override(flags.swapFrom, flags.swapDayId) else null
            val session = WorkoutWeek.assign(plan?.days.orEmpty(), today, weekStart, logs, override).session
            val todayLog = logs.firstOrNull { it.epochDay == today }
            val workout = when {
                plan == null -> tr("Sem ficha ativa")
                session == null -> tr("Descanso · o corpo cresce hoje")
                todayLog?.finished == true -> tr("{0} · feito", tr(session.day.name))
                todayLog?.finished == false -> tr("{0} · não realizado", tr(session.day.name))
                else -> tr(session.day.name)
            }

            // Próxima refeição do dia (as feitas e as não realizadas já passaram).
            val diet = db.dietDao().observeByStatus(PlanStatus.ACTIVE).first()
            val slots = db.dietDao().getSlots()
            val checks = db.dietDao().observeChecks(today).first()
            val dow = dayOfWeekFromEpochDay(today)
            val meals = diet?.meals.orEmpty().filter { it.meal.dayOfWeek == dow || it.meal.dayOfWeek == 0 }
            val marked = checks.map { it.type }.toSet()
            val next = MealSchedule.next(meals.map { it.meal.type }, slots, marked, now.minutes)
                ?: meals.map { it.meal.type }.firstOrNull { it !in marked }
            val nextMeal = meals.firstOrNull { it.meal.type == next }
            val doneCount = checks.count { !it.skipped }
            val (mealLabel, meal) = when {
                diet == null -> tr("PRÓXIMA REFEIÇÃO") to tr("Sem dieta ativa")
                nextMeal == null -> tr("REFEIÇÕES · {0}/{1}", doneCount, meals.size) to tr("Tudo feito hoje!")
                else -> tr("PRÓXIMA · {0}/{1}", doneCount, meals.size) to
                    "${tr(nextMeal.meal.name)} · ${MealSchedule.timeOf(nextMeal.meal.type, slots).format()}"
            }

            val water = db.dietDao().getWater(today)?.ml ?: 0
            val goal = Water.goalMl(db.profileDao().getProfile()?.weightKg)
            return Content(WEEK_DAYS_SHORT[dow - 1], workout, mealLabel, meal, nextMeal?.meal?.type, water, goal)
        }

        private fun render(context: Context, c: Content): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_dojo)
            views.setTextViewText(R.id.widget_title, "道 " + tr("Dojo").uppercase())
            views.setTextViewText(R.id.widget_day, tr("Hoje · {0}", c.day))
            views.setTextViewText(R.id.widget_workout_label, tr("TREINO DE HOJE"))
            views.setTextViewText(R.id.widget_workout, c.workout)
            views.setTextViewText(R.id.widget_meal_label, c.mealLabel)
            views.setTextViewText(R.id.widget_meal, c.meal)
            views.setTextViewText(R.id.widget_meal_done, tr("Feito ✓"))
            views.setViewVisibility(R.id.widget_meal_done, if (c.mealType != null) View.VISIBLE else View.GONE)
            views.setTextViewText(R.id.widget_water, tr("Água · {0} / {1}", Water.label(c.waterMl), Water.label(c.waterGoal)))
            views.setProgressBar(R.id.widget_water_bar, 100, (Water.fraction(c.waterMl, c.waterGoal) * 100).toInt(), false)
            views.setTextViewText(R.id.widget_water_add, "+${Water.GLASS_ML} ml")

            val open = PendingIntent.getActivity(
                context, 10, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            views.setOnClickPendingIntent(R.id.widget_water_add, broadcast(context, ACTION_WATER, 11))
            views.setOnClickPendingIntent(R.id.widget_meal_done, broadcast(context, ACTION_MEAL_DONE, 12))
            return views
        }

        private fun broadcast(context: Context, action: String, code: Int): PendingIntent = PendingIntent.getBroadcast(
            context, code, Intent(context, DojoWidget::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        private suspend fun addWater(app: ZenApp) {
            app.repository.addWater(Clock.now().epochDay, Water.GLASS_ML)
        }

        /** Marca a próxima refeição como feita (as calorias ficam as do cardápio, sem o ajuste do dia). */
        private suspend fun markNextMeal(app: ZenApp) {
            val type = load(app).mealType ?: return
            app.database.dietDao().upsertCheck(MealCheck(type, Clock.now().epochDay))
        }
    }
}
