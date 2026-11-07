package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.NutritionLog
import com.example.data.local.UserPreferences
import com.example.util.JalaliCalendar
import com.example.util.PersianNumberHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WaterAppWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_ADD_WATER = "com.example.ACTION_ADD_WATER"
        const val EXTRA_AMOUNT = "extra_amount"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, WaterAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            val intent = Intent(context, WaterAppWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val prefs = UserPreferences(context)
                val today = JalaliCalendar.getTodayGregorian()
                val log = db.nutritionDao().getNutritionLogSync(today)
                val userSettings = prefs.userSettingsFlow.first()

                val currentWater = log?.waterIntakeMl ?: 0
                val goalWater = if (userSettings.waterGoalMl > 0) userSettings.waterGoalMl else 2500
                val progressPercent = ((currentWater.toDouble() / goalWater.toDouble()) * 100).toInt().coerceIn(0, 100)

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_water)

                    val percentStr = PersianNumberHelper.toPersianDigits("$progressPercent٪")
                    val amountStr = PersianNumberHelper.toPersianDigits("$currentWater / $goalWater میلی‌لیتر")

                    views.setTextViewText(R.id.widget_percentage, percentStr)
                    views.setTextViewText(R.id.widget_amount, amountStr)
                    views.setProgressBar(R.id.widget_progress, 100, progressPercent, false)

                    // Click root -> Open App
                    val appIntent = Intent(context, MainActivity::class.java)
                    val appPendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        appIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, appPendingIntent)

                    // Button +250ml
                    val plus250Intent = Intent(context, WaterAppWidgetProvider::class.java).apply {
                        action = ACTION_ADD_WATER
                        putExtra(EXTRA_AMOUNT, 250)
                    }
                    val plus250PendingIntent = PendingIntent.getBroadcast(
                        context,
                        1,
                        plus250Intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_plus_250, plus250PendingIntent)

                    // Button +500ml
                    val plus500Intent = Intent(context, WaterAppWidgetProvider::class.java).apply {
                        action = ACTION_ADD_WATER
                        putExtra(EXTRA_AMOUNT, 500)
                    }
                    val plus500PendingIntent = PendingIntent.getBroadcast(
                        context,
                        2,
                        plus500Intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_plus_500, plus500PendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_ADD_WATER) {
            val amount = intent.getIntExtra(EXTRA_AMOUNT, 250)
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val today = JalaliCalendar.getTodayGregorian()
                    val current = db.nutritionDao().getNutritionLogSync(today)
                    val newAmount = (current?.waterIntakeMl ?: 0) + amount
                    val updated = (current ?: NutritionLog(date = today)).copy(waterIntakeMl = newAmount)
                    db.nutritionDao().insertOrUpdateNutritionLog(updated)

                    // Trigger update on widgets
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val thisWidget = ComponentName(context, WaterAppWidgetProvider::class.java)
                    val ids = appWidgetManager.getAppWidgetIds(thisWidget)
                    onUpdate(context, appWidgetManager, ids)
                } catch (_: Exception) {
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
