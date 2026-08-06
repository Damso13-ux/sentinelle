package com.sentinelle.app.util

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.sentinelle.app.spam.HeuristicSettings
import com.sentinelle.app.ui.theme.ThemeVariant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Covers the Pro entitlement rules in PreferencesManager, which are
// DataStore-backed and so need a real Context — hence Robolectric rather
// than a plain JVM test.
//
// application = Application::class replaces SentinelleApplication, whose
// onCreate schedules WorkManager jobs that aren't initialized under
// Robolectric. Nothing here depends on app startup, so a plain Application
// is both sufficient and faster than standing up WorkManager for it.
//
// Unit tests build against the debug variant, so BuildConfig.DEBUG is true
// here and the debug-override path is exercisable. The release behaviour
// (override inert) can't be asserted from this source set.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ProEntitlementTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @After
    fun tearDown() =
        runTest {
            PreferencesManager.clear(context)
        }

    @Test
    fun proIsLockedByDefault() =
        runTest {
            assertFalse(PreferencesManager.isProUnlocked(context))
        }

    @Test
    fun realPurchaseUnlocksPro() =
        runTest {
            PreferencesManager.setProUnlocked(context, true)
            assertTrue(PreferencesManager.isProUnlocked(context))
        }

    @Test
    fun debugOverrideUnlocksPro() =
        runTest {
            PreferencesManager.setProDebugOverride(context, true)
            assertTrue(PreferencesManager.isProUnlocked(context))
        }

    /**
     * The regression this whole split exists for: BillingManager re-syncs
     * against Play on every connection and writes `false` when it finds no
     * purchase. Before the override had its own key, that wiped a debug
     * unlock the moment the Settings screen reconnected — Pro would switch
     * itself back off on leaving the screen.
     */
    @Test
    fun billingSyncFindingNoPurchaseDoesNotWipeTheDebugOverride() =
        runTest {
            PreferencesManager.setProDebugOverride(context, true)
            PreferencesManager.setProUnlocked(context, false)
            assertTrue(PreferencesManager.isProUnlocked(context))
        }

    @Test
    fun revokingARealPurchaseLocksProAgainWhenNoOverrideIsSet() =
        runTest {
            PreferencesManager.setProUnlocked(context, true)
            PreferencesManager.setProUnlocked(context, false)
            assertFalse(PreferencesManager.isProUnlocked(context))
        }

    @Test
    fun clearingTheDebugOverrideLocksProAgain() =
        runTest {
            PreferencesManager.setProDebugOverride(context, true)
            PreferencesManager.setProDebugOverride(context, false)
            assertFalse(PreferencesManager.isProUnlocked(context))
        }

    // --- Effective settings gating ---------------------------------------

    @Test
    fun heuristicTuningIsIgnoredWhileProIsLocked() =
        runTest {
            PreferencesManager.setHeuristicSettings(
                context,
                HeuristicSettings(historyWindowDays = 30, blockThreshold = 0.5, sensitivity = 1.5),
            )
            val effective = PreferencesManager.getEffectiveHeuristicSettings(context)
            assertEquals(HeuristicSettings(), effective)
        }

    @Test
    fun heuristicTuningAppliesOnceProIsUnlocked() =
        runTest {
            val custom = HeuristicSettings(historyWindowDays = 30, blockThreshold = 0.5, sensitivity = 1.5)
            PreferencesManager.setHeuristicSettings(context, custom)
            PreferencesManager.setProUnlocked(context, true)
            assertEquals(custom, PreferencesManager.getEffectiveHeuristicSettings(context))
        }

    @Test
    fun losingProRevertsTuningImmediatelyRatherThanLeavingItStale() =
        runTest {
            PreferencesManager.setHeuristicSettings(context, HeuristicSettings(sensitivity = 1.5))
            PreferencesManager.setProUnlocked(context, true)
            PreferencesManager.setProUnlocked(context, false)
            assertEquals(HeuristicSettings(), PreferencesManager.getEffectiveHeuristicSettings(context))
        }

    // --- Theme gating -----------------------------------------------------

    @Test
    fun themeVariantFallsBackToGardeWhileProIsLocked() =
        runTest {
            PreferencesManager.setThemeVariant(context, ThemeVariant.VIOLET)
            assertEquals(ThemeVariant.GARDE, PreferencesManager.getEffectiveThemeVariantFlow(context).first())
            // The choice is remembered, just not applied — so it comes back
            // if Pro is unlocked later rather than being silently discarded.
            assertEquals(ThemeVariant.VIOLET, PreferencesManager.getStoredThemeVariantFlow(context).first())
        }

    @Test
    fun themeVariantAppliesOnceProIsUnlocked() =
        runTest {
            PreferencesManager.setThemeVariant(context, ThemeVariant.CORAIL)
            PreferencesManager.setProUnlocked(context, true)
            assertEquals(ThemeVariant.CORAIL, PreferencesManager.getEffectiveThemeVariantFlow(context).first())
        }

    /**
     * The second half of the reported bug: with the debug unlock active,
     * picking Corail/Violet applied — but coming back to Settings showed
     * only Garde as selectable, because the effective theme had silently
     * reverted along with the wiped entitlement.
     */
    @Test
    fun themeVariantAppliesUnderTheDebugOverrideToo() =
        runTest {
            PreferencesManager.setProDebugOverride(context, true)
            PreferencesManager.setThemeVariant(context, ThemeVariant.CORAIL)
            PreferencesManager.setProUnlocked(context, false)
            assertEquals(ThemeVariant.CORAIL, PreferencesManager.getEffectiveThemeVariantFlow(context).first())
        }
}
