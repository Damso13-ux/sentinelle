package com.sentinelle.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sentinelle.app.config.Config
import com.sentinelle.app.util.PreferencesManager
import com.sentinelle.app.worker.CallHistoryCleanupWorker
import com.sentinelle.app.worker.HealthCheckWorker
import com.sentinelle.app.worker.ListUpdateWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SentinelleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        readMdmConfig()
        scheduleListUpdate()
        scheduleHealthCheck()
        scheduleCallHistoryCleanup()
    }

    private fun readMdmConfig() {
        CoroutineScope(Dispatchers.IO).launch {
            PreferencesManager.applyMdmRestrictions(this@SentinelleApplication)
        }
    }

    private fun scheduleListUpdate() {
        val constraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(true)
                .build()

        val updateRequest =
            PeriodicWorkRequestBuilder<ListUpdateWorker>(
                Config.BACKGROUND_UPDATE_INTERVAL_HOURS,
                TimeUnit.HOURS,
            ).setConstraints(constraints)
                .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ListUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            updateRequest,
        )
    }

    private fun scheduleHealthCheck() {
        val constraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val request =
            PeriodicWorkRequestBuilder<HealthCheckWorker>(
                Config.ORGANIZATION_DEVICE_HEALTH_CHECK_INTERVAL_HOURS,
                TimeUnit.HOURS,
            ).setConstraints(constraints)
                .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            HealthCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun scheduleCallHistoryCleanup() {
        val request =
            PeriodicWorkRequestBuilder<CallHistoryCleanupWorker>(
                Config.CALL_HISTORY_CLEANUP_INTERVAL_HOURS,
                TimeUnit.HOURS,
            ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            CallHistoryCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
