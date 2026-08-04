package com.sentinelle.app.service

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.sentinelle.app.R

// Plain Android Views on purpose (not Compose): a WindowManager overlay
// hosted by a bare Service has no natural LifecycleOwner/SavedStateRegistry,
// which Compose's ComposeView needs. Views avoid that whole problem and are
// the same approach every "chat heads"-style overlay has used since Android
// O introduced TYPE_APPLICATION_OVERLAY.
class CallerIdOverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var dismissRunnable: Runnable? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val displayNumber = intent?.getStringExtra(EXTRA_DISPLAY_NUMBER)
        if (displayNumber == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        showBubble(displayNumber, intent.getStringExtra(EXTRA_LABEL))
        return START_NOT_STICKY
    }

    private fun showBubble(
        number: String,
        label: String?,
    ) {
        removeBubble()

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val container =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(12), dp(16), dp(12))
                background =
                    GradientDrawable().apply {
                        setColor(Color.parseColor("#152030"))
                        cornerRadius = dp(16).toFloat()
                    }
            }

        val icon =
            ImageView(this).apply {
                setImageResource(R.drawable.notification_icon)
                setColorFilter(Color.parseColor("#5DCAA5"))
                layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(12) }
            }

        val textColumn =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
        val numberText =
            TextView(this).apply {
                text = number
                setTextColor(Color.parseColor("#F1EFE8"))
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
            }
        val labelText =
            TextView(this).apply {
                text = label ?: "Numéro non identifié"
                setTextColor(Color.parseColor("#8B96A3"))
                textSize = 12f
            }
        textColumn.addView(numberText)
        textColumn.addView(labelText)

        val closeButton =
            ImageView(this).apply {
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setColorFilter(Color.parseColor("#8B96A3"))
                layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply { marginStart = dp(12) }
                setOnClickListener { stopSelf() }
            }

        container.addView(icon)
        container.addView(textColumn)
        container.addView(closeButton)

        val params =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = dp(80)
            }

        try {
            wm.addView(container, params)
            overlayView = container
        } catch (e: Exception) {
            Log.e(TAG, "Could not add caller ID overlay (permission revoked?)", e)
            stopSelf()
            return
        }

        dismissRunnable =
            Runnable { stopSelf() }.also {
                handler.postDelayed(it, AUTO_DISMISS_MILLIS)
            }
    }

    private fun removeBubble() {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        dismissRunnable = null
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Log.w(TAG, "Overlay view already removed", e)
            }
        }
        overlayView = null
    }

    override fun onDestroy() {
        removeBubble()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "CallerIdOverlayService"
        const val EXTRA_DISPLAY_NUMBER = "display_number"
        const val EXTRA_LABEL = "label"
        private const val AUTO_DISMISS_MILLIS = 25_000L
    }
}
