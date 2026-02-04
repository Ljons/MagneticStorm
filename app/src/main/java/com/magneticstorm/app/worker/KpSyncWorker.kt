package com.magneticstorm.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.magneticstorm.app.MainActivity
import com.magneticstorm.app.R
import com.magneticstorm.app.data.preferences.PreferencesManager
import com.magneticstorm.app.data.repository.KpRepository
import kotlinx.coroutines.flow.first

private const val CHANNEL_ID = "magnetic_storm_alerts"
private const val NOTIFICATION_ID = 1
private const val COOLDOWN_MS = 6 * 60 * 60 * 1000L // 6 годин

class KpSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val prefs = PreferencesManager(context)
    private val kpRepository = KpRepository()

    override suspend fun doWork(): Result {
        return try {
            val mode = prefs.refreshMode.first()
            if (mode != PreferencesManager.REFRESH_MODE_BACKGROUND) return Result.success()

            val result = kpRepository.getForecast().getOrNull() ?: return Result.retry()
            val current = kpRepository.currentKpForNow(result) ?: return Result.success()

            val notificationsEnabled = prefs.notificationEnabled.first()
            if (!notificationsEnabled) return Result.success()

            val threshold = prefs.notificationKpThreshold.first()
            if (current.kp < threshold) return Result.success()

            val lastNotif = prefs.getLastNotificationTime()
            if (System.currentTimeMillis() - lastNotif < COOLDOWN_MS) return Result.success()

            showNotification(current.kp)
            prefs.setLastNotificationTime(System.currentTimeMillis())
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(kp: Double) {
        val channelId = CHANNEL_ID
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    applicationContext.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        val intent = Intent(applicationContext, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pending = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = applicationContext.getString(R.string.notification_title)
        val body = applicationContext.getString(R.string.notification_body, kp.toString().take(3))
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIFICATION_ID, notification)
    }
}
