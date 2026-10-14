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
import com.zenkaigym.data.entity.MealItem
import com.zenkaigym.data.entity.MealSlot
import com.zenkaigym.data.entity.UserProfile
import com.zenkaigym.data.entity.WeightEntry
import com.zenkaigym.data.entity.WorkoutDay
import com.zenkaigym.data.entity.WorkoutItem
import com.zenkaigym.data.entity.WorkoutPlan

@Database(
    entities = [
        UserProfile::class, WeightEntry::class, Availability::class, Food::class,
        Exercise::class, WorkoutPlan::class, WorkoutDay::class, WorkoutItem::class,
        DietPlan::class, Meal::class, MealItem::class, MealSlot::class, MealCheck::class
    ],
    version = 4,
    exportSchema = false
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

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "zenkaigym.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
