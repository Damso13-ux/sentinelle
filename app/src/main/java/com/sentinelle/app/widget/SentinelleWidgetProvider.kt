package com.sentinelle.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.sentinelle.app.MainActivity
import com.sentinelle.app.R
import com.sentinelle.app.data.AppDatabase
import com.sentinelle.app.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Plain RemoteViews (not Compose/Glance) — the standard, dependency-free
// approach for a classic home-screen widget. Refreshed on the system's
// ~30min minimum cadence (updatePeriodMillis) and immediately after every
// block via requestUpdate(), called from BlockEventLogger.
class SentinelleWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // onUpdate is a manifest-registered BroadcastReceiver callback —
        // runs on the main thread, and the blockedEventDao().getTotalCount()
        // read inside updateWidget is real DB I/O. goAsync() extends the
        // receiver's lifetime so that read can move to Dispatchers.IO
        // instead of blocking here, same pattern as UnblockActionReceiver.
        // Only one goAsync() per onReceive/onUpdate call, so this wraps the
        // whole batch of widget ids rather than one per id.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, id) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SentinelleWidget"

        /**
         * Asks the launcher to pin the widget directly, skipping the manual
         * "long-press home screen → Widgets → find Sentinelle → drag" flow.
         *
         * The returned value only says whether the launcher was *asked*, not
         * whether the widget was added: requestPinAppWidget has no refusal
         * callback. A `true` here is compatible with the user never seeing a
         * dialog at all — MIUI puts an auto-dismiss countdown on its confirm
         * dialog and, once it lapses, silently drops every later request from
         * the same install. Callers must therefore always surface the manual
         * route too, never treat `true` as success.
         */
        fun requestPin(context: Context): Boolean =
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val provider = ComponentName(context, SentinelleWidgetProvider::class.java)
                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    appWidgetManager.requestPinAppWidget(provider, null, null)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error requesting widget pin", e)
                false
            }

        /**
         * Called from BlockEventLogger right after logging a block — in
         * practice that's always already off the main thread (inside a
         * screening/notification coroutine), but dispatching to IO here too
         * means this stays safe even if a future caller isn't.
         */
        fun requestUpdate(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val manager = AppWidgetManager.getInstance(context)
                    val ids = manager.getAppWidgetIds(ComponentName(context, SentinelleWidgetProvider::class.java))
                    ids.forEach { id -> updateWidget(context, manager, id) }
                } catch (e: Exception) {
                    Log.e(TAG, "Error requesting widget update", e)
                }
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_sentinelle)

            val isProtectionActive = PermissionUtils.isCallScreeningEnabled(context)
            views.setTextViewText(
                R.id.widget_status_text,
                if (isProtectionActive) "Protection active" else "Protection inactive",
            )

            val blockedCount =
                try {
                    AppDatabase.getInstance(context).blockedEventDao().getTotalCount()
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading blocked count for widget", e)
                    0
                }
            views.setTextViewText(R.id.widget_blocked_count, blockedCount.toString())

            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
