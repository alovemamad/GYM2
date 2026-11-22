package com.example.util

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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

data class BackupSaveResult(
    val appFile: File,
    val exportedToDownloads: Boolean,
    val displayLocation: String
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
        root.put("schemaVersion", 2)
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
            put("appearanceMode", settings.appearanceMode)
            put("accentPalette", settings.accentPalette)
            put("theme", settings.theme)
            put("glassMaterial", settings.glassMaterial)
            put("glassWallpaper", settings.glassWallpaper)
            put("glassLists", settings.glassLists)
            put("shrinkTabBar", settings.shrinkTabBar)
            put("motionHighlights", settings.motionHighlights)
            put("reduceTransparency", settings.reduceTransparency)
            put("workoutReminderEnabled", settings.workoutReminderEnabled)
            put("workoutReminderTime", settings.workoutReminderTime)
            put("waterReminderEnabled", settings.waterReminderEnabled)
            put("waterReminderTime", settings.waterReminderTime)
            put("supplementReminderEnabled", settings.supplementReminderEnabled)
            put("supplementReminderTime", settings.supplementReminderTime)
            put("onboardingCompleted", settings.onboardingCompleted)
            put("inAppSoundsEnabled", settings.inAppSoundsEnabled)
            put("restTimerVolume", settings.restTimerVolume.toDouble())
            put("timedAlarmVolume", settings.timedAlarmVolume.toDouble())
            put("reminderVolume", settings.reminderVolume.toDouble())
            put("githubRepo", settings.githubRepo)
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

        // Skips
        val skipArr = JSONArray()
        skips.forEach { sk ->
            skipArr.put(JSONObject().apply {
                put("id", sk.id)
                put("planId", sk.planId)
                put("originalDate", sk.originalDate)
                if (sk.newDate != null) put("newDate", sk.newDate)
                put("createdAt", sk.createdAt)
            })
        }
        root.put("skips", skipArr)

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

    private fun optNullableInt(obj: JSONObject, key: String): Int? =
        if (obj.has(key) && !obj.isNull(key)) obj.getInt(key) else null

    private fun optNullableDouble(obj: JSONObject, key: String): Double? =
        if (obj.has(key) && !obj.isNull(key)) obj.getDouble(key) else null

    private fun optNullableString(obj: JSONObject, key: String): String? =
        if (obj.has(key) && !obj.isNull(key)) obj.getString(key) else null

    fun validateAndParseBackup(jsonString: String): Result<BackupContainer> {
        return runCatching {
            val cleanJson = jsonString.trim().removePrefix("\uFEFF")
            val root = JSONObject(cleanJson)
            if (!root.has("app") || root.getString("app") != "GYM_SMART_COACH") {
                throw IllegalArgumentException("فایل پشتیبان نامعتبر است.")
            }

            val setObj = root.optJSONObject("settings")
            val settings = if (setObj != null) {
                UserSettings(
                    userName = setObj.optString("userName", ""),
                    profilePhotoUri = setObj.optString("profilePhotoUri", ""),
                    gender = setObj.optString("gender", ""),
                    age = optNullableInt(setObj, "age"),
                    heightCm = optNullableDouble(setObj, "heightCm"),
                    weightKg = optNullableDouble(setObj, "weightKg"),
                    activityLevel = setObj.optString("activityLevel", "MODERATE"),
                    fitnessGoal = setObj.optString("fitnessGoal", "BULK"),
                    mealsCount = setObj.optInt("mealsCount", 4),
                    waterGoalMl = setObj.optInt("waterGoalMl", 2500),
                    weightUnit = setObj.optString("weightUnit", "kg"),
                    appearanceMode = setObj.optString("appearanceMode", "SYSTEM"),
                    accentPalette = when (val rawPal = setObj.optString("accentPalette", "SPORT_RED").uppercase()) {
                        "SUNSET" -> "BRONZE_MEDAL"
                        "CYBER" -> "PLATINUM_STEEL"
                        else -> rawPal
                    },
                    theme = setObj.optString("theme", "DARK"),
                    glassMaterial = setObj.optString("glassMaterial", "CLEAR"),
                    glassWallpaper = setObj.optString("glassWallpaper", "BOTANICAL"),
                    glassLists = setObj.optBoolean("glassLists", true),
                    shrinkTabBar = setObj.optBoolean("shrinkTabBar", true),
                    motionHighlights = setObj.optBoolean("motionHighlights", false),
                    reduceTransparency = setObj.optBoolean("reduceTransparency", false),
                    workoutReminderEnabled = setObj.optBoolean("workoutReminderEnabled", false),
                    workoutReminderTime = setObj.optString("workoutReminderTime", "18:00"),
                    waterReminderEnabled = setObj.optBoolean("waterReminderEnabled", false),
                    waterReminderTime = setObj.optString("waterReminderTime", "10:00"),
                    supplementReminderEnabled = setObj.optBoolean("supplementReminderEnabled", false),
                    supplementReminderTime = setObj.optString("supplementReminderTime", "09:00"),
                    onboardingCompleted = setObj.optBoolean("onboardingCompleted", true),
                    inAppSoundsEnabled = setObj.optBoolean("inAppSoundsEnabled", true),
                    restTimerVolume = setObj.optDouble("restTimerVolume", 0.8).toFloat(),
                    timedAlarmVolume = setObj.optDouble("timedAlarmVolume", 0.9).toFloat(),
                    reminderVolume = setObj.optDouble("reminderVolume", 0.8).toFloat(),
                    githubRepo = setObj.optString("githubRepo", "")
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
                        repsMax = optNullableInt(o, "repsMax"),
                        weight = o.optDouble("weight", 0.0),
                        notes = o.optString("notes", ""),
                        supersetGroup = optNullableString(o, "supersetGroup"),
                        isTimedExercise = o.optBoolean("isTimedExercise", false),
                        customMetrics = optNullableString(o, "customMetrics")
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
                        customData = optNullableString(o, "customData")
                    )
                )
            }

            // Skips
            val skips = mutableListOf<WorkoutPlanSkip>()
            val skArr = root.optJSONArray("skips") ?: JSONArray()
            for (i in 0 until skArr.length()) {
                val o = skArr.getJSONObject(i)
                skips.add(
                    WorkoutPlanSkip(
                        id = o.optLong("id", 0),
                        planId = o.getLong("planId"),
                        originalDate = o.getString("originalDate"),
                        newDate = if (o.has("newDate") && !o.isNull("newDate")) o.getString("newDate") else null,
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
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
                        heightCm = optNullableDouble(o, "heightCm"),
                        weightKg = optNullableDouble(o, "weightKg"),
                        neckCm = optNullableDouble(o, "neckCm"),
                        waistCm = optNullableDouble(o, "waistCm"),
                        chestCm = optNullableDouble(o, "chestCm"),
                        armsCm = optNullableDouble(o, "armsCm"),
                        thighCm = optNullableDouble(o, "thighCm"),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }

            BackupContainer(
                settings = settings,
                plans = plans,
                exercises = exercises,
                histories = histories,
                skips = skips,
                supplements = supplements,
                supplementHistories = supHistories,
                nutritionLogs = nutritionLogs,
                measurements = measurements
            )
        }
    }

    const val BACKUP_FILENAME = "gym_pro_backup.json"
    private const val BACKUP_FOLDER_NAME = "GymPro_Backups"

    fun hasLegacyStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true // Android 10+ uses MediaStore / SAF with zero storage permissions
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getAppBackupDirectory(context: Context): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(baseDir, BACKUP_FOLDER_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getAppBackupFile(context: Context): File {
        return File(getAppBackupDirectory(context), BACKUP_FILENAME)
    }

    fun listBackupFilesSafely(context: Context): List<File> {
        return try {
            val result = mutableListOf<File>()
            val appDir = getAppBackupDirectory(context)
            appDir.listFiles { f -> f.isFile && f.extension.equals("json", ignoreCase = true) }
                ?.let { result.addAll(it) }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && hasLegacyStoragePermission(context)) {
                @Suppress("DEPRECATION")
                val pubDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    BACKUP_FOLDER_NAME
                )
                if (pubDir.exists()) {
                    pubDir.listFiles { f -> f.isFile && f.extension.equals("json", ignoreCase = true) }
                        ?.let { files ->
                            files.forEach { pf ->
                                if (result.none { it.absolutePath == pf.absolutePath }) {
                                    result.add(pf)
                                }
                            }
                        }
                }
            }
            result.sortedByDescending { it.lastModified() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveBackupToFile(context: Context, backupJson: String): File {
        return saveBackupWithDetails(context, backupJson).appFile
    }

    fun saveBackupWithDetails(context: Context, backupJson: String): BackupSaveResult {
        val appFile = getAppBackupFile(context)
        appFile.writeText(backupJson)

        val exportedToDownloads = saveToPublicDownloadsFolder(context, backupJson)
        val displayLocation = if (exportedToDownloads) {
            "Download/$BACKUP_FOLDER_NAME/$BACKUP_FILENAME"
        } else {
            "پوشه اختصاصی برنامه ($BACKUP_FOLDER_NAME/$BACKUP_FILENAME)"
        }

        return BackupSaveResult(
            appFile = appFile,
            exportedToDownloads = exportedToDownloads,
            displayLocation = displayLocation
        )
    }

    private fun saveToPublicDownloadsFolder(context: Context, backupJson: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$BACKUP_FOLDER_NAME/"

                // Delete previous gym_pro_backup*.json in MediaStore owned by our app to avoid duplicates
                try {
                    resolver.delete(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
                        arrayOf(BACKUP_FILENAME, relativePath)
                    )
                } catch (_: Exception) {
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, BACKUP_FILENAME)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return false

                try {
                    val outStream = resolver.openOutputStream(uri)
                    if (outStream == null) {
                        resolver.delete(uri, null, null)
                        return false
                    }
                    outStream.use { out ->
                        out.write(backupJson.toByteArray(Charsets.UTF_8))
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    true
                } catch (e: Exception) {
                    try {
                        resolver.delete(uri, null, null)
                    } catch (_: Exception) {
                    }
                    throw e
                }
            } else {
                if (!hasLegacyStoragePermission(context)) return false
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, BACKUP_FOLDER_NAME)
                if (!targetDir.exists()) targetDir.mkdirs()
                val targetFile = File(targetDir, BACKUP_FILENAME)
                targetFile.writeText(backupJson)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun writeBackupToUri(context: Context, uri: Uri, backupJson: String): Result<Unit> {
        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(backupJson.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("امکان نوشتن در مسیر انتخاب‌شده وجود ندارد.")
        }
    }

    fun getShareUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
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
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: throw IllegalArgumentException("امکان باز کردن فایل انتخاب‌شده وجود ندارد.")
            validateAndParseBackup(json).getOrThrow()
        }
    }
}
