package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.receiver.ReminderReceiver
import java.util.Calendar

object ReminderScheduler {

    const val ID_WORKOUT = 101
    const val ID_WATER = 102
    const val ID_SUPPLEMENT = 103
    const val ID_REST_TIMER = 104

    fun scheduleDailyReminder(context: Context, reminderId: Int, timeStr: String, title: String, message: String) {
        val parts = timeStr.split(":")
        if (parts.size != 2) return

        val hour = parts[0].toIntOrNull() ?: 18
        val minute = parts[1].toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ID, reminderId)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_MESSAGE, message)
            putExtra(ReminderReceiver.EXTRA_CHANNEL, NotificationHelper.CHANNEL_REMINDERS)
            putExtra(ReminderReceiver.EXTRA_IS_DAILY, true)
            putExtra(ReminderReceiver.EXTRA_TIME_STR, timeStr)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarmSafely(alarmManager, calendar.timeInMillis, pendingIntent)
    }

    fun scheduleSupplementItemReminder(context: Context, supplementId: Long, timeStr: String, name: String, dosage: String) {
        val reminderId = 2000 + (supplementId % 1000).toInt()
        val title = "یادآور مصرف مکمل: $name"
        val message = "زمان مصرف دوز $dosage از مکمل $name فرا رسیده است."
        scheduleDailyReminder(context, reminderId, timeStr, title, message)
    }

    fun cancelSupplementItemReminder(context: Context, supplementId: Long) {
        val reminderId = 2000 + (supplementId % 1000).toInt()
        cancelReminder(context, reminderId)
    }

    fun scheduleRestTimer(context: Context, durationSeconds: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + (durationSeconds * 1000)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ID, ID_REST_TIMER)
            putExtra(ReminderReceiver.EXTRA_TITLE, "پایان استراحت ست!")
            putExtra(ReminderReceiver.EXTRA_MESSAGE, "زمان استراحت شما تمام شد. آماده‌ی ست بعدی هستید؟")
            putExtra(ReminderReceiver.EXTRA_CHANNEL, NotificationHelper.CHANNEL_REST_TIMER)
            putExtra(ReminderReceiver.EXTRA_IS_DAILY, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ID_REST_TIMER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarmSafely(alarmManager, triggerTime, pendingIntent)
    }

    private fun setAlarmSafely(alarmManager: AlarmManager, triggerMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
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
