package com.magneticstorm.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.magneticstorm.app.data.model.SavedLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "magnetic_storm_settings")

class PreferencesManager(context: Context) {

    private val store = context.dataStore

    companion object {
        private val KEY_LOCATION_DISPLAY_NAME = stringPreferencesKey("location_display_name")
        private val KEY_LOCATION_TIMEZONE = stringPreferencesKey("location_timezone")
        private val KEY_THEME = stringPreferencesKey("theme")
        private val KEY_REFRESH_MODE = stringPreferencesKey("refresh_mode") // "on_open" | "background"
        private val KEY_NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        private val KEY_NOTIFICATION_KP_THRESHOLD = intPreferencesKey("notification_kp_threshold") // 1..9
        private val KEY_LAST_NOTIFICATION_TIME = longPreferencesKey("last_notification_time")

        const val REFRESH_MODE_ON_OPEN = "on_open"
        const val REFRESH_MODE_BACKGROUND = "background"
    }

    val savedLocation: Flow<SavedLocation> = store.data.map { prefs ->
        val name = prefs[KEY_LOCATION_DISPLAY_NAME] ?: SavedLocation.DEFAULT.displayName
        val tz = prefs[KEY_LOCATION_TIMEZONE] ?: SavedLocation.DEFAULT.timeZoneId
        SavedLocation(displayName = name, timeZoneId = tz)
    }

    val themeMode: Flow<String> = store.data.map { prefs ->
        prefs[KEY_THEME] ?: "system"
    }

    fun getSavedLocationSync(): SavedLocation = runBlocking {
        store.data.first().let { prefs ->
            SavedLocation(
                displayName = prefs[KEY_LOCATION_DISPLAY_NAME] ?: SavedLocation.DEFAULT.displayName,
                timeZoneId = prefs[KEY_LOCATION_TIMEZONE] ?: SavedLocation.DEFAULT.timeZoneId
            )
        }
    }

    fun getThemeModeSync(): String = runBlocking {
        store.data.first()[KEY_THEME] ?: "system"
    }

    suspend fun setSavedLocation(location: SavedLocation) {
        store.edit { prefs ->
            prefs[KEY_LOCATION_DISPLAY_NAME] = location.displayName
            prefs[KEY_LOCATION_TIMEZONE] = location.timeZoneId
        }
    }

    suspend fun setThemeMode(mode: String) {
        store.edit { prefs ->
            prefs[KEY_THEME] = mode
        }
    }

    val refreshMode: Flow<String> = store.data.map { prefs ->
        prefs[KEY_REFRESH_MODE] ?: REFRESH_MODE_ON_OPEN
    }

    fun getRefreshModeSync(): String = runBlocking {
        store.data.first()[KEY_REFRESH_MODE] ?: REFRESH_MODE_ON_OPEN
    }

    suspend fun setRefreshMode(mode: String) {
        store.edit { prefs ->
            prefs[KEY_REFRESH_MODE] = mode
        }
    }

    val notificationEnabled: Flow<Boolean> = store.data.map { prefs ->
        prefs[KEY_NOTIFICATION_ENABLED] ?: false
    }

    fun getNotificationEnabledSync(): Boolean = runBlocking {
        store.data.first()[KEY_NOTIFICATION_ENABLED] ?: false
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        store.edit { prefs ->
            prefs[KEY_NOTIFICATION_ENABLED] = enabled
        }
    }

    val notificationKpThreshold: Flow<Int> = store.data.map { prefs ->
        (prefs[KEY_NOTIFICATION_KP_THRESHOLD] ?: 5).coerceIn(1, 9)
    }

    fun getNotificationKpThresholdSync(): Int = runBlocking {
        (store.data.first()[KEY_NOTIFICATION_KP_THRESHOLD] ?: 5).coerceIn(1, 9)
    }

    suspend fun setNotificationKpThreshold(threshold: Int) {
        store.edit { prefs ->
            prefs[KEY_NOTIFICATION_KP_THRESHOLD] = threshold.coerceIn(1, 9)
        }
    }

    suspend fun setLastNotificationTime(timeMillis: Long) {
        store.edit { prefs ->
            prefs[KEY_LAST_NOTIFICATION_TIME] = timeMillis
        }
    }

    suspend fun getLastNotificationTime(): Long = store.data.first()[KEY_LAST_NOTIFICATION_TIME] ?: 0L
}
