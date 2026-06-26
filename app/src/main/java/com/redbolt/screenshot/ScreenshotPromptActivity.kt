package com.redbolt.screenshot

import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.redbolt.screenshot.handler.ScreenshotActions
import com.redbolt.screenshot.handler.ScreenshotNotifier
import com.redbolt.screenshot.handler.ScreenshotPreferences
import com.redbolt.screenshot.ui.prompt.ScreenshotPromptContent
import com.redbolt.screenshot.ui.theme.BoltScreenshotTheme

class ScreenshotPromptActivity : ComponentActivity() {
    private val deleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val uri = pendingUri
        if (result.resultCode == RESULT_OK && uri != null) {
            ScreenshotNotifier.cancelPrompt(this, uri)
            Toast.makeText(this, "Screenshot deleted", Toast.LENGTH_SHORT).show()
        } else if (uri != null) {
            Toast.makeText(this, "Delete cancelled — screenshot copied to clipboard", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private var pendingUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON,
        )
        enableEdgeToEdge()

        val uriString = intent.getStringExtra(EXTRA_URI)
        if (uriString == null) {
            finish()
            return
        }
        val uri = Uri.parse(uriString)
        pendingUri = uri

        val deleteOnly = intent.getBooleanExtra(EXTRA_DELETE_ONLY, false)
        val deleteSender = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_DELETE_INTENT, IntentSender::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_DELETE_INTENT)
        }
        if (deleteOnly && deleteSender != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            deleteLauncher.launch(IntentSenderRequest.Builder(deleteSender).build())
            return
        }

        setContent {
            val prefs = ScreenshotPreferences(this)
            BoltScreenshotTheme(themeId = prefs.themeId) {
                ScreenshotPromptContent(
                    uri = uri,
                    tapOutsideToDismiss = prefs.tapOutsideToDismiss,
                    onCopyDelete = {
                        ScreenshotActions.copyAndDelete(this, uri)
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
                            ScreenshotActions.hasAllFilesAccess(this)
                        ) {
                            finish()
                        }
                    },
                    onCopySave = {
                        ScreenshotActions.copyAndSave(this, uri)
                        finish()
                    },
                    onShare = {
                        ScreenshotActions.shareScreenshot(this, uri)
                        finish()
                    },
                    onDismiss = { finish() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    companion object {
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_DELETE_ONLY = "extra_delete_only"
        const val EXTRA_DELETE_INTENT = "extra_delete_intent"
    }
}
