package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.*
import com.example.util.BackupContainer
import com.example.util.BackupHelper
import com.example.util.ImageStorageHelper
import com.example.util.JalaliCalendar
import com.example.util.ReminderScheduler
import com.example.widget.WaterAppWidgetProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class GymRepository(
    private val context: Context,
    private val appDatabase: AppDatabase,
    private val userPreferences: UserPreferences
) {
    private val workoutDao = appDatabase.workoutDao()
    private val supplementDao = appDatabase.supplementDao()
    private val nutritionDao = appDatabase.nutritionDao()
    private val measurementDao = appDatabase.measurementDao()

    val userSettings: Flow<UserSettings> = userPreferences.userSettingsFlow

    suspend fun updateSettings(settings: UserSettings) {
        userPreferences.updateSettings(settings)
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        userPreferences.setOnboardingCompleted(completed)
    }

    suspend fun setTheme(theme: String) {
        userPreferences.setTheme(theme)
    }

    suspend fun setAppearanceAndPalette(appearanceMode: String, accentPalette: String) {
        userPreferences.setAppearanceAndPalette(appearanceMode, accentPalette)
    }

    suspend fun updateGlassSettings(
        material: String,
        wallpaper: String,
        glassLists: Boolean,
        shrinkTabBar: Boolean,
        motionHighlights: Boolean,
        reduceTransparency: Boolean
    ) {
        userPreferences.updateGlassSettings(
            material = material,
            wallpaper = wallpaper,
            glassLists = glassLists,
            shrinkTabBar = shrinkTabBar,
            motionHighlights = motionHighlights,
            reduceTransparency = reduceTransparency
        )
    }

    suspend fun setWaterGoal(goalMl: Int) {
        userPreferences.setWaterGoal(goalMl)
        WaterAppWidgetProvider.updateAllWidgets(context)
    }

    // Workouts & Plans
    val allPlans: Flow<List<WorkoutPlan>> = workoutDao.getAllPlans()

    suspend fun getPlanById(id: Long): WorkoutPlan? = workoutDao.getPlanById(id)

    fun getExercisesForPlan(planId: Long): Flow<List<WorkoutExercise>> =
        workoutDao.getExercisesForPlan(planId)

    suspend fun getExercisesForPlanSync(planId: Long): List<WorkoutExercise> =
        workoutDao.getExercisesForPlanSync(planId)

    suspend fun savePlanWithExercises(plan: WorkoutPlan, exercises: List<WorkoutExercise>): Long {
        return appDatabase.withTransaction {
            val planId = if (plan.id == 0L) {
                workoutDao.insertPlan(plan)
            } else {
                workoutDao.updatePlan(plan)
                workoutDao.deleteExercisesForPlan(plan.id)
                plan.id
            }

            val updatedExercises = exercises.map { it.copy(planId = planId, id = 0) }
            workoutDao.insertExercises(updatedExercises)
            planId
        }
    }

    suspend fun deletePlan(planId: Long) {
        appDatabase.withTransaction {
            workoutDao.deleteExercisesForPlan(planId)
            workoutDao.deleteSkipsForPlan(planId)
            workoutDao.deletePlanById(planId)
        }
    }

    suspend fun duplicatePlan(planId: Long) {
        val plan = workoutDao.getPlanById(planId) ?: return
        val exercises = workoutDao.getExercisesForPlanSync(planId)
        val newPlan = plan.copy(id = 0, name = "${plan.name} (کپی)", createdAt = System.currentTimeMillis())
        savePlanWithExercises(newPlan, exercises)
    }

    suspend fun duplicateAllPlans() {
        val plans = workoutDao.getAllPlans().first()
        plans.forEach { plan ->
            val exercises = workoutDao.getExercisesForPlanSync(plan.id)
            val newPlan = plan.copy(id = 0, name = "${plan.name} (کپی)", createdAt = System.currentTimeMillis())
            savePlanWithExercises(newPlan, exercises)
        }
    }

    // Skips and Rescheduling
    val allSkips: Flow<List<WorkoutPlanSkip>> = workoutDao.getAllSkips()

    suspend fun recordPlanSkip(planId: Long, originalDate: String, newDate: String?) {
        appDatabase.withTransaction {
            workoutDao.deleteSkipForPlanAndDate(planId, originalDate)
            workoutDao.insertSkip(
                WorkoutPlanSkip(
                    planId = planId,
                    originalDate = originalDate,
                    newDate = newDate,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun undoPlanSkip(planId: Long, originalDate: String) {
        workoutDao.deleteSkipForPlanAndDate(planId, originalDate)
    }

    // Active Workout & Set Logs
    fun getSetLogsForDate(date: String): Flow<List<WorkoutSetLog>> =
        workoutDao.getSetLogsForDate(date)

    fun getSetLogsForDateAndPlan(date: String, planId: Long): Flow<List<WorkoutSetLog>> =
        workoutDao.getSetLogsForDateAndPlan(date, planId)

    val allSetLogs: Flow<List<WorkoutSetLog>> = workoutDao.getAllSetLogs()

    suspend fun logSet(log: WorkoutSetLog): Long {
        return if (log.id == 0L) {
            workoutDao.insertSetLog(log)
        } else {
            workoutDao.updateSetLog(log)
            log.id
        }
    }

    suspend fun deleteSetLog(log: WorkoutSetLog) {
        workoutDao.deleteSetLog(log)
    }

    suspend fun clearActiveSets(date: String, planId: Long) {
        workoutDao.clearSetLogsForDateAndPlan(date, planId)
    }

    // History
    val allHistories: Flow<List<WorkoutHistory>> = workoutDao.getAllHistories()

    fun getHistoriesForDate(date: String): Flow<List<WorkoutHistory>> =
        workoutDao.getHistoriesForDate(date)

    suspend fun saveWorkoutHistory(
        date: String,
        planName: String,
        exerciseSummaries: List<WorkoutHistory>
    ) {
        workoutDao.insertHistories(exerciseSummaries)
    }

    suspend fun deleteHistory(id: Long) {
        workoutDao.deleteHistoryById(id)
    }

    // Supplements
    val allSupplements: Flow<List<Supplement>> = supplementDao.getAllSupplements()

    suspend fun saveSupplement(supplement: Supplement): Long {
        val savedId = if (supplement.id == 0L) {
            supplementDao.insertSupplement(supplement)
        } else {
            supplementDao.updateSupplement(supplement)
            supplement.id
        }
        if (supplement.isActive) {
            com.example.util.ReminderScheduler.scheduleSupplementItemReminder(
                context = context,
                supplementId = savedId,
                timeStr = supplement.reminderTime,
                name = supplement.name,
                dosage = supplement.dosage
            )
        } else {
            com.example.util.ReminderScheduler.cancelSupplementItemReminder(context, savedId)
        }
        return savedId
    }

    suspend fun deleteSupplement(supplement: Supplement) {
        supplementDao.deleteSupplement(supplement)
        com.example.util.ReminderScheduler.cancelSupplementItemReminder(context, supplement.id)
    }

    fun getSupplementIntakeForDate(date: String): Flow<List<SupplementIntakeHistory>> =
        supplementDao.getIntakeHistoriesForDate(date)

    val allSupplementIntakeHistories: Flow<List<SupplementIntakeHistory>> =
        supplementDao.getAllIntakeHistories()

    suspend fun recordSupplementIntake(supplement: Supplement, date: String, time: String) {
        supplementDao.insertIntakeHistory(
            SupplementIntakeHistory(
                supplementId = supplement.id,
                supplementName = supplement.name,
                date = date,
                time = time,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeSupplementIntake(supplementId: Long, date: String) {
        supplementDao.deleteIntakeHistoryForSupplementAndDate(supplementId, date)
    }

    // Nutrition & Water
    fun getNutritionLog(date: String): Flow<NutritionLog?> =
        nutritionDao.getNutritionLog(date)

    val allNutritionLogs: Flow<List<NutritionLog>> =
        nutritionDao.getAllNutritionLogs()

    val datesWithNutrition: Flow<List<String>> =
        nutritionDao.getDatesWithNutrition()

    suspend fun saveNutritionLog(log: NutritionLog) {
        nutritionDao.insertOrUpdateNutritionLog(log)
    }

    suspend fun addWater(date: String, amountMl: Int) {
        val current = nutritionDao.getNutritionLogSync(date)
        val newAmount = ((current?.waterIntakeMl ?: 0) + amountMl).coerceAtLeast(0)
        val updated = (current ?: NutritionLog(date = date)).copy(waterIntakeMl = newAmount)
        nutritionDao.insertOrUpdateNutritionLog(updated)
        WaterAppWidgetProvider.updateAllWidgets(context)
    }

    suspend fun resetWater(date: String) {
        val current = nutritionDao.getNutritionLogSync(date)
        if (current != null) {
            nutritionDao.insertOrUpdateNutritionLog(current.copy(waterIntakeMl = 0))
        }
        WaterAppWidgetProvider.updateAllWidgets(context)
    }

    // Measurements
    val allMeasurements: Flow<List<BodyMeasurement>> = measurementDao.getAllMeasurements()
    val latestMeasurement: Flow<BodyMeasurement?> = measurementDao.getLatestMeasurement()

    suspend fun saveMeasurement(measurement: BodyMeasurement) {
        measurementDao.insertOrUpdateMeasurement(measurement)
        val latest = measurementDao.getLatestMeasurementSync()
        if (latest != null && latest.date == measurement.date) {
            val validWeight = measurement.weightKg?.takeIf { it > 0.0 }
            val validHeight = measurement.heightCm?.takeIf { it > 0.0 }
            if (validWeight != null || validHeight != null) {
                val s = userPreferences.userSettingsFlow.first()
                userPreferences.updateSettings(
                    s.copy(
                        weightKg = validWeight ?: s.weightKg,
                        heightCm = validHeight ?: s.heightCm
                    )
                )
            }
        }
    }

    suspend fun deleteMeasurement(date: String) {
        measurementDao.deleteMeasurement(date)
    }

    // Backup & Restore
    suspend fun exportAllDataJson(): String {
        val settings = userPreferences.userSettingsFlow.first()
        val plans = workoutDao.getAllPlans().first()
        val exercises = mutableListOf<WorkoutExercise>()
        plans.forEach { p ->
            exercises.addAll(workoutDao.getExercisesForPlanSync(p.id))
        }
        val histories = workoutDao.getAllHistories().first()
        val skips = workoutDao.getAllSkips().first()
        val supplements = supplementDao.getAllSupplements().first()
        val supHistories = supplementDao.getAllIntakeHistories().first()
        val nutrition = nutritionDao.getAllNutritionLogs().first()
        val measurements = measurementDao.getAllMeasurements().first()

        val json = BackupHelper.createBackupJson(
            settings = settings,
            plans = plans,
            exercises = exercises,
            histories = histories,
            skips = skips,
            supplements = supplements,
            supplementHistories = supHistories,
            nutritionLogs = nutrition,
            measurements = measurements
        )

        userPreferences.updateBackupTime(System.currentTimeMillis())
        return json
    }

    suspend fun restoreFromJson(backupJson: String): Result<Unit> {
        val result = BackupHelper.validateAndParseBackup(backupJson)
        if (result.isFailure) return Result.failure(result.exceptionOrNull()!!)
        return restoreFromContainer(result.getOrNull()!!)
    }

    suspend fun restoreFromContainer(data: BackupContainer): Result<Unit> = withContext(Dispatchers.IO) {
        // clearAllTables روی ترد اصلی کرش می‌کند؛ کل بازیابی روی IO و با گرفتن خطا انجام می‌شود
        try {
            doRestore(data)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun cancelAllScheduledAlarms() {
        try {
            ReminderScheduler.cancelReminder(context, ReminderScheduler.ID_WORKOUT)
            ReminderScheduler.cancelReminder(context, ReminderScheduler.ID_WATER)
            ReminderScheduler.cancelReminder(context, ReminderScheduler.ID_SUPPLEMENT)
            ReminderScheduler.cancelReminder(context, ReminderScheduler.ID_REST_TIMER)
            supplementDao.getAllSupplements().first().forEach { sup ->
                ReminderScheduler.cancelSupplementItemReminder(context, sup.id)
            }
        } catch (_: Exception) {
        }
    }

    private suspend fun doRestore(data: BackupContainer) {
        cancelAllScheduledAlarms()

        // Clear existing tables
        workoutDao.clearAllSetLogs()
        appDatabase.clearAllTables()

        // Restore settings
        userPreferences.updateSettings(data.settings)

        // Batch restore all database records inside a single atomic transaction
        appDatabase.withTransaction {
            val planIdMap = mutableMapOf<Long, Long>()
            data.plans.forEach { p ->
                val oldId = p.id
                val newId = workoutDao.insertPlan(p.copy(id = 0))
                planIdMap[oldId] = newId
            }

            data.exercises.forEach { e ->
                val targetPlanId = planIdMap[e.planId] ?: e.planId
                workoutDao.insertExercise(e.copy(id = 0, planId = targetPlanId))
            }

            data.histories.forEach { h ->
                workoutDao.insertHistory(h.copy(id = 0))
            }

            val supIdMap = mutableMapOf<Long, Long>()
            data.supplements.forEach { s ->
                val oldId = s.id
                val newId = supplementDao.insertSupplement(s.copy(id = 0))
                supIdMap[oldId] = newId
            }

            data.supplementHistories.forEach { sh ->
                val newSupId = supIdMap[sh.supplementId] ?: sh.supplementId
                supplementDao.insertIntakeHistory(sh.copy(id = 0, supplementId = newSupId))
            }

            data.nutritionLogs.forEach { n ->
                nutritionDao.insertOrUpdateNutritionLog(n)
            }

            data.measurements.forEach { m ->
                measurementDao.insertOrUpdateMeasurement(m)
            }

            data.skips.forEach { sk ->
                val targetPlanId = planIdMap[sk.planId] ?: sk.planId
                workoutDao.insertSkip(sk.copy(id = 0, planId = targetPlanId))
            }
        }

        ReminderScheduler.syncAllReminders(context)
        try {
            WaterAppWidgetProvider.updateAllWidgets(context)
        } catch (_: Exception) {
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        cancelAllScheduledAlarms()
        appDatabase.clearAllTables()
        userPreferences.clearAll()
        ImageStorageHelper.deleteProfilePhoto(context)
        WaterAppWidgetProvider.updateAllWidgets(context)
    }

    suspend fun syncSoundCache() {
        userPreferences.syncSoundCache()
    }

    suspend fun updateSoundSettings(enabled: Boolean, restVol: Float, timedVol: Float, reminderVol: Float) {
        userPreferences.updateSoundSettings(enabled, restVol, timedVol, reminderVol)
    }

    suspend fun setGithubRepo(repo: String) {
        userPreferences.setGithubRepo(repo)
    }

    // Do not seed any pre-made workout plans as per user requirement (clean slate)
    suspend fun seedStarterPlanIfEmpty() {
        // Intentionally empty: users create or import their own plans
    }
}
