package com.rinx.artRINXapp.core.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.rinx.artRINXapp.R

/** Creates the default notification channel used by FCM (Android O+). Safe to call repeatedly. */
object NotificationChannels {

    fun ensureDefaultChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val id = context.getString(R.string.default_notification_channel_id)
        if (manager.getNotificationChannel(id) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                id,
                context.getString(R.string.default_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
    }
}
