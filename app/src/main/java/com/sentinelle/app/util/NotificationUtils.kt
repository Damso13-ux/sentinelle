package com.sentinelle.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

object NotificationUtils {
    const val BLOCKED_CALLS_CHANNEL_ID = "blocked_calls_channel"
    const val BLOCKED_CALLS_CHANNEL_NAME = "Appels bloqués"
    const val BLOCKED_CALLS_CHANNEL_DESCRIPTION = "Notifications des appels bloqués."
    const val BLOCKED_CALLS_CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_DEFAULT
    const val BLOCKED_CALLS_NOTIFICATION_PRIORITY = NotificationCompat.PRIORITY_DEFAULT

    const val BLOCKED_UNKNOWN_CALLS_CHANNEL_ID = "blocked_unknown_calls_channel"
    const val BLOCKED_UNKNOWN_CALLS_CHANNEL_NAME = "Appels masqués bloqués"
    const val BLOCKED_UNKNOWN_CALLS_CHANNEL_DESCRIPTION =
        "Notifications des appels masqués bloqués."
    const val BLOCKED_UNKNOWN_CALLS_CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_DEFAULT
    const val BLOCKED_UNKNOWN_CALLS_NOTIFICATION_PRIORITY = NotificationCompat.PRIORITY_DEFAULT

    const val BLOCKED_SMS_CHANNEL_ID = "blocked_sms_channel"
    const val BLOCKED_SMS_CHANNEL_NAME = "SMS bloqués"
    const val BLOCKED_SMS_CHANNEL_DESCRIPTION = "Notifications des SMS bloqués."
    const val BLOCKED_SMS_CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_DEFAULT
    const val BLOCKED_SMS_NOTIFICATION_PRIORITY = NotificationCompat.PRIORITY_DEFAULT

    const val ALLOWED_CALLS_CHANNEL_ID = "allowed_calls_channel"
    const val ALLOWED_CALLS_CHANNEL_NAME = "Appels identifiés"
    const val ALLOWED_CALLS_CHANNEL_DESCRIPTION = "Notifications des appels identifiés."
    const val ALLOWED_CALLS_CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_HIGH
    const val ALLOWED_CALLS_NOTIFICATION_PRIORITY = NotificationCompat.PRIORITY_HIGH

    private fun createCallsNotificationChannel(context: Context) {
        val channel =
            NotificationChannel(
                BLOCKED_CALLS_CHANNEL_ID,
                BLOCKED_CALLS_CHANNEL_NAME,
                BLOCKED_CALLS_CHANNEL_IMPORTANCE,
            ).apply { description = BLOCKED_CALLS_CHANNEL_DESCRIPTION }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(channel)
    }

    private fun createUnknownCallsNotificationChannel(context: Context) {
        val channel =
            NotificationChannel(
                BLOCKED_UNKNOWN_CALLS_CHANNEL_ID,
                BLOCKED_UNKNOWN_CALLS_CHANNEL_NAME,
                BLOCKED_UNKNOWN_CALLS_CHANNEL_IMPORTANCE,
            ).apply { description = BLOCKED_UNKNOWN_CALLS_CHANNEL_DESCRIPTION }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(channel)
    }

    private fun createBlockedSmsNotificationChannel(context: Context) {
        val channel =
            NotificationChannel(
                BLOCKED_SMS_CHANNEL_ID,
                BLOCKED_SMS_CHANNEL_NAME,
                BLOCKED_SMS_CHANNEL_IMPORTANCE,
            ).apply { description = BLOCKED_SMS_CHANNEL_DESCRIPTION }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(channel)
    }

    private fun createAllowedCallsNotificationChannel(context: Context) {
        val channel =
            NotificationChannel(
                ALLOWED_CALLS_CHANNEL_ID,
                ALLOWED_CALLS_CHANNEL_NAME,
                ALLOWED_CALLS_CHANNEL_IMPORTANCE,
            ).apply { description = ALLOWED_CALLS_CHANNEL_DESCRIPTION }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(channel)
    }

    fun createAllNotificationChannels(context: Context) {
        createCallsNotificationChannel(context)
        createUnknownCallsNotificationChannel(context)
        createBlockedSmsNotificationChannel(context)
        createAllowedCallsNotificationChannel(context)
    }
}
