package com.example

import com.example.ui.navigation.Screen
import com.example.util.JalaliCalendar
import com.example.util.PersianNumberHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
