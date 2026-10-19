package com.zenkaigym.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.zenkaigym.data.entity.Availability
import com.zenkaigym.data.entity.DietPlan
import com.zenkaigym.data.entity.DietPlanFull
import com.zenkaigym.data.entity.Exercise
import com.zenkaigym.data.entity.Food
import com.zenkaigym.data.entity.Meal
import com.zenkaigym.data.entity.MealCheck
import com.zenkaigym.data.entity.MealExtra
import com.zenkaigym.data.entity.MealItem
import com.zenkaigym.data.entity.MealSlot
import com.zenkaigym.data.entity.PlanStatus
import com.zenkaigym.data.entity.SetLog
import com.zenkaigym.data.entity.UserProfile
import com.zenkaigym.data.entity.WeightEntry
import com.zenkaigym.data.entity.WorkoutDay
import com.zenkaigym.data.entity.WorkoutItem
import com.zenkaigym.data.entity.WorkoutLog
import com.zenkaigym.data.entity.WorkoutPlan
import com.zenkaigym.data.entity.WorkoutPlanFull
import com.zenkaigym.data.entity.WorkoutSwap
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observeProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfile)

    @Query("SELECT * FROM weight_entry ORDER BY epochDay ASC, id ASC")
    fun observeWeights(): Flow<List<WeightEntry>>

    @Insert
    suspend fun insertWeight(entry: WeightEntry)

    @Query("SELECT * FROM availability ORDER BY dayOfWeek")
    fun observeAvailability(): Flow<List<Availability>>

    @Query("SELECT * FROM availability ORDER BY dayOfWeek")
    suspend fun getAvailability(): List<Availability>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAvailability(list: List<Availability>)
}

@Dao
interface FoodDao {
    @Query("SELECT * FROM food ORDER BY category, name")
    fun observeFoods(): Flow<List<Food>>

    @Query("SELECT * FROM food")
    suspend fun getFoods(): List<Food>

    @Insert
    suspend fun insert(food: Food): Long

    @Insert
    suspend fun insertAll(foods: List<Food>)

    @Update
    suspend fun update(food: Food)

    @Query("DELETE FROM food WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM food")
    suspend fun deleteAll()
}

@Dao
interface WorkoutDao {
    @Query("SELECT COUNT(*) FROM exercise")
    suspend fun exerciseCount(): Int

    @Query("SELECT * FROM exercise ORDER BY id")
    suspend fun exercises(): List<Exercise>

    @Query("SELECT * FROM exercise ORDER BY `group`, name")
    fun observeExercises(): Flow<List<Exercise>>

    @Insert
    suspend fun insertExercises(list: List<Exercise>)

    @Insert
    suspend fun insertExercise(exercise: Exercise): Long

    @Query("SELECT COUNT(*) FROM workout_plan")
    suspend fun planCount(): Int

    @Query("SELECT COALESCE(MAX(position), -1) FROM workout_item WHERE dayId = :dayId")
    suspend fun lastPosition(dayId: Long): Int

    @Query("UPDATE workout_item SET exerciseId = :exerciseId WHERE id = :id")
    suspend fun updateExercise(id: Long, exerciseId: Long)

    @Insert
    suspend fun insertPlan(plan: WorkoutPlan): Long

    @Insert
    suspend fun insertDay(day: WorkoutDay): Long

    @Insert
    suspend fun insertItem(item: WorkoutItem): Long

    @Transaction
    @Query("SELECT * FROM workout_plan WHERE status = :status ORDER BY id DESC LIMIT 1")
    fun observeByStatus(status: PlanStatus): Flow<WorkoutPlanFull?>

    @Query("DELETE FROM workout_plan WHERE status = :status")
    suspend fun deleteByStatus(status: PlanStatus)

    @Query("UPDATE workout_plan SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: PlanStatus)

    @Query("DELETE FROM workout_item WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("UPDATE workout_item SET sets = :sets WHERE id = :id")
    suspend fun updateSets(id: Long, sets: Int)

    // ---- Registro de treino ----

    @Query("SELECT * FROM set_log WHERE epochDay = :epochDay")
    fun observeSets(epochDay: Long): Flow<List<SetLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSet(log: SetLog)

    @Query("DELETE FROM set_log WHERE epochDay = :epochDay AND exerciseId = :exerciseId AND setIndex = :setIndex")
    suspend fun deleteSet(epochDay: Long, exerciseId: Long, setIndex: Int)

    /** Séries da última vez em que o exercício foi feito antes de [beforeDay]; vazio se nunca. */
    @Query(
        "SELECT * FROM set_log WHERE exerciseId = :exerciseId AND epochDay = " +
            "(SELECT MAX(epochDay) FROM set_log WHERE exerciseId = :exerciseId AND epochDay < :beforeDay) " +
            "ORDER BY setIndex"
    )
    suspend fun lastSets(exerciseId: Long, beforeDay: Long): List<SetLog>

    @Query("SELECT COALESCE(MAX(weightKg), 0) FROM set_log WHERE exerciseId = :exerciseId AND epochDay < :beforeDay")
    suspend fun bestWeight(exerciseId: Long, beforeDay: Long): Double

    @Query("SELECT * FROM workout_log WHERE epochDay = :epochDay")
    fun observeWorkoutLog(epochDay: Long): Flow<WorkoutLog?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutLog(log: WorkoutLog)

    @Query("DELETE FROM workout_log WHERE epochDay = :epochDay")
    suspend fun deleteWorkoutLog(epochDay: Long)

    /** Volume (carga × repetições) e séries por dia, a partir de [from]. */
    @Query("SELECT epochDay, SUM(weightKg * reps) AS volume, COUNT(*) AS sets FROM set_log WHERE epochDay >= :from GROUP BY epochDay")
    fun observeVolumePerDay(from: Long): Flow<List<DayVolume>>

    @Query("SELECT epochDay FROM workout_log WHERE epochDay >= :from AND finished = 1")
    fun observeFinishedDays(from: Long): Flow<List<Long>>

    // ---- Trocas de exercício só do dia ----

    @Query("SELECT * FROM workout_swap WHERE epochDay = :epochDay")
    fun observeSwaps(epochDay: Long): Flow<List<WorkoutSwap>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSwap(swap: WorkoutSwap)

    @Query("DELETE FROM workout_swap WHERE epochDay = :epochDay AND itemId = :itemId")
    suspend fun deleteSwap(epochDay: Long, itemId: Long)

    @Query("SELECT exerciseId FROM workout_item WHERE id = :id")
    suspend fun itemExercise(id: Long): Long?

    @Query("DELETE FROM workout_swap WHERE epochDay < :epochDay")
    suspend fun pruneSwapsBefore(epochDay: Long)
}

@Dao
interface DietDao {
    @Insert
    suspend fun insertPlan(plan: DietPlan): Long

    @Insert
    suspend fun insertMeal(meal: Meal): Long

    @Insert
    suspend fun insertItem(item: MealItem): Long

    @Transaction
    @Query("SELECT * FROM diet_plan WHERE status = :status ORDER BY id DESC LIMIT 1")
    fun observeByStatus(status: PlanStatus): Flow<DietPlanFull?>

    @Query("DELETE FROM diet_plan WHERE status = :status")
    suspend fun deleteByStatus(status: PlanStatus)

    @Query("UPDATE diet_plan SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: PlanStatus)

    @Query("DELETE FROM meal_item WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("UPDATE meal_item SET grams = :grams WHERE id = :id")
    suspend fun updateGrams(id: Long, grams: Int)

    @Query("UPDATE meal_item SET foodId = :foodId, grams = :grams WHERE id = :id")
    suspend fun updateFood(id: Long, foodId: Long, grams: Int)

    @Query("SELECT COUNT(*) FROM diet_plan")
    suspend fun planCount(): Int

    @Query("SELECT * FROM diet_plan ORDER BY id DESC LIMIT 1")
    suspend fun latestPlan(): DietPlan?

    @Query("SELECT * FROM meal WHERE planId = :planId AND cheatKcal IS NOT NULL")
    suspend fun cheatMeals(planId: Long): List<Meal>

    // ---- Agenda de refeições e checagem diária ----

    @Query("SELECT * FROM meal_slot ORDER BY position")
    fun observeSlots(): Flow<List<MealSlot>>

    @Query("SELECT * FROM meal_slot ORDER BY position")
    suspend fun getSlots(): List<MealSlot>

    @Query("DELETE FROM meal_slot")
    suspend fun clearSlots()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(list: List<MealSlot>)

    @Query("UPDATE meal_slot SET time = :time WHERE type = :type")
    suspend fun updateSlotTime(type: com.zenkaigym.data.entity.MealType, time: String)

    @Query("SELECT * FROM meal_check WHERE epochDay = :epochDay")
    fun observeChecks(epochDay: Long): Flow<List<MealCheck>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCheck(check: MealCheck)

    @Query("DELETE FROM meal_check WHERE type = :type AND epochDay = :epochDay")
    suspend fun deleteCheck(type: com.zenkaigym.data.entity.MealType, epochDay: Long)

    @Query("DELETE FROM meal_check WHERE epochDay < :epochDay")
    suspend fun pruneChecksBefore(epochDay: Long)

    // ---- Pratos extras (fora do cardápio) ----

    @Query("SELECT * FROM meal_extra WHERE epochDay = :epochDay ORDER BY id")
    fun observeExtras(epochDay: Long): Flow<List<MealExtra>>

    @Insert
    suspend fun insertExtra(extra: MealExtra): Long

    @Query("DELETE FROM meal_extra WHERE id = :id")
    suspend fun deleteExtra(id: Long)

    @Query("DELETE FROM meal_extra WHERE epochDay < :epochDay")
    suspend fun pruneExtrasBefore(epochDay: Long)

    /** Refeições feitas por dia, a partir de [from] (para o gráfico de aderência). */
    @Query("SELECT epochDay, COUNT(*) AS count FROM meal_check WHERE epochDay >= :from GROUP BY epochDay")
    fun observeChecksPerDay(from: Long): Flow<List<DayCount>>
}

/** Agregados por dia usados nos gráficos de progresso. */
data class DayCount(val epochDay: Long, val count: Int)

data class DayVolume(val epochDay: Long, val volume: Double, val sets: Int)
