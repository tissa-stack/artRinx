package com.rinx.artRINXapp.core.push

import com.rinx.artRINXapp.core.push.dto.FcmTokenRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/** Push-token registration (API §11). Uses the authed Retrofit so the Bearer is attached. */
interface PushApiService {

    @POST("api/me/fcm-token")
    suspend fun registerToken(@Body body: FcmTokenRequest): Response<ResponseBody>
}
