package com.magneticstorm.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.magneticstorm.app.R

/**
 * Зберігає Kp для віджета та оновлює його на головному екрані.
 */
object WidgetUpdateHelper {

    fun saveAndUpdateWidget(context: Context, kp: Double) {
        WidgetPreferences(context).setLastKp(kp)
        updateWidget(context)
    }

    fun updateWidget(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, KpWidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(componentName)
        if (ids.isNotEmpty()) {
            KpWidgetProvider.updateAppWidget(context, appWidgetManager, ids)
        }
    }
}
