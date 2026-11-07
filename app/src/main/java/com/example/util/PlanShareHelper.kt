package com.example.util

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

    fun exportPlanToJson(plan: WorkoutPlan, exercises: List<WorkoutExercise>): String {
        val json = JSONObject()
        json.put("app", "GYM_APP")
        json.put("version", 1)
        json.put("planName", plan.name)
        json.put("notes", plan.notes)
        json.put("scheduledDay", plan.scheduledDay)

        val exerciseArray = JSONArray()
        exercises.forEach { ex ->
            val exObj = JSONObject()
            exObj.put("name", ex.name)
            exObj.put("sets", ex.sets)
            exObj.put("reps", ex.reps)
            if (ex.repsMax != null) exObj.put("repsMax", ex.repsMax)
            exObj.put("weight", ex.weight)
            exObj.put("notes", ex.notes)
            if (ex.supersetGroup != null) exObj.put("supersetGroup", ex.supersetGroup)
            exObj.put("isTimedExercise", ex.isTimedExercise)
            if (ex.customMetrics != null) exObj.put("customMetrics", ex.customMetrics)
            exerciseArray.put(exObj)
        }
        json.put("exercises", exerciseArray)

        return json.toString(2)
    }

    fun parsePlanFromJson(jsonString: String): Result<PlanExportData> {
        return runCatching {
            val json = JSONObject(jsonString)
            val name = json.getString("planName")
            val notes = json.optString("notes", "")
            val scheduledDay = json.optString("scheduledDay", "")

            val exercises = mutableListOf<ExerciseExportData>()
            val arr = json.optJSONArray("exercises") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                exercises.add(
                    ExerciseExportData(
                        name = item.getString("name"),
                        sets = item.optInt("sets", 3),
                        reps = item.optInt("reps", 10),
                        repsMax = if (item.has("repsMax")) item.getInt("repsMax") else null,
                        weight = item.optDouble("weight", 0.0),
                        notes = item.optString("notes", ""),
                        supersetGroup = if (item.has("supersetGroup")) item.getString("supersetGroup") else null,
                        isTimedExercise = item.optBoolean("isTimedExercise", false),
                        customMetrics = if (item.has("customMetrics")) item.getString("customMetrics") else null
                    )
                )
            }
            PlanExportData(name, notes, scheduledDay, exercises)
        }
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
