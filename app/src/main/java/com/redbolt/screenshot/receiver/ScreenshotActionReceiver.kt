package com.redbolt.screenshot.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.redbolt.screenshot.ScreenshotApplication
import com.redbolt.screenshot.handler.ScreenshotActions
import com.redbolt.screenshot.handler.ScreenshotNotifier
import com.redbolt.screenshot.service.ScreenshotMonitorService

class ScreenshotActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val uriString = intent.getStringExtra(EXTRA_URI) ?: return
        val uri = Uri.parse(uriString)
        when (intent.action) {
            ACTION_COPY_DELETE -> ScreenshotActions.copyAndDelete(context, uri)
            ACTION_COPY_SAVE -> ScreenshotActions.copyAndSave(context, uri)
            ACTION_DISMISS -> ScreenshotActions.dismissScreenshot(context, uri)
            ACTION_SHARE -> ScreenshotActions.shareScreenshot(context, uri)
        }
        ScreenshotNotifier.cancelPrompt(context, uri)
    }

    companion object {
        const val ACTION_COPY_DELETE = "com.redbolt.screenshot.COPY_DELETE"
        const val ACTION_COPY_SAVE = "com.redbolt.screenshot.COPY_SAVE"
        const val ACTION_DISMISS = "com.redbolt.screenshot.DISMISS"
        const val ACTION_SHARE = "com.redbolt.screenshot.SHARE"
        const val EXTRA_URI = "extra_uri"
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as ScreenshotApplication
        if (app.preferences.isMonitorEnabled) {
            ScreenshotMonitorService.start(context)
        }
    }
}
