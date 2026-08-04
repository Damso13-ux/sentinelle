package com.sentinelle.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sentinelle.app.network.NetworkService
import com.sentinelle.app.util.PreferencesManager

class HealthCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    companion object {
        const val WORK_NAME = "health_check"
        private const val TAG = "HealthCheckWorker"
    }

    override suspend fun doWork(): Result {
        val deviceId = PreferencesManager.getDeviceId(applicationContext)
        val apiKey = PreferencesManager.getApiKey(applicationContext)

        if (apiKey.isNullOrBlank()) {
            Log.d(TAG, "No API key configured, skipping health check")
            return Result.success()
        }

        return try {
            val networkService = NetworkService()
            networkService.healthCheck(deviceId, apiKey)
            Log.d(TAG, "Health check successful")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Health check failed", e)
            Result.retry()
        }
    }
}
