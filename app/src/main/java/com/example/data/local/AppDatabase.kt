package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WorkoutPlan::class,
        WorkoutExercise::class,
        WorkoutHistory::class,
        WorkoutSetLog::class,
        WorkoutPlanSkip::class,
        Supplement::class,
        SupplementIntakeHistory::class,
        NutritionLog::class,
        BodyMeasurement::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao
    abstract fun supplementDao(): SupplementDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun measurementDao(): MeasurementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Placeholder for future version migrations without destructive fallback
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Non-destructive migration rules for future schemas
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gym_smart_coach.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    // Note: Specifically adhering to requirement: no fallbackToDestructiveMigration
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
