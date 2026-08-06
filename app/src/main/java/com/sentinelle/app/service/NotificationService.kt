package com.sentinelle.app.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sentinelle.app.MainActivity
import com.sentinelle.app.R
import com.sentinelle.app.receiver.UnblockActionReceiver
import com.sentinelle.app.util.NotificationUtils

object NotificationService {
    private const val TAG = "NotificationService"
    private val NOTIFICATION_ICON = R.drawable.notification_icon

    fun sendBlockedCallNotification(
        context: Context,
        phoneNumber: String?,
        label: String? = null,
        normalizedPhoneNumber: Long? = null,
    ) {
        if (phoneNumber.isNullOrBlank()) {
            sendUnknownBlockedCallNotification(context)
        } else {
            sendKnownBlockedCallNotification(context, phoneNumber, label, normalizedPhoneNumber)
        }
    }

    private fun sendUnknownBlockedCallNotification(context: Context) {
        Log.d(TAG, "Sending notification for blocked unknown call")

        val notificationId = "unknown-caller-${System.currentTimeMillis()}".hashCode()

        val notification =
            NotificationCompat
                .Builder(context, NotificationUtils.BLOCKED_UNKNOWN_CALLS_CHANNEL_ID)
                .setSmallIcon(NOTIFICATION_ICON)
                .setContentTitle("Appel bloqué")
                .setContentText("Numéro masqué")
                .setPriority(NotificationUtils.BLOCKED_UNKNOWN_CALLS_NOTIFICATION_PRIORITY)
                .setAutoCancel(true)

        send(context, notificationId, notification)
    }

    private fun sendKnownBlockedCallNotification(
        context: Context,
        phoneNumber: String,
        label: String? = null,
        normalizedPhoneNumber: Long? = null,
    ) {
        Log.d(TAG, "Sending blocked-call notification")

        val notificationId = "$phoneNumber-${System.currentTimeMillis()}".hashCode()
        val contentText = if (label != null) "$label · $phoneNumber" else phoneNumber

        val notification =
            NotificationCompat
                .Builder(context, NotificationUtils.BLOCKED_CALLS_CHANNEL_ID)
                .setSmallIcon(NOTIFICATION_ICON)
                .setContentTitle("Appel bloqué")
                .setContentText(contentText)
                .setPriority(NotificationUtils.BLOCKED_CALLS_NOTIFICATION_PRIORITY)
                .setAutoCancel(true)

        if (normalizedPhoneNumber != null) {
            val unblockIntent =
                Intent(context, UnblockActionReceiver::class.java).apply {
                    putExtra(UnblockActionReceiver.EXTRA_PHONE_NUMBER, normalizedPhoneNumber)
                    putExtra(UnblockActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                }
            val unblockPendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    notificationId,
                    unblockIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            notification.addAction(NOTIFICATION_ICON, "Ce n'est pas un spam", unblockPendingIntent)
        }

        send(context, notificationId, notification)
    }

    private fun send(
        context: Context,
        notificationId: Int,
        notification: NotificationCompat.Builder,
    ) {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        notification.setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.i(TAG, "POST_NOTIFICATIONS permission not granted")
                return
            }
            notify(notificationId, notification.build())
        }
    }

    fun sendBlockedSmsNotification(
        context: Context,
        phoneNumber: String,
    ) {
        Log.d(TAG, "Sending blocked-SMS notification")

        val notificationId = "sms-$phoneNumber-${System.currentTimeMillis()}".hashCode()

        val notification =
            NotificationCompat
                .Builder(context, NotificationUtils.BLOCKED_SMS_CHANNEL_ID)
                .setSmallIcon(NOTIFICATION_ICON)
                .setContentTitle("SMS bloqué")
                .setContentText(phoneNumber)
                .setPriority(NotificationUtils.BLOCKED_SMS_NOTIFICATION_PRIORITY)
                .setAutoCancel(true)

        send(context, notificationId, notification)
    }

    fun sendAllowedCallNotification(
        context: Context,
        phoneNumber: String,
        patternName: String,
    ) {
        Log.d(TAG, "Sending identified-call notification ($patternName)")

        val notificationId = "allowed-$phoneNumber-${System.currentTimeMillis()}".hashCode()

        val notification =
            NotificationCompat
                .Builder(context, NotificationUtils.ALLOWED_CALLS_CHANNEL_ID)
                .setSmallIcon(NOTIFICATION_ICON)
                .setContentTitle("Appel identifié")
                .setContentText("$patternName - $phoneNumber")
                .setPriority(NotificationUtils.ALLOWED_CALLS_NOTIFICATION_PRIORITY)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.i(TAG, "POST_NOTIFICATIONS permission not granted")
                return
            }
            notify(notificationId, notification.build())
        }
    }
}
