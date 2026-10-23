package com.zenkaigym.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zenkaigym.data.dao.DietDao
import com.zenkaigym.data.dao.FoodDao
import com.zenkaigym.data.dao.ProfileDao
import com.zenkaigym.data.dao.WorkoutDao
import com.zenkaigym.data.entity.Availability
import com.zenkaigym.data.entity.DietPlan
import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.Meal
import com.zenkaigym.data.entity.MealCheck
import com.zenkaigym.data.entity.MealExtra
import com.zenkaigym.data.entity.MealItem
import com.zenkaigym.data.entity.MealSlot
import com.zenkaigym.data.entity.SetLog
import com.zenkaigym.data.entity.UserProfile
import com.zenkaigym.data.entity.WeightEntry
import com.zenkaigym.data.entity.WorkoutDay
import com.zenkaigym.data.entity.WorkoutItem
import com.zenkaigym.data.entity.WorkoutLog
import com.zenkaigym.data.entity.WorkoutPlan
import com.zenkaigym.data.entity.WorkoutSwap

@Database(
    entities = [
        UserProfile::class, WeightEntry::class, Availability::class, Food::class,
        Exercise::class, WorkoutPlan::class, WorkoutDay::class, WorkoutItem::class,
        DietPlan::class, Meal::class, MealItem::class, MealSlot::class, MealCheck::class,
        WorkoutLog::class, SetLog::class, WorkoutSwap::class, MealExtra::class
    ],
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun foodDao(): FoodDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun dietDao(): DietDao

    companion object {
        /** v2: janela de horário por dia (fim do treino). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE availability ADD COLUMN endTime TEXT")
            }
        }

        /** v3: tipo da refeição, agenda de refeições e checagem diária. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meal ADD COLUMN type TEXT NOT NULL DEFAULT 'LUNCH'")
                db.execSQL("UPDATE meal SET type = 'BREAKFAST' WHERE name = 'Café da manhã'")
                db.execSQL("UPDATE meal SET type = 'SNACK_AM' WHERE name = 'Lanche da manhã'")
                db.execSQL("UPDATE meal SET type = 'SNACK_PM' WHERE name = 'Lanche da tarde'")
                db.execSQL("UPDATE meal SET type = 'DINNER' WHERE name = 'Jantar'")
                db.execSQL("UPDATE meal SET type = 'SUPPER' WHERE name = 'Ceia'")
                db.execSQL("CREATE TABLE IF NOT EXISTS meal_slot (type TEXT NOT NULL, position INTEGER NOT NULL, time TEXT NOT NULL, PRIMARY KEY(type))")
                db.execSQL("CREATE TABLE IF NOT EXISTS meal_check (type TEXT NOT NULL, epochDay INTEGER NOT NULL, PRIMARY KEY(type, epochDay))")
            }
        }

        /** v4: cardápio semanal (dia da semana e refeição livre) e medida caseira dos alimentos. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meal ADD COLUMN dayOfWeek INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE meal ADD COLUMN cheatKcal INTEGER")
                db.execSQL("ALTER TABLE diet_plan ADD COLUMN cheatDay INTEGER")
                db.execSQL("ALTER TABLE food ADD COLUMN unitLabel TEXT")
                db.execSQL("ALTER TABLE food ADD COLUMN unitGrams REAL")
                // Alimentos da lista pronta ganham a medida caseira; os cadastrados pelo usuário ficam como estão.
                FoodExamples.all().filter { it.unitLabel != null }.forEach {
                    db.execSQL(
                        "UPDATE food SET unitLabel = ?, unitGrams = ? WHERE name = ? AND unitLabel IS NULL",
                        arrayOf<Any>(it.unitLabel!!, it.unitGrams!!, it.name)
                    )
                }
            }
        }

        /** v5: registro de treino (séries realizadas com carga e repetições). */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS workout_log (epochDay INTEGER NOT NULL, finished INTEGER NOT NULL, PRIMARY KEY(epochDay))")
                db.execSQL("CREATE TABLE IF NOT EXISTS set_log (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, epochDay INTEGER NOT NULL, exerciseId INTEGER NOT NULL, setIndex INTEGER NOT NULL, reps INTEGER NOT NULL, weightKg REAL NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_set_log_epochDay_exerciseId_setIndex ON set_log (epochDay, exerciseId, setIndex)")
            }
        }

        /** v6: trocas de exercício só do dia e pratos extras comidos numa refeição. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS workout_swap (epochDay INTEGER NOT NULL, itemId INTEGER NOT NULL, exerciseId INTEGER NOT NULL, PRIMARY KEY(epochDay, itemId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS meal_extra (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, epochDay INTEGER NOT NULL, type TEXT NOT NULL, foodId INTEGER, grams INTEGER, name TEXT NOT NULL, kcal INTEGER NOT NULL, protein INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_extra_epochDay ON meal_extra (epochDay)")
            }
        }

        /**
         * v7: grupos musculares detalhados (Pernas e Costas se dividem), tipo e equipamento do exercício,
         * preferências de treino no perfil e refeição "não realizada" (com as kcal comidas de fato).
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercise ADD COLUMN kind TEXT NOT NULL DEFAULT 'ISOLATION'")
                db.execSQL("ALTER TABLE exercise ADD COLUMN equipment TEXT NOT NULL DEFAULT 'GYM'")
                // Exercícios próprios do usuário nos grupos que deixaram de existir.
                db.execSQL("UPDATE exercise SET `group` = 'QUADS' WHERE `group` = 'LEGS'")
                db.execSQL("UPDATE exercise SET `group` = 'LATS' WHERE `group` = 'BACK'")
                syncCatalog(db)

                db.execSQL("ALTER TABLE user_profile ADD COLUMN level TEXT NOT NULL DEFAULT 'INTERMEDIATE'")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN cardio TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN focus TEXT NOT NULL DEFAULT 'BALANCED'")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN equipment TEXT NOT NULL DEFAULT 'GYM'")

                db.execSQL("ALTER TABLE meal_check ADD COLUMN skipped INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE meal_check ADD COLUMN kcal INTEGER")
            }
        }

        /** v8: músculos separados como na anatomia (ombro em três partes, meio das costas, trapézio, lombar, oblíquos…). */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Exercícios próprios do usuário em grupos que foram divididos ou renomeados.
                db.execSQL("UPDATE exercise SET `group` = 'MID_BACK' WHERE `group` = 'UPPER_BACK'")
                db.execSQL("UPDATE exercise SET `group` = 'SHOULDERS_SIDE' WHERE `group` = 'SHOULDERS'")
                db.execSQL("UPDATE exercise SET `group` = 'ABS' WHERE `group` = 'CORE'")
                syncCatalog(db)
            }
        }

        /** Põe os exercícios do catálogo no grupo, tipo e equipamento atuais e insere os que ainda não existem. */
        private fun syncCatalog(db: SupportSQLiteDatabase) {
            ExerciseCatalog.entries.forEach {
                val args = arrayOf<Any>(it.group.name, it.kind.name, it.equipment.name, it.name)
                db.execSQL("UPDATE exercise SET `group` = ?, kind = ?, equipment = ? WHERE name = ?", args)
                db.execSQL(
                    "INSERT INTO exercise (`group`, kind, equipment, name) SELECT ?, ?, ?, ? " +
                        "WHERE NOT EXISTS (SELECT 1 FROM exercise WHERE name = ?)",
                    args + it.name
                )
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "zenkaigym.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .build()
    }
}
