package com.sentinelle.app.service

import android.content.Intent
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.PhoneNumberUtils
import android.util.Log
import com.sentinelle.app.arcep.ArcepNpvPrefixes
import com.sentinelle.app.data.AppDatabase
import com.sentinelle.app.data.CallHistoryEntity
import com.sentinelle.app.data.NumberLabelEntity
import com.sentinelle.app.data.PatternListEntity
import com.sentinelle.app.util.BlockSource
import com.sentinelle.app.util.CallAction
import com.sentinelle.app.util.PatternManager
import com.sentinelle.app.util.PermissionUtils
import com.sentinelle.app.util.PhoneNumberMatcher
import com.sentinelle.app.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class CallScreeningService : CallScreeningService() {
    private companion object {
        private const val TAG = "CallScreeningService"
    }

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        // CallScreeningService has no async response path — respondToCall()
        // must be called before this callback returns, so the decision can't
        // be a fire-and-forget coroutine. But onScreenCall may run on the
        // app's main thread, and the decision now does real DB/DataStore I/O
        // (pattern lists, call history, heuristic scoring) — dispatch that
        // work to Dispatchers.IO explicitly rather than letting it execute
        // wherever this callback happened to be invoked from.
        runBlocking(Dispatchers.IO) {
            screenCall(callDetails)
        }
    }

    private fun screenCall(callDetails: Call.Details) {
        if (!runBlocking {
                try {
                    PreferencesManager.isCallFilteringEnabled(this@CallScreeningService)
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading call filtering enabled preference", e)
                    true
                }
            }
        ) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val rawPhoneNumber = callDetails.handle?.schemeSpecificPart
        Log.d(TAG, "Incoming call from: $rawPhoneNumber")

        val countryPrefixes =
            runBlocking {
                try {
                    PreferencesManager.getCountryPrefixes(this@CallScreeningService)
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading country prefixes preference", e)
                    setOf("33")
                }
            }

        val phoneNumber =
            rawPhoneNumber?.let {
                PhoneNumberMatcher.normalizePhoneNumber(it, countryPrefixes).firstOrNull()
            }

        var blockSource: BlockSource? = null
        var allowedPatternName: String? = null

        if (phoneNumber != null) {
            val action = PatternManager.evaluateCall(phoneNumber, countryPrefixes, this)
            if (action is CallAction.Allow) {
                allowedPatternName = action.pattern.name
                Log.d(TAG, "Allow list match: ${action.pattern.name} for $rawPhoneNumber")
            } else if (action is CallAction.Block) {
                blockSource = action.source
            } else {
                val onlyContactsAllowed =
                    runBlocking {
                        try {
                            PreferencesManager.isOnlyContactsAllowed(this@CallScreeningService)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error reading allow only contacts preference", e)
                            false
                        }
                    }
                if (onlyContactsAllowed) {
                    blockSource = BlockSource.OnlyContacts
                }
            }
        } else {
            val shouldBlockAnonymous =
                runBlocking {
                    try {
                        PreferencesManager.getBlockAnonymousCalls(this@CallScreeningService) ||
                            PreferencesManager.isOnlyContactsAllowed(this@CallScreeningService)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading anonymous call preferences", e)
                        false
                    }
                }
            if (shouldBlockAnonymous) {
                blockSource = BlockSource.Anonymous
            }
        }

        if (phoneNumber != null) {
            try {
                val historyTrackingEnabled =
                    runBlocking {
                        try {
                            PreferencesManager.isCallHistoryTrackingEnabled(this@CallScreeningService)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error reading call history tracking preference", e)
                            false
                        }
                    }
                if (historyTrackingEnabled) {
                    AppDatabase
                        .getInstance(this@CallScreeningService)
                        .callHistoryDao()
                        .insert(
                            CallHistoryEntity(
                                phoneNumber = phoneNumber,
                                timestamp = System.currentTimeMillis(),
                                wasBlocked = blockSource != null,
                            ),
                        )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error logging call history", e)
            }
        }

        if (blockSource != null) {
            Log.d(TAG, "Blocking call from: $rawPhoneNumber")
            respondToCall(
                callDetails,
                CallResponse
                    .Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(true)
                    .setSkipNotification(true)
                    .build(),
            )

            try {
                BlockEventLogger.log(
                    context = this@CallScreeningService,
                    channel = PatternListEntity.CHANNEL_PHONE,
                    phoneNumber = phoneNumber ?: 0L,
                    source = blockSource,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error logging blocked call", e)
            }

            if (runBlocking {
                    try {
                        PreferencesManager.getBlockedCallNotification(this@CallScreeningService)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading blocked call notification preference", e)
                        false
                    }
                }
            ) {
                val label =
                    phoneNumber?.let {
                        try {
                            AppDatabase
                                .getInstance(this@CallScreeningService)
                                .numberLabelDao()
                                .getByPhoneNumber(it)
                                ?.let { entity -> NumberLabelEntity.displayName(entity.category) }
                        } catch (e: Exception) {
                            null
                        }
                    }
                NotificationService.sendBlockedCallNotification(this, rawPhoneNumber ?: "", label, phoneNumber)
            }
        } else {
            Log.d(TAG, "Allowing call from: $rawPhoneNumber")
            respondToCall(callDetails, CallResponse.Builder().build())
            if (allowedPatternName != null) {
                NotificationService.sendAllowedCallNotification(this, rawPhoneNumber ?: "", allowedPatternName)
            }
            maybeShowCallerIdBubble(phoneNumber, rawPhoneNumber, allowedPatternName)
        }
    }

    // Only shown when there's actually something to say about the number -
    // a local label, an allow-list match, or the official ARCEP demarchage
    // badge - never for ordinary unmatched calls, to avoid noise.
    private fun maybeShowCallerIdBubble(
        phoneNumber: Long?,
        rawPhoneNumber: String?,
        allowedPatternName: String?,
    ) {
        if (phoneNumber == null || rawPhoneNumber == null) return

        try {
            val bubbleEnabled =
                runBlocking { PreferencesManager.isCallerIdBubbleEnabled(this@CallScreeningService) }
            if (!bubbleEnabled || !PermissionUtils.canDrawOverlays(this)) return

            val localLabel =
                AppDatabase
                    .getInstance(this)
                    .numberLabelDao()
                    .getByPhoneNumber(phoneNumber)
                    ?.let { NumberLabelEntity.displayName(it.category) }

            val label =
                localLabel
                    ?: allowedPatternName
                    ?: if (ArcepNpvPrefixes.isNpvNumber(phoneNumber)) "Démarchage officiel (ARCEP)" else null
            if (label == null) return

            val displayNumber = PhoneNumberUtils.formatNumberToE164(phoneNumber.toString(), "FR") ?: rawPhoneNumber
            val intent =
                Intent(this, CallerIdOverlayService::class.java).apply {
                    putExtra(CallerIdOverlayService.EXTRA_DISPLAY_NUMBER, displayNumber)
                    putExtra(CallerIdOverlayService.EXTRA_LABEL, label)
                }
            startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing caller ID bubble", e)
        }
    }
}
