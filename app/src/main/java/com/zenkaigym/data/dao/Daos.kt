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
import com.zenkaigym.data.entity.MealItem
import com.zenkaigym.data.entity.MealSlot
import com.zenkaigym.data.entity.PlanStatus
import com.zenkaigym.data.entity.UserProfile
import com.zenkaigym.data.entity.WeightEntry
import com.zenkaigym.data.entity.WorkoutDay
import com.zenkaigym.data.entity.WorkoutItem
import com.zenkaigym.data.entity.WorkoutPlan
import com.zenkaigym.data.entity.WorkoutPlanFull
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
}
