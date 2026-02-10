package com.magneticstorm.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.magneticstorm.app.MainActivity
import com.magneticstorm.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KpWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val prefs = WidgetPreferences(context)
            val kp = prefs.getLastKp()
            val timeMillis = prefs.getLastKpTimeMillis()

            val kpText = if (timeMillis > 0) String.format(Locale.US, "%.1f", kp) else "—"
            val timeText = if (timeMillis > 0) {
                val sdf = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
                context.getString(R.string.widget_updated_format, sdf.format(Date(timeMillis)))
            } else ""

            for (id in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_kp).apply {
                    setTextViewText(R.id.widget_kp_value, kpText)
                    setTextViewText(R.id.widget_updated, timeText)
                }
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pending = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pending)
                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }
}
