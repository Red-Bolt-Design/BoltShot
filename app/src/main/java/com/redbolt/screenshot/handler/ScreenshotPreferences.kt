package com.redbolt.screenshot.handler

import android.content.Context

class ScreenshotPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isMonitorEnabled: Boolean
        get() = prefs.getBoolean(KEY_MONITOR_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MONITOR_ENABLED, value).apply()

    var showInstantPrompt: Boolean
        get() = prefs.getBoolean(KEY_INSTANT_PROMPT, true)
        set(value) = prefs.edit().putBoolean(KEY_INSTANT_PROMPT, value).apply()

    var promptPosition: PromptPosition
        get() = PromptPosition.fromStorage(prefs.getString(KEY_PROMPT_POSITION, null))
        set(value) = prefs.edit().putString(KEY_PROMPT_POSITION, value.storageValue).apply()

    var vibrateOnPrompt: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE_ON_PROMPT, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATE_ON_PROMPT, value).apply()

    var tapOutsideToDismiss: Boolean
        get() = prefs.getBoolean(KEY_TAP_OUTSIDE_DISMISS, true)
        set(value) = prefs.edit().putBoolean(KEY_TAP_OUTSIDE_DISMISS, value).apply()

    var detectionDelayMs: Long
        get() = prefs.getLong(KEY_DETECTION_DELAY_MS, DEFAULT_DETECTION_DELAY_MS)
        set(value) = prefs.edit().putLong(KEY_DETECTION_DELAY_MS, value.coerceIn(0L, 1000L)).apply()

    var themeId: BoltThemeId
        get() = BoltThemeId.fromStorage(prefs.getString(KEY_THEME_ID, null))
        set(value) = prefs.edit().putString(KEY_THEME_ID, value.storageValue).apply()

    fun markSaved(uriKey: String) {
        val saved = prefs.getStringSet(KEY_SAVED_URIS, emptySet()).orEmpty().toMutableSet()
        saved.add(uriKey)
        if (saved.size > 200) {
            saved.remove(saved.first())
        }
        prefs.edit().putStringSet(KEY_SAVED_URIS, saved).apply()
    }

    fun isSaved(uriKey: String): Boolean {
        return prefs.getStringSet(KEY_SAVED_URIS, emptySet()).orEmpty().contains(uriKey)
    }

    companion object {
        private const val PREFS_NAME = "bolt_screenshot_prefs"
        private const val KEY_MONITOR_ENABLED = "monitor_enabled"
        private const val KEY_INSTANT_PROMPT = "instant_prompt"
        private const val KEY_PROMPT_POSITION = "prompt_position"
        private const val KEY_VIBRATE_ON_PROMPT = "vibrate_on_prompt"
        private const val KEY_TAP_OUTSIDE_DISMISS = "tap_outside_dismiss"
        private const val KEY_DETECTION_DELAY_MS = "detection_delay_ms"
        private const val KEY_THEME_ID = "theme_id"
        private const val KEY_SAVED_URIS = "saved_uris"
        const val DEFAULT_DETECTION_DELAY_MS = 300L
    }
}
