package com.example.util

import com.example.data.local.*

object AiExportHelper {

    fun generateAiAnalysisPrompt(
        settings: UserSettings,
        plans: List<WorkoutPlan>,
        histories: List<WorkoutHistory>,
        nutritionLogs: List<NutritionLog>,
        measurements: List<BodyMeasurement>
    ): String {
        val sb = java.lang.StringBuilder()
        sb.append("### اطلاعات جامع ورزشی، تغذیه و بدنی کاربر جهت تحلیل هوشمند و برنامه‌ریزی\n\n")

        sb.append("#### ۱. پروفایل کاربر:\n")
        sb.append("- نام/لقب: ${settings.userName.ifBlank { "کاربر" }}\n")
        val genderStr = when (settings.gender) {
            "MALE" -> "مرد"
            "FEMALE" -> "زن"
            else -> "نامشخص"
        }
        sb.append("- جنسیت: $genderStr\n")
        sb.append("- سن: ${settings.age?.let { "$it سال" } ?: "ثبت نشده"}\n")
        sb.append("- قد: ${settings.heightCm?.let { "$it سانتی‌متر" } ?: "ثبت نشده"}\n")
        sb.append("- وزن فعلی: ${settings.weightKg?.let { "$it کیلوگرم" } ?: "ثبت نشده"}\n")

        if (settings.heightCm != null && settings.weightKg != null && settings.heightCm > 0) {
            val hM = settings.heightCm / 100.0
            val bmi = settings.weightKg / (hM * hM)
            sb.append("- شاخص توده بدنی (BMI): ${String.format(java.util.Locale.US, "%.1f", bmi)}\n")
        }

        val activityStr = when (settings.activityLevel) {
            "SEDENTARY" -> "کم‌تحرک"
            "MODERATE" -> "متوسط"
            "ACTIVE" -> "پرتحرک"
            "ATHLETE" -> "ورزشکار حرفه‌ای"
            else -> settings.activityLevel
        }
        sb.append("- سطح فعالیت: $activityStr\n")

        val goalStr = when (settings.fitnessGoal) {
            "BULK" -> "افزایش حجم عضلانی (Bulk)"
            "CUT" -> "کاهش چربی و کات (Cut)"
            "MAINTAIN" -> "حفظ تناسب و ثبات (Maintain)"
            else -> settings.fitnessGoal
        }
        sb.append("- هدف بدنسازی: $goalStr\n")
        sb.append("- تعداد وعده‌های غذایی: ${settings.mealsCount} وعده\n")
        sb.append("- هدف روزانه آب: ${settings.waterGoalMl} میلی‌لیتر\n\n")

        sb.append("#### ۲. برنامه‌های تمرینی فعال:\n")
        if (plans.isEmpty()) {
            sb.append("هنوز برنامه‌ای ثبت نشده است.\n")
        } else {
            plans.forEachIndexed { idx, plan ->
                sb.append("${idx + 1}. **${plan.name}** (روزهای اجرا: ${plan.scheduledDay})\n")
                if (plan.notes.isNotBlank()) sb.append("   یادداشت: ${plan.notes}\n")
            }
        }
        sb.append("\n")

        sb.append("#### ۳. سوابق اخیر تمرینات (۱۰ جلسه آخر):\n")
        val recentHistories = histories.take(10)
        if (recentHistories.isEmpty()) {
            sb.append("هنوز تمرینی پایان نیافته است.\n")
        } else {
            recentHistories.forEach { h ->
                val jDate = JalaliCalendar.formatGregorianToPersian(h.date)
                val durationMin = h.durationSeconds / 60
                val restMin = h.totalRestSecondsUsed / 60
                sb.append("- تاریخ: $jDate | برنامه: ${h.planName} | حرکت: ${h.exerciseName} | ${h.sets} ست، ${h.reps} تکرار، وزن: ${h.weight} کیلو | مدت کل: $durationMin دقیقه | استراحت: $restMin دقیقه\n")
            }
        }
        sb.append("\n")

        sb.append("#### ۴. تغذیه و هیدراتاسیون ۷ روز اخیر:\n")
        val recentNutrition = nutritionLogs.take(7)
        if (recentNutrition.isEmpty()) {
            sb.append("هنوز گزارشی برای تغذیه ثبت نشده است.\n")
        } else {
            recentNutrition.forEach { n ->
                val jDate = JalaliCalendar.formatGregorianToPersian(n.date)
                sb.append("- تاریخ: $jDate | آب: ${n.waterIntakeMl} ml\n")
                if (n.breakfast.isNotBlank()) sb.append("   صبحانه: ${n.breakfast}\n")
                if (n.lunch.isNotBlank()) sb.append("   ناهار: ${n.lunch}\n")
                if (n.dinner.isNotBlank()) sb.append("   شام: ${n.dinner}\n")
                if (n.snacks.isNotBlank()) sb.append("   میان‌وعده: ${n.snacks}\n")
            }
        }
        sb.append("\n")

        sb.append("#### ۵. تاریخچه سنجش سایز و وزن بدن:\n")
        if (measurements.isEmpty()) {
            sb.append("هنوز اندازه‌گیری بدنی ثبت نشده است.\n")
        } else {
            measurements.take(5).forEach { m ->
                val jDate = JalaliCalendar.formatGregorianToPersian(m.date)
                sb.append("- تاریخ: $jDate | وزن: ${m.weightKg ?: "-"} kg | قد: ${m.heightCm ?: "-"} cm | دور کمر: ${m.waistCm ?: "-"} | دور بازو: ${m.armsCm ?: "-"} | دور سینه: ${m.chestCm ?: "-"} | ران: ${m.thighCm ?: "-"} | گردن: ${m.neckCm ?: "-"}\n")
            }
        }

        sb.append("\n---\n")
        sb.append("لطفاً بر اساس این اطلاعات، تحلیل کاملی از روند پیشرفت من ارائه دهید، نقاط ضعف و قوت برنامه‌ی تمرین و تغذیه را مشخص کنید و توصیه‌های تخصصی برای دستیابی سریع‌تر به هدف ارائه دهید.")

        return sb.toString()
    }
}
