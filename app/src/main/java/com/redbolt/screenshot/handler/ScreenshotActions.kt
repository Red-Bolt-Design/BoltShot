package com.redbolt.screenshot.handler

import android.content.ClipData
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
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

    fun shareScreenshot(context: Context, uri: Uri) {
        markHandled(context, uri)
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(share, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
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
        requestDeleteWithSystemDialog(context, uri)
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

    private fun requestDeleteWithSystemDialog(context: Context, uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pending = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
            val intent = Intent(context, ScreenshotPromptActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(ScreenshotPromptActivity.EXTRA_URI, uri.toString())
                putExtra(ScreenshotPromptActivity.EXTRA_DELETE_ONLY, true)
                putExtra(ScreenshotPromptActivity.EXTRA_DELETE_INTENT, pending.intentSender)
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
}
