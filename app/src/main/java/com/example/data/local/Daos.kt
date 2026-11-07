package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_plans ORDER BY id DESC")
    fun getAllPlans(): Flow<List<WorkoutPlan>>

    @Query("SELECT * FROM workout_plans WHERE id = :id LIMIT 1")
    suspend fun getPlanById(id: Long): WorkoutPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: WorkoutPlan): Long

    @Update
    suspend fun updatePlan(plan: WorkoutPlan)

    @Delete
    suspend fun deletePlan(plan: WorkoutPlan)

    @Query("DELETE FROM workout_plans WHERE id = :id")
    suspend fun deletePlanById(id: Long)

    @Query("SELECT * FROM workout_exercises WHERE planId = :planId ORDER BY id ASC")
    fun getExercisesForPlan(planId: Long): Flow<List<WorkoutExercise>>

    @Query("SELECT * FROM workout_exercises WHERE planId = :planId ORDER BY id ASC")
    suspend fun getExercisesForPlanSync(planId: Long): List<WorkoutExercise>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: WorkoutExercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<WorkoutExercise>)

    @Update
    suspend fun updateExercise(exercise: WorkoutExercise)

    @Delete
    suspend fun deleteExercise(exercise: WorkoutExercise)

    @Query("DELETE FROM workout_exercises WHERE planId = :planId")
    suspend fun deleteExercisesForPlan(planId: Long)

    @Query("SELECT * FROM workout_histories ORDER BY timestamp DESC")
    fun getAllHistories(): Flow<List<WorkoutHistory>>

    @Query("SELECT * FROM workout_histories WHERE date = :date ORDER BY timestamp DESC")
    fun getHistoriesForDate(date: String): Flow<List<WorkoutHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: WorkoutHistory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistories(histories: List<WorkoutHistory>)

    @Query("DELETE FROM workout_histories WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("SELECT * FROM workout_set_logs WHERE date = :date AND planId = :planId ORDER BY id ASC")
    fun getSetLogsForDateAndPlan(date: String, planId: Long): Flow<List<WorkoutSetLog>>

    @Query("SELECT * FROM workout_set_logs WHERE date = :date ORDER BY id ASC")
    fun getSetLogsForDate(date: String): Flow<List<WorkoutSetLog>>

    @Query("SELECT * FROM workout_set_logs ORDER BY id DESC")
    fun getAllSetLogs(): Flow<List<WorkoutSetLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetLog(log: WorkoutSetLog): Long

    @Update
    suspend fun updateSetLog(log: WorkoutSetLog)

    @Delete
    suspend fun deleteSetLog(log: WorkoutSetLog)

    @Query("DELETE FROM workout_set_logs WHERE date = :date AND planId = :planId")
    suspend fun clearSetLogsForDateAndPlan(date: String, planId: Long)

    @Query("DELETE FROM workout_set_logs")
    suspend fun clearAllSetLogs()

    @Query("SELECT * FROM workout_plan_skips")
    fun getAllSkips(): Flow<List<WorkoutPlanSkip>>

    @Query("SELECT * FROM workout_plan_skips WHERE planId = :planId")
    fun getSkipsForPlan(planId: Long): Flow<List<WorkoutPlanSkip>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkip(skip: WorkoutPlanSkip): Long

    @Query("DELETE FROM workout_plan_skips WHERE id = :id")
    suspend fun deleteSkipById(id: Long)

    @Query("DELETE FROM workout_plan_skips WHERE planId = :planId AND originalDate = :originalDate")
    suspend fun deleteSkipForPlanAndDate(planId: Long, originalDate: String)
}

@Dao
interface SupplementDao {
    @Query("SELECT * FROM supplements ORDER BY id ASC")
    fun getAllSupplements(): Flow<List<Supplement>>

    @Query("SELECT * FROM supplements WHERE isActive = 1 ORDER BY id ASC")
    fun getActiveSupplements(): Flow<List<Supplement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplement(supplement: Supplement): Long

    @Update
    suspend fun updateSupplement(supplement: Supplement)

    @Delete
    suspend fun deleteSupplement(supplement: Supplement)

    @Query("SELECT * FROM supplement_intake_histories WHERE date = :date ORDER BY timestamp DESC")
    fun getIntakeHistoriesForDate(date: String): Flow<List<SupplementIntakeHistory>>

    @Query("SELECT * FROM supplement_intake_histories ORDER BY timestamp DESC")
    fun getAllIntakeHistories(): Flow<List<SupplementIntakeHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntakeHistory(history: SupplementIntakeHistory): Long

    @Query("DELETE FROM supplement_intake_histories WHERE id = :id")
    suspend fun deleteIntakeHistory(id: Long)

    @Query("DELETE FROM supplement_intake_histories WHERE supplementId = :supplementId AND date = :date")
    suspend fun deleteIntakeHistoryForSupplementAndDate(supplementId: Long, date: String)
}

@Dao
interface NutritionDao {
    @Query("SELECT * FROM nutrition_logs WHERE date = :date LIMIT 1")
    fun getNutritionLog(date: String): Flow<NutritionLog?>

    @Query("SELECT * FROM nutrition_logs WHERE date = :date LIMIT 1")
    suspend fun getNutritionLogSync(date: String): NutritionLog?

    @Query("SELECT * FROM nutrition_logs ORDER BY date DESC")
    fun getAllNutritionLogs(): Flow<List<NutritionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNutritionLog(log: NutritionLog)

    @Query("SELECT DISTINCT date FROM nutrition_logs WHERE breakfast != '' OR lunch != '' OR dinner != '' OR snacks != '' OR waterIntakeMl > 0")
    fun getDatesWithNutrition(): Flow<List<String>>

    @Query("DELETE FROM nutrition_logs WHERE date = :date")
    suspend fun deleteNutritionLog(date: String)
}

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM body_measurements ORDER BY date DESC")
    fun getAllMeasurements(): Flow<List<BodyMeasurement>>

    @Query("SELECT * FROM body_measurements WHERE date = :date LIMIT 1")
    fun getMeasurement(date: String): Flow<BodyMeasurement?>

    @Query("SELECT * FROM body_measurements ORDER BY date DESC LIMIT 1")
    fun getLatestMeasurement(): Flow<BodyMeasurement?>

    @Query("SELECT * FROM body_measurements ORDER BY date DESC LIMIT 1")
    suspend fun getLatestMeasurementSync(): BodyMeasurement?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMeasurement(measurement: BodyMeasurement)

    @Query("DELETE FROM body_measurements WHERE date = :date")
    suspend fun deleteMeasurement(date: String)
}
