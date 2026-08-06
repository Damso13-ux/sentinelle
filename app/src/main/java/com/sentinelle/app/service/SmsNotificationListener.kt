package com.sentinelle.app.service

import android.app.Person
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.os.BundleCompat
import com.sentinelle.app.data.AppDatabase
import com.sentinelle.app.data.PatternListEntity
import com.sentinelle.app.data.SmsHistoryEntity
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

        // Deliberately no logging of the notification extras here. They
        // carry the message body, the sender's display name and contact
        // URIs, and this runs for every incoming SMS — dumping that into
        // logcat contradicts the whole point of processing messages
        // on-device only. Sender extraction is exercised by
        // SmsNumberExtractorTest instead of by reading logs.
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
        // Kept separate from sender extraction: some senders use short
        // alphanumeric IDs ("FreeMobile", marketing short codes...) with no
        // phone number at all — content-based keyword lists still need to
        // see the text, so we no longer bail out just because there's no
        // resolvable number.
        val messageText = extras.getString("android.bigText") ?: extras.getString("android.text")
        if (senderNumber == null && messageText.isNullOrBlank()) {
            Log.w(TAG, "Could not extract sender number or message text from notification extras")
            return
        }

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
                    senderNumber?.let { PhoneNumberMatcher.normalizePhoneNumber(it, countryPrefixes).firstOrNull() }
                val action =
                    PatternManager.evaluateSms(phoneNumber, messageText, countryPrefixes, this@SmsNotificationListener)

                if (phoneNumber != null) {
                    try {
                        val historyTrackingEnabled =
                            PreferencesManager.isCallHistoryTrackingEnabled(this@SmsNotificationListener)
                        if (historyTrackingEnabled) {
                            AppDatabase
                                .getInstance(this@SmsNotificationListener)
                                .smsHistoryDao()
                                .insert(
                                    SmsHistoryEntity(
                                        phoneNumber = phoneNumber,
                                        timestamp = System.currentTimeMillis(),
                                        wasBlocked = action is SmsAction.Hide,
                                    ),
                                )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error logging SMS history", e)
                    }
                }

                if (action is SmsAction.Hide) {
                    // Only the notification is hidden — the SMS itself stays
                    // in the messaging app, since Sentinelle isn't the
                    // default SMS handler. Sender omitted from the log on
                    // purpose; the blocked event is recorded in the DB below
                    // and visible in the dashboard.
                    Log.d(TAG, "Hiding notification for a filtered SMS")
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
                                senderNumber ?: "Expéditeur inconnu",
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
