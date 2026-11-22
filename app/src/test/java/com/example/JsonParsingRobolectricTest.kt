package com.example

import com.example.util.BackupHelper
import com.example.util.PlanShareHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JsonParsingRobolectricTest {

    @Test
    fun backupParsing_handlesExplicitJsonNullsInNullableFields() {
        val json = """
            {
              "app": "GYM_SMART_COACH",
              "schemaVersion": 2,
              "settings": {
                "userName": "Ali",
                "age": null,
                "heightCm": null,
                "weightKg": 80.5
              },
              "plans": [
                { "id": 1, "name": "Push", "notes": "", "scheduledDay": "شنبه", "createdAt": 1000 }
              ],
              "exercises": [
                {
                  "id": 10,
                  "planId": 1,
                  "name": "Bench Press",
                  "sets": 4,
                  "reps": 8,
                  "repsMax": null,
                  "weight": 60.0,
                  "supersetGroup": null,
                  "isTimedExercise": false,
                  "customMetrics": null
                }
              ],
              "histories": [
                {
                  "id": 20,
                  "date": "2026-05-01",
                  "planName": "Push",
                  "exerciseName": "Bench Press",
                  "sets": 4,
                  "reps": 8,
                  "weight": 60.0,
                  "customData": null
                }
              ],
              "skips": [
                { "id": 30, "planId": 1, "originalDate": "2026-05-02", "newDate": null }
              ],
              "measurements": [
                { "date": "2026-05-01", "heightCm": null, "weightKg": 80.5, "waistCm": null }
              ]
            }
        """.trimIndent()

        val result = BackupHelper.validateAndParseBackup(json)
        assertTrue(result.isSuccess)
        val data = result.getOrThrow()

        assertNull(data.settings.age)
        assertNull(data.settings.heightCm)
        assertEquals(80.5, data.settings.weightKg!!, 0.001)

        assertEquals(1, data.exercises.size)
        assertNull(data.exercises[0].repsMax)
        assertNull(data.exercises[0].supersetGroup)
        assertNull(data.exercises[0].customMetrics)

        assertEquals(1, data.histories.size)
        assertNull(data.histories[0].customData)

        assertEquals(1, data.skips.size)
        assertNull(data.skips[0].newDate)

        assertEquals(1, data.measurements.size)
        assertNull(data.measurements[0].heightCm)
        assertNull(data.measurements[0].waistCm)
        assertEquals(80.5, data.measurements[0].weightKg!!, 0.001)
    }

    @Test
    fun planShareParsing_handlesExplicitJsonNullsInBothFormats() {
        val weeklyJson = """
            {
              "program_name": "Upper Lower",
              "exercises": [
                {
                  "day": "شنبه",
                  "day_name": "Upper",
                  "workouts": [
                    {
                      "name": "Pull Up",
                      "sets": 3,
                      "reps": 10,
                      "reps_max": null,
                      "supersetGroup": null,
                      "customMetrics": null
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val weeklyRes = PlanShareHelper.parsePlansFromJson(weeklyJson)
        assertTrue(weeklyRes.isSuccess)
        val weeklyEx = weeklyRes.getOrThrow().first().exercises.first()
        assertNull(weeklyEx.repsMax)
        assertNull(weeklyEx.supersetGroup)
        assertNull(weeklyEx.customMetrics)

        val flatJson = """
            {
              "planName": "Full Body",
              "exercises": [
                {
                  "name": "Squat",
                  "sets": 4,
                  "reps": 6,
                  "repsMax": null,
                  "supersetGroup": null,
                  "customMetrics": null
                }
              ]
            }
        """.trimIndent()

        val flatRes = PlanShareHelper.parsePlansFromJson(flatJson)
        assertTrue(flatRes.isSuccess)
        val flatEx = flatRes.getOrThrow().first().exercises.first()
        assertNull(flatEx.repsMax)
        assertNull(flatEx.supersetGroup)
        assertNull(flatEx.customMetrics)
    }

    @Test
    fun backupParsing_roundTripsGlassSettingsAndMigratesLegacyPalettes() {
        val originalSettings = com.example.data.local.UserSettings(
            userName = "Sina",
            appearanceMode = "LIGHT",
            accentPalette = "PLATINUM_STEEL",
            glassMaterial = "TINTED",
            glassWallpaper = "LAGOON",
            glassLists = false,
            shrinkTabBar = false,
            motionHighlights = true,
            reduceTransparency = true
        )

        val json = BackupHelper.createBackupJson(
            settings = originalSettings,
            plans = emptyList(),
            exercises = emptyList(),
            histories = emptyList(),
            skips = emptyList(),
            supplements = emptyList(),
            supplementHistories = emptyList(),
            nutritionLogs = emptyList(),
            measurements = emptyList()
        )

        val parsed = BackupHelper.validateAndParseBackup(json)
        assertTrue(parsed.isSuccess)
        val restored = parsed.getOrThrow().settings
        assertEquals("PLATINUM_STEEL", restored.accentPalette)
        assertEquals("TINTED", restored.glassMaterial)
        assertEquals("LAGOON", restored.glassWallpaper)
        assertEquals(false, restored.glassLists)
        assertEquals(false, restored.shrinkTabBar)
        assertEquals(true, restored.motionHighlights)
        assertEquals(true, restored.reduceTransparency)

        val legacyJson = json.replace("\"PLATINUM_STEEL\"", "\"CYBER\"")
        val legacyParsed = BackupHelper.validateAndParseBackup(legacyJson).getOrThrow().settings
        assertEquals("PLATINUM_STEEL", legacyParsed.accentPalette)
    }
}
