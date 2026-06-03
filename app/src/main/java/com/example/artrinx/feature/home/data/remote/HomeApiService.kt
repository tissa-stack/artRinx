package com.example.artrinx.feature.home.data.remote

import com.example.artrinx.feature.home.data.remote.dto.ArtworkDto
import com.example.artrinx.feature.home.data.remote.dto.CurationDto
import com.example.artrinx.feature.home.data.remote.dto.DiscoverFeedDto
import com.example.artrinx.feature.home.data.remote.dto.EnvelopeDto
import com.example.artrinx.feature.home.data.remote.dto.LikeArtworkRequest
import com.example.artrinx.feature.home.data.remote.dto.LikeCurationRequest
import com.example.artrinx.feature.home.data.remote.dto.PageDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface HomeApiService {

    /** One call returns sponsored banners + new_art + popular_curations for the Discover tab. */
    @GET("api/feed/discover")
    suspend fun getDiscoverFeed(): Response<EnvelopeDto<DiscoverFeedDto>>

    @GET("api/artworks/shop")
    suspend fun getShopArtworks(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Response<EnvelopeDto<PageDto<ArtworkDto>>>

    @POST("api/artworks/like")
    suspend fun likeArtwork(@Body body: LikeArtworkRequest): Response<ResponseBody>

    @DELETE("api/artworks/{artworkId}/like")
    suspend fun unlikeArtwork(@Path("artworkId") artworkId: Int): Response<ResponseBody>

    // ── Detail screens ──────────────────────────────────────────────────────

    @GET("api/artworks/{id}")
    suspend fun getArtwork(@Path("id") id: Int): Response<EnvelopeDto<ArtworkDto>>

    @GET("api/artworks/{id}/similar")
    suspend fun getSimilarArtworks(
        @Path("id") id: Int,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Response<EnvelopeDto<PageDto<ArtworkDto>>>

    @GET("api/curations/{id}")
    suspend fun getCuration(@Path("id") id: Int): Response<EnvelopeDto<CurationDto>>

    @GET("api/curations/all")
    suspend fun getAllCurations(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Response<EnvelopeDto<PageDto<CurationDto>>>

    @POST("api/curations/like")
    suspend fun likeCuration(@Body body: LikeCurationRequest): Response<ResponseBody>

    @DELETE("api/curations/{curationId}/like")
    suspend fun unlikeCuration(@Path("curationId") curationId: Int): Response<ResponseBody>
}