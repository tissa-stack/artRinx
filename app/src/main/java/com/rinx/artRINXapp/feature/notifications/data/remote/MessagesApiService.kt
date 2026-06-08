package com.rinx.artRINXapp.feature.notifications.data.remote

import com.rinx.artRINXapp.feature.home.data.remote.dto.EnvelopeDto
import com.rinx.artRINXapp.feature.notifications.data.remote.dto.ChatThreadDto
import com.rinx.artRINXapp.feature.notifications.data.remote.dto.ChatroomPreviewDto
import com.rinx.artRINXapp.feature.notifications.data.remote.dto.ChatroomResolveDto
import com.rinx.artRINXapp.feature.notifications.data.remote.dto.EditMessageRequest
import com.rinx.artRINXapp.feature.notifications.data.remote.dto.MessageDto
import com.rinx.artRINXapp.feature.notifications.data.remote.dto.ResolveChatroomRequest
import com.rinx.artRINXapp.feature.notifications.data.remote.dto.SendMessageRequest
import com.rinx.artRINXapp.feature.notifications.data.remote.dto.SendMessageResponseDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Chat / messaging endpoints (API §7). */
interface MessagesApiService {

    /** Send a message / invitation (§7.2). */
    @POST("api/messages/")
    suspend fun sendMessage(@Body body: SendMessageRequest): Response<EnvelopeDto<SendMessageResponseDto>>

    /** Fetch a thread with cursor pagination (§7.3). `before` omitted → newest page. */
    @GET("api/messages/with/{userId}")
    suspend fun getThread(
        @Path("userId") userId: Int,
        @Query("before") before: String?,
        @Query("limit") limit: Int,
    ): Response<EnvelopeDto<ChatThreadDto>>

    /** Edit a message's text — 15-min server-side window (§7.5). */
    @PATCH("api/messages/{messageId}")
    suspend fun editMessage(
        @Path("messageId") messageId: String,
        @Body body: EditMessageRequest,
    ): Response<EnvelopeDto<MessageDto>>

    /** Soft-delete a message (§7.6). */
    @DELETE("api/messages/{messageId}")
    suspend fun deleteMessage(@Path("messageId") messageId: String): Response<ResponseBody>

    /** Mark a single message read (§7.7). */
    @POST("api/messages/{messageId}/read")
    suspend fun markRead(@Path("messageId") messageId: String): Response<ResponseBody>

    /** Chatroom previews for the inbox (§7.8). */
    @GET("api/my/chatrooms")
    suspend fun getChatrooms(): Response<EnvelopeDto<List<ChatroomPreviewDto>>>

    /** Resolve a user id → chatroom + invitation/block/invite-count state (§7.9). */
    @POST("api/chatroom_id")
    suspend fun resolveChatroom(@Body body: ResolveChatroomRequest): Response<EnvelopeDto<ChatroomResolveDto>>

    /** Delete a chat from the current user's side only (§7.10). */
    @DELETE("api/chatroom/with/{userId}")
    suspend fun deleteChat(@Path("userId") userId: Int): Response<ResponseBody>
}
