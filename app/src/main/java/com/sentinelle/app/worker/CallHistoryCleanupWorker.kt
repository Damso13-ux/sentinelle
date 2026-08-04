package com.sentinelle.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sentinelle.app.config.Config
import com.sentinelle.app.data.AppDatabase
import java.util.concurrent.TimeUnit

// Purges the local-only call_history table (used by HeuristicSpamDetector)
// beyond the retention window, so it never grows unbounded on-device. This
// history is never uploaded — see PreferencesManager.isCallHistoryTrackingEnabled.
class CallHistoryCleanupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    companion object {
        const val WORK_NAME = "call_history_cleanup"
        private const val TAG = "CallHistoryCleanupWorker"
    }

    override suspend fun doWork(): Result =
        try {
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(Config.CALL_HISTORY_RETENTION_DAYS)
            AppDatabase
                .getInstance(applicationContext)
                .callHistoryDao()
                .deleteOlderThan(cutoff)
            Log.d(TAG, "Call history cleanup successful")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Call history cleanup failed", e)
            Result.retry()
        }
}
