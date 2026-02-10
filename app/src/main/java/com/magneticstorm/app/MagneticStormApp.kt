package com.magneticstorm.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.magneticstorm.app.data.preferences.PreferencesManager
import com.magneticstorm.app.data.repository.KpRepository
import com.magneticstorm.app.data.repository.LocationRepository
import com.magneticstorm.app.receiver.DeviceUnlockReceiver
import com.magneticstorm.app.ui.viewmodel.MainViewModel
import com.magneticstorm.app.worker.KpSyncWorker
import com.magneticstorm.app.worker.KpWidgetRefreshWorker
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
        registerUnlockReceiver()
    }

    /** Реєстрація отримання розблокування пристрою для оновлення віджета. */
    private fun registerUnlockReceiver() {
        val receiver = DeviceUnlockReceiver()
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
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
