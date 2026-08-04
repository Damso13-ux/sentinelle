package com.sentinelle.app.util

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.sentinelle.app.service.SmsNotificationListener

/**
 * Utility for managing call screening permissions
 */
object PermissionUtils {
    private const val TAG = "PermissionUtils"

    /**
     * Check if the app is set as the default call screening app
     */
    fun isCallScreeningEnabled(context: Context): Boolean =
        try {
            val telecomManager =
                context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager?

            // Check if app is default dialer
            val isDefaultDialer = telecomManager?.defaultDialerPackage == context.packageName

            // Check if call screening role is granted (API 29+)
            val hasCallScreeningRole = isCallScreeningRoleGranted(context)

            Log.d(
                TAG,
                "Call screening status - isDefaultDialer: $isDefaultDialer, hasCallScreeningRole: $hasCallScreeningRole",
            )

            // Return true if any of the call screening mechanisms are enabled
            isDefaultDialer || hasCallScreeningRole
        } catch (t: Throwable) {
            Log.e(TAG, "Error checking call screening status", t)
            false
        }

    /**
     * Check if call screening role is granted (API 29+)
     */
    private fun isCallScreeningRoleGranted(context: Context): Boolean =
        try {
            val roleManager =
                context.getSystemService(Context.ROLE_SERVICE) as android.app.role.RoleManager?
            roleManager?.isRoleHeld(android.app.role.RoleManager.ROLE_CALL_SCREENING) ?: false
        } catch (t: Throwable) {
            Log.e(TAG, "Error checking call screening role", t)
            false
        }

    /**
     * Create an intent to request the call screening role (API 29+).
     * Returns the intent to be launched by the caller.
     */
    fun createCallScreeningRoleIntent(context: Context): Intent? =
        try {
            val roleManager =
                context.getSystemService(Context.ROLE_SERVICE) as android.app.role.RoleManager?
            roleManager?.createRequestRoleIntent(android.app.role.RoleManager.ROLE_CALL_SCREENING)
        } catch (t: Throwable) {
            Log.e(TAG, "Error creating call screening role intent", t)
            null
        }

    /**
     * Open call screening settings
     */
    fun openCallScreeningSettings(context: Context) {
        try {
            // Try to open default apps settings first (most direct approach)
            val intent =
                Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                context.startActivity(intent)
                return
            }
            // Fallback to general settings
            val fallbackIntent =
                Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(fallbackIntent)
        } catch (t: Throwable) {
            Log.e(TAG, "Error opening call screening settings", t)
        }
    }

    /**
     * Open phone settings as fallback
     */
    private fun openPhoneSettings(context: Context) {
        val settingsIntents =
            listOf(
                // Try to open default apps settings for calls
                Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
                // Try phone app settings
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:com.android.phone".toUri()
                },
                // Try Google Dialer settings
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:com.google.android.dialer".toUri()
                },
                // General phone settings
                Intent(Settings.ACTION_SOUND_SETTINGS),
                // App settings as last resort
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:${context.packageName}".toUri()
                },
            )

        for (intent in settingsIntents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (context.packageManager.resolveActivity(intent, 0) != null) {
                    context.startActivity(intent)
                    Log.d(TAG, "Opened settings with intent: ${intent.action}")
                    return
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Could not open settings with intent: ${intent.action}", t)
            }
        }

        Log.e(TAG, "Failed to open any settings")
    }

    fun openAppNotificationsSettings(context: Context) {
        try {
            val intent =
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            context.startActivity(intent)
        } catch (t: Throwable) {
            Log.e(TAG, "Error opening app notifications settings", t)
            // fallback try other settings
            openPhoneSettings(context)
        }
    }

    fun isNotificationPermissionGranted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val componentName = ComponentName(context, SmsNotificationListener::class.java)
        val enabledListeners =
            Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ) ?: return false
        return enabledListeners.contains(componentName.flattenToString())
    }

    fun openNotificationListenerSettings(context: Context) {
        try {
            val intent =
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
        } catch (t: Throwable) {
            Log.e(TAG, "Error opening notification listener settings", t)
            openPhoneSettings(context)
        }
    }

    /**
     * Check whether the app can draw over other apps (needed for the caller-ID
     * bubble overlay). This is a "special" permission, not a runtime prompt.
     */
    fun canDrawOverlays(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }

    /**
     * Open the system screen where the user grants the "draw over other apps"
     * special permission.
     */
    fun openOverlayPermissionSettings(context: Context) {
        try {
            val intent =
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri(),
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
        } catch (t: Throwable) {
            Log.e(TAG, "Error opening overlay permission settings", t)
        }
    }
}
