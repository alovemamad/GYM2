package com.example

import com.example.data.local.WorkoutExercise
import com.example.data.local.WorkoutSetLog
import com.example.receiver.ReminderReceiver
import com.example.ui.navigation.Screen
import com.example.ui.theme.LiquidPalettes
import com.example.ui.theme.createLiquidColorScheme
import com.example.ui.viewmodel.ActiveWorkoutRestoreHelper
import com.example.util.AppUpdateHelper
import com.example.util.JalaliCalendar
import com.example.util.PersianNumberHelper
import com.example.util.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {

    @Test
    fun testPersianDigitsConversion() {
        val input = "1234567890"
        val expected = "۱۲۳۴۵۶۷۸۹۰"
        assertEquals(expected, PersianNumberHelper.toPersianDigits(input))
        assertEquals(input, PersianNumberHelper.toEnglishDigits(expected))
    }

    @Test
    fun testJalaliDateConversion() {
        // Test known conversion: 2026-03-21 is 1405-01-01
        val jDate = JalaliCalendar.gregorianToJalali(2026, 3, 21)
        assertEquals(1405, jDate.year)
        assertEquals(1, jDate.month)
        assertEquals(1, jDate.day)

        // Convert back to Gregorian
        val gDate = JalaliCalendar.jalaliToGregorian(1405, 1, 1)
        assertEquals("2026-03-21", gDate)
    }

    @Test
    fun testWeekdayPersian() {
        assertEquals("شنبه", JalaliCalendar.WEEKDAY_NAMES[0])
        assertEquals("جمعه", JalaliCalendar.WEEKDAY_NAMES[6])
    }

    @Test
    fun testBottomNavItemsNotNull() {
        val items = Screen.bottomNavItems
        assertEquals(5, items.size)
        items.forEach { screen ->
            assertNotNull(screen)
            assertNotNull(screen.route)
            assertTrue(screen.route.isNotBlank())
            assertNotNull(screen.title)
            assertNotNull(screen.selectedIcon)
            assertNotNull(screen.unselectedIcon)
        }
    }

    @Test
    fun testAppUpdateVersionComparison() {
        assertTrue(AppUpdateHelper.isNewerVersion("v1.0.1", "1.0.0"))
        assertTrue(AppUpdateHelper.isNewerVersion("1.0.10", "1.0.2"))
        assertTrue(AppUpdateHelper.isNewerVersion("2.0", "1.9.9"))
        assertFalse(AppUpdateHelper.isNewerVersion("v1.0.0", "1.0.0"))
        assertFalse(AppUpdateHelper.isNewerVersion("1.0.2", "1.0.5"))
    }

    @Test
    fun testAppUpdateRepoNormalization() {
        assertEquals("alovemamad/GYM2", AppUpdateHelper.getEffectiveRepo(""))
        assertEquals("alovemamad/GYM2", AppUpdateHelper.getEffectiveRepo("git clone https://wrong"))
        assertEquals("alovemamad/GYM2", AppUpdateHelper.getEffectiveRepo("gh repo clone alovemamad/GYM2"))
        assertEquals("alovemamad/GYM2", AppUpdateHelper.normalizeRepoSlug("alovemamad/GYM2"))
        assertEquals("alovemamad/GYM2", AppUpdateHelper.normalizeRepoSlug("gh repo clone alovemamad/GYM2"))
        assertEquals("alovemamad/GYM2", AppUpdateHelper.normalizeRepoSlug("git clone https://github.com/alovemamad/GYM2.git"))
        assertEquals("alovemamad/GYM2", AppUpdateHelper.normalizeRepoSlug("git@github.com:alovemamad/GYM2.git"))
        assertEquals("alovemamad/GYM2", AppUpdateHelper.normalizeRepoSlug("https://github.com/alovemamad/GYM2.git"))
        assertEquals("alovemamad/GYM2", AppUpdateHelper.normalizeRepoSlug("https://github.com/alovemamad/GYM2/"))
        assertNull(AppUpdateHelper.normalizeRepoSlug("invalid_repo"))
    }

    @Test
    fun testActiveWorkoutSetRestorationPreservesCompletedAndExtraSets() {
        val exercises = listOf(
            WorkoutExercise(id = 1, planId = 10, name = "پرس سینه", sets = 3, reps = 10, weight = 60.0),
            WorkoutExercise(id = 2, planId = 10, name = "جلو بازو", sets = 2, reps = 12, weight = 15.0)
        )
        val savedLogs = listOf(
            WorkoutSetLog(
                id = 51, date = "2026-11-17", planId = 10, planName = "سینه",
                exerciseName = "پرس سینه", setNumber = 1, weight = 65.0, reps = 10,
                completed = true, timestamp = 1000L, customMetricValues = "وزن (kg):65.0;تکرار:10"
            ),
            WorkoutSetLog(
                id = 52, date = "2026-11-17", planId = 10, planName = "سینه",
                exerciseName = "پرس سینه", setNumber = 4, weight = 70.0, reps = 8,
                completed = false, timestamp = 2000L, customMetricValues = ""
            )
        )

        val restored = ActiveWorkoutRestoreHelper.buildExerciseStatesWithLogs(exercises, savedLogs)
        assertEquals(2, restored.size)
        // Extra 4th set must be preserved
        assertEquals(4, restored[0].sets.size)
        assertTrue(restored[0].sets[0].completed)
        assertEquals(65.0, restored[0].sets[0].weight, 0.001)
        assertEquals(51L, restored[0].sets[0].setLogId)
        assertFalse(restored[0].sets[3].completed)
        assertEquals(70.0, restored[0].sets[3].weight, 0.001)
        // Unstarted exercise stays at template defaults
        assertEquals(2, restored[1].sets.size)
        assertEquals(15.0, restored[1].sets[0].weight, 0.001)
    }

    @Test
    fun testStaleDelayedAlarmGuard() {
        val scheduled = 1_000_000_000L
        assertFalse(ReminderReceiver.shouldDropStaleAlarm(scheduled, scheduled + 60_000L))
        assertFalse(ReminderReceiver.shouldDropStaleAlarm(scheduled, scheduled + 2L * 3600L * 1000L))
        assertTrue(ReminderReceiver.shouldDropStaleAlarm(scheduled, scheduled + 4L * 3600L * 1000L))
        assertFalse(ReminderReceiver.shouldDropStaleAlarm(0L, scheduled + 10L * 3600L * 1000L))
    }

    @Test
    fun testComputeNextDailyTriggerRollover() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 10, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis
        val sameDayTrigger = ReminderScheduler.computeNextDailyTriggerMillis("18:30", now)!!
        val nextDayTrigger = ReminderScheduler.computeNextDailyTriggerMillis("09:00", now)!!

        val sameCal = Calendar.getInstance().apply { timeInMillis = sameDayTrigger }
        val nextCal = Calendar.getInstance().apply { timeInMillis = nextDayTrigger }
        assertEquals(10, sameCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, sameCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(11, nextCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, nextCal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun testDayAndNightPaletteColorSchemesDistinctAndTinted() {
        val lightOcean = createLiquidColorScheme(LiquidPalettes.OCEAN, isDark = false)
        val lightRed = createLiquidColorScheme(LiquidPalettes.SPORT_RED, isDark = false)
        val darkOcean = createLiquidColorScheme(LiquidPalettes.OCEAN, isDark = true)

        assertNotEquals(lightOcean.background, lightRed.background)
        assertNotEquals(lightOcean.surface, lightRed.surface)
        assertNotEquals(lightOcean.background, darkOcean.background)
    }
}
