package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Exact Jalali (Solar Hijri / شمسی) Calendar conversion and helper utilities.
 */
data class JalaliDate(
    val year: Int,
    val month: Int,
    val day: Int
) {
    fun toFormattedString(withMonthName: Boolean = false): String {
        return if (withMonthName) {
            val monthName = JalaliCalendar.MONTH_NAMES.getOrNull(month - 1) ?: ""
            PersianNumberHelper.toPersianDigits("$day $monthName $year")
        } else {
            PersianNumberHelper.toPersianDigits(String.format(Locale.US, "%04d/%02d/%02d", year, month, day))
        }
    }
}

object JalaliCalendar {

    val MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    val WEEKDAY_NAMES = listOf(
        "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه"
    )

    fun getTodayGregorian(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun getTodayJalali(): JalaliDate {
        val cal = Calendar.getInstance()
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun getTodayWeekdayPersian(): String {
        val cal = Calendar.getInstance()
        return getWeekdayPersian(cal.get(Calendar.DAY_OF_WEEK))
    }

    fun getWeekdayPersian(calendarDayOfWeek: Int): String {
        return when (calendarDayOfWeek) {
            Calendar.SATURDAY -> "شنبه"
            Calendar.SUNDAY -> "یکشنبه"
            Calendar.MONDAY -> "دوشنبه"
            Calendar.TUESDAY -> "سه‌شنبه"
            Calendar.WEDNESDAY -> "چهارشنبه"
            Calendar.THURSDAY -> "پنجشنبه"
            Calendar.FRIDAY -> "جمعه"
            else -> "شنبه"
        }
    }

    /**
     * Converts Gregorian YYYY-MM-DD string to JalaliDate
     */
    fun gregorianToJalali(dateStr: String): JalaliDate {
        try {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                val gy = parts[0].toInt()
                val gm = parts[1].toInt()
                val gd = parts[2].toInt()
                return gregorianToJalali(gy, gm, gd)
            }
        } catch (_: Exception) {}
        return getTodayJalali()
    }

    /**
     * Convert Gregorian (gy, gm, gd) to Jalali (jy, jm, jd)
     */
    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        var gy2 = gy - 1600
        var gm2 = gm - 1
        val gd2 = gd - 1

        var gDayNo = 365 * gy2 + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400

        for (i in 0 until gm2) {
            gDayNo += gDaysInMonth[i]
        }
        if (gm2 > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo++
        }
        gDayNo += gd2

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        for (i in 0..11) {
            if (jDayNo < jDaysInMonth[i]) {
                jm = i + 1
                break
            }
            jDayNo -= jDaysInMonth[i]
        }
        val jd = jDayNo + 1

        return JalaliDate(jy, jm, jd)
    }

    /**
     * Convert Jalali (jy, jm, jd) to Gregorian YYYY-MM-DD
     */
    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): String {
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

        val jy2 = jy - 979
        val jm2 = jm - 1
        val jd2 = jd - 1

        var jDayNo = 365 * jy2 + (jy2 / 33) * 8 + ((jy2 % 33 + 3) / 4)
        for (i in 0 until jm2) {
            jDayNo += jDaysInMonth[i]
        }
        jDayNo += jd2

        var gDayNo = jDayNo + 79
        var gy = 1600 + 400 * (gDayNo / 146097)
        gDayNo %= 146097

        var leap = true
        if (gDayNo >= 36525) {
            gDayNo--
            gy += 100 * (gDayNo / 36524)
            gDayNo %= 36524

            if (gDayNo >= 365) {
                gDayNo++
            } else {
                leap = false
            }
        }

        gy += 4 * (gDayNo / 1461)
        gDayNo %= 1461

        if (gDayNo >= 366) {
            leap = false
            gDayNo--
            gy += gDayNo / 365
            gDayNo %= 365
        }

        var gm = 0
        while (gm < 12) {
            val dim = if (gm == 1 && leap) 29 else gDaysInMonth[gm]
            if (gDayNo < dim) break
            gDayNo -= dim
            gm++
        }
        val gd = gDayNo + 1
        return String.format(Locale.US, "%04d-%02d-%02d", gy, gm + 1, gd)
    }

    /**
     * Format a Gregorian date string (e.g. 2026-11-05) to Persian display (e.g. ۱۵ آبان ۱۴۰۵)
     */
    fun formatGregorianToPersian(dateStr: String, withMonthName: Boolean = true): String {
        val jDate = gregorianToJalali(dateStr)
        return jDate.toFormattedString(withMonthName)
    }

    /**
     * Get day count in Jalali month
     */
    fun getDaysInJalaliMonth(year: Int, month: Int): Int {
        return when {
            month in 1..6 -> 31
            month in 7..11 -> 30
            isJalaliLeapYear(year) -> 30
            else -> 29
        }
    }

    fun isJalaliLeapYear(year: Int): Boolean {
        val a = year - 474
        val b = (a % 2820 + 2820) % 2820 + 474
        return (b + 38) * 682 % 2816 < 682
    }

    /**
     * Returns list of dates for the next [count] days in Gregorian format
     */
    fun getNextDaysGregorian(count: Int = 14): List<String> {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        for (i in 0 until count) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return list
    }

    /**
     * Returns list of dates for the current week starting from Saturday (شنبه) to Friday (جمعه)
     */
    fun getCurrentWeekDays(): List<Pair<String, String>> {
        val cal = Calendar.getInstance()
        // Find last Saturday
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val result = mutableListOf<Pair<String, String>>()
        for (i in 0..6) {
            val dateStr = sdf.format(cal.time)
            val weekday = getWeekdayPersian(cal.get(Calendar.DAY_OF_WEEK))
            result.add(Pair(dateStr, weekday))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    fun formatWeekdayShort(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(dateStr) ?: return ""
            val cal = Calendar.getInstance()
            cal.time = date
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SATURDAY -> "ش"
                Calendar.SUNDAY -> "ی"
                Calendar.MONDAY -> "د"
                Calendar.TUESDAY -> "س"
                Calendar.WEDNESDAY -> "چ"
                Calendar.THURSDAY -> "پ"
                Calendar.FRIDAY -> "ج"
                else -> ""
            }
        } catch (_: Exception) {
            ""
        }
    }
}
