package com.redbolt.screenshot

import android.app.Application
import com.redbolt.screenshot.handler.ScreenshotPreferences
import com.redbolt.screenshot.service.ScreenshotMonitorService

class ScreenshotApplication : Application() {
    lateinit var preferences: ScreenshotPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        preferences = ScreenshotPreferences(this)
        if (preferences.isMonitorEnabled) {
            ScreenshotMonitorService.start(this)
        }
    }
}
