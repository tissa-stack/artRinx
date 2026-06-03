package com.example.artrinx.feature.profile.data.remote

import com.example.artrinx.feature.profile.data.remote.dto.CreateProfileResponseDto
import com.example.artrinx.feature.profile.data.remote.dto.MediumsResponseDto
import com.example.artrinx.feature.profile.data.remote.dto.ProfileTypesResponseDto
import com.example.artrinx.feature.profile.data.remote.dto.UsernameCheckResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Query

interface ProfileApiService {

    @GET("api/profile/types")
    suspend fun getProfileTypes(): Response<ProfileTypesResponseDto>

    @GET("api/profile/username-check")
    suspend fun checkUsername(
        @Query("username") username: String,
    ): Response<UsernameCheckResponseDto>

    @GET("api/mediums/")
    suspend fun getMediums(): Response<MediumsResponseDto>

    @Multipart
    @POST("api/profile")
    suspend fun createProfile(
        @PartMap parts: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part profilePicture: MultipartBody.Part?,
    ): Response<CreateProfileResponseDto>
}
