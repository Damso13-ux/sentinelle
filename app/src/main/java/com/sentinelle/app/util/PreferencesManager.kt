package com.sentinelle.app.util

import android.content.Context
import android.content.RestrictionsManager
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

// Extension to create DataStore instance with corruption handler for protobuf format changes
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_preferences",
    corruptionHandler =
        ReplaceFileCorruptionHandler { context ->
            emptyPreferences()
        },
)

/**
 * Manager for app preferences using DataStore
 */
object PreferencesManager {
    private val CALL_FILTERING_ENABLED_KEY = booleanPreferencesKey("filtering_enabled")
    private val BLOCK_ANONYMOUS_CALLS_KEY = booleanPreferencesKey("block_anonymous_calls")
    private val ALLOW_ONLY_CONTACTS_CALLS_KEY = booleanPreferencesKey("allow_only_contacts_calls")
    private val BLOCKED_CALL_NOTIFICATION_KEY = booleanPreferencesKey("blocked_call_notification")
    private val DEVICE_ID_KEY = stringPreferencesKey("device_id")
    private val COUNTRY_PREFIXES_KEY = stringSetPreferencesKey("country_prefixes")
    private val COUNTRY_CODES_KEY = stringPreferencesKey("country_codes")
    private val API_KEY_KEY = stringPreferencesKey("api_key")
    private val LAST_LIST_UPDATE_KEY = longPreferencesKey("last_list_update")
    private val SMS_BLOCKING_ENABLED_KEY = booleanPreferencesKey("sms_blocking_enabled")
    private val BLOCKED_SMS_NOTIFICATION_KEY = booleanPreferencesKey("blocked_sms_notification")

    // Off by default: no call/SMS history is written and no heuristic scoring
    // runs unless the user explicitly opts in (see CallSettingsSheet). Local
    // only — this is never uploaded anywhere.
    private val CALL_HISTORY_TRACKING_ENABLED_KEY = booleanPreferencesKey("call_history_tracking_enabled")

    // Off by default: requires the SYSTEM_ALERT_WINDOW special permission,
    // requested only once the user turns this on.
    private val CALLER_ID_BUBBLE_ENABLED_KEY = booleanPreferencesKey("caller_id_bubble_enabled")

    private val DEFAULT_COUNTRY_PREFIXES = setOf("33")
    private const val DEFAULT_COUNTRY_CODES = "FR"

    /**
     * Get the flow of the call filtering enabled setting
     */
    fun getCallFilteringEnabledFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[CALL_FILTERING_ENABLED_KEY] ?: true
        }

    /**
     * Set the call filtering enabled setting
     */
    suspend fun setCallFilteringEnabled(
        context: Context,
        enabled: Boolean,
    ) {
        context.dataStore.edit { preferences ->
            preferences[CALL_FILTERING_ENABLED_KEY] = enabled
        }
    }

    /**
     * Get the current value of the call filtering enabled setting (suspend function)
     */
    suspend fun isCallFilteringEnabled(context: Context): Boolean =
        context.dataStore.data
            .map { preferences ->
                preferences[CALL_FILTERING_ENABLED_KEY] ?: true
            }.first()

    /**
     * Get the flow of block anonymous calls setting
     */
    fun getBlockAnonymousCallsFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[BLOCK_ANONYMOUS_CALLS_KEY] ?: false
        }

    /**
     * Set the block anonymous calls setting
     */
    suspend fun setBlockAnonymousCalls(
        context: Context,
        blockAnonymous: Boolean,
    ) {
        context.dataStore.edit { preferences ->
            preferences[BLOCK_ANONYMOUS_CALLS_KEY] = blockAnonymous
        }
    }

    /**
     * Get the current value of block anonymous calls setting (suspend function)
     */
    suspend fun getBlockAnonymousCalls(context: Context): Boolean =
        context.dataStore.data
            .map { preferences ->
                preferences[BLOCK_ANONYMOUS_CALLS_KEY] ?: false
            }.first()

    /**
     * Get the flow of the allow only contacts setting
     */
    fun getAllowOnlyContactsFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[ALLOW_ONLY_CONTACTS_CALLS_KEY] ?: false
        }

    /**
     * Set the allow only contacts setting
     */
    suspend fun setAllowOnlyContacts(
        context: Context,
        allowOnlyContacts: Boolean,
    ) {
        context.dataStore.edit { preferences ->
            preferences[ALLOW_ONLY_CONTACTS_CALLS_KEY] = allowOnlyContacts
        }
    }

    /**
     * Get the current value of the allow only contacts setting (suspend function)
     */
    suspend fun isOnlyContactsAllowed(context: Context): Boolean =
        context.dataStore.data
            .map { preferences ->
                preferences[ALLOW_ONLY_CONTACTS_CALLS_KEY] ?: false
            }.first()

    suspend fun getCountryPrefixes(context: Context): Set<String> =
        context.dataStore.data
            .map { preferences ->
                preferences[COUNTRY_PREFIXES_KEY] ?: DEFAULT_COUNTRY_PREFIXES
            }.first()

    suspend fun getCountryCodes(context: Context): String =
        context.dataStore.data
            .map { preferences ->
                preferences[COUNTRY_CODES_KEY] ?: DEFAULT_COUNTRY_CODES
            }.first()

    suspend fun setCountryCodes(
        context: Context,
        codes: String,
    ) {
        context.dataStore.edit { preferences ->
            preferences[COUNTRY_CODES_KEY] = codes
        }
    }

    suspend fun getApiKey(context: Context): String? =
        context.dataStore.data
            .map { preferences ->
                preferences[API_KEY_KEY]
            }.first()

    suspend fun setApiKey(
        context: Context,
        apiKey: String?,
    ) {
        context.dataStore.edit { preferences ->
            if (apiKey != null) {
                preferences[API_KEY_KEY] = apiKey
            } else {
                preferences.remove(API_KEY_KEY)
            }
        }
    }

    suspend fun applyMdmRestrictions(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return

        val restrictionsManager =
            context.getSystemService(Context.RESTRICTIONS_SERVICE) as? RestrictionsManager
                ?: return

        val restrictions = restrictionsManager.applicationRestrictions ?: return

        restrictions.getString("api_key")?.let { apiKey ->
            if (apiKey.isNotBlank()) {
                setApiKey(context, apiKey)
            } else {
                setApiKey(context, null)
            }
        }
    }

    fun getLastListUpdateFlow(context: Context): Flow<Long> =
        context.dataStore.data.map { preferences ->
            preferences[LAST_LIST_UPDATE_KEY] ?: 0L
        }

    suspend fun setLastListUpdate(
        context: Context,
        timestamp: Long,
    ) {
        context.dataStore.edit { preferences ->
            preferences[LAST_LIST_UPDATE_KEY] = timestamp
        }
    }

    suspend fun getDeviceId(context: Context): String {
        val stored =
            context.dataStore.data
                .map { preferences ->
                    preferences[DEVICE_ID_KEY]
                }.first()
        if (stored != null) return stored

        val newId = UUID.randomUUID().toString().uppercase()
        context.dataStore.edit { preferences ->
            preferences[DEVICE_ID_KEY] = newId
        }
        return newId
    }

    suspend fun clear(context: Context) {
        context.dataStore.edit { it.clear() }
    }

    suspend fun setBlockedCallNotification(
        context: Context,
        blockedCallNotifications: Boolean,
    ) {
        context.dataStore.edit { preferences ->
            preferences[BLOCKED_CALL_NOTIFICATION_KEY] = blockedCallNotifications
        }
    }

    fun getBlockedCallNotificationFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[BLOCKED_CALL_NOTIFICATION_KEY] ?: false
        }

    suspend fun getBlockedCallNotification(context: Context): Boolean =
        context.dataStore.data
            .map { preferences ->
                preferences[BLOCKED_CALL_NOTIFICATION_KEY] ?: false
            }.first()

    fun getSmsBlockingEnabledFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[SMS_BLOCKING_ENABLED_KEY] ?: true
        }

    suspend fun isSmsBlockingEnabled(context: Context): Boolean =
        context.dataStore.data
            .map { preferences ->
                preferences[SMS_BLOCKING_ENABLED_KEY] ?: true
            }.first()

    suspend fun setSmsBlockingEnabled(
        context: Context,
        enabled: Boolean,
    ) {
        context.dataStore.edit { preferences ->
            preferences[SMS_BLOCKING_ENABLED_KEY] = enabled
        }
    }

    fun getBlockedSmsNotificationFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[BLOCKED_SMS_NOTIFICATION_KEY] ?: false
        }

    suspend fun getBlockedSmsNotification(context: Context): Boolean =
        context.dataStore.data
            .map { preferences ->
                preferences[BLOCKED_SMS_NOTIFICATION_KEY] ?: false
            }.first()

    suspend fun setBlockedSmsNotification(
        context: Context,
        enabled: Boolean,
    ) {
        context.dataStore.edit { preferences ->
            preferences[BLOCKED_SMS_NOTIFICATION_KEY] = enabled
        }
    }

    /**
     * Whether the on-device heuristic spam detector is allowed to keep a
     * short local history of filtered calls and SMS (never uploaded) so it
     * can score frequency/timing signals. Off by default.
     */
    fun getCallHistoryTrackingEnabledFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[CALL_HISTORY_TRACKING_ENABLED_KEY] ?: false
        }

    suspend fun isCallHistoryTrackingEnabled(context: Context): Boolean =
        context.dataStore.data
            .map { preferences ->
                preferences[CALL_HISTORY_TRACKING_ENABLED_KEY] ?: false
            }.first()

    suspend fun setCallHistoryTrackingEnabled(
        context: Context,
        enabled: Boolean,
    ) {
        context.dataStore.edit { preferences ->
            preferences[CALL_HISTORY_TRACKING_ENABLED_KEY] = enabled
        }
    }

    /**
     * Whether to show the small in-call overlay bubble identifying a number
     * (local label, allow-list match, or ARCEP demarchage badge). Requires the
     * SYSTEM_ALERT_WINDOW special permission. Off by default.
     */
    fun getCallerIdBubbleEnabledFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[CALLER_ID_BUBBLE_ENABLED_KEY] ?: false
        }

    suspend fun isCallerIdBubbleEnabled(context: Context): Boolean =
        context.dataStore.data
            .map { preferences ->
                preferences[CALLER_ID_BUBBLE_ENABLED_KEY] ?: false
            }.first()

    suspend fun setCallerIdBubbleEnabled(
        context: Context,
        enabled: Boolean,
    ) {
        context.dataStore.edit { preferences ->
            preferences[CALLER_ID_BUBBLE_ENABLED_KEY] = enabled
        }
    }
}
