package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.example.data.local.WorkoutExercise
import com.example.data.local.WorkoutPlan
import org.json.JSONArray
import org.json.JSONObject

data class PlanExportData(
    val name: String,
    val notes: String,
    val scheduledDay: String,
    val exercises: List<ExerciseExportData>
)

data class ExerciseExportData(
    val name: String,
    val sets: Int,
    val reps: Int,
    val repsMax: Int?,
    val weight: Double,
    val notes: String,
    val supersetGroup: String?,
    val isTimedExercise: Boolean,
    val customMetrics: String?
)

object PlanShareHelper {

    /**
     * Exports a plan in the user's weekly routine JSON schema if applicable, or single plan format.
     */
    fun exportPlanToJson(plan: WorkoutPlan, exercises: List<WorkoutExercise>): String {
        val json = JSONObject()
        json.put("program_name", plan.name)
        json.put("description", plan.notes)

        val daysList = if (plan.scheduledDay.isNotBlank()) {
            plan.scheduledDay.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else {
            listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
        }

        val daysArray = JSONArray()
        daysList.forEach { daysArray.put(it) }
        json.put("training_days", daysArray)

        val exercisesArray = JSONArray()
        val allWeekDays = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
        
        // Populate standard weekly structure
        allWeekDays.forEach { dayName ->
            val dayObj = JSONObject()
            dayObj.put("day", dayName)
            dayObj.put("day_name", if (daysList.contains(dayName)) plan.name else "")

            val workoutsArray = JSONArray()
            if (daysList.contains(dayName) || daysList.isEmpty()) {
                exercises.forEach { ex ->
                    val wObj = JSONObject()
                    wObj.put("name", ex.name)
                    wObj.put("sets", ex.sets)
                    wObj.put("reps", ex.reps)
                    if (ex.repsMax != null) wObj.put("reps_max", ex.repsMax)
                    wObj.put("weight", ex.weight)
                    wObj.put("notes", ex.notes)
                    wObj.put("is_timed", ex.isTimedExercise)
                    workoutsArray.put(wObj)
                }
            }
            dayObj.put("workouts", workoutsArray)
            exercisesArray.put(dayObj)
        }

        json.put("exercises", exercisesArray)
        return json.toString(2)
    }

    /**
     * Parses plans from JSON. Supports both:
     * 1. Weekly routine format:
     *    {
     *      "program_name": "...",
     *      "description": "...",
     *      "training_days": ["شنبه", ...],
     *      "exercises": [ { "day": "شنبه", "day_name": "...", "workouts": [...] } ]
     *    }
     * 2. Flat plan format:
     *    { "planName": "...", "exercises": [...] }
     */
    fun parsePlansFromJson(jsonString: String): Result<List<PlanExportData>> {
        return runCatching {
            val json = JSONObject(jsonString)
            val programName = json.optString("program_name", json.optString("planName", "برنامه تمرینی"))
            val description = json.optString("description", json.optString("notes", ""))

            val trainingDaysList = mutableListOf<String>()
            val trainingDaysArr = json.optJSONArray("training_days")
            if (trainingDaysArr != null) {
                for (i in 0 until trainingDaysArr.length()) {
                    trainingDaysList.add(trainingDaysArr.optString(i))
                }
            }

            val exercisesArr = json.optJSONArray("exercises") ?: JSONArray()
            val resultPlans = mutableListOf<PlanExportData>()

            // Check if items in `exercises` are daily schedule groups with "workouts"
            var isWeeklyScheduleFormat = false
            for (i in 0 until exercisesArr.length()) {
                val item = exercisesArr.optJSONObject(i)
                if (item != null && (item.has("workouts") || item.has("day"))) {
                    isWeeklyScheduleFormat = true
                    break
                }
            }

            if (isWeeklyScheduleFormat) {
                for (i in 0 until exercisesArr.length()) {
                    val dayObj = exercisesArr.optJSONObject(i) ?: continue
                    val day = dayObj.optString("day", "")
                    val dayName = dayObj.optString("day_name", "")
                    val workoutsArr = dayObj.optJSONArray("workouts") ?: JSONArray()

                    if (workoutsArr.length() == 0) continue

                    val exercisesList = mutableListOf<ExerciseExportData>()
                    for (j in 0 until workoutsArr.length()) {
                        val wItem = workoutsArr.opt(j)
                        if (wItem is JSONObject) {
                            exercisesList.add(
                                ExerciseExportData(
                                    name = wItem.optString("name", wItem.optString("workout_name", "حرکت")),
                                    sets = wItem.optInt("sets", 3),
                                    reps = wItem.optInt("reps", 10),
                                    repsMax = if (wItem.has("reps_max")) wItem.optInt("reps_max") else if (wItem.has("repsMax")) wItem.optInt("repsMax") else null,
                                    weight = wItem.optDouble("weight", 0.0),
                                    notes = wItem.optString("notes", ""),
                                    supersetGroup = if (wItem.has("supersetGroup")) wItem.getString("supersetGroup") else null,
                                    isTimedExercise = wItem.optBoolean("is_timed", wItem.optBoolean("isTimedExercise", false)),
                                    customMetrics = if (wItem.has("customMetrics")) wItem.getString("customMetrics") else null
                                )
                            )
                        } else if (wItem is String && wItem.isNotBlank()) {
                            exercisesList.add(
                                ExerciseExportData(
                                    name = wItem,
                                    sets = 3,
                                    reps = 10,
                                    repsMax = null,
                                    weight = 0.0,
                                    notes = "",
                                    supersetGroup = null,
                                    isTimedExercise = false,
                                    customMetrics = null
                                )
                            )
                        }
                    }

                    if (exercisesList.isNotEmpty()) {
                        val planName = when {
                            dayName.isNotBlank() && programName.isNotBlank() && !dayName.contains(programName) -> "$programName - $dayName"
                            dayName.isNotBlank() -> dayName
                            programName.isNotBlank() && day.isNotBlank() -> "$programName ($day)"
                            day.isNotBlank() -> "تمرین $day"
                            else -> programName
                        }

                        resultPlans.add(
                            PlanExportData(
                                name = planName,
                                notes = description,
                                scheduledDay = day,
                                exercises = exercisesList
                            )
                        )
                    }
                }
            }

            // Fallback to flat plan format if not parsed as weekly schedule
            if (resultPlans.isEmpty()) {
                val flatExercises = mutableListOf<ExerciseExportData>()
                for (i in 0 until exercisesArr.length()) {
                    val item = exercisesArr.optJSONObject(i) ?: continue
                    val name = item.optString("name", item.optString("workout_name", ""))
                    if (name.isBlank()) continue

                    flatExercises.add(
                        ExerciseExportData(
                            name = name,
                            sets = item.optInt("sets", 3),
                            reps = item.optInt("reps", 10),
                            repsMax = if (item.has("reps_max")) item.optInt("reps_max") else if (item.has("repsMax")) item.optInt("repsMax") else null,
                            weight = item.optDouble("weight", 0.0),
                            notes = item.optString("notes", ""),
                            supersetGroup = if (item.has("supersetGroup")) item.getString("supersetGroup") else null,
                            isTimedExercise = item.optBoolean("is_timed", item.optBoolean("isTimedExercise", false)),
                            customMetrics = if (item.has("customMetrics")) item.getString("customMetrics") else null
                        )
                    )
                }

                val scheduledDay = if (trainingDaysList.isNotEmpty()) {
                    trainingDaysList.joinToString(",")
                } else {
                    json.optString("scheduledDay", "")
                }

                resultPlans.add(
                    PlanExportData(
                        name = programName,
                        notes = description,
                        scheduledDay = scheduledDay,
                        exercises = flatExercises
                    )
                )
            }

            resultPlans
        }
    }

    /**
     * Backward-compatible single plan parser.
     */
    fun parsePlanFromJson(jsonString: String): Result<PlanExportData> {
        return parsePlansFromJson(jsonString).mapCatching { list ->
            list.firstOrNull() ?: throw IllegalArgumentException("هیچ برنامه‌ای در متن JSON یافت نشد")
        }
    }

    /**
     * Formats a plan and its exercises into clean, readable Persian text for clipboard sharing.
     */
    fun formatPlanAsShareableText(plan: WorkoutPlan, exercises: List<WorkoutExercise>): String {
        val sb = StringBuilder()
        sb.append("📋 برنامه تمرینی: ${plan.name}\n")
        if (plan.scheduledDay.isNotBlank()) {
            sb.append("🗓 روزهای اجرا: ${plan.scheduledDay}\n")
        }
        if (plan.notes.isNotBlank()) {
            sb.append("📝 توضیحات: ${plan.notes}\n")
        }
        sb.append("\n🏋️ حرکات (${PersianNumberHelper.toPersianDigits(exercises.size)} حرکت):\n")
        exercises.forEachIndexed { index, ex ->
            val numStr = PersianNumberHelper.toPersianDigits(index + 1)
            val repsStr = if (ex.isTimedExercise) {
                "${PersianNumberHelper.toPersianDigits(ex.reps)} ثانیه تایمر"
            } else if (ex.repsMax != null && ex.repsMax > ex.reps) {
                "${PersianNumberHelper.toPersianDigits(ex.reps)} الی ${PersianNumberHelper.toPersianDigits(ex.repsMax)} تکرار"
            } else {
                "${PersianNumberHelper.toPersianDigits(ex.reps)} تکرار"
            }
            val weightStr = if (ex.weight > 0) " (وزن: ${PersianNumberHelper.toPersianDigits(ex.weight)}kg)" else ""
            val setsStr = "${PersianNumberHelper.toPersianDigits(ex.sets)} ست"
            sb.append("$numStr. ${ex.name} — $setsStr × $repsStr$weightStr\n")
            if (ex.notes.isNotBlank()) {
                sb.append("   نکته: ${ex.notes}\n")
            }
        }
        sb.append("\nاپلیکیشن GYM")
        return sb.toString()
    }

    /**
     * Copies text directly to Android clipboard.
     */
    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    fun sharePlan(context: Context, plan: WorkoutPlan, exercises: List<WorkoutExercise>) {
        val json = exportPlanToJson(plan, exercises)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "برنامه بدنسازی: ${plan.name}")
            putExtra(Intent.EXTRA_TEXT, json)
        }
        val chooser = Intent.createChooser(shareIntent, "اشتراک‌گذاری برنامه‌ی تمرینی")
        context.startActivity(chooser)
    }
}
