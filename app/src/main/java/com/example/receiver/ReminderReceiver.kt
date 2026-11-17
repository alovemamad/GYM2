package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.example.data.local.SoundPrefsCache
import com.example.util.AlertType
import com.example.util.NotificationHelper
import com.example.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_reminder_title"
        const val EXTRA_MESSAGE = "extra_reminder_message"
        const val EXTRA_CHANNEL = "extra_reminder_channel"
        const val EXTRA_IS_DAILY = "extra_is_daily"
        const val EXTRA_TIME_STR = "extra_time_str"
        const val EXTRA_SCHEDULED_AT = "extra_scheduled_at"

        // حداکثر تاخیر مجاز (۳ ساعت)؛ اگر سیستم پس از روزها آلارم انباشته‌شده را یکجا تخلیه کند، نمایش داده نمی‌شود
        const val MAX_ACCEPTABLE_DELAY_MS = 3L * 3600L * 1000L
        private const val SOUND_COOLDOWN_MS = 2500L

        @Volatile
        private var lastSoundPlayedAt: Long = 0L

        fun shouldDropStaleAlarm(scheduledAtMillis: Long, nowMillis: Long): Boolean {
            return scheduledAtMillis > 0L && (nowMillis - scheduledAtMillis) > MAX_ACCEPTABLE_DELAY_MS
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            var wakeLock: PowerManager.WakeLock? = null
            try {
                try {
                    val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GymPro:ReminderWakeLock")?.apply {
                        acquire(4000L)
                    }
                } catch (_: Exception) {
                }

                val id = intent.getIntExtra(EXTRA_ID, 1)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "یادآور GYM"
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "زمان تمرین یا نوشیدن آب است!"
                val channel = intent.getStringExtra(EXTRA_CHANNEL) ?: NotificationHelper.CHANNEL_REMINDERS
                val isDaily = intent.getBooleanExtra(EXTRA_IS_DAILY, false)
                val timeStr = intent.getStringExtra(EXTRA_TIME_STR)
                val scheduledAt = intent.getLongExtra(EXTRA_SCHEDULED_AT, 0L)
                val now = System.currentTimeMillis()

                // ابتدا نوبت روز بعد را ثبت می‌کنیم تا زنجیره یادآور روزانه هرگز قطع نشود
                if (isDaily && !timeStr.isNullOrBlank()) {
                    ReminderScheduler.scheduleDailyReminder(
                        context = appContext,
                        reminderId = id,
                        timeStr = timeStr,
                        title = title,
                        message = message,
                        channelId = channel
                    )
                }

                // جلوگیری از تخلیه یکجای اعلان‌های قدیمی پس از باز کردن برنامه
                if (shouldDropStaleAlarm(scheduledAt, now)) {
                    return@launch
                }

                val soundConfig = SoundPrefsCache.get(appContext)
                val alertType = AlertType.fromChannel(channel)
                val targetVolume = when (alertType) {
                    AlertType.REST_END -> soundConfig.restTimerVolume
                    AlertType.EXERCISE_END -> soundConfig.timedAlarmVolume
                    AlertType.WORKOUT, AlertType.WATER, AlertType.SUPPLEMENT -> soundConfig.reminderVolume
                }

                val soundAllowed = soundConfig.inAppSoundsEnabled && targetVolume > 0.01f
                val withinSoundCooldown = (now - lastSoundPlayedAt) < SOUND_COOLDOWN_MS
                val shouldBeSilent = !soundAllowed || withinSoundCooldown
                val useCustomVolumePlayer = !shouldBeSilent && targetVolume < 0.98f

                if (!shouldBeSilent) {
                    lastSoundPlayedAt = now
                }

                if (useCustomVolumePlayer) {
                    NotificationHelper.playAlarmSoundAndVibrate(appContext, alertType)
                }

                NotificationHelper.showNotification(
                    context = appContext,
                    notificationId = id,
                    channelId = channel,
                    title = title,
                    message = message,
                    silent = shouldBeSilent || useCustomVolumePlayer
                )
            } finally {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (_: Exception) {
                }
                try {
                    pendingResult.finish()
                } catch (_: Exception) {
                }
            }
        }
    }
}
