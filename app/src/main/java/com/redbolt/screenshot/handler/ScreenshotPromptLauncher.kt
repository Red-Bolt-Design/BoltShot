package com.redbolt.screenshot.handler

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import com.redbolt.screenshot.ScreenshotPromptActivity
import com.redbolt.screenshot.overlay.ScreenshotOverlay

object ScreenshotPromptLauncher {
    val testUri: Uri = Uri.parse("content://com.redbolt.screenshot/test")

    fun show(context: Context, uri: Uri) {
        present(context.applicationContext, uri)
    }

    fun showTest(context: Context) {
        present(context.applicationContext, testUri)
    }

    private fun present(context: Context, uri: Uri) {
        val prefs = ScreenshotPreferences(context)
        if (prefs.vibrateOnPrompt) {
            vibrate(context)
        }
        if (Settings.canDrawOverlays(context) && ScreenshotOverlay.show(context, uri)) {
            return
        }
        if (launchPromptActivity(context, uri)) {
            return
        }
        ScreenshotNotifier.showPrompt(context, uri, fullScreen = true)
    }

    private fun launchPromptActivity(context: Context, uri: Uri): Boolean {
        return runCatching {
            val intent = Intent(context, ScreenshotPromptActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_HISTORY
                putExtra(ScreenshotPromptActivity.EXTRA_URI, uri.toString())
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic().apply {
                    setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
                    )
                }
                context.startActivity(intent, options.toBundle())
            } else {
                context.startActivity(intent)
            }
            true
        }.getOrDefault(false)
    }

    private fun vibrate(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java) ?: return
            manager.defaultVibrator.vibrate(
                VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            @Suppress("DEPRECATION")
            vibrator.vibrate(40)
        }
    }
}
