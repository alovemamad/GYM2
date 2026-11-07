package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Utility functions for Persian digits and localized number formatting.
 */
object PersianNumberHelper {

    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun format(number: Number?): String {
        if (number == null) return ""
        return toPersianDigits(number.toString())
    }

    fun toPersianDigits(number: Number?): String = format(number)

    fun formatDecimal(number: Double?, maxDecimals: Int = 1): String {
        if (number == null) return ""
        val pattern = if (maxDecimals <= 0) "#" else "#." + "#".repeat(maxDecimals)
        val df = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
        return toPersianDigits(df.format(number))
    }

    fun toPersianDigits(text: String): String {
        val sb = java.lang.StringBuilder(text.length)
        for (char in text) {
            if (char in '0'..'9') {
                sb.append(persianDigits[char - '0'])
            } else {
                sb.append(char)
            }
        }
        return sb.toString()
    }

    fun toEnglishDigits(text: String): String {
        val sb = java.lang.StringBuilder(text.length)
        for (char in text) {
            val idx = persianDigits.indexOf(char)
            if (idx != -1) {
                sb.append(('0' + idx))
            } else {
                sb.append(char)
            }
        }
        return sb.toString()
    }

    fun formatSecondsToTime(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return toPersianDigits(String.format(Locale.US, "%02d:%02d", m, s))
    }
}
