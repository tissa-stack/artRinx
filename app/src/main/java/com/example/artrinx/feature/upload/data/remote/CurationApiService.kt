package com.example.artrinx.feature.upload.data.remote

import com.example.artrinx.feature.home.data.remote.dto.ArtworkDto
import com.example.artrinx.feature.home.data.remote.dto.CurationDto
import com.example.artrinx.feature.home.data.remote.dto.EnvelopeDto
import com.example.artrinx.feature.home.data.remote.dto.PageDto
import com.example.artrinx.feature.upload.data.remote.dto.CreateCurationBody
import com.example.artrinx.feature.upload.data.remote.dto.UpdateCurationBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CurationApiService {

    /** Create a curation from existing artwork ids. */
    @POST("api/curations/")
    suspend fun createCuration(@Body body: CreateCurationBody): Response<EnvelopeDto<CurationDto>>

    /** Fetch a curation (with its artworks) — used to read existing artwork ids before adding. */
    @GET("api/curations/{id}")
    suspend fun getCuration(@Path("id") id: Int): Response<EnvelopeDto<CurationDto>>

    /** Update a curation's artwork membership (full replace). */
    @PUT("api/curations/{id}")
    suspend fun updateCuration(
        @Path("id") id: Int,
        @Body body: UpdateCurationBody,
    ): Response<EnvelopeDto<CurationDto>>

    /** Selection source: the current user's own artworks. */
    @GET("api/artworks/")
    suspend fun getMyArtworks(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Response<EnvelopeDto<PageDto<ArtworkDto>>>

    /** Selection source: artworks the current user has liked. */
    @GET("api/artworks/liked")
    suspend fun getLikedArtworks(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Response<EnvelopeDto<PageDto<ArtworkDto>>>
}
