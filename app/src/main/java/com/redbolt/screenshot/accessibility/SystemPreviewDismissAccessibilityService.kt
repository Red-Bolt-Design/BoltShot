package com.redbolt.screenshot.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.redbolt.screenshot.handler.ScreenshotPreferences

class SystemPreviewDismissAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    private fun scheduleDismiss(delayMs: Long) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_BACK)
        }, delayMs)
    }

    companion object {
        @Volatile
        private var instance: SystemPreviewDismissAccessibilityService? = null

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ).orEmpty()
            val component = ComponentName(context, SystemPreviewDismissAccessibilityService::class.java)
            return enabled.split(':').any { token ->
                ComponentName.unflattenFromString(token) == component
            }
        }

        fun openSettings(context: Context) {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }

        fun scheduleIfEnabled(context: Context) {
            val prefs = ScreenshotPreferences(context)
            if (!prefs.dismissSystemPreview) return
            instance?.scheduleDismiss(prefs.systemPreviewDismissDelayMs)
        }
    }
}
