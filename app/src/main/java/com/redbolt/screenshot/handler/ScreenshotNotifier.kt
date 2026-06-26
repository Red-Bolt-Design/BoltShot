package com.redbolt.screenshot.handler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.redbolt.screenshot.R
import com.redbolt.screenshot.ScreenshotPromptActivity
import com.redbolt.screenshot.receiver.ScreenshotActionReceiver

object ScreenshotNotifier {
    const val MONITOR_NOTIFICATION_ID = 1001

    private const val CHANNEL_MONITOR = "monitor"
    private const val CHANNEL_PROMPT = "prompt"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MONITOR,
                context.getString(R.string.channel_monitor),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PROMPT,
                context.getString(R.string.channel_prompt),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                setBypassDnd(true)
            },
        )
    }

    fun buildMonitorNotification(context: Context): Notification {
        ensureChannels(context)
        return NotificationCompat.Builder(context, CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.monitor_notification_title))
            .setContentText(context.getString(R.string.monitor_notification_text))
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun showPrompt(context: Context, uri: Uri, fullScreen: Boolean = false) {
        ensureChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val thumbnail = loadThumbnail(context, uri)
        val openPrompt = PendingIntent.getActivity(
            context,
            uri.hashCode(),
            Intent(context, ScreenshotPromptActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(ScreenshotPromptActivity.EXTRA_URI, uri.toString())
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val actionOrder = PromptAction.orderedActions(
            ScreenshotPreferences(context).copyRowOnTop,
        )
        val actionIntents = mapOf(
            PromptAction.COPY_DELETE to actionPendingIntent(context, uri, ScreenshotActionReceiver.ACTION_COPY_DELETE),
            PromptAction.COPY_SAVE to actionPendingIntent(context, uri, ScreenshotActionReceiver.ACTION_COPY_SAVE),
            PromptAction.SHARE_DELETE to actionPendingIntent(context, uri, ScreenshotActionReceiver.ACTION_SHARE_DELETE),
            PromptAction.SHARE_SAVE to actionPendingIntent(context, uri, ScreenshotActionReceiver.ACTION_SHARE_SAVE),
        )
        val dismiss = actionPendingIntent(context, uri, ScreenshotActionReceiver.ACTION_DISMISS)

        val builder = NotificationCompat.Builder(context, CHANNEL_PROMPT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.screenshot_detected_title))
            .setContentText(context.getString(R.string.notification_prompt_text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(openPrompt)
        actionOrder.forEach { action ->
            builder.addAction(0, context.getString(action.labelRes), actionIntents.getValue(action))
        }
        builder.setDeleteIntent(dismiss)

        if (fullScreen) {
            builder.setFullScreenIntent(openPrompt, true)
        }

        if (thumbnail != null) {
            builder.setLargeIcon(thumbnail)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(thumbnail)
                    .bigLargeIcon(null as Bitmap?),
            )
        }

        manager.notify(notificationId(uri), builder.build())
    }

    fun cancelPrompt(context: Context, uri: Uri) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.cancel(notificationId(uri))
    }

    private fun actionPendingIntent(context: Context, uri: Uri, action: String): PendingIntent {
        val intent = Intent(context, ScreenshotActionReceiver::class.java).apply {
            this.action = action
            putExtra(ScreenshotActionReceiver.EXTRA_URI, uri.toString())
        }
        return PendingIntent.getBroadcast(
            context,
            (uri.toString() + action).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationId(uri: Uri): Int = uri.hashCode()

    private fun loadThumbnail(context: Context, uri: Uri): Bitmap? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        }.getOrNull()
    }
}
