package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RawRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/**
 * هر نوع هشدار یک صدای کوتاه (کمتر از ~۱ ثانیه)، الگوی لرزش و کانال مخصوص خودش را دارد.
 */
enum class AlertType(
    val channelId: String,
    val channelName: String,
    val channelDescription: String,
    @RawRes val soundRes: Int,
    val vibration: LongArray,
    val isAlarmUsage: Boolean
) {
    WORKOUT(
        channelId = "gym_alert_workout_v2",
        channelName = "یادآور تمرین",
        channelDescription = "یادآوری روزانه‌ی زمان تمرین",
        soundRes = R.raw.alert_workout,
        vibration = longArrayOf(0, 120, 80, 120, 80, 260),
        isAlarmUsage = false
    ),
    WATER(
        channelId = "gym_alert_water_v2",
        channelName = "یادآور نوشیدن آب",
        channelDescription = "یادآوری نوشیدن آب",
        soundRes = R.raw.alert_water,
        vibration = longArrayOf(0, 80, 120, 80),
        isAlarmUsage = false
    ),
    SUPPLEMENT(
        channelId = "gym_alert_supplement_v2",
        channelName = "یادآور مکمل‌ها",
        channelDescription = "یادآوری مصرف مکمل‌ها",
        soundRes = R.raw.alert_supplement,
        vibration = longArrayOf(0, 200, 150, 200),
        isAlarmUsage = false
    ),
    REST_END(
        channelId = "gym_alert_rest_end_v2",
        channelName = "پایان استراحت بین ست‌ها",
        channelDescription = "اعلان پایان زمان استراحت بین ست‌ها",
        soundRes = R.raw.alert_rest_end,
        vibration = longArrayOf(0, 100, 80, 100),
        isAlarmUsage = true
    ),
    EXERCISE_END(
        channelId = "gym_alert_exercise_end_v2",
        channelName = "پایان حرکت زمان‌دار",
        channelDescription = "اعلان پایان تایمر حرکت (مثل پلانک)",
        soundRes = R.raw.alert_exercise_end,
        vibration = longArrayOf(0, 90, 70, 90, 70, 90, 100, 400),
        isAlarmUsage = true
    );

    companion object {
        fun fromChannel(channelId: String?): AlertType =
            entries.firstOrNull { it.channelId == channelId } ?: WORKOUT
    }
}

object NotificationHelper {

    // برای سازگاری با کدهای قبلی
    val CHANNEL_WORKOUT = AlertType.WORKOUT.channelId
    val CHANNEL_WATER = AlertType.WATER.channelId
    val CHANNEL_SUPPLEMENT = AlertType.SUPPLEMENT.channelId
    val CHANNEL_REST_TIMER = AlertType.REST_END.channelId
    val CHANNEL_EXERCISE_END = AlertType.EXERCISE_END.channelId
    val CHANNEL_REMINDERS = AlertType.WORKOUT.channelId

    private val LEGACY_CHANNELS = listOf("channel_gym_reminders", "channel_gym_rest_timer")

    private fun soundUri(context: Context, type: AlertType): Uri =
        Uri.parse("android.resource://${context.packageName}/${type.soundRes}")

    private fun audioAttributes(type: AlertType): AudioAttributes =
        AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(if (type.isAlarmUsage) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
            .build()

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // کانال‌های قدیمی صدای پیش‌فرض داشتند و بعد از ساخت قابل تغییر نیستند
        LEGACY_CHANNELS.forEach { nm.deleteNotificationChannel(it) }

        AlertType.entries.forEach { type ->
            val channel = NotificationChannel(
                type.channelId,
                type.channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = type.channelDescription
                enableVibration(true)
                vibrationPattern = type.vibration
                setSound(soundUri(context, type), audioAttributes(type))
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /** پخش صدای کوتاه اختصاصی همان هشدار + لرزش (برای وقتی که برنامه باز است). */
    fun playAlarmSoundAndVibrate(context: Context, type: AlertType = AlertType.REST_END) {
        try {
            val afd = context.resources.openRawResourceFd(type.soundRes)
            MediaPlayer().apply {
                setAudioAttributes(audioAttributes(type))
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                setOnCompletionListener { it.release() }
                setOnErrorListener { mp, _, _ -> mp.release(); true }
                prepare()
                start()
            }
            afd.close()
        } catch (_: Exception) {
        }
        vibrate(context, type)
    }

    private fun vibrate(context: Context, type: AlertType) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(type.vibration, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(type.vibration, -1)
            }
        } catch (_: Exception) {
        }
    }

    /**
     * @param silent اگر true باشد اعلان بدون صدا/لرزش نمایش داده می‌شود
     * (وقتی صدا را خود برنامه پخش کرده است، تا دوبار پخش نشود).
     */
    fun showNotification(
        context: Context,
        notificationId: Int,
        channelId: String,
        title: String,
        message: String,
        silent: Boolean = false
    ) {
        createNotificationChannels(context)

        if (!hasNotificationPermission(context)) {
            return
        }

        val type = AlertType.fromChannel(channelId)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, type.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFD90D3C.toInt())
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (silent) {
            builder.setSilent(true)
        } else {
            // برای اندروید ۷ و پایین‌تر (کانال ندارند)
            builder.setSound(soundUri(context, type))
            builder.setVibrate(type.vibration)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }
}
