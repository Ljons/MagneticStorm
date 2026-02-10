package com.magneticstorm.app.widget

import android.content.Context
import android.content.SharedPreferences

/**
 * Зберігає останнє значення Kp для віджета (читається в іншому процесі).
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

    companion object {
        private const val PREFS_NAME = "widget_kp_prefs"
        private const val KEY_LAST_KP = "last_kp"
        private const val KEY_LAST_KP_TIME = "last_kp_time"
    }
}
