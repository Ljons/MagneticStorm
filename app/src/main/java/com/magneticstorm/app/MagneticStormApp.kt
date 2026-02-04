package com.magneticstorm.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.magneticstorm.app.data.preferences.PreferencesManager
import com.magneticstorm.app.data.repository.KpRepository
import com.magneticstorm.app.data.repository.LocationRepository
import com.magneticstorm.app.ui.viewmodel.MainViewModel
import com.magneticstorm.app.worker.KpSyncWorker
import java.util.concurrent.TimeUnit

class MagneticStormApp : Application() {

    val preferencesManager by lazy { PreferencesManager(this) }
    val kpRepository by lazy { KpRepository() }
    val locationRepository by lazy { LocationRepository() }

    val mainViewModel: MainViewModel by lazy {
        MainViewModel(kpRepository, locationRepository, preferencesManager)
    }

    override fun onCreate() {
        super.onCreate()
        scheduleKpSyncIfNeeded()
    }

    /**
     * Планує або скасовує фонове оновлення Kp залежно від налаштування.
     * Викликати при старті додатку та при зміні режиму оновлення в Налаштуваннях.
     */
    fun scheduleKpSyncIfNeeded() {
        val wm = WorkManager.getInstance(this)
        if (preferencesManager.getRefreshModeSync() == PreferencesManager.REFRESH_MODE_BACKGROUND) {
            val request = PeriodicWorkRequestBuilder<KpSyncWorker>(3, TimeUnit.HOURS)
                .build()
            wm.enqueueUniquePeriodicWork(
                "kp_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        } else {
            wm.cancelUniqueWork("kp_sync")
        }
    }
}
