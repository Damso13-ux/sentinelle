package com.sentinelle.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sentinelle.app.config.Config
import com.sentinelle.app.service.ListService
import com.sentinelle.app.util.PreferencesManager
import kotlinx.coroutines.flow.first

class ListUpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    companion object {
        const val WORK_NAME = "list_update"
        const val WORK_NAME_LAUNCH = "list_update_launch"
        const val KEY_REINSTALL = "reinstall"
        const val KEY_FORCE_UPDATE = "force_update"
        private const val TAG = "ListUpdateWorker"
    }

    override suspend fun doWork(): Result {
        val reinstall = inputData.getBoolean(KEY_REINSTALL, false)
        val forceUpdate = inputData.getBoolean(KEY_FORCE_UPDATE, false)
        Log.d(TAG, "Worker started, reinstall=$reinstall, forceUpdate=$forceUpdate")

        if (!reinstall && !forceUpdate && !shouldUpdateLists(applicationContext)) {
            Log.d(TAG, "Skipping list update: less than 12h since last update")
            return Result.success()
        }

        val success =
            if (reinstall) {
                ListService.reinstallLists(applicationContext)
            } else {
                ListService.updateLists(applicationContext)
            }
        return if (success) {
            Log.d(TAG, "List updated successfully")
            Result.success()
        } else {
            Log.e(TAG, "List update failed")
            Result.retry()
        }
    }

    private suspend fun shouldUpdateLists(context: Context): Boolean {
        val lastUpdate = PreferencesManager.getLastListUpdateFlow(context).first()
        if (lastUpdate == 0L) return true

        val intervalMillis = Config.LIST_UPDATE_INTERVAL_HOURS * 60 * 60 * 1000
        return System.currentTimeMillis() - lastUpdate >= intervalMillis
    }
}
