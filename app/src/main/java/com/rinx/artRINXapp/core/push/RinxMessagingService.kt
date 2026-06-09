package com.rinx.artRINXapp.core.push

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rinx.artRINXapp.MainActivity
import com.rinx.artRINXapp.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receives FCM token rotations and push messages (API §11 / handout Push Notifications).
 * Background notification-messages are auto-displayed by the system; this handles token refresh
 * and foreground / data-only messages.
 */
@AndroidEntryPoint
class RinxMessagingService : FirebaseMessagingService() {

    @Inject lateinit var pushTokenManager: PushTokenManager

    override fun onNewToken(token: String) {
        pushTokenManager.onTokenRefreshed(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: return

        NotificationChannels.ensureDefaultChannel(this)

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            message.data["notification_id"]?.let { putExtra(EXTRA_NOTIFICATION_ID, it) }
            message.data["route"]?.let { putExtra(EXTRA_ROUTE, it) }
            message.data["url"]?.let { putExtra(EXTRA_URL, it) }
            message.data["kind"]?.let { putExtra(EXTRA_KIND, it) }
        }
        // Collapse by collapse_id so repeated pushes about the same resource (e.g. "curation_297")
        // replace each other instead of stacking (handout §collapse_key). Fall back to the unique
        // notification_id, then a timestamp.
        val collapseId = message.data["collapse_id"]
        val notifId = collapseId?.hashCode()
            ?: message.data["notification_id"]?.toIntOrNull()
            ?: System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(
            this,
            notifId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .apply { if (collapseId != null) setGroup(collapseId) }
            .build()

        // POST_NOTIFICATIONS is runtime-gated on Android 13+; skip silently if not granted.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(this).notify(notifId, notification)
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "push_notification_id"
        const val EXTRA_ROUTE = "push_route"
        const val EXTRA_URL = "push_url"
        const val EXTRA_KIND = "push_kind"
    }
}
