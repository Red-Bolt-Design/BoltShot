package com.redbolt.screenshot.handler

import android.content.ClipData
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.redbolt.screenshot.ScreenshotPromptActivity

object ScreenshotActions {
    fun copyToClipboard(context: Context, uri: Uri): Boolean {
        val resolver = context.contentResolver
        val clip = ClipData.newUri(resolver, "Screenshot", uri)
        val clipboard = ContextCompat.getSystemService(context, android.content.ClipboardManager::class.java)
            ?: return false
        clipboard.setPrimaryClip(clip)
        return true
    }

    fun copyAndSave(context: Context, uri: Uri) {
        if (!copyToClipboard(context, uri)) {
            Toast.makeText(context, "Could not copy screenshot", Toast.LENGTH_SHORT).show()
            return
        }
        markHandled(context, uri)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun dismissScreenshot(context: Context, uri: Uri) {
        markHandled(context, uri)
    }

    fun launchShareAndDelete(context: Context, uri: Uri) {
        launchShareFlow(context, uri, deleteAfterShare = true)
    }

    fun launchShareAndSave(context: Context, uri: Uri) {
        launchShareFlow(context, uri, deleteAfterShare = false)
    }

    private fun launchShareFlow(context: Context, uri: Uri, deleteAfterShare: Boolean) {
        val intent = Intent(context, ScreenshotPromptActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(ScreenshotPromptActivity.EXTRA_URI, uri.toString())
            putExtra(ScreenshotPromptActivity.EXTRA_SHARE_FLOW, deleteAfterShare)
        }
        context.startActivity(intent)
    }

    fun onShareCompleted(context: Context, uri: Uri, deleteAfterShare: Boolean) {
        acknowledgeScreenshot(context, uri)
        if (deleteAfterShare) {
            scheduleShareDelete(context, uri)
            return
        }
        Toast.makeText(context, "Shared and saved", Toast.LENGTH_SHORT).show()
    }

    fun buildShareChooserIntent(
        context: Context,
        uri: Uri,
        onTargetChosen: (() -> Unit)? = null,
    ): Intent {
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(share, null)
        if (onTargetChosen != null) {
            val receiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    if (resultData?.chosenComponent() != null) {
                        onTargetChosen()
                    }
                }
            }
            chooser.putExtra(Intent.EXTRA_RESULT_RECEIVER, receiver)
        }
        return chooser
    }

    private fun Bundle.chosenComponent(): ComponentName? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelable(Intent.EXTRA_CHOSEN_COMPONENT, ComponentName::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelable(Intent.EXTRA_CHOSEN_COMPONENT)
        }
    }

    fun acknowledgeScreenshot(context: Context, uri: Uri) {
        markHandled(context, uri)
    }

    fun launchDeleteConfirmation(
        context: Context,
        uri: Uri,
        cancelMessage: String = "Delete cancelled — screenshot copied to clipboard",
        successMessage: String = "Screenshot deleted",
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pending = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
            val intent = Intent(context, ScreenshotPromptActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(ScreenshotPromptActivity.EXTRA_URI, uri.toString())
                putExtra(ScreenshotPromptActivity.EXTRA_DELETE_ONLY, true)
                putExtra(ScreenshotPromptActivity.EXTRA_DELETE_INTENT, pending.intentSender)
                putExtra(ScreenshotPromptActivity.EXTRA_DELETE_CANCEL_MESSAGE, cancelMessage)
                putExtra(ScreenshotPromptActivity.EXTRA_DELETE_SUCCESS_MESSAGE, successMessage)
            }
            context.startActivity(intent)
            return
        }
        Toast.makeText(
            context,
            "Could not delete — enable All files access in BoltShot settings",
            Toast.LENGTH_LONG,
        ).show()
    }

    fun scheduleShareDelete(context: Context, uri: Uri) {
        val appContext = context.applicationContext
        acknowledgeScreenshot(appContext, uri)
        ScreenshotPreferences(appContext).pendingShareDeleteUri = uri.toString()
        cancelScheduledShareDelete()
        val runnable = Runnable {
            pendingShareDeleteRunnable = null
            executeShareDelete(appContext, uri)
        }
        pendingShareDeleteRunnable = runnable
        shareDeleteHandler.postDelayed(runnable, SHARE_DELETE_DELAY_MS)
    }

    fun cancelScheduledShareDelete() {
        pendingShareDeleteRunnable?.let(shareDeleteHandler::removeCallbacks)
        pendingShareDeleteRunnable = null
    }

    fun executeShareDelete(context: Context, uri: Uri) {
        ScreenshotPreferences(context).pendingShareDeleteUri = null
        if (deleteScreenshotSilently(context, uri)) {
            Toast.makeText(context, "Shared and deleted", Toast.LENGTH_SHORT).show()
            return
        }
        launchDeleteConfirmation(
            context,
            uri,
            cancelMessage = "Delete cancelled — screenshot was shared",
            successMessage = "Shared and deleted",
        )
    }

    fun completePendingShareDelete(context: Context): Boolean {
        val prefs = ScreenshotPreferences(context)
        val uriString = prefs.pendingShareDeleteUri ?: return false
        cancelScheduledShareDelete()
        executeShareDelete(context, Uri.parse(uriString))
        return true
    }

    fun copyAndDelete(context: Context, uri: Uri) {
        if (!copyToClipboard(context, uri)) {
            Toast.makeText(context, "Could not copy screenshot", Toast.LENGTH_SHORT).show()
            return
        }
        if (deleteScreenshotSilently(context, uri)) {
            Toast.makeText(context, "Copied and deleted", Toast.LENGTH_SHORT).show()
            return
        }
        requestDeleteWithSystemDialog(context, uri, successMessage = "Copied and deleted")
    }

    fun deleteScreenshotSilently(context: Context, uri: Uri): Boolean {
        val deleted = tryDirectDelete(context.contentResolver, uri)
        if (deleted) {
            ScreenshotNotifier.cancelPrompt(context, uri)
        }
        return deleted
    }

    fun deleteScreenshot(context: Context, uri: Uri) {
        if (deleteScreenshotSilently(context, uri)) {
            Toast.makeText(context, "Screenshot deleted", Toast.LENGTH_SHORT).show()
            return
        }
        requestDeleteWithSystemDialog(context, uri)
    }

    fun hasAllFilesAccess(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()
    }

    fun openAllFilesAccessSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val intent = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun requestDeleteWithSystemDialog(
        context: Context,
        uri: Uri,
        deleteOnlyMessage: String = "Delete cancelled — screenshot copied to clipboard",
        successMessage: String = "Screenshot deleted",
    ) {
        launchDeleteConfirmation(context, uri, cancelMessage = deleteOnlyMessage, successMessage = successMessage)
    }

    private fun tryDirectDelete(resolver: ContentResolver, uri: Uri): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            !Environment.isExternalStorageManager()
        ) {
            return false
        }
        return runCatching { resolver.delete(uri, null, null) }.getOrDefault(0) > 0
    }

    private fun markHandled(context: Context, uri: Uri) {
        ScreenshotPreferences(context).markSaved(uri.toString())
        ScreenshotNotifier.cancelPrompt(context, uri)
    }

    fun isScreenshot(displayName: String?, relativePath: String?): Boolean {
        val name = displayName.orEmpty().lowercase()
        val path = relativePath.orEmpty().lowercase()
        return name.contains("screenshot") ||
            path.contains("screenshot") ||
            path.contains("screen_shot") ||
            name.startsWith("scr_")
    }

    fun queryDisplayName(resolver: ContentResolver, uri: Uri): Pair<String?, String?> {
        resolver.query(
            uri,
            arrayOf(
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.RELATIVE_PATH,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0) to cursor.getString(1)
            }
        }
        return null to null
    }

    private val shareDeleteHandler = Handler(Looper.getMainLooper())
    private var pendingShareDeleteRunnable: Runnable? = null
    private const val SHARE_DELETE_DELAY_MS = 1_500L
}
