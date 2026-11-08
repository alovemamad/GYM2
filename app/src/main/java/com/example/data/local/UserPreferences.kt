package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "gym_user_preferences")

data class UserSettings(
    val userName: String = "",
    val profilePhotoUri: String = "",
    val gender: String = "", // "MALE", "FEMALE", ""
    val age: Int? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val activityLevel: String = "MODERATE", // "SEDENTARY", "MODERATE", "ACTIVE", "ATHLETE"
    val fitnessGoal: String = "BULK", // "BULK", "CUT", "MAINTAIN"
    val mealsCount: Int = 4,
    val waterGoalMl: Int = 2500,
    val weightUnit: String = "kg", // "kg" or "lb"
    val appearanceMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val accentPalette: String = "SPORT_RED", // "SPORT_RED", "GOLD_CARBON", "OCEAN", "EMERALD", "SUNSET", "VIOLET", "ROSE", "CYBER"
    val theme: String = "DARK", // Kept for backward compatibility
    val workoutReminderEnabled: Boolean = false,
    val workoutReminderTime: String = "18:00",
    val waterReminderEnabled: Boolean = false,
    val waterReminderTime: String = "10:00",
    val supplementReminderEnabled: Boolean = false,
    val supplementReminderTime: String = "09:00",
    val onboardingCompleted: Boolean = false,
    val lastBackupTime: Long = 0L,
    val inAppSoundsEnabled: Boolean = true,
    val restTimerVolume: Float = 0.8f,
    val timedAlarmVolume: Float = 0.9f,
    val reminderVolume: Float = 0.8f
)

class UserPreferences(private val context: Context) {

    private val KEY_USER_NAME = stringPreferencesKey("user_name")
    private val KEY_PROFILE_PHOTO = stringPreferencesKey("profile_photo_uri")
    private val KEY_GENDER = stringPreferencesKey("gender")
    private val KEY_AGE = intPreferencesKey("age")
    private val KEY_HEIGHT = doublePreferencesKey("height_cm")
    private val KEY_WEIGHT = doublePreferencesKey("weight_kg")
    private val KEY_ACTIVITY_LEVEL = stringPreferencesKey("activity_level")
    private val KEY_FITNESS_GOAL = stringPreferencesKey("fitness_goal")
    private val KEY_MEALS_COUNT = intPreferencesKey("meals_count")
    private val KEY_WATER_GOAL = intPreferencesKey("water_goal_ml")
    private val KEY_WEIGHT_UNIT = stringPreferencesKey("weight_unit")
    private val KEY_THEME = stringPreferencesKey("app_theme")
    private val KEY_APPEARANCE_MODE = stringPreferencesKey("appearance_mode")
    private val KEY_ACCENT_PALETTE = stringPreferencesKey("accent_palette")
    private val KEY_WORKOUT_REMINDER_ENABLED = booleanPreferencesKey("workout_reminder_enabled")
    private val KEY_WORKOUT_REMINDER_TIME = stringPreferencesKey("workout_reminder_time")
    private val KEY_WATER_REMINDER_ENABLED = booleanPreferencesKey("water_reminder_enabled")
    private val KEY_WATER_REMINDER_TIME = stringPreferencesKey("water_reminder_time")
    private val KEY_SUPPLEMENT_REMINDER_ENABLED = booleanPreferencesKey("supplement_reminder_enabled")
    private val KEY_SUPPLEMENT_REMINDER_TIME = stringPreferencesKey("supplement_reminder_time")
    private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    private val KEY_LAST_BACKUP = longPreferencesKey("last_backup_time")
    private val KEY_IN_APP_SOUNDS_ENABLED = booleanPreferencesKey("in_app_sounds_enabled")
    private val KEY_REST_TIMER_VOLUME = floatPreferencesKey("rest_timer_volume")
    private val KEY_TIMED_ALARM_VOLUME = floatPreferencesKey("timed_alarm_volume")
    private val KEY_REMINDER_VOLUME = floatPreferencesKey("reminder_volume")

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val rawAge = prefs[KEY_AGE]
        val rawHeight = prefs[KEY_HEIGHT]
        val rawWeight = prefs[KEY_WEIGHT]

        // Migration logic from old app_theme
        val savedAppearance = prefs[KEY_APPEARANCE_MODE]
        val savedPalette = prefs[KEY_ACCENT_PALETTE]
        val oldTheme = prefs[KEY_THEME] ?: "DARK"

        val (migratedAppearance, migratedPalette) = when {
            savedAppearance != null && savedPalette != null -> {
                savedAppearance to savedPalette
            }
            oldTheme == "SYSTEM" -> "SYSTEM" to "SPORT_RED"
            oldTheme == "DARK" -> "DARK" to "SPORT_RED"
            oldTheme == "LIGHT" -> "LIGHT" to "SPORT_RED"
            oldTheme == "RED" -> "SYSTEM" to "SPORT_RED"
            oldTheme == "PINK" -> "SYSTEM" to "ROSE"
            oldTheme == "OCEAN" -> "SYSTEM" to "OCEAN"
            oldTheme == "EMERALD" -> "SYSTEM" to "EMERALD"
            oldTheme == "SUNSET" -> "SYSTEM" to "SUNSET"
            oldTheme == "PURPLE" -> "SYSTEM" to "VIOLET"
            oldTheme == "CYBER" -> "SYSTEM" to "CYBER"
            else -> "SYSTEM" to "SPORT_RED"
        }

        UserSettings(
            userName = prefs[KEY_USER_NAME] ?: "",
            profilePhotoUri = prefs[KEY_PROFILE_PHOTO] ?: "",
            gender = prefs[KEY_GENDER] ?: "",
            age = if (rawAge != null && rawAge > 0) rawAge else null,
            heightCm = if (rawHeight != null && rawHeight > 0.0) rawHeight else null,
            weightKg = if (rawWeight != null && rawWeight > 0.0) rawWeight else null,
            activityLevel = prefs[KEY_ACTIVITY_LEVEL] ?: "MODERATE",
            fitnessGoal = prefs[KEY_FITNESS_GOAL] ?: "BULK",
            mealsCount = prefs[KEY_MEALS_COUNT] ?: 4,
            waterGoalMl = prefs[KEY_WATER_GOAL] ?: 2500,
            weightUnit = prefs[KEY_WEIGHT_UNIT] ?: "kg",
            appearanceMode = migratedAppearance,
            accentPalette = migratedPalette,
            theme = oldTheme,
            workoutReminderEnabled = prefs[KEY_WORKOUT_REMINDER_ENABLED] ?: false,
            workoutReminderTime = prefs[KEY_WORKOUT_REMINDER_TIME] ?: "18:00",
            waterReminderEnabled = prefs[KEY_WATER_REMINDER_ENABLED] ?: false,
            waterReminderTime = prefs[KEY_WATER_REMINDER_TIME] ?: "10:00",
            supplementReminderEnabled = prefs[KEY_SUPPLEMENT_REMINDER_ENABLED] ?: false,
            supplementReminderTime = prefs[KEY_SUPPLEMENT_REMINDER_TIME] ?: "09:00",
            onboardingCompleted = prefs[KEY_ONBOARDING_COMPLETED] ?: false,
            lastBackupTime = prefs[KEY_LAST_BACKUP] ?: 0L,
            inAppSoundsEnabled = prefs[KEY_IN_APP_SOUNDS_ENABLED] ?: true,
            restTimerVolume = prefs[KEY_REST_TIMER_VOLUME] ?: 0.8f,
            timedAlarmVolume = prefs[KEY_TIMED_ALARM_VOLUME] ?: 0.9f,
            reminderVolume = prefs[KEY_REMINDER_VOLUME] ?: 0.8f
        )
    }

    suspend fun updateSettings(settings: UserSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = settings.userName
            prefs[KEY_PROFILE_PHOTO] = settings.profilePhotoUri
            prefs[KEY_GENDER] = settings.gender
            if (settings.age != null) prefs[KEY_AGE] = settings.age else prefs.remove(KEY_AGE)
            if (settings.heightCm != null) prefs[KEY_HEIGHT] = settings.heightCm else prefs.remove(KEY_HEIGHT)
            if (settings.weightKg != null) prefs[KEY_WEIGHT] = settings.weightKg else prefs.remove(KEY_WEIGHT)
            prefs[KEY_ACTIVITY_LEVEL] = settings.activityLevel
            prefs[KEY_FITNESS_GOAL] = settings.fitnessGoal
            prefs[KEY_MEALS_COUNT] = settings.mealsCount
            prefs[KEY_WATER_GOAL] = settings.waterGoalMl
            prefs[KEY_WEIGHT_UNIT] = settings.weightUnit
            prefs[KEY_THEME] = settings.theme
            prefs[KEY_APPEARANCE_MODE] = settings.appearanceMode
            prefs[KEY_ACCENT_PALETTE] = settings.accentPalette
            prefs[KEY_WORKOUT_REMINDER_ENABLED] = settings.workoutReminderEnabled
            prefs[KEY_WORKOUT_REMINDER_TIME] = settings.workoutReminderTime
            prefs[KEY_WATER_REMINDER_ENABLED] = settings.waterReminderEnabled
            prefs[KEY_WATER_REMINDER_TIME] = settings.waterReminderTime
            prefs[KEY_SUPPLEMENT_REMINDER_ENABLED] = settings.supplementReminderEnabled
            prefs[KEY_SUPPLEMENT_REMINDER_TIME] = settings.supplementReminderTime
            prefs[KEY_ONBOARDING_COMPLETED] = settings.onboardingCompleted
            prefs[KEY_LAST_BACKUP] = settings.lastBackupTime
            prefs[KEY_IN_APP_SOUNDS_ENABLED] = settings.inAppSoundsEnabled
            prefs[KEY_REST_TIMER_VOLUME] = settings.restTimerVolume
            prefs[KEY_TIMED_ALARM_VOLUME] = settings.timedAlarmVolume
            prefs[KEY_REMINDER_VOLUME] = settings.reminderVolume
        }
    }

    suspend fun updateSoundSettings(enabled: Boolean, restVol: Float, timedVol: Float, reminderVol: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IN_APP_SOUNDS_ENABLED] = enabled
            prefs[KEY_REST_TIMER_VOLUME] = restVol
            prefs[KEY_TIMED_ALARM_VOLUME] = timedVol
            prefs[KEY_REMINDER_VOLUME] = reminderVol
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setAppearanceAndPalette(appearanceMode: String, accentPalette: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APPEARANCE_MODE] = appearanceMode
            prefs[KEY_ACCENT_PALETTE] = accentPalette
            prefs[KEY_THEME] = if (appearanceMode == "LIGHT") "LIGHT" else accentPalette
        }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }

    suspend fun setWaterGoal(goalMl: Int) {
        context.dataStore.edit { it[KEY_WATER_GOAL] = goalMl }
    }

    suspend fun updateBackupTime(time: Long) {
        context.dataStore.edit { it[KEY_LAST_BACKUP] = time }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
