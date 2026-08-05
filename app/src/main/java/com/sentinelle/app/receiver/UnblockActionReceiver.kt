package com.sentinelle.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.sentinelle.app.data.PatternListEntity
import com.sentinelle.app.service.PatternService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Handles the "Ce n'est pas un spam" quick action on a blocked-call
// notification: adds the number to the personal allow list (checked before
// any block list or the heuristic, so it takes effect immediately) without
// making the user open the app and search for it manually.
class UnblockActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val phoneNumber = intent.getLongExtra(EXTRA_PHONE_NUMBER, 0L)
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (phoneNumber == 0L) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pattern = "+$phoneNumber"
                val alreadyAllowed = PatternService.detectDuplicate(pattern, appContext) != null
                if (!alreadyAllowed) {
                    PatternService.addUserPattern(
                        pattern = pattern,
                        name = "Débloqué depuis une notification",
                        listId = PatternListEntity.USER_ALLOW_LIST_ID,
                        context = appContext,
                    )
                }
                withContext(Dispatchers.Main) {
                    Toast
                        .makeText(
                            appContext,
                            if (alreadyAllowed) "Numéro déjà autorisé" else "Numéro débloqué et autorisé",
                            Toast.LENGTH_SHORT,
                        ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error unblocking number from notification action", e)
            } finally {
                if (notificationId != -1) {
                    NotificationManagerCompat.from(appContext).cancel(notificationId)
                }
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_PHONE_NUMBER = "phone_number"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val TAG = "UnblockActionReceiver"
    }
}
