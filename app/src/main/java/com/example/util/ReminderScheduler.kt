package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferences
import com.example.receiver.ReminderReceiver
import kotlinx.coroutines.flow.first
import java.util.Calendar

object ReminderScheduler {

    const val ID_WORKOUT = 101
    const val ID_WATER = 102
    const val ID_SUPPLEMENT = 103
    const val ID_REST_TIMER = 104

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) {
            }
        }
        openAppDetailsSettings(context)
    }

    fun openBatteryOptimizationSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) {
            }
        }
        openAppDetailsSettings(context)
    }

    private fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun computeNextDailyTriggerMillis(timeStr: String, nowMillis: Long = System.currentTimeMillis()): Long? {
        val parts = timeStr.trim().split(":")
        if (parts.size != 2) return null

        val hour = parts[0].toIntOrNull()?.coerceIn(0, 23) ?: 18
        val minute = parts[1].toIntOrNull()?.coerceIn(0, 59) ?: 0

        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }

    fun scheduleDailyReminder(
        context: Context,
        reminderId: Int,
        timeStr: String,
        title: String,
        message: String,
        channelId: String = NotificationHelper.CHANNEL_WORKOUT
    ) {
        val triggerMillis = computeNextDailyTriggerMillis(timeStr) ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ID, reminderId)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_MESSAGE, message)
            putExtra(ReminderReceiver.EXTRA_CHANNEL, channelId)
            putExtra(ReminderReceiver.EXTRA_IS_DAILY, true)
            putExtra(ReminderReceiver.EXTRA_TIME_STR, timeStr)
            putExtra(ReminderReceiver.EXTRA_SCHEDULED_AT, triggerMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarmSafely(
            context = context,
            alarmManager = alarmManager,
            reminderId = reminderId,
            triggerMillis = triggerMillis,
            pendingIntent = pendingIntent,
            useAlarmClock = true
        )
    }

    fun scheduleSupplementItemReminder(context: Context, supplementId: Long, timeStr: String, name: String, dosage: String) {
        val reminderId = 2000 + (supplementId % 1000).toInt()
        val title = "یادآور مصرف مکمل: $name"
        val message = "زمان مصرف دوز $dosage از مکمل $name فرا رسیده است."
        scheduleDailyReminder(context, reminderId, timeStr, title, message, NotificationHelper.CHANNEL_SUPPLEMENT)
    }

    fun cancelSupplementItemReminder(context: Context, supplementId: Long) {
        val reminderId = 2000 + (supplementId % 1000).toInt()
        cancelReminder(context, reminderId)
    }

    fun scheduleRestTimer(context: Context, durationSeconds: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + (durationSeconds * 1000L)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ID, ID_REST_TIMER)
            putExtra(ReminderReceiver.EXTRA_TITLE, "پایان استراحت ست!")
            putExtra(ReminderReceiver.EXTRA_MESSAGE, "زمان استراحت شما تمام شد. آماده‌ی ست بعدی هستید؟")
            putExtra(ReminderReceiver.EXTRA_CHANNEL, NotificationHelper.CHANNEL_REST_TIMER)
            putExtra(ReminderReceiver.EXTRA_IS_DAILY, false)
            putExtra(ReminderReceiver.EXTRA_SCHEDULED_AT, triggerTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ID_REST_TIMER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarmSafely(
            context = context,
            alarmManager = alarmManager,
            reminderId = ID_REST_TIMER,
            triggerMillis = triggerTime,
            pendingIntent = pendingIntent,
            useAlarmClock = false
        )
    }

    /**
     * همگام‌سازی کامل تمامی یادآورهای روزانه و مکمل‌های فعال
     * (پس از ریبوت، تغییر ساعت/منطقه زمانی، آپدیت برنامه یا ورود به برنامه).
     */
    suspend fun syncAllReminders(context: Context) {
        try {
            val prefs = UserPreferences(context)
            val settings = prefs.userSettingsFlow.first()

            if (settings.workoutReminderEnabled) {
                scheduleDailyReminder(
                    context = context,
                    reminderId = ID_WORKOUT,
                    timeStr = settings.workoutReminderTime,
                    title = "وقت تمرین و باشگاه!",
                    message = "امروز روز ساختن بدنی قوی‌تر است. برنامه‌ی تمرینت منتظرته!",
                    channelId = NotificationHelper.CHANNEL_WORKOUT
                )
            } else {
                cancelReminder(context, ID_WORKOUT)
            }

            if (settings.waterReminderEnabled) {
                scheduleDailyReminder(
                    context = context,
                    reminderId = ID_WATER,
                    timeStr = settings.waterReminderTime,
                    title = "یادآور نوشیدن آب",
                    message = "هیدراته بمانید! یک لیوان آب خنک بنوشید.",
                    channelId = NotificationHelper.CHANNEL_WATER
                )
            } else {
                cancelReminder(context, ID_WATER)
            }

            if (settings.supplementReminderEnabled) {
                scheduleDailyReminder(
                    context = context,
                    reminderId = ID_SUPPLEMENT,
                    timeStr = settings.supplementReminderTime,
                    title = "یادآور مصرف مکمل‌ها",
                    message = "زمان مصرف مکمل‌های روزانه فرا رسیده است.",
                    channelId = NotificationHelper.CHANNEL_SUPPLEMENT
                )
            } else {
                cancelReminder(context, ID_SUPPLEMENT)
            }

            val supplements = AppDatabase.getInstance(context).supplementDao().getAllSupplements().first()
            supplements.forEach { sup ->
                if (sup.isActive) {
                    scheduleSupplementItemReminder(context, sup.id, sup.reminderTime, sup.name, sup.dosage)
                } else {
                    cancelSupplementItemReminder(context, sup.id)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun setAlarmSafely(
        context: Context,
        alarmManager: AlarmManager,
        reminderId: Int,
        triggerMillis: Long,
        pendingIntent: PendingIntent,
        useAlarmClock: Boolean
    ) {
        try {
            val exactAllowed = canScheduleExactAlarms(context)
            if (useAlarmClock && exactAllowed) {
                // setAlarmClock در حالت بسته بودن طولانی‌مدت برنامه (Doze / Standby) همیشه بیدار می‌شود
                val showIntent = PendingIntent.getActivity(
                    context,
                    reminderId,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerMillis, showIntent),
                    pendingIntent
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (exactAllowed) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } catch (_: Exception) {
            }
        } catch (_: Exception) {
        }
    }

    fun cancelReminder(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
