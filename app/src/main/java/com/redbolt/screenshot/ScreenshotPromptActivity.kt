package com.redbolt.screenshot

import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private val mainHandler = Handler(Looper.getMainLooper())

    private val deleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val uri = pendingUri
        if (result.resultCode == RESULT_OK && uri != null) {
            ScreenshotNotifier.cancelPrompt(this, uri)
            ScreenshotPreferences(this).pendingShareDeleteUri = null
            val message = intent.getStringExtra(EXTRA_DELETE_SUCCESS_MESSAGE) ?: "Screenshot deleted"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } else if (uri != null) {
            val message = intent.getStringExtra(EXTRA_DELETE_CANCEL_MESSAGE)
                ?: "Delete cancelled — screenshot copied to clipboard"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private val shareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        shareChooserActive = false
        val uri = pendingUri ?: return@registerForActivityResult
        shareChooserReturned = true
        if (shareTargetChosen) {
            onShareInitiated(uri)
            return@registerForActivityResult
        }
        scheduleChooserDismissCheck()
    }

    private var pendingUri: Uri? = null
    private var shareOnlyMode = false
    private var deleteAfterShare = true
    private var shareTargetChosen = false
    private var shareHandled = false
    private var shareChooserReturned = false
    private var shareChooserActive = false
    private var chooserDismissCheck: Runnable? = null

    override fun onStop() {
        super.onStop()
        if (!shareHandled && pendingUri != null &&
            (shareChooserReturned || shareChooserActive)
        ) {
            onShareInitiated(pendingUri!!)
        }
    }

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
        shareOnlyMode = intent.hasExtra(EXTRA_SHARE_FLOW)
        if (shareOnlyMode) {
            deleteAfterShare = intent.getBooleanExtra(EXTRA_SHARE_FLOW, true)
        }
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
        if (shareOnlyMode) {
            launchShareChooser(uri, deleteAfterShare)
            return
        }

        setContent {
            val prefs = ScreenshotPreferences(this)
            BoltScreenshotTheme(themeId = prefs.themeId) {
                ScreenshotPromptContent(
                    uri = uri,
                    copyRowOnTop = prefs.copyRowOnTop,
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
                    onShareAndDelete = {
                        launchShareChooser(uri, deleteAfterShare = true)
                    },
                    onShareAndSave = {
                        launchShareChooser(uri, deleteAfterShare = false)
                    },
                    onDismiss = { finish() },
                )
            }
        }
    }

    override fun onDestroy() {
        chooserDismissCheck?.let(mainHandler::removeCallbacks)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    private fun launchShareChooser(uri: Uri, deleteAfterShare: Boolean) {
        this.deleteAfterShare = deleteAfterShare
        shareTargetChosen = false
        shareChooserReturned = false
        shareHandled = false
        shareChooserActive = true
        shareLauncher.launch(
            ScreenshotActions.buildShareChooserIntent(this, uri) {
                shareTargetChosen = true
                onShareInitiated(uri)
            },
        )
    }

    private fun onShareInitiated(uri: Uri) {
        if (shareHandled) return
        shareHandled = true
        chooserDismissCheck?.let(mainHandler::removeCallbacks)
        ScreenshotActions.onShareCompleted(applicationContext, uri, deleteAfterShare)
        finish()
    }

    private fun scheduleChooserDismissCheck() {
        chooserDismissCheck?.let(mainHandler::removeCallbacks)
        chooserDismissCheck = Runnable {
            if (!shareHandled && shareOnlyMode) {
                ScreenshotActions.cancelScheduledShareDelete()
                ScreenshotPreferences(this).pendingShareDeleteUri = null
                finish()
            }
        }
        mainHandler.postDelayed(chooserDismissCheck!!, CHOOSER_DISMISS_CHECK_MS)
    }

    companion object {
        private const val CHOOSER_DISMISS_CHECK_MS = 1_000L

        const val EXTRA_URI = "extra_uri"
        const val EXTRA_DELETE_ONLY = "extra_delete_only"
        const val EXTRA_DELETE_INTENT = "extra_delete_intent"
        const val EXTRA_DELETE_CANCEL_MESSAGE = "extra_delete_cancel_message"
        const val EXTRA_DELETE_SUCCESS_MESSAGE = "extra_delete_success_message"
        const val EXTRA_SHARE_FLOW = "extra_share_flow"
        const val EXTRA_SHARE_AND_DELETE = "extra_share_and_delete"
    }
}
