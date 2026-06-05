package com.example.artrinx.feature.profile.data.remote

import com.example.artrinx.feature.home.data.remote.dto.ArtworkDto
import com.example.artrinx.feature.home.data.remote.dto.CurationDto
import com.example.artrinx.feature.home.data.remote.dto.EnvelopeDto
import com.example.artrinx.feature.home.data.remote.dto.PageDto
import com.example.artrinx.feature.profile.data.remote.dto.BlockRequest
import com.example.artrinx.feature.profile.data.remote.dto.BlockedUserDto
import com.example.artrinx.feature.profile.data.remote.dto.CreateProfileResponseDto
import com.example.artrinx.feature.profile.data.remote.dto.FollowRequest
import com.example.artrinx.feature.profile.data.remote.dto.FollowUserDto
import com.example.artrinx.feature.profile.data.remote.dto.InvitedUserDto
import com.example.artrinx.feature.profile.data.remote.dto.MediumsResponseDto
import com.example.artrinx.feature.profile.data.remote.dto.MyProfileDto
import com.example.artrinx.feature.profile.data.remote.dto.ProfileTypesResponseDto
import com.example.artrinx.feature.profile.data.remote.dto.PublicUserProfileDto
import com.example.artrinx.feature.profile.data.remote.dto.ReportArtworkRequest
import com.example.artrinx.feature.profile.data.remote.dto.ReportCurationRequest
import com.example.artrinx.feature.profile.data.remote.dto.ReportMessageRequest
import com.example.artrinx.feature.profile.data.remote.dto.UsernameCheckResponseDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Query

interface ProfileApiService {

    @GET("api/profile/types")
    suspend fun getProfileTypes(): Response<ProfileTypesResponseDto>

    /** The current logged-in user's own profile. */
    @GET("api/profile")
    suspend fun getMyProfile(): Response<EnvelopeDto<MyProfileDto>>

    /** Users who joined via [invitationCode] — the "people I invited" list (§2.3). */
    @GET("api/profiles/by-invite-code")
    suspend fun getInvitedUsers(
        @Query("invitation_code") invitationCode: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Response<EnvelopeDto<PageDto<InvitedUserDto>>>

    /** The current user's blocked users (§3.8). */
    @GET("api/blocked/users")
    suspend fun getBlockedUsers(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Response<EnvelopeDto<PageDto<BlockedUserDto>>>

    /** Unblock a user (§3.8). */
    @DELETE("api/unblock")
    suspend fun unblockUser(@Query("user_id") userId: Int): Response<ResponseBody>

    /** Block a user or an artwork (§3.8). */
    @POST("api/block")
    suspend fun block(@Body body: BlockRequest): Response<ResponseBody>

    /** Report an artwork (§3.9). */
    @POST("api/report-artwork")
    suspend fun reportArtwork(@Body body: ReportArtworkRequest): Response<ResponseBody>

    /** Report a curation. */
    @POST("api/report-curation")
    suspend fun reportCuration(@Body body: ReportCurationRequest): Response<ResponseBody>

    /** Report a user/profile (uses the report-message endpoint). */
    @POST("api/report-message")
    suspend fun reportMessage(@Body body: ReportMessageRequest): Response<ResponseBody>

    // ── Other user's public profile ─────────────────────────────────────────
    @GET("api/profile/{userId}/public/info")
    suspend fun getPublicProfile(@Path("userId") userId: Int): Response<EnvelopeDto<PublicUserProfileDto>>

    @GET("api/profile/{userId}/public/artworks")
    suspend fun getPublicArtworks(
        @Path("userId") userId: Int,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Response<EnvelopeDto<PageDto<ArtworkDto>>>

    @GET("api/profile/{userId}/public/curations")
    suspend fun getPublicCurations(
        @Path("userId") userId: Int,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Response<EnvelopeDto<PageDto<CurationDto>>>

    /** Follow a user. */
    @POST("api/follow")
    suspend fun follow(@Body body: FollowRequest): Response<ResponseBody>

    /** Unfollow a user. */
    @DELETE("api/users/{userId}/unfollow")
    suspend fun unfollow(@Path("userId") userId: Int): Response<ResponseBody>

    /** The current user's followers. */
    @GET("api/followers")
    suspend fun getFollowers(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Response<EnvelopeDto<PageDto<FollowUserDto>>>

    /** The users the current user follows. */
    @GET("api/followed-users")
    suspend fun getFollowing(
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): Response<EnvelopeDto<PageDto<FollowUserDto>>>

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

    /** Update the current user's profile. Multipart, same field shape as createProfile;
     *  only changed fields are sent (see ProfileRepository.updateProfile). */
    @Multipart
    @PUT("api/profile/update")
    suspend fun updateProfile(
        @PartMap parts: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part profilePicture: MultipartBody.Part?,
    ): Response<CreateProfileResponseDto>
}
