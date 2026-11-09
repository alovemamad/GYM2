package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.GymRepository
import com.example.util.AiExportHelper
import com.example.util.BackupHelper
import com.example.util.JalaliCalendar
import com.example.util.NotificationHelper
import com.example.util.ReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class GymViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * مقدار اولیه را همان لحظه از دیتابیس/تنظیمات می‌خواند تا هنگام ورود به برنامه
     * صفحه‌ی خالی، آنبوردینگ یا تم پیش‌فرض برای چند فریم نمایش داده نشود.
     */
    private fun <T> Flow<T>.stateInPreloaded(default: T): StateFlow<T> {
        val initial = try {
            kotlinx.coroutines.runBlocking { this@stateInPreloaded.first() }
        } catch (_: Exception) {
            default
        }
        return stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)
    }

    private val db = AppDatabase.getInstance(application)
    private val prefs = UserPreferences(application)
    val repository = GymRepository(application, db, prefs)

    val userSettings = repository.userSettings.stateInPreloaded(UserSettings()
    )

    val allPlans = repository.allPlans.stateInPreloaded(emptyList()
    )

    val allHistories = repository.allHistories.stateInPreloaded(emptyList()
    )

    val allSupplements = repository.allSupplements.stateInPreloaded(emptyList()
    )

    val allMeasurements = repository.allMeasurements.stateInPreloaded(emptyList()
    )

    val latestMeasurement = repository.latestMeasurement.stateInPreloaded(null)

    val allSkips = repository.allSkips.stateInPreloaded(emptyList()
    )

    val datesWithNutrition = repository.datesWithNutrition.stateInPreloaded(emptyList()
    )

    // Selected date for nutrition tab (defaults to today Gregorian)
    private val _selectedNutritionDate = MutableStateFlow(JalaliCalendar.getTodayGregorian())
    val selectedNutritionDate: StateFlow<String> = _selectedNutritionDate.asStateFlow()

    val selectedNutritionLog = _selectedNutritionDate.flatMapLatest { date ->
        repository.getNutritionLog(date)
    }.stateInPreloaded(null)

    val selectedDateSupplementIntakes = _selectedNutritionDate.flatMapLatest { date ->
        repository.getSupplementIntakeForDate(date)
    }.stateInPreloaded(emptyList())

    val allSupplementIntakeHistories = repository.allSupplementIntakeHistories.stateInPreloaded(emptyList()
    )

    val todayNutritionLog = repository.getNutritionLog(JalaliCalendar.getTodayGregorian())
        .stateInPreloaded(null)

    // Unfinished active sets check (for resumption)
    val unfinishedActiveSets = repository.allSetLogs.stateInPreloaded(emptyList()
    )

    init {
        viewModelScope.launch {
            repository.seedStarterPlanIfEmpty()
        }
    }

    fun setSelectedNutritionDate(date: String) {
        _selectedNutritionDate.value = date
    }

    // Settings actions
    fun updateProfile(
        name: String,
        photoUri: String,
        gender: String,
        age: Int?,
        heightCm: Double?,
        weightKg: Double?,
        activity: String,
        goal: String,
        mealsCount: Int,
        waterGoal: Int,
        weightUnit: String
    ) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.updateSettings(
                current.copy(
                    userName = name,
                    profilePhotoUri = photoUri,
                    gender = gender,
                    age = age,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    activityLevel = activity,
                    fitnessGoal = goal,
                    mealsCount = mealsCount,
                    waterGoalMl = waterGoal,
                    weightUnit = weightUnit,
                    onboardingCompleted = true
                )
            )

            // Also record body measurement if weight/height provided
            if (heightCm != null || weightKg != null) {
                val today = JalaliCalendar.getTodayGregorian()
                repository.saveMeasurement(
                    BodyMeasurement(
                        date = today,
                        heightCm = heightCm,
                        weightKg = weightKg,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun updateProfilePhoto(photoUri: String) {
        viewModelScope.launch {
            val current = userSettings.value
            repository.updateSettings(current.copy(profilePhotoUri = photoUri))
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            repository.setTheme(theme)
        }
    }

    fun setAppearanceAndPalette(appearanceMode: String, accentPalette: String) {
        viewModelScope.launch {
            repository.setAppearanceAndPalette(appearanceMode, accentPalette)
        }
    }

    fun setWaterGoal(goalMl: Int) {
        viewModelScope.launch {
            repository.setWaterGoal(goalMl)
        }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch {
            repository.setOnboardingCompleted(completed)
        }
    }

    fun updateSoundSettings(enabled: Boolean, restVol: Float, timedVol: Float, reminderVol: Float) {
        viewModelScope.launch {
            repository.updateSoundSettings(enabled, restVol, timedVol, reminderVol)
        }
    }

    // Reminder toggles
    fun toggleReminder(type: String, enabled: Boolean, timeStr: String) {
        viewModelScope.launch {
            val s = userSettings.value
            val app = getApplication<Application>()
            when (type) {
                "WORKOUT" -> {
                    repository.updateSettings(s.copy(workoutReminderEnabled = enabled, workoutReminderTime = timeStr))
                    if (enabled) {
                        ReminderScheduler.scheduleDailyReminder(
                            app, ReminderScheduler.ID_WORKOUT, timeStr,
                            "وقت تمرین و باشگاه!", "امروز روز ساختن بدنی قوی‌تر است. برنامه‌ی تمرینت منتظرته!",
                            NotificationHelper.CHANNEL_WORKOUT
                        )
                    } else {
                        ReminderScheduler.cancelReminder(app, ReminderScheduler.ID_WORKOUT)
                    }
                }
                "WATER" -> {
                    repository.updateSettings(s.copy(waterReminderEnabled = enabled, waterReminderTime = timeStr))
                    if (enabled) {
                        ReminderScheduler.scheduleDailyReminder(
                            app, ReminderScheduler.ID_WATER, timeStr,
                            "یادآور نوشیدن آب", "هیدراته بمانید! یک لیوان آب خنک بنوشید.",
                            NotificationHelper.CHANNEL_WATER
                        )
                    } else {
                        ReminderScheduler.cancelReminder(app, ReminderScheduler.ID_WATER)
                    }
                }
                "SUPPLEMENT" -> {
                    repository.updateSettings(s.copy(supplementReminderEnabled = enabled, supplementReminderTime = timeStr))
                    if (enabled) {
                        ReminderScheduler.scheduleDailyReminder(
                            app, ReminderScheduler.ID_SUPPLEMENT, timeStr,
                            "یادآور مصرف مکمل‌ها", "زمان مصرف مکمل‌های روزانه فرا رسیده است.",
                            NotificationHelper.CHANNEL_SUPPLEMENT
                        )
                    } else {
                        ReminderScheduler.cancelReminder(app, ReminderScheduler.ID_SUPPLEMENT)
                    }
                }
            }
        }
    }

    fun triggerTestNotification() {
        val app = getApplication<Application>()
        NotificationHelper.showNotification(
            context = app,
            notificationId = 999,
            channelId = NotificationHelper.CHANNEL_REMINDERS,
            title = "اعلان تستی GYM",
            message = "سیستم اعلان‌ها و یادآورها به درستی فعال است!"
        )
    }

    // Water actions
    fun addWater(amountMl: Int, date: String = JalaliCalendar.getTodayGregorian()) {
        viewModelScope.launch {
            repository.addWater(date, amountMl)
        }
    }

    fun resetWater(date: String = JalaliCalendar.getTodayGregorian()) {
        viewModelScope.launch {
            repository.resetWater(date)
        }
    }

    // Nutrition meals
    fun updateMealsLog(
        date: String,
        breakfast: String,
        lunch: String,
        dinner: String,
        snacks: String,
        notes: String
    ) {
        viewModelScope.launch {
            val existing = repository.getNutritionLog(date).first()
            val updated = (existing ?: NutritionLog(date = date)).copy(
                breakfast = breakfast,
                lunch = lunch,
                dinner = dinner,
                snacks = snacks,
                dailyNotes = notes
            )
            repository.saveNutritionLog(updated)
        }
    }

    // Supplements
    fun toggleSupplementActive(supplement: Supplement) {
        viewModelScope.launch {
            repository.saveSupplement(supplement.copy(isActive = !supplement.isActive))
        }
    }

    fun saveSupplement(supplement: Supplement) {
        viewModelScope.launch {
            repository.saveSupplement(supplement)
        }
    }

    fun deleteSupplement(supplement: Supplement) {
        viewModelScope.launch {
            repository.deleteSupplement(supplement)
        }
    }

    fun toggleSupplementIntake(supplement: Supplement, isCurrentlyTaken: Boolean, date: String) {
        viewModelScope.launch {
            if (isCurrentlyTaken) {
                repository.removeSupplementIntake(supplement.id, date)
            } else {
                val timeNow = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date())
                repository.recordSupplementIntake(supplement, date, timeNow)
            }
        }
    }

    // Plans
    fun deletePlan(planId: Long) {
        viewModelScope.launch {
            repository.deletePlan(planId)
        }
    }

    fun duplicatePlan(planId: Long) {
        viewModelScope.launch {
            repository.duplicatePlan(planId)
        }
    }

    fun duplicateAllPlans() {
        viewModelScope.launch {
            repository.duplicateAllPlans()
        }
    }

    fun reschedulePlan(planId: Long, originalDate: String, newDate: String?) {
        viewModelScope.launch {
            repository.recordPlanSkip(planId, originalDate, newDate)
        }
    }

    fun undoSkip(planId: Long, originalDate: String) {
        viewModelScope.launch {
            repository.undoPlanSkip(planId, originalDate)
        }
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    // Measurements
    fun saveMeasurement(measurement: BodyMeasurement) {
        viewModelScope.launch {
            repository.saveMeasurement(measurement)
        }
    }

    fun deleteMeasurement(date: String) {
        viewModelScope.launch {
            repository.deleteMeasurement(date)
        }
    }

    // AI export prompt
    suspend fun getAiExportPrompt(): String {
        val s = userSettings.value
        val p = allPlans.value
        val h = allHistories.value
        val n = repository.allNutritionLogs.first()
        val m = allMeasurements.value
        return AiExportHelper.generateAiAnalysisPrompt(s, p, h, n, m)
    }

    // Backup & Restore
    suspend fun exportDataJson(): String {
        return repository.exportAllDataJson()
    }

    suspend fun saveBackupToDeviceFile(context: android.content.Context): File {
        val json = exportDataJson()
        return BackupHelper.saveBackupToFile(context, json)
    }

    suspend fun restoreDataJson(json: String): Result<Unit> {
        return repository.restoreFromJson(json)
    }

    suspend fun restoreFromUri(context: android.content.Context, uri: Uri): Result<Unit> {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: return Result.failure(Exception("امکان باز کردن فایل وجود ندارد"))
            repository.restoreFromJson(jsonString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromDeviceFile(file: File): Result<Unit> {
        return try {
            val jsonString = file.readText()
            repository.restoreFromJson(jsonString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }
}
