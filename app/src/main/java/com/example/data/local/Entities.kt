package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_plans")
data class WorkoutPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val notes: String = "",
    val scheduledDay: String = "", // e.g. "شنبه,دوشنبه,چهارشنبه"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "workout_exercises")
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val planId: Long,
    val name: String,
    val sets: Int = 3,
    val reps: Int = 10,
    val repsMax: Int? = null, // For rep range e.g. 8-12
    val weight: Double = 0.0,
    val notes: String = "",
    val supersetGroup: String? = null, // e.g. "A", "B"
    val isTimedExercise: Boolean = false,
    val customMetrics: String? = null // Comma-separated or JSON custom metrics e.g. "وزن,تکرار,زمان"
)

@Entity(tableName = "workout_histories")
data class WorkoutHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val planName: String,
    val exerciseName: String,
    val sets: Int,
    val reps: Int,
    val weight: Double,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0,
    val totalRestSecondsUsed: Long = 0,
    val customData: String? = null
)

@Entity(tableName = "workout_set_logs")
data class WorkoutSetLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val planId: Long,
    val planName: String,
    val exerciseName: String,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val completed: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val restSecondsUsed: Long = 0,
    val customMetricValues: String? = null // e.g. "weight:50,reps:10,time:45"
)

@Entity(tableName = "workout_plan_skips")
data class WorkoutPlanSkip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val planId: Long,
    val originalDate: String, // YYYY-MM-DD
    val newDate: String? = null, // null means completely skipped/rejected
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "supplements")
data class Supplement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val reminderTime: String = "09:00", // HH:mm
    val notes: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "supplement_intake_histories")
data class SupplementIntakeHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val supplementId: Long,
    val supplementName: String,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:mm
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "nutrition_logs")
data class NutritionLog(
    @PrimaryKey
    val date: String, // YYYY-MM-DD
    val breakfast: String = "",
    val lunch: String = "",
    val dinner: String = "",
    val snacks: String = "",
    val dailyNotes: String = "",
    val waterIntakeMl: Int = 0
)

@Entity(tableName = "body_measurements")
data class BodyMeasurement(
    @PrimaryKey
    val date: String, // YYYY-MM-DD
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val neckCm: Double? = null,
    val waistCm: Double? = null,
    val chestCm: Double? = null,
    val armsCm: Double? = null,
    val thighCm: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
)
