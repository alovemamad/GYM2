package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.util.NotificationHelper
import com.example.util.ReminderScheduler

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_reminder_title"
        const val EXTRA_MESSAGE = "extra_reminder_message"
        const val EXTRA_CHANNEL = "extra_reminder_channel"
        const val EXTRA_IS_DAILY = "extra_is_daily"
        const val EXTRA_TIME_STR = "extra_time_str"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, 1)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "یادآور GYM"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "زمان تمرین یا نوشیدن آب است!"
        val channel = intent.getStringExtra(EXTRA_CHANNEL) ?: NotificationHelper.CHANNEL_REMINDERS
        val isDaily = intent.getBooleanExtra(EXTRA_IS_DAILY, false)
        val timeStr = intent.getStringExtra(EXTRA_TIME_STR)

        // Show the notification and play the alert at the user's chosen volume
        val pending = goAsync()
        val finish = { try { pending.finish() } catch (_: Exception) {} }
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(finish, 6000)
        NotificationHelper.showNotification(
            context = context,
            notificationId = id,
            channelId = channel,
            title = title,
            message = message,
            onSoundDone = finish
        )

        // If it's a repeating daily alarm, schedule next day's alarm
        if (isDaily && !timeStr.isNullOrBlank()) {
            ReminderScheduler.scheduleDailyReminder(
                context = context,
                reminderId = id,
                timeStr = timeStr,
                title = title,
                message = message,
                channelId = channel
            )
        }
    }
}
