package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.WorkoutExercise
import com.example.data.local.WorkoutPlan
import com.example.util.BackupHelper
import com.example.util.PlanShareHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GYM", appName)
    }

    @Test
    fun `test plan share export and import`() {
        val plan = WorkoutPlan(name = "برنامه سینه و بازو", notes = "فشار حداکثری", scheduledDay = "شنبه,دوشنبه")
        val exercises = listOf(
            WorkoutExercise(planId = 1, name = "پرس سینه هالتر", sets = 4, reps = 8, weight = 70.0),
            WorkoutExercise(planId = 1, name = "جلو بازو دمبل لاری", sets = 3, reps = 10, weight = 16.0, supersetGroup = "A")
        )

        val jsonString = PlanShareHelper.exportPlanToJson(plan, exercises)
        assertTrue(jsonString.contains("برنامه سینه و بازو"))
        assertTrue(jsonString.contains("پرس سینه هالتر"))

        val parseResult = PlanShareHelper.parsePlanFromJson(jsonString)
        assertTrue(parseResult.isSuccess)
        val imported = parseResult.getOrNull()!!
        assertEquals("برنامه سینه و بازو", imported.name)
        assertEquals(2, imported.exercises.size)
        assertEquals("پرس سینه هالتر", imported.exercises[0].name)
        assertEquals("A", imported.exercises[1].supersetGroup)
    }

    @Test
    fun `test backup validation`() {
        val invalidJson = """{"foo": "bar"}"""
        val result = BackupHelper.validateAndParseBackup(invalidJson)
        assertTrue(result.isFailure)
    }
}
