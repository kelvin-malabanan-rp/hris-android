package io.rocketpartners.hris.app.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import io.rocketpartners.hris.R
import io.rocketpartners.hris.app.MainActivity

/**
 * Builds and posts a system notification for an incoming push, carrying the deep-link reference as
 * intent extras so a tap re-enters [MainActivity] and routes via its `DeepLinkRouter` — the Android
 * analog of iOS forwarding the push `userInfo` to the deep-link router.
 */
object PushNotifications {

    /** Idempotently creates the default channel (no-op below API 26, but minSdk is 26). */
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val id = context.getString(R.string.default_notification_channel_id)
        if (manager.getNotificationChannel(id) != null) return
        manager.createNotificationChannel(
            NotificationChannel(id, context.getString(R.string.default_notification_channel_name), NotificationManager.IMPORTANCE_HIGH),
        )
    }

    /** Posts a notification with [title]/[body] and a tap intent carrying the deep-link keys. */
    fun show(context: Context, title: String?, body: String?, referenceType: String?, referenceId: String?, type: String?) {
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            referenceType?.let { putExtra("referenceType", it) }
            referenceId?.let { putExtra("referenceId", it) }
            type?.let { putExtra("type", it) }
        }
        val pending = PendingIntent.getActivity(
            context,
            (referenceId ?: type ?: title).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, context.getString(R.string.default_notification_channel_id))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title ?: context.getString(R.string.app_name))
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .build()
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.notify((referenceId ?: type ?: title ?: "hris").hashCode(), notification)
    }
}
