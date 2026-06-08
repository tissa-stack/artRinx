package com.example.artrinx.feature.notifications.domain.repository

import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.notifications.domain.model.NotificationItem

interface NotificationsRepository {

    /** List the current user's notifications (§8.1). */
    suspend fun getNotifications(): ApiResult<List<NotificationItem>>

    /** Mark a single notification read (§8.1). [id] is the notification's id as a String. */
    suspend fun markRead(id: String): ApiResult<Unit>
}
