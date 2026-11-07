package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.local.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class BackupContainer(
    val settings: UserSettings,
    val plans: List<WorkoutPlan>,
    val exercises: List<WorkoutExercise>,
    val histories: List<WorkoutHistory>,
    val skips: List<WorkoutPlanSkip>,
    val supplements: List<Supplement>,
    val supplementHistories: List<SupplementIntakeHistory>,
    val nutritionLogs: List<NutritionLog>,
    val measurements: List<BodyMeasurement>
)

object BackupHelper {

    fun createBackupJson(
        settings: UserSettings,
        plans: List<WorkoutPlan>,
        exercises: List<WorkoutExercise>,
        histories: List<WorkoutHistory>,
        skips: List<WorkoutPlanSkip>,
        supplements: List<Supplement>,
        supplementHistories: List<SupplementIntakeHistory>,
        nutritionLogs: List<NutritionLog>,
        measurements: List<BodyMeasurement>
    ): String {
        val root = JSONObject()
        root.put("app", "GYM_SMART_COACH")
        root.put("schemaVersion", 1)
        root.put("timestamp", System.currentTimeMillis())

        // Settings
        val setObj = JSONObject().apply {
            put("userName", settings.userName)
            put("profilePhotoUri", settings.profilePhotoUri)
            put("gender", settings.gender)
            if (settings.age != null) put("age", settings.age)
            if (settings.heightCm != null) put("heightCm", settings.heightCm)
            if (settings.weightKg != null) put("weightKg", settings.weightKg)
            put("activityLevel", settings.activityLevel)
            put("fitnessGoal", settings.fitnessGoal)
            put("mealsCount", settings.mealsCount)
            put("waterGoalMl", settings.waterGoalMl)
            put("weightUnit", settings.weightUnit)
            put("theme", settings.theme)
            put("workoutReminderEnabled", settings.workoutReminderEnabled)
            put("workoutReminderTime", settings.workoutReminderTime)
            put("waterReminderEnabled", settings.waterReminderEnabled)
            put("waterReminderTime", settings.waterReminderTime)
            put("supplementReminderEnabled", settings.supplementReminderEnabled)
            put("supplementReminderTime", settings.supplementReminderTime)
            put("onboardingCompleted", settings.onboardingCompleted)
        }
        root.put("settings", setObj)

        // Plans
        val plansArr = JSONArray()
        plans.forEach { p ->
            plansArr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("notes", p.notes)
                put("scheduledDay", p.scheduledDay)
                put("createdAt", p.createdAt)
            })
        }
        root.put("plans", plansArr)

        // Exercises
        val exArr = JSONArray()
        exercises.forEach { e ->
            exArr.put(JSONObject().apply {
                put("id", e.id)
                put("planId", e.planId)
                put("name", e.name)
                put("sets", e.sets)
                put("reps", e.reps)
                if (e.repsMax != null) put("repsMax", e.repsMax)
                put("weight", e.weight)
                put("notes", e.notes)
                if (e.supersetGroup != null) put("supersetGroup", e.supersetGroup)
                put("isTimedExercise", e.isTimedExercise)
                if (e.customMetrics != null) put("customMetrics", e.customMetrics)
            })
        }
        root.put("exercises", exArr)

        // Histories
        val hArr = JSONArray()
        histories.forEach { h ->
            hArr.put(JSONObject().apply {
                put("id", h.id)
                put("date", h.date)
                put("planName", h.planName)
                put("exerciseName", h.exerciseName)
                put("sets", h.sets)
                put("reps", h.reps)
                put("weight", h.weight)
                put("notes", h.notes)
                put("timestamp", h.timestamp)
                put("durationSeconds", h.durationSeconds)
                put("totalRestSecondsUsed", h.totalRestSecondsUsed)
                if (h.customData != null) put("customData", h.customData)
            })
        }
        root.put("histories", hArr)

        // Supplements
        val supArr = JSONArray()
        supplements.forEach { s ->
            supArr.put(JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("dosage", s.dosage)
                put("reminderTime", s.reminderTime)
                put("notes", s.notes)
                put("isActive", s.isActive)
            })
        }
        root.put("supplements", supArr)

        // Supplement Histories
        val supHArr = JSONArray()
        supplementHistories.forEach { sh ->
            supHArr.put(JSONObject().apply {
                put("id", sh.id)
                put("supplementId", sh.supplementId)
                put("supplementName", sh.supplementName)
                put("date", sh.date)
                put("time", sh.time)
                put("timestamp", sh.timestamp)
            })
        }
        root.put("supplementHistories", supHArr)

        // Nutrition
        val nArr = JSONArray()
        nutritionLogs.forEach { n ->
            nArr.put(JSONObject().apply {
                put("date", n.date)
                put("breakfast", n.breakfast)
                put("lunch", n.lunch)
                put("dinner", n.dinner)
                put("snacks", n.snacks)
                put("dailyNotes", n.dailyNotes)
                put("waterIntakeMl", n.waterIntakeMl)
            })
        }
        root.put("nutritionLogs", nArr)

        // Measurements
        val mArr = JSONArray()
        measurements.forEach { m ->
            mArr.put(JSONObject().apply {
                put("date", m.date)
                if (m.heightCm != null) put("heightCm", m.heightCm)
                if (m.weightKg != null) put("weightKg", m.weightKg)
                if (m.neckCm != null) put("neckCm", m.neckCm)
                if (m.waistCm != null) put("waistCm", m.waistCm)
                if (m.chestCm != null) put("chestCm", m.chestCm)
                if (m.armsCm != null) put("armsCm", m.armsCm)
                if (m.thighCm != null) put("thighCm", m.thighCm)
                put("timestamp", m.timestamp)
            })
        }
        root.put("measurements", mArr)

        return root.toString(2)
    }

    fun validateAndParseBackup(jsonString: String): Result<BackupContainer> {
        return runCatching {
            val root = JSONObject(jsonString)
            if (!root.has("app") || root.getString("app") != "GYM_SMART_COACH") {
                throw IllegalArgumentException("فایل پشتیبان نامعتبر است.")
            }

            val setObj = root.optJSONObject("settings")
            val settings = if (setObj != null) {
                UserSettings(
                    userName = setObj.optString("userName", ""),
                    profilePhotoUri = setObj.optString("profilePhotoUri", ""),
                    gender = setObj.optString("gender", ""),
                    age = if (setObj.has("age")) setObj.getInt("age") else null,
                    heightCm = if (setObj.has("heightCm")) setObj.getDouble("heightCm") else null,
                    weightKg = if (setObj.has("weightKg")) setObj.getDouble("weightKg") else null,
                    activityLevel = setObj.optString("activityLevel", "MODERATE"),
                    fitnessGoal = setObj.optString("fitnessGoal", "BULK"),
                    mealsCount = setObj.optInt("mealsCount", 4),
                    waterGoalMl = setObj.optInt("waterGoalMl", 2500),
                    weightUnit = setObj.optString("weightUnit", "kg"),
                    theme = setObj.optString("theme", "DARK"),
                    workoutReminderEnabled = setObj.optBoolean("workoutReminderEnabled", false),
                    workoutReminderTime = setObj.optString("workoutReminderTime", "18:00"),
                    waterReminderEnabled = setObj.optBoolean("waterReminderEnabled", false),
                    waterReminderTime = setObj.optString("waterReminderTime", "10:00"),
                    supplementReminderEnabled = setObj.optBoolean("supplementReminderEnabled", false),
                    supplementReminderTime = setObj.optString("supplementReminderTime", "09:00"),
                    onboardingCompleted = setObj.optBoolean("onboardingCompleted", true)
                )
            } else {
                UserSettings()
            }

            // Plans
            val plans = mutableListOf<WorkoutPlan>()
            val pArr = root.optJSONArray("plans") ?: JSONArray()
            for (i in 0 until pArr.length()) {
                val o = pArr.getJSONObject(i)
                plans.add(
                    WorkoutPlan(
                        id = o.optLong("id", 0),
                        name = o.getString("name"),
                        notes = o.optString("notes", ""),
                        scheduledDay = o.optString("scheduledDay", ""),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            // Exercises
            val exercises = mutableListOf<WorkoutExercise>()
            val eArr = root.optJSONArray("exercises") ?: JSONArray()
            for (i in 0 until eArr.length()) {
                val o = eArr.getJSONObject(i)
                exercises.add(
                    WorkoutExercise(
                        id = o.optLong("id", 0),
                        planId = o.getLong("planId"),
                        name = o.getString("name"),
                        sets = o.optInt("sets", 3),
                        reps = o.optInt("reps", 10),
                        repsMax = if (o.has("repsMax")) o.getInt("repsMax") else null,
                        weight = o.optDouble("weight", 0.0),
                        notes = o.optString("notes", ""),
                        supersetGroup = if (o.has("supersetGroup")) o.getString("supersetGroup") else null,
                        isTimedExercise = o.optBoolean("isTimedExercise", false),
                        customMetrics = if (o.has("customMetrics")) o.getString("customMetrics") else null
                    )
                )
            }

            // Histories
            val histories = mutableListOf<WorkoutHistory>()
            val hArr = root.optJSONArray("histories") ?: JSONArray()
            for (i in 0 until hArr.length()) {
                val o = hArr.getJSONObject(i)
                histories.add(
                    WorkoutHistory(
                        id = o.optLong("id", 0),
                        date = o.getString("date"),
                        planName = o.getString("planName"),
                        exerciseName = o.getString("exerciseName"),
                        sets = o.getInt("sets"),
                        reps = o.getInt("reps"),
                        weight = o.getDouble("weight"),
                        notes = o.optString("notes", ""),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                        durationSeconds = o.optLong("durationSeconds", 0),
                        totalRestSecondsUsed = o.optLong("totalRestSecondsUsed", 0),
                        customData = if (o.has("customData")) o.getString("customData") else null
                    )
                )
            }

            // Supplements
            val supplements = mutableListOf<Supplement>()
            val sArr = root.optJSONArray("supplements") ?: JSONArray()
            for (i in 0 until sArr.length()) {
                val o = sArr.getJSONObject(i)
                supplements.add(
                    Supplement(
                        id = o.optLong("id", 0),
                        name = o.getString("name"),
                        dosage = o.getString("dosage"),
                        reminderTime = o.optString("reminderTime", "09:00"),
                        notes = o.optString("notes", ""),
                        isActive = o.optBoolean("isActive", true)
                    )
                )
            }

            // Supplement Histories
            val supHistories = mutableListOf<SupplementIntakeHistory>()
            val shArr = root.optJSONArray("supplementHistories") ?: JSONArray()
            for (i in 0 until shArr.length()) {
                val o = shArr.getJSONObject(i)
                supHistories.add(
                    SupplementIntakeHistory(
                        id = o.optLong("id", 0),
                        supplementId = o.getLong("supplementId"),
                        supplementName = o.getString("supplementName"),
                        date = o.getString("date"),
                        time = o.getString("time"),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }

            // Nutrition
            val nutritionLogs = mutableListOf<NutritionLog>()
            val nArr = root.optJSONArray("nutritionLogs") ?: JSONArray()
            for (i in 0 until nArr.length()) {
                val o = nArr.getJSONObject(i)
                nutritionLogs.add(
                    NutritionLog(
                        date = o.getString("date"),
                        breakfast = o.optString("breakfast", ""),
                        lunch = o.optString("lunch", ""),
                        dinner = o.optString("dinner", ""),
                        snacks = o.optString("snacks", ""),
                        dailyNotes = o.optString("dailyNotes", ""),
                        waterIntakeMl = o.optInt("waterIntakeMl", 0)
                    )
                )
            }

            // Measurements
            val measurements = mutableListOf<BodyMeasurement>()
            val mArr = root.optJSONArray("measurements") ?: JSONArray()
            for (i in 0 until mArr.length()) {
                val o = mArr.getJSONObject(i)
                measurements.add(
                    BodyMeasurement(
                        date = o.getString("date"),
                        heightCm = if (o.has("heightCm")) o.getDouble("heightCm") else null,
                        weightKg = if (o.has("weightKg")) o.getDouble("weightKg") else null,
                        neckCm = if (o.has("neckCm")) o.getDouble("neckCm") else null,
                        waistCm = if (o.has("waistCm")) o.getDouble("waistCm") else null,
                        chestCm = if (o.has("chestCm")) o.getDouble("chestCm") else null,
                        armsCm = if (o.has("armsCm")) o.getDouble("armsCm") else null,
                        thighCm = if (o.has("thighCm")) o.getDouble("thighCm") else null,
                        timestamp = o.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }

            BackupContainer(
                settings = settings,
                plans = plans,
                exercises = exercises,
                histories = histories,
                skips = emptyList(),
                supplements = supplements,
                supplementHistories = supHistories,
                nutritionLogs = nutritionLogs,
                measurements = measurements
            )
        }
    }

    const val BACKUP_FILENAME = "gym_pro_backup.json"

    fun getAppBackupDirectory(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "GymPro_Backups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getAppBackupFile(context: Context): File {
        return File(getAppBackupDirectory(context), BACKUP_FILENAME)
    }

    fun saveBackupToFile(context: Context, backupJson: String): File {
        val file = getAppBackupFile(context)
        file.writeText(backupJson)
        return file
    }

    fun getExistingBackupFile(context: Context): File? {
        val file = getAppBackupFile(context)
        return if (file.exists() && file.length() > 0) file else null
    }

    fun readBackupFromFile(file: File): Result<BackupContainer> {
        return runCatching {
            val json = file.readText()
            validateAndParseBackup(json).getOrThrow()
        }
    }

    fun readBackupFromUri(context: Context, uri: Uri): Result<BackupContainer> {
        return runCatching {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: throw IllegalArgumentException("امکان باز کردن فایل انتخاب‌شده وجود ندارد.")
            validateAndParseBackup(json).getOrThrow()
        }
    }
}
