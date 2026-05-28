package com.example.artrinx.feature.auth.data.remote

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface AuthApiService {

    @FormUrlEncoded
    @POST("api/verify-invite")
    suspend fun verifyInvite(
        @Field("invitation_code") invitationCode: String,
    ): Response<String>
}
