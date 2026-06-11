package com.rinx.artRINXapp.feature.share.data.remote

import com.rinx.artRINXapp.feature.share.data.remote.dto.ShareRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/** Share endpoint (API §11). */
interface ShareApiService {

    /** Share an entity to selected followers — backend fans out the `*_share` notifications. */
    @POST("api/share")
    suspend fun share(@Body body: ShareRequest): Response<ResponseBody>
}
