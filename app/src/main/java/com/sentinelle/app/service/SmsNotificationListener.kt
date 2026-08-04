package com.sentinelle.app.service

import android.app.Person
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.os.BundleCompat
import com.sentinelle.app.data.PatternListEntity
import com.sentinelle.app.util.PatternManager
import com.sentinelle.app.util.PhoneNumberMatcher
import com.sentinelle.app.util.PreferencesManager
import com.sentinelle.app.util.SmsAction
import com.sentinelle.app.util.SmsNumberExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmsNotificationListener : NotificationListenerService() {
    companion object {
        private const val TAG = "SmsNotificationListener"
    }

    private var scope: CoroutineScope? = null

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val scope =
            scope ?: run {
                Log.d(TAG, "Notification received but service not connected, ignoring")
                return
            }

        val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(this)
        if (defaultSmsPackage == null) {
            Log.w(TAG, "No default SMS package found")
            return
        }
        if (sbn.packageName != defaultSmsPackage) {
            Log.d(TAG, "Skipping notification from ${sbn.packageName} (expected $defaultSmsPackage)")
            return
        }

        val extras = sbn.notification.extras ?: return

        val peopleUriList = BundleCompat.getParcelableArrayList(extras, "android.people.list", Person::class.java)?.map { it.uri }
        Log.d(
            TAG,
            "SMS notification extras: title=${extras.getString(
                "android.title",
            )}, text=${extras.getString("android.text")}, people=$peopleUriList",
        )

        val peopleList =
            BundleCompat
                .getParcelableArrayList(extras, "android.people.list", Person::class.java)
                ?.map { SmsNumberExtractor.PersonInfo(it.uri) }
        val messagingPerson =
            BundleCompat
                .getParcelable(extras, "android.messagingPerson", Person::class.java)
                ?.let { SmsNumberExtractor.PersonInfo(it.uri) }

        val senderNumber =
            SmsNumberExtractor.extractSenderNumber(
                peopleList = peopleList,
                messagingPerson = messagingPerson,
                title = extras.getString("android.title"),
                text = extras.getString("android.text"),
                bigText = extras.getString("android.bigText"),
                charSequenceText = extras.getCharSequence("android.text"),
            )
        if (senderNumber == null) {
            Log.w(TAG, "Could not extract sender number from notification extras")
            return
        }

        Log.d(TAG, "SMS notification from: $senderNumber (package: ${sbn.packageName})")

        val notificationKey = sbn.key

        scope.launch {
            try {
                val smsBlockingEnabled =
                    PreferencesManager.isSmsBlockingEnabled(this@SmsNotificationListener)
                if (!smsBlockingEnabled) return@launch

                val countryPrefixes =
                    try {
                        PreferencesManager.getCountryPrefixes(this@SmsNotificationListener)
                    } catch (e: Exception) {
                        setOf("33")
                    }

                val phoneNumber =
                    PhoneNumberMatcher.normalizePhoneNumber(senderNumber, countryPrefixes).firstOrNull()
                val action =
                    phoneNumber?.let {
                        PatternManager.evaluateSms(it, countryPrefixes, this@SmsNotificationListener)
                    }

                if (action is SmsAction.Hide) {
                    Log.d(
                        TAG,
                        "Masquage de la notification de SMS indésirable depuis : $senderNumber (le SMS n'est pas bloqué, seule la notification est masquée)",
                    )
                    cancelNotification(notificationKey)

                    try {
                        BlockEventLogger.log(
                            context = this@SmsNotificationListener,
                            channel = PatternListEntity.CHANNEL_SMS,
                            phoneNumber = phoneNumber ?: 0L,
                            source = action.source,
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error logging blocked SMS", e)
                    }

                    val shouldNotify =
                        PreferencesManager.getBlockedSmsNotification(this@SmsNotificationListener)
                    if (shouldNotify) {
                        withContext(Dispatchers.Main) {
                            NotificationService.sendBlockedSmsNotification(
                                this@SmsNotificationListener,
                                senderNumber,
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS notification", e)
            }
        }
    }

    override fun onListenerConnected() {
        Log.d(TAG, "Notification listener connected")
        if (scope == null) {
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        }
    }

    override fun onListenerDisconnected() {
        Log.d(TAG, "Notification listener disconnected")
        scope?.cancel()
        scope = null
    }
}
