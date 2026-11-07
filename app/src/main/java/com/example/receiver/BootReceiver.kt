package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.UserPreferences
import com.example.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = UserPreferences(context)
                    val settings = prefs.userSettingsFlow.first()

                    if (settings.workoutReminderEnabled) {
                        ReminderScheduler.scheduleDailyReminder(
                            context = context,
                            reminderId = ReminderScheduler.ID_WORKOUT,
                            timeStr = settings.workoutReminderTime,
                            title = "وقت تمرین و باشگاه!",
                            message = "امروز روز ساختن بدنی قوی‌تر است. برنامه‌ی تمرینت منتظرته!"
                        )
                    }

                    if (settings.waterReminderEnabled) {
                        ReminderScheduler.scheduleDailyReminder(
                            context = context,
                            reminderId = ReminderScheduler.ID_WATER,
                            timeStr = settings.waterReminderTime,
                            title = "یادآور نوشیدن آب",
                            message = "هیدراته بمانید! یک لیوان آب خنک بنوشید."
                        )
                    }

                    if (settings.supplementReminderEnabled) {
                        ReminderScheduler.scheduleDailyReminder(
                            context = context,
                            reminderId = ReminderScheduler.ID_SUPPLEMENT,
                            timeStr = settings.supplementReminderTime,
                            title = "یادآور مصرف مکمل‌ها",
                            message = "زمان مصرف مکمل‌های روزانه فرا رسیده است."
                        )
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
