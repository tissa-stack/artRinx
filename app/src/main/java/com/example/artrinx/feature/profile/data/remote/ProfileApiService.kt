package com.example.artrinx.feature.profile.data.remote

import com.example.artrinx.feature.home.data.remote.dto.ArtworkDto
import com.example.artrinx.feature.home.data.remote.dto.CurationDto
import com.example.artrinx.feature.home.data.remote.dto.EnvelopeDto
import com.example.artrinx.feature.home.data.remote.dto.PageDto
import com.example.artrinx.feature.profile.data.remote.dto.CreateProfileResponseDto
import com.example.artrinx.feature.profile.data.remote.dto.MediumsResponseDto
import com.example.artrinx.feature.profile.data.remote.dto.MyProfileDto
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

    /** The current logged-in user's own profile. */
    @GET("api/profile")
    suspend fun getMyProfile(): Response<EnvelopeDto<MyProfileDto>>

    /** Current user's own artworks (both public and private). */
    @GET("api/artworks/")
    suspend fun getMyArtworks(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Response<EnvelopeDto<PageDto<ArtworkDto>>>

    /** Current user's own curations (both public and private). */
    @GET("api/curations/")
    suspend fun getMyCurations(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Response<EnvelopeDto<PageDto<CurationDto>>>

    /** Artworks the current user has liked (the "Liked" tab). */
    @GET("api/artworks/liked")
    suspend fun getLikedArtworks(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Response<EnvelopeDto<PageDto<ArtworkDto>>>

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
