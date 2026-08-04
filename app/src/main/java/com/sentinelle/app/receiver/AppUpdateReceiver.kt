package com.sentinelle.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.sentinelle.app.util.PreferencesManager
import com.sentinelle.app.worker.ListUpdateWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED,
            -> {
                CoroutineScope(Dispatchers.IO).launch {
                    PreferencesManager.applyMdmRestrictions(context)
                }
            }
        }

        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        Log.d("AppUpdateReceiver", "App package replaced, enqueuing list update")
        val constraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val workData = workDataOf(ListUpdateWorker.KEY_FORCE_UPDATE to true)

        val updateRequest =
            OneTimeWorkRequestBuilder<ListUpdateWorker>()
                .setConstraints(constraints)
                .setInputData(workData)
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(
                ListUpdateWorker.WORK_NAME_LAUNCH,
                ExistingWorkPolicy.KEEP,
                updateRequest,
            )
    }
}
