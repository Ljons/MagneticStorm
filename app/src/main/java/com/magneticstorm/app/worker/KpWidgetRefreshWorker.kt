package com.magneticstorm.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.magneticstorm.app.data.preferences.PreferencesManager
import com.magneticstorm.app.data.repository.KpRepository
import com.magneticstorm.app.widget.WidgetUpdateHelper

/**
 * Одноразовий воркер: завантажує Kp та оновлює віджет (повна картка «Рівень на сьогодні»).
 * Викликається при розблокуванні пристрою.
 */
class KpWidgetRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val kpRepository = KpRepository()
    private val prefs = PreferencesManager(context)

    override suspend fun doWork(): Result {
        return try {
            val result = kpRepository.getForecast().getOrNull() ?: return Result.retry()
            val current = kpRepository.currentKpForNow(result) ?: return Result.success()
            val location = prefs.getSavedLocationSync()
            val todayStr = kpRepository.getTodayString(location.timeZoneId)
            val byDay = kpRepository.groupByDay(result, location.timeZoneId, todayStr, daysBack = 4, daysForward = 3)
            val todayRecords = byDay[todayStr].orEmpty()
            val state = WidgetUpdateHelper.buildCardState(current, todayRecords, location.timeZoneId, location.displayName)
            WidgetUpdateHelper.saveAndUpdateWidget(applicationContext, state)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
