package com.rinx.artRINXapp.feature.upload.data.remote

import com.rinx.artRINXapp.feature.home.data.remote.dto.ArtworkDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.EnvelopeDto
import com.rinx.artRINXapp.feature.upload.data.remote.dto.CreateArtworkResultDto
import com.rinx.artRINXapp.feature.upload.data.remote.dto.PrepareUploadDto
import com.rinx.artRINXapp.feature.upload.data.remote.dto.UpdateArtworkBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path

interface UploadApiService {

    /** Step 1: ask the backend for a signed CDN URL + the file path to finalize with. No body. */
    @POST("api/prepare-upload/")
    suspend fun prepareUpload(): Response<EnvelopeDto<PrepareUploadDto>>

    /**
     * Step 3: finalize the artwork. The image bytes already went to the signed URL (step 2);
     * this call references the prepared file path via [imageUrl]. Sent as
     * application/x-www-form-urlencoded (per the live swagger). [tags] is a single
     * comma-separated string. [artistId]/[artistName] attribute the work (both optional —
     * omit for "no artist", which defaults to the uploader).
     */
    @FormUrlEncoded
    @POST("api/artworks/")
    suspend fun createArtwork(
        @Field("image_url") imageUrl: String,
        @Field("title") title: String,
        @Field("description") description: String?,
        @Field("artist_id") artistId: Int?,
        @Field("artist_name") artistName: String?,
        @Field("tags") tags: String?,
        @Field("medium_id") mediumId: Int?,
        @Field("shop_link") shopLink: String?,
        @Field("price") price: Double?,
        @Field("privacy") privacy: Boolean?,
        @Field("rekognition_tags") rekognitionTags: String?,
        @Field("aspect_ratio") aspectRatio: String?,
    ): Response<EnvelopeDto<CreateArtworkResultDto>>

    /** Fetch a single artwork (with all fields) — used to prefill the edit form. */
    @GET("api/artworks/{id}")
    suspend fun getArtwork(@Path("id") id: Int): Response<EnvelopeDto<ArtworkDto>>

    /** Edit an artwork's metadata (JSON; image is not changed). */
    @PUT("api/artworks/{id}")
    suspend fun updateArtwork(
        @Path("id") id: Int,
        @Body body: UpdateArtworkBody,
    ): Response<EnvelopeDto<CreateArtworkResultDto>>

    @DELETE("api/artworks/{id}")
    suspend fun deleteArtwork(@Path("id") id: Int): Response<ResponseBody>
}
