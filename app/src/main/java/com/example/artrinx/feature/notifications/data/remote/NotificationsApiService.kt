package com.example.artrinx.feature.notifications.data.remote

import com.example.artrinx.feature.home.data.remote.dto.EnvelopeDto
import com.example.artrinx.feature.notifications.data.remote.dto.NotificationListDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

/** Notification endpoints (API §8). */
interface NotificationsApiService {

    /** List the current user's notifications (§8.1/§8.2). */
    @GET("api/notifications")
    suspend fun getNotifications(): Response<EnvelopeDto<NotificationListDto>>

    /** Mark a single notification read (§8.1). Returns an empty envelope. */
    @PATCH("api/notifications/{id}/read")
    suspend fun markRead(@Path("id") id: Long): Response<ResponseBody>
}
