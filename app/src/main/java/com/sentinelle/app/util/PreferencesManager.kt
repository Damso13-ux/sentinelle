package com.sentinelle.app.util

import android.content.Context
import android.content.RestrictionsManager
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sentinelle.app.BuildConfig
import com.sentinelle.app.spam.HeuristicSettings
import com.sentinelle.app.ui.theme.ThemeVariant
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

    // Off by default. Only meaningful when CALL_HISTORY_TRACKING_ENABLED_KEY
    // is also on: instead of actually blocking a heuristic match, it's logged
    // to heuristic_shadow_events for the user to review before trusting the
    // detector to block for real.
    private val HEURISTIC_SHADOW_MODE_ENABLED_KEY = booleanPreferencesKey("heuristic_shadow_mode_enabled")

    // Whether the one-time "Sentinelle Pro" purchase is unlocked (export
    // des stats, historique étendu, thèmes additionnels, réglages avancés
    // de l'heuristique). Kept in sync with Play Billing by BillingManager,
    // which re-checks Play's own purchase records on every app start rather
    // than trusting this flag blindly — it's a local cache of entitlement,
    // not the source of truth. DebugSheet can also flip it directly, but
    // only in debug builds (see BuildConfig.DEBUG check there), to test
    // Pro-gated UI before a real Play Console product exists.
    private val PRO_UNLOCKED_KEY = booleanPreferencesKey("pro_unlocked")

    // Debug-only simulated unlock, kept separate from PRO_UNLOCKED_KEY so
    // BillingManager's purchase re-sync can't clobber it. See proUnlocked().
    private val PRO_DEBUG_OVERRIDE_KEY = booleanPreferencesKey("pro_debug_override")

    // Pro-gated heuristic tuning — see HeuristicSettings for defaults/ranges
    // and getEffectiveHeuristicSettings for how Pro status is enforced.
    private val HEURISTIC_HISTORY_WINDOW_DAYS_KEY = intPreferencesKey("heuristic_history_window_days")
    private val HEURISTIC_BLOCK_THRESHOLD_KEY = doublePreferencesKey("heuristic_block_threshold")
    private val HEURISTIC_SENSITIVITY_KEY = doublePreferencesKey("heuristic_sensitivity")

    // Pro-gated theme variant — see ThemeVariant and getEffectiveThemeVariant.
    private val THEME_VARIANT_KEY = stringPreferencesKey("theme_variant")

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

    /**
     * "Shadow mode" for the heuristic detector: logs what would have been
     * blocked instead of actually blocking it, so accuracy can be reviewed
     * before trusting real blocking. Off by default.
     */
    fun getHeuristicShadowModeEnabledFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[HEURISTIC_SHADOW_MODE_ENABLED_KEY] ?: false
        }

    suspend fun isHeuristicShadowModeEnabled(context: Context): Boolean =
        context.dataStore.data
            .map { preferences ->
                preferences[HEURISTIC_SHADOW_MODE_ENABLED_KEY] ?: false
            }.first()

    suspend fun setHeuristicShadowModeEnabled(
        context: Context,
        enabled: Boolean,
    ) {
        context.dataStore.edit { preferences ->
            preferences[HEURISTIC_SHADOW_MODE_ENABLED_KEY] = enabled
        }
    }

    /**
     * Whether Pro features should be treated as unlocked: either Play
     * reported a real purchase, or the debug override is on in a debug
     * build.
     *
     * The two are deliberately separate keys. BillingManager re-syncs
     * [PRO_UNLOCKED_KEY] against Play's records on every connection and
     * writes `false` when it finds no purchase — which is correct for real
     * entitlement (a refund must take effect immediately) but would wipe a
     * debug unlock the moment the Settings screen reconnects. Keeping the
     * override in its own key means billing sync never touches it.
     *
     * The BuildConfig.DEBUG check is at *read* time, not just where the
     * override is written, so a stored `true` is inert in a release build
     * no matter how it got there.
     */
    private fun Preferences.proUnlocked(): Boolean {
        val purchased = this[PRO_UNLOCKED_KEY] ?: false
        val debugOverride = BuildConfig.DEBUG && (this[PRO_DEBUG_OVERRIDE_KEY] ?: false)
        return purchased || debugOverride
    }

    fun getProUnlockedFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences.proUnlocked()
        }

    suspend fun isProUnlocked(context: Context): Boolean =
        context.dataStore.data
            .map { preferences -> preferences.proUnlocked() }
            .first()

    /**
     * Records what Play actually reports. Only BillingManager should call
     * this — it's the real entitlement, and it gets overwritten (including
     * back to `false`) on every purchase re-check.
     */
    suspend fun setProUnlocked(
        context: Context,
        unlocked: Boolean,
    ) {
        context.dataStore.edit { preferences ->
            preferences[PRO_UNLOCKED_KEY] = unlocked
        }
    }

    /** Reads the debug override alone, for the debug toggles' own on/off state. */
    suspend fun isProDebugOverrideEnabled(context: Context): Boolean =
        BuildConfig.DEBUG &&
            context.dataStore.data
                .map { preferences -> preferences[PRO_DEBUG_OVERRIDE_KEY] ?: false }
                .first()

    /** No-op outside debug builds, so this can't unlock anything in release. */
    suspend fun setProDebugOverride(
        context: Context,
        enabled: Boolean,
    ) {
        if (!BuildConfig.DEBUG) return
        context.dataStore.edit { preferences ->
            preferences[PRO_DEBUG_OVERRIDE_KEY] = enabled
        }
    }

    // --- Heuristic tuning (Pro) ---------------------------------------

    /**
     * The raw, persisted values regardless of Pro status — used by the
     * settings UI, which only renders when Pro is already confirmed
     * unlocked. Everything that actually *applies* these values (the
     * detector, PatternManager's threshold check) must go through
     * [getEffectiveHeuristicSettings] instead, never this.
     */
    fun getStoredHeuristicSettingsFlow(context: Context): Flow<HeuristicSettings> =
        context.dataStore.data.map { preferences ->
            HeuristicSettings(
                historyWindowDays = preferences[HEURISTIC_HISTORY_WINDOW_DAYS_KEY] ?: HeuristicSettings.DEFAULT_HISTORY_WINDOW_DAYS,
                blockThreshold = preferences[HEURISTIC_BLOCK_THRESHOLD_KEY] ?: HeuristicSettings.DEFAULT_BLOCK_THRESHOLD,
                sensitivity = preferences[HEURISTIC_SENSITIVITY_KEY] ?: HeuristicSettings.DEFAULT_SENSITIVITY,
            )
        }

    suspend fun setHeuristicSettings(
        context: Context,
        settings: HeuristicSettings,
    ) {
        context.dataStore.edit { preferences ->
            preferences[HEURISTIC_HISTORY_WINDOW_DAYS_KEY] = settings.historyWindowDays
            preferences[HEURISTIC_BLOCK_THRESHOLD_KEY] = settings.blockThreshold
            preferences[HEURISTIC_SENSITIVITY_KEY] = settings.sensitivity
        }
    }

    /**
     * What the detector should actually use: the stored tuning if — and
     * only if — Pro is currently unlocked, otherwise the hard defaults.
     * Re-checked on every call rather than cached, so a refund takes effect
     * immediately instead of leaving a stale custom tuning active.
     */
    suspend fun getEffectiveHeuristicSettings(context: Context): HeuristicSettings {
        if (!isProUnlocked(context)) return HeuristicSettings()
        return getStoredHeuristicSettingsFlow(context).first()
    }

    // --- Theme variant (Pro) --------------------------------------------

    fun getStoredThemeVariantFlow(context: Context): Flow<ThemeVariant> =
        context.dataStore.data.map { preferences ->
            ThemeVariant.fromStorageKey(preferences[THEME_VARIANT_KEY])
        }

    suspend fun setThemeVariant(
        context: Context,
        variant: ThemeVariant,
    ) {
        context.dataStore.edit { preferences ->
            preferences[THEME_VARIANT_KEY] = variant.storageKey
        }
    }

    /** Same idea as getEffectiveHeuristicSettings: Garde for everyone unless Pro is confirmed unlocked right now. */
    fun getEffectiveThemeVariantFlow(context: Context): Flow<ThemeVariant> =
        context.dataStore.data.map { preferences ->
            if (!preferences.proUnlocked()) {
                ThemeVariant.INDIGO
            } else {
                ThemeVariant.fromStorageKey(preferences[THEME_VARIANT_KEY])
            }
        }
}
