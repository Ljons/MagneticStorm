package com.magneticstorm.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.magneticstorm.app.R
import com.magneticstorm.app.data.model.KpRecord
import com.magneticstorm.app.data.model.KpScale
import com.magneticstorm.app.data.util.formatKp

/**
 * Зберігає дані для віджета та оновлює його на головному екрані.
 * Підтримує повний стан картки «Рівень на сьогодні» та простий варіант (лише Kp).
 */
object WidgetUpdateHelper {

    fun saveAndUpdateWidget(context: Context, kp: Double) {
        WidgetPreferences(context).setLastKp(kp)
        updateWidget(context)
    }

    fun saveAndUpdateWidget(context: Context, state: WidgetCardState) {
        WidgetPreferences(context).setCardState(state)
        updateWidget(context)
    }

    /** Збирає стан картки «Рівень на сьогодні» для віджета з поточного Kp та погодинних записів за сьогодні. */
    fun buildCardState(
        currentKp: KpRecord,
        todayRecords: List<KpRecord>,
        timeZoneId: String,
        locationDisplayName: String
    ): WidgetCardState {
        val dateStr = currentKp.formatTime(timeZoneId, "dd-MM-yyyy")
        val currentTimeStr = currentKp.formatTime(timeZoneId, "HH:mm")
        val categoryOrdinal = KpScale.category(currentKp.kp).ordinal.coerceIn(0, 5)
        val slots = (0 until 8).map { i ->
            val r = todayRecords.getOrNull(i)
            if (r != null) r.kp to r.formatTime(timeZoneId, "HH:mm") else null to null
        }
        return WidgetCardState(
            dateStr = dateStr,
            locationDisplayName = locationDisplayName,
            currentKp = currentKp.kp,
            currentTimeStr = currentTimeStr,
            circleCategoryOrdinal = categoryOrdinal,
            slots = slots
        )
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
