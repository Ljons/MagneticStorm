package com.magneticstorm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.magneticstorm.app.worker.KpWidgetRefreshWorker

/**
 * При розблокуванні пристрою планує одноразове оновлення даних віджета (Kp).
 */
class DeviceUnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_USER_PRESENT) return
        val request = OneTimeWorkRequestBuilder<KpWidgetRefreshWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        private const val WORK_NAME = "widget_refresh_on_unlock"
    }
}
