package com.example

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews

class BatteryAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Retrieve last known battery stats from SharedPreferences to populate widgets
        val prefs = context.getSharedPreferences("BatteryWidgetPrefs", Context.MODE_PRIVATE)
        val level = prefs.getInt("level", 100)
        val isCharging = prefs.getBoolean("isCharging", false)
        val temperature = prefs.getFloat("temperature", 25.0f)
        val voltage = prefs.getFloat("voltage", 4.2f)
        val widgetColor = prefs.getLong("widgetColor", 0xFF4CAF50)
        val widgetStyle = prefs.getString("widgetStyle", "CIRCLE_RING") ?: "CIRCLE_RING"

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, level, isCharging, temperature, voltage, widgetStyle, widgetColor)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.example.BATTERY_UPDATE_ACTION") {
            val level = intent.getIntExtra("level", 100)
            val isCharging = intent.getBooleanExtra("isCharging", false)
            val temperature = intent.getFloatExtra("temperature", 25.0f)
            val voltage = intent.getFloatExtra("voltage", 4.2f)
            val widgetColor = intent.getLongExtra("widgetColor", 0xFF4CAF50)
            val widgetStyle = intent.getStringExtra("widgetStyle") ?: "CIRCLE_RING"

            // Save variables to SharedPreferences so widget update can retrieve them on system request
            context.getSharedPreferences("BatteryWidgetPrefs", Context.MODE_PRIVATE).edit().apply {
                putInt("level", level)
                putBoolean("isCharging", isCharging)
                putFloat("temperature", temperature)
                putFloat("voltage", voltage)
                putLong("widgetColor", widgetColor)
                putString("widgetStyle", widgetStyle)
                apply()
            }

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, BatteryAppWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            for (appWidgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, level, isCharging, temperature, voltage, widgetStyle, widgetColor)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        level: Int,
        isCharging: Boolean,
        temperature: Float,
        voltage: Float,
        widgetStyle: String,
        widgetColor: Long
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_battery_layout)

        // Update progress bar
        views.setProgressBar(R.id.widget_progress, 100, level, false)
        views.setTextViewText(R.id.widget_percentage, "$level%")

        // Status text showing charging or temperature
        val statusText = if (isCharging) {
            "⚡ กำลังชาร์จ • ${temperature}°C"
        } else {
            "🔋 แบตเตอรี่ • ${temperature}°C"
        }
        views.setTextViewText(R.id.widget_status, statusText)

        // Setup pending intent to open the app when widget container is clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        // Tell the AppWidgetManager to perform an update on the current app widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
