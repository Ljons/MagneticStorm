package com.magneticstorm.app.widget

import android.content.Context
import android.content.SharedPreferences

/**
 * Зберігає дані для віджета (читається в іншому процесі).
 * Підтримує повний стан картки «Рівень на сьогодні» та legacy (лише Kp + час).
 */
class WidgetPreferences(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getLastKp(): Double = prefs.getFloat(KEY_LAST_KP, 0f).toDouble()

    fun getLastKpTimeMillis(): Long = prefs.getLong(KEY_LAST_KP_TIME, 0L)

    fun setLastKp(kp: Double, timeMillis: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putFloat(KEY_LAST_KP, kp.toFloat())
            .putLong(KEY_LAST_KP_TIME, timeMillis)
            .apply()
    }

    fun getCardState(): WidgetCardState? {
        val dateStr = prefs.getString(KEY_DATE_STR, null) ?: return null
        val currentKp = prefs.getFloat(KEY_LAST_KP, -1f).toDouble()
        if (currentKp < 0) return null
        val currentTimeStr = prefs.getString(KEY_CURRENT_TIME_STR, "") ?: ""
        val circleCategoryOrdinal = prefs.getInt(KEY_CIRCLE_CATEGORY, 0).coerceIn(0, 5)
        val locationDisplayName = prefs.getString(KEY_LOCATION_DISPLAY_NAME, "") ?: ""
        val slots = (0 until 8).map { i ->
            val kp = prefs.getFloat("$KEY_SLOT_KP_PREFIX$i", -1f).toDouble()
            val time = prefs.getString("$KEY_SLOT_TIME_PREFIX$i", "") ?: ""
            (if (kp >= 0) kp else null) to (if (time.isNotEmpty()) time else null)
        }
        return WidgetCardState(
            dateStr = dateStr,
            locationDisplayName = locationDisplayName,
            currentKp = currentKp,
            currentTimeStr = currentTimeStr,
            circleCategoryOrdinal = circleCategoryOrdinal,
            slots = slots
        )
    }

    fun setCardState(state: WidgetCardState) {
        prefs.edit().apply {
            putString(KEY_DATE_STR, state.dateStr)
            putString(KEY_LOCATION_DISPLAY_NAME, state.locationDisplayName)
            putFloat(KEY_LAST_KP, state.currentKp.toFloat())
            putLong(KEY_LAST_KP_TIME, System.currentTimeMillis())
            putString(KEY_CURRENT_TIME_STR, state.currentTimeStr)
            putInt(KEY_CIRCLE_CATEGORY, state.circleCategoryOrdinal.coerceIn(0, 5))
            state.slots.take(8).forEachIndexed { i, (kp, time) ->
                putFloat("$KEY_SLOT_KP_PREFIX$i", kp?.toFloat() ?: -1f)
                putString("$KEY_SLOT_TIME_PREFIX$i", time ?: "")
            }
            // fill remaining slots if less than 8
            for (i in state.slots.size until 8) {
                putFloat("$KEY_SLOT_KP_PREFIX$i", -1f)
                putString("$KEY_SLOT_TIME_PREFIX$i", "")
            }
        }.apply()
    }

    companion object {
        private const val PREFS_NAME = "widget_kp_prefs"
        private const val KEY_LAST_KP = "last_kp"
        private const val KEY_LAST_KP_TIME = "last_kp_time"
        private const val KEY_DATE_STR = "date_str"
        private const val KEY_LOCATION_DISPLAY_NAME = "location_display_name"
        private const val KEY_CURRENT_TIME_STR = "current_time_str"
        private const val KEY_CIRCLE_CATEGORY = "circle_category"
        private const val KEY_SLOT_KP_PREFIX = "slot_kp_"
        private const val KEY_SLOT_TIME_PREFIX = "slot_time_"
    }
}
