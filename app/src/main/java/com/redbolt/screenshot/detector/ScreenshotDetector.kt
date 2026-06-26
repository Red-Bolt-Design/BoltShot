package com.redbolt.screenshot.detector

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.redbolt.screenshot.handler.ScreenshotActions
import com.redbolt.screenshot.handler.ScreenshotPreferences

class ScreenshotDetector(
    private val context: Context,
    private val onScreenshot: (Uri) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val preferences = ScreenshotPreferences(context)
    private var lastUri: Uri? = null
    private var lastDetectedAt = 0L

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            handler.removeCallbacks(checkRunnable)
            val delay = preferences.detectionDelayMs
            if (delay <= 0L) {
                handler.post(checkRunnable)
            } else {
                handler.postDelayed(checkRunnable, delay)
            }
        }
    }

    private val checkRunnable = Runnable { checkLatestScreenshot() }

    fun start() {
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer,
        )
    }

    fun stop() {
        handler.removeCallbacks(checkRunnable)
        context.contentResolver.unregisterContentObserver(observer)
    }

    private fun checkLatestScreenshot() {
        val resolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED,
        )
        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return
            val id = cursor.getLong(0)
            val name = cursor.getString(1)
            val path = cursor.getString(2)
            val dateAdded = cursor.getLong(3)
            if (!ScreenshotActions.isScreenshot(name, path)) return

            val nowSeconds = System.currentTimeMillis() / 1000
            if (dateAdded < nowSeconds - 30) return

            val uri = Uri.withAppendedPath(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id.toString(),
            )
            if (preferences.isSaved(uri.toString())) return

            val now = System.currentTimeMillis()
            if (uri == lastUri && now - lastDetectedAt < 2500) return
            lastUri = uri
            lastDetectedAt = now
            onScreenshot(uri)
        }
    }
}
