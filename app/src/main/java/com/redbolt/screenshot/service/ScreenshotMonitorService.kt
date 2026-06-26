package com.redbolt.screenshot.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.redbolt.screenshot.detector.ScreenshotDetector
import com.redbolt.screenshot.handler.ScreenshotNotifier
import com.redbolt.screenshot.handler.ScreenshotPreferences
import com.redbolt.screenshot.handler.ScreenshotPromptLauncher

class ScreenshotMonitorService : LifecycleService() {
    private var detector: ScreenshotDetector? = null

    override fun onCreate() {
        super.onCreate()
        ScreenshotNotifier.ensureChannels(this)
        startForeground(
            ScreenshotNotifier.MONITOR_NOTIFICATION_ID,
            ScreenshotNotifier.buildMonitorNotification(this),
        )
        detector = ScreenshotDetector(this, ::onScreenshotDetected).also { it.start() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return Service.START_STICKY
    }

    override fun onDestroy() {
        detector?.stop()
        detector = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

    private fun onScreenshotDetected(uri: Uri) {
        val prefs = ScreenshotPreferences(this)
        if (!prefs.showInstantPrompt) {
            ScreenshotNotifier.showPrompt(this, uri)
            return
        }
        ScreenshotPromptLauncher.show(this, uri)
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, ScreenshotMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenshotMonitorService::class.java))
        }
    }
}
