package com.rinx.artRINXapp.feature.profile.data.repository

import android.content.Context
import android.net.Uri
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.util.ProfileRefreshBus
import com.rinx.artRINXapp.feature.home.data.remote.dto.ArtworkDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.CurationDto
import com.rinx.artRINXapp.feature.profile.data.remote.ProfileApiService
import com.rinx.artRINXapp.feature.profile.data.remote.dto.BlockRequest
import com.rinx.artRINXapp.feature.profile.data.remote.dto.FeedbackRequest
import com.rinx.artRINXapp.feature.profile.data.remote.dto.FollowRequest
import com.rinx.artRINXapp.feature.profile.data.remote.dto.FollowUserDto
import com.rinx.artRINXapp.feature.profile.data.remote.dto.ReportArtworkRequest
import com.rinx.artRINXapp.feature.profile.data.remote.dto.ReportCurationRequest
import com.rinx.artRINXapp.feature.profile.data.remote.dto.ReportMessageRequest
import com.rinx.artRINXapp.feature.profile.domain.model.BlockedArtwork
import com.rinx.artRINXapp.feature.profile.domain.model.BlockedUser
import com.rinx.artRINXapp.feature.profile.domain.model.CurrentUser
import com.rinx.artRINXapp.feature.profile.domain.model.EditableProfile
import com.rinx.artRINXapp.feature.profile.domain.model.FollowUser
import com.rinx.artRINXapp.feature.profile.domain.model.InviteInfo
import com.rinx.artRINXapp.feature.profile.domain.model.InvitedUser
import com.rinx.artRINXapp.feature.profile.domain.model.Medium
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileArtItem
import com.rinx.artRINXapp.feature.profile.domain.model.PublicProfile
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileCurationItem
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileDraft
import com.rinx.artRINXapp.feature.profile.domain.model.ProfilePlanSummary
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileType
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileUpdate
import com.rinx.artRINXapp.feature.profile.domain.model.UploadQuota
import com.rinx.artRINXapp.feature.profile.domain.model.UserProfileData
import com.rinx.artRINXapp.core.network.toApiError
import com.rinx.artRINXapp.feature.auth.data.local.SessionDataSource
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import com.rinx.artRINXapp.feature.search.domain.model.CardHeight
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val apiService: ProfileApiService,
    @ApplicationContext private val context: Context,
    private val profileRefreshBus: ProfileRefreshBus,
    private val session: SessionDataSource,
    private val blockedUsersStore: com.rinx.artRINXapp.core.util.BlockedUsersStore,
) : ProfileRepository {

    // SWR cache for the Profile tab (first page only) — survives navigation (@Singleton);
    // cleared on logout/delete.
    @Volatile private var profileDataCache: UserProfileData? = null
    @Volatile private var myArtworksCache: List<ProfileArtItem>? = null
    @Volatile private var myCurationsCache: List<ProfileCurationItem>? = null
    @Volatile private var likedArtworksCache: List<ProfileArtItem>? = null
    // Logged-in user's numeric id — cached from getMyProfile so "is this my profile?" is instant.
    @Volatile private var currentUserIdCache: Int? = null

    override fun cachedCurrentUserId(): Int? = currentUserIdCache
    override fun cachedProfileData(): UserProfileData? = profileDataCache
    override fun cachedMyArtworks(): List<ProfileArtItem>? = myArtworksCache
    override fun cachedMyCurations(): List<ProfileCurationItem>? = myCurationsCache
    override fun cachedLikedArtworks(): List<ProfileArtItem>? = likedArtworksCache
    override fun clearCache() {
        profileDataCache = null
        myArtworksCache = null
        myCurationsCache = null
        likedArtworksCache = null
        currentUserIdCache = null
    }

    private val gson = Gson()
    private val textPlain = "text/plain; charset=utf-8".toMediaType()

    override suspend fun getProfileTypes(): ApiResult<List<ProfileType>> {
        return try {
            val response = apiService.getProfileTypes()
            if (response.isSuccessful) {
                val types = response.body()?.data?.map { ProfileType(it.id, it.name) } ?: emptyList()
                ApiResult.Success(types)
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun getMyProfile(): ApiResult<CurrentUser> {
        return try {
            val response = apiService.getMyProfile()
            val dto = response.body()?.data
            if (response.isSuccessful && dto?.id != null) {
                currentUserIdCache = dto.id
                ApiResult.Success(
                    CurrentUser(
                        id = dto.id,
                        username = dto.username.orEmpty(),
                        displayName = dto.displayName ?: dto.username.orEmpty(),
                        avatarUrl = dto.profilePictureUrl,
                    ),
                )
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun getUploadQuota(): ApiResult<UploadQuota> {
        return try {
            val response = apiService.getMyProfile()
            val dto = response.body()?.data
            if (response.isSuccessful && dto != null) {
                val sub = dto.subscription
                // is_paid is the source of truth; fall back to plan+status if absent (handout §subscription).
                val isPaid = sub?.isPaid
                    ?: (sub?.plan == "artist_pro" && (sub.status == "active" || sub.status == "trialing"))
                // profile_type_name preferred; fall back to the cached session role.
                val role = dto.profileTypeName?.takeIf { it.isNotBlank() }
                    ?: session.getUserRole().orEmpty()
                // max_uploads preferred; else derive from the 2026-06 tier matrix (10 / 99 / 99).
                val maxUploads = dto.maxUploads ?: tierUploadCap(role, isPaid)
                ApiResult.Success(
                    UploadQuota(
                        isPaid = isPaid,
                        role = role,
                        artworkCount = dto.artworkCount ?: 0,
                        maxUploads = maxUploads,
                    ),
                )
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    /** 2026-06 tier matrix fallback when the server omits max_uploads. Basic 10, paid (Pro/Gallery) 99. */
    private fun tierUploadCap(role: String, isPaid: Boolean): Int = when {
        role.contains("gallery", ignoreCase = true) -> 99
        isPaid -> 99
        else -> 10
    }

    override suspend fun getProfileData(): ApiResult<UserProfileData> {
        return try {
            val response = apiService.getMyProfile()
            val dto = response.body()?.data
            if (response.isSuccessful && dto != null) {
                val data = UserProfileData(
                    handle = dto.username?.let { "@$it" } ?: "",
                    displayName = dto.displayName ?: dto.fullName ?: dto.username.orEmpty(),
                    // Role name lives in profile_type_name (e.g. "Collector"), matching every
                    // other mapping; profile_title is a legacy fallback.
                    role = dto.profileTypeName?.takeIf { it.isNotBlank() } ?: dto.profileTitle.orEmpty(),
                    bio = dto.bio.orEmpty(),
                    website = dto.profileLink.orEmpty(),
                    avatarUrl = dto.profilePictureUrl,
                    artCount = dto.artworkCount ?: 0,
                    curationCount = dto.curationCount ?: 0,
                    followerCount = dto.followerCount ?: 0,
                    followingCount = dto.followingCount ?: 0,
                )
                profileDataCache = data
                ApiResult.Success(data)
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun getEditableProfile(): ApiResult<EditableProfile> {
        return try {
            val response = apiService.getMyProfile()
            val dto = response.body()?.data
            if (response.isSuccessful && dto != null) {
                ApiResult.Success(
                    EditableProfile(
                        username = dto.username.orEmpty(),
                        fullName = dto.fullName.orEmpty(),
                        displayName = dto.displayName ?: dto.fullName ?: dto.username.orEmpty(),
                        bio = dto.bio.orEmpty(),
                        dob = dto.dob.orEmpty(),
                        country = dto.country.orEmpty(),
                        state = dto.state.orEmpty(),
                        city = dto.city.orEmpty(),
                        profilePictureUrl = dto.profilePictureUrl,
                        fullNameEditCount = dto.fullNameEditCount ?: 0,
                        marketingSmsConsent = dto.marketingSmsConsent ?: false,
                    ),
                )
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun getProfilePlanSummary(): ApiResult<ProfilePlanSummary> {
        return try {
            val response = apiService.getMyProfile()
            val dto = response.body()?.data
            if (response.isSuccessful && dto != null) {
                val sub = dto.subscription
                // Premium only when on the artist_pro plan AND the subscription is live.
                val isPremium = sub?.plan == "artist_pro" &&
                    sub.status in setOf("active", "trialing")
                ApiResult.Success(
                    ProfilePlanSummary(
                        // Role name lives in profile_type_name (e.g. "Art Curious"); profile_title is
                        // a legacy fallback and is empty for new users.
                        profileTitle = dto.profileTypeName?.takeIf { it.isNotBlank() } ?: dto.profileTitle.orEmpty(),
                        isPremium = isPremium,
                        nextBillingDate = if (isPremium) formatBillingDate(sub?.renewsAt ?: sub?.expiresAt) else "",
                    ),
                )
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    /** ISO 8601 (e.g. "2026-12-31T...") → "31 Dec 2026". Returns "" on null/unparseable input. */
    private fun formatBillingDate(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            val datePart = iso.take(10) // yyyy-MM-dd — avoids timezone/fractional parsing
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(datePart) ?: return ""
            SimpleDateFormat("d MMM yyyy", Locale.US).format(parsed)
        } catch (_: Exception) {
            ""
        }
    }

    override suspend fun updateProfile(changes: ProfileUpdate, newPictureUri: Uri?): ApiResult<Unit> =
        withContext(Dispatchers.IO) {
            // Nothing changed and no new picture — treat as a successful no-op (no network call).
            if (!changes.hasAnyField && newPictureUri == null) return@withContext ApiResult.Success(Unit)
            try {
                val parts = mutableMapOf<String, RequestBody>()
                changes.username?.let { parts["username"] = it.toRequestBody(textPlain) }
                changes.fullName?.let { parts["full_name"] = it.toRequestBody(textPlain) }
                changes.displayName?.let { parts["display_name"] = it.toRequestBody(textPlain) }
                changes.bio?.let { parts["bio"] = it.toRequestBody(textPlain) }
                // Date of birth as ISO YYYY-MM-DD; the server derives age and enforces a 13+ minimum.
                changes.dob?.let { dob -> dob.ifBlank { null }?.let { parts["dob"] = it.toRequestBody(textPlain) } }
                changes.country?.let { parts["country"] = it.toRequestBody(textPlain) }
                changes.state?.let { parts["state"] = it.toRequestBody(textPlain) }
                changes.city?.let { parts["city"] = it.toRequestBody(textPlain) }
                changes.profileTypeId?.let { parts["profile_type_id"] = it.toString().toRequestBody(textPlain) }
                changes.marketingSmsConsent?.let { parts["marketing_sms_consent"] = it.toString().toRequestBody(textPlain) }

                val picturePart = newPictureUri?.let { uri ->
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bytes = stream.readBytes()
                        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                        val body = bytes.toRequestBody(mimeType.toMediaType())
                        MultipartBody.Part.createFormData("profile_picture", "profile_picture.jpg", body)
                    }
                }

                val response = apiService.updateProfile(parts, picturePart)
                if (response.isSuccessful) {
                    profileRefreshBus.signal()
                    ApiResult.Success(Unit)
                } else {
                    val rawError = response.errorBody()?.string()
                    when (response.code()) {
                        422 -> ApiResult.Error.Validation(parseValidationError(rawError))
                        in 400..499 -> ApiResult.Error.Validation(
                            if (isUnderMinimumAge(rawError)) UNDER_MIN_AGE_MESSAGE
                            else "Couldn't save changes. Please check your details.",
                        )
                        in 500..599 -> ApiResult.Error.Server(response.code())
                        else -> ApiResult.Error.Unknown(RuntimeException("HTTP ${response.code()}"))
                    }
                }
            } catch (e: IOException) {
                ApiResult.Error.Network(e)
            } catch (e: Exception) {
                ApiResult.Error.Unknown(e)
            }
        }

    override suspend fun getInviteInfo(): ApiResult<InviteInfo> {
        return try {
            val response = apiService.getMyProfile()
            val dto = response.body()?.data
            if (response.isSuccessful && dto != null) {
                ApiResult.Success(
                    InviteInfo(
                        code = dto.invitationCode,
                        // Prefer the override-aware nested counters (handout §16); fall back to legacy flat fields.
                        remainingInvites = dto.peerInvites?.remaining ?: dto.remainingInvites,
                        remainingChatInvites = dto.messageRequests?.remaining ?: dto.remainingChatInvites,
                        chatInvitesMonthlyCap = dto.messageRequests?.monthlyCap,
                        invitesMonthlyCap = dto.peerInvites?.monthlyCap ?: dto.totalUserInvites,
                    ),
                )
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun getInvitedUsers(code: String, page: Int, size: Int): ApiResult<List<InvitedUser>> {
        return try {
            val response = apiService.getInvitedUsers(code, page, size)
            if (response.isSuccessful) {
                val items = response.body()?.data?.items.orEmpty().map { dto ->
                    val username = dto.username.orEmpty()
                    InvitedUser(
                        id = dto.id?.toString() ?: username,
                        name = dto.displayName ?: dto.fullName ?: username,
                        handle = if (username.isNotBlank()) "@$username" else "",
                        joinedDate = formatJoinedDate(dto.joinedAt),
                        avatarUrl = dto.profilePictureUrl,
                    )
                }
                ApiResult.Success(items)
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    /** ISO 8601 (e.g. "2026-06-05T14:16:03.122Z") → "05/06/26". Returns "" on null/unparseable input. */
    private fun formatJoinedDate(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso.take(10)) ?: return ""
            SimpleDateFormat("dd/MM/yy", Locale.US).format(parsed)
        } catch (_: Exception) {
            ""
        }
    }

    override suspend fun getBlockedUsers(page: Int, size: Int): ApiResult<List<BlockedUser>> {
        return try {
            val response = apiService.getBlockedUsers(page, size)
            if (response.isSuccessful) {
                val items = response.body()?.data?.items.orEmpty().mapNotNull { dto ->
                    val userId = dto.userId ?: dto.id ?: return@mapNotNull null
                    val username = dto.username.orEmpty()
                    BlockedUser(
                        userId = userId,
                        name = dto.displayName ?: dto.fullName ?: username,
                        role = dto.profileTypeName.orEmpty(),
                        avatarUrl = dto.profilePictureUrl,
                    )
                }
                blockedUsersStore.seed(items.map { it.userId }) // keep the local record in sync
                ApiResult.Success(items)
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun unblockUser(userId: Int): ApiResult<Unit> {
        return try {
            val response = apiService.unblockUser(userId)
            if (response.isSuccessful) {
                blockedUsersStore.markUnblocked(userId)
                ApiResult.Success(Unit)
            } else {
                response.toApiError() // carry the server's reason (e.g. business-rule 400) to the UI
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun getBlockedArtworks(page: Int, size: Int): ApiResult<List<BlockedArtwork>> {
        return try {
            val response = apiService.getBlockedArtworks(page, size)
            if (response.isSuccessful) {
                val items = response.body()?.data?.items.orEmpty().mapNotNull { dto ->
                    val artId = dto.id ?: return@mapNotNull null
                    BlockedArtwork(
                        artId = artId,
                        title = dto.title.orEmpty(),
                        thumbnailUrl = dto.thumbnailUrl ?: dto.webpUrl ?: dto.imageUrl,
                    )
                }
                ApiResult.Success(items)
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun unblockArtwork(artworkId: Int): ApiResult<Unit> {
        return try {
            val response = apiService.unblockArtwork(artworkId)
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                response.toApiError() // carry the server's reason to the UI
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun reportArtwork(artworkId: Int, message: String): ApiResult<Unit> {
        return try {
            val response = apiService.reportArtwork(ReportArtworkRequest(artworkId = artworkId, message = message))
            if (response.isSuccessful) ApiResult.Success(Unit) else response.toApiError()
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun reportCuration(curationId: Int, message: String): ApiResult<Unit> {
        return try {
            val response = apiService.reportCuration(ReportCurationRequest(curationId = curationId, message = message))
            if (response.isSuccessful) ApiResult.Success(Unit) else response.toApiError()
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun reportUser(userId: Int, message: String): ApiResult<Unit> {
        return try {
            val response = apiService.reportMessage(ReportMessageRequest(reportedUserId = userId, message = message))
            if (response.isSuccessful) ApiResult.Success(Unit) else response.toApiError()
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun getPublicProfile(userId: Int): ApiResult<PublicProfile> {
        return try {
            val response = apiService.getPublicProfile(userId)
            val dto = response.body()?.data
            if (response.isSuccessful && dto != null) {
                ApiResult.Success(
                    PublicProfile(
                        userId = userId,
                        handle = dto.username?.let { "@$it" } ?: "",
                        displayName = dto.displayName ?: dto.username.orEmpty(),
                        role = dto.profileTypeName.orEmpty(),
                        bio = dto.bio.orEmpty(),
                        website = dto.profileLink.orEmpty(),
                        avatarUrl = dto.profilePictureUrl,
                        artCount = dto.artworkCount ?: 0,
                        curationCount = dto.curationCount ?: 0,
                        followerCount = dto.followerCount ?: 0,
                        followingCount = dto.followingCount ?: 0,
                        isFollowing = dto.isFollowing ?: false,
                        iBlocked = dto.iBlocked ?: false,
                        theyBlocked = dto.theyBlocked ?: false,
                        canMessage = dto.canMessage ?: true,
                        blockReason = dto.blockReason,
                        chatroomId = dto.chatroomId,
                    ),
                )
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun getPublicArtworks(userId: Int, page: Int, size: Int): ApiResult<List<ProfileArtItem>> {
        return try {
            val response = apiService.getPublicArtworks(userId, page, size)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.data?.items.orEmpty().map { it.toProfileArtItem() })
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun getArtworksByName(name: String, page: Int, size: Int): ApiResult<List<ProfileArtItem>> {
        return try {
            val response = apiService.getArtworksByName(name, page, size)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.data?.items.orEmpty().map { it.toProfileArtItem() })
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun getPublicCurations(userId: Int, page: Int, size: Int): ApiResult<List<ProfileCurationItem>> {
        return try {
            val response = apiService.getPublicCurations(userId, page, size)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.data?.items.orEmpty().map { it.toProfileCurationItem() })
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun followUser(userId: Int): ApiResult<Unit> {
        return try {
            val response = apiService.follow(FollowRequest(followedId = userId))
            if (response.isSuccessful) ApiResult.Success(Unit) else response.toApiError()
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun unfollowUser(userId: Int): ApiResult<Unit> {
        return try {
            val response = apiService.unfollow(userId)
            if (response.isSuccessful) ApiResult.Success(Unit) else response.toApiError()
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun getFollowers(page: Int, size: Int): ApiResult<List<FollowUser>> {
        return try {
            val response = apiService.getFollowers(page, size)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.data?.items.orEmpty().mapNotNull { it.toFollowUser() })
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun getFollowing(page: Int, size: Int): ApiResult<List<FollowUser>> {
        return try {
            val response = apiService.getFollowing(page, size)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.data?.items.orEmpty().mapNotNull { it.toFollowUser() })
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    private fun FollowUserDto.toFollowUser(): FollowUser? {
        val uid = id ?: return null
        val uname = username.orEmpty()
        return FollowUser(
            userId = uid,
            name = displayName ?: uname,
            handle = if (uname.isNotBlank()) "@$uname" else "",
            avatarUrl = profilePictureUrl,
            isFollowing = isFollowing ?: false,
        )
    }

    override suspend fun blockArtwork(artworkId: Int, message: String): ApiResult<Unit> {
        return try {
            val response = apiService.block(BlockRequest(artId = artworkId, message = message))
            if (response.isSuccessful) ApiResult.Success(Unit) else response.toApiError()
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun submitFeedback(review: String): ApiResult<Unit> {
        return try {
            val response = apiService.submitFeedback(FeedbackRequest(review = review))
            if (response.isSuccessful) ApiResult.Success(Unit) else response.toApiError()
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun blockUser(userId: Int): ApiResult<Unit> {
        return try {
            val response = apiService.block(BlockRequest(userId = userId))
            if (response.isSuccessful) {
                blockedUsersStore.markBlocked(userId) // authoritative local record (survives profile 500s)
                ApiResult.Success(Unit)
            } else {
                response.toApiError()
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun checkUsername(username: String): ApiResult<Boolean> {
        return try {
            val response = apiService.checkUsername(username)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.data ?: false)
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun getMediums(): ApiResult<List<Medium>> {
        return try {
            val response = apiService.getMediums()
            if (response.isSuccessful) {
                val mediums = response.body()?.data?.map {
                    Medium(it.id, it.title, it.picture)
                } ?: emptyList()
                ApiResult.Success(mediums)
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun getMyArtworks(page: Int, size: Int): ApiResult<List<ProfileArtItem>> {
        return try {
            val response = apiService.getMyArtworks(page, size)
            if (response.isSuccessful) {
                val items = response.body()?.data?.items.orEmpty().map { it.toProfileArtItem() }
                if (page == 1) myArtworksCache = items
                ApiResult.Success(items)
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun getMyCurations(page: Int, size: Int): ApiResult<List<ProfileCurationItem>> {
        return try {
            val response = apiService.getMyCurations(page, size)
            if (response.isSuccessful) {
                val items = response.body()?.data?.items.orEmpty().map { it.toProfileCurationItem() }
                if (page == 1) myCurationsCache = items
                ApiResult.Success(items)
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun getLikedArtworks(page: Int, size: Int): ApiResult<List<ProfileArtItem>> {
        return try {
            val response = apiService.getLikedArtworks(page, size)
            if (response.isSuccessful) {
                val items = response.body()?.data?.items.orEmpty().map { it.toProfileArtItem() }
                if (page == 1) likedArtworksCache = items
                ApiResult.Success(items)
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    private fun CurationDto.toProfileCurationItem(): ProfileCurationItem = ProfileCurationItem(
        id = id?.toString().orEmpty(),
        title = title ?: "Untitled",
        handle = author?.username?.let { "@$it" } ?: "",
        artworkRes = emptyList(),
        artworkUrls = artworks.orEmpty().mapNotNull { it.imageUrl ?: it.thumbnailUrl },
        isPrivate = privacy ?: false,
    )

    private fun ArtworkDto.toProfileArtItem(): ProfileArtItem = ProfileArtItem(
        id = id?.toString().orEmpty(),
        imageRes = null,
        imageUrl = imageUrl ?: thumbnailUrl ?: webpUrl,
        title = title ?: "Untitled",
        artistName = artist?.artistName?.takeIf { it.isNotBlank() } ?: displayName.orEmpty(),
        isPrivate = privacy ?: false,
        cardHeight = aspectRatio.toCardHeight(),
    )

    /** aspect_ratio is width/height: <1 = portrait (tall), >1 = landscape (short). */
    private fun Double?.toCardHeight(): CardHeight = when {
        this == null -> CardHeight.MEDIUM
        this <= 0.85 -> CardHeight.TALL
        this >= 1.3 -> CardHeight.SHORT
        else -> CardHeight.MEDIUM
    }

    override suspend fun createProfile(draft: ProfileDraft, pictureUri: Uri?): ApiResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val parts = mutableMapOf<String, RequestBody>()
                parts["username"] = draft.username.toRequestBody(textPlain)
                parts["display_name"] = draft.displayName.toRequestBody(textPlain)
                parts["full_name"] = draft.fullName.toRequestBody(textPlain)
                parts["profile_type_id"] = (draft.profileTypeId?.toString() ?: "").toRequestBody(textPlain)
                parts["accepted_terms"] = "true".toRequestBody(textPlain)
                parts["sms_2fa_consent"] = "false".toRequestBody(textPlain)
                parts["account_notification_sms"] = "false".toRequestBody(textPlain)
                parts["marketing_sms_consent"] = "false".toRequestBody(textPlain)
                if (draft.bio.isNotBlank()) parts["bio"] = draft.bio.toRequestBody(textPlain)
                // Date of birth as ISO YYYY-MM-DD; the server derives age and enforces a 13+ minimum.
                if (draft.dob.isNotBlank()) parts["dob"] = draft.dob.toRequestBody(textPlain)
                if (draft.country.isNotBlank()) parts["country"] = draft.country.toRequestBody(textPlain)
                if (draft.state.isNotBlank()) parts["state"] = draft.state.toRequestBody(textPlain)
                if (draft.city.isNotBlank()) parts["city"] = draft.city.toRequestBody(textPlain)
                if (draft.mediumIds.isNotEmpty()) {
                    parts["medium_ids"] = draft.mediumIds.joinToString(",").toRequestBody(textPlain)
                }

                val picturePart = pictureUri?.let { uri ->
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bytes = stream.readBytes()
                        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                        val body = bytes.toRequestBody(mimeType.toMediaType())
                        MultipartBody.Part.createFormData("profile_picture", "profile_picture.jpg", body)
                    }
                }

                val response = apiService.createProfile(parts, picturePart)
                if (response.isSuccessful) {
                    ApiResult.Success(Unit)
                } else {
                    val rawError = response.errorBody()?.string()
                    when (response.code()) {
                        // Cold-launch recovery: if the user force-quit between a successful
                        // POST profile and the final wizard step, re-firing returns 409
                        // "already exists". iOS treats 409 as success and advances; do the same.
                        409 -> ApiResult.Success(Unit)
                        422 -> ApiResult.Error.Validation(parseValidationError(rawError))
                        in 400..499 -> ApiResult.Error.Validation(
                            if (isUnderMinimumAge(rawError)) UNDER_MIN_AGE_MESSAGE
                            else "Profile creation failed. Please check your details.",
                        )
                        in 500..599 -> ApiResult.Error.Server(response.code())
                        else -> ApiResult.Error.Unknown(RuntimeException("HTTP ${response.code()}"))
                    }
                }
            } catch (e: IOException) {
                ApiResult.Error.Network(e)
            } catch (e: Exception) {
                ApiResult.Error.Unknown(e)
            }
        }

    private fun profileError(code: Int): ApiResult.Error = when (code) {
        in 400..499 -> ApiResult.Error.Validation("Request failed ($code)")
        in 500..599 -> ApiResult.Error.Server(code)
        else -> ApiResult.Error.Unknown(RuntimeException("HTTP $code"))
    }

    private fun parseValidationError(body: String?): String {
        if (body == null) return "Validation failed"
        return try {
            val obj = gson.fromJson(body, Map::class.java)
            (obj["detail"] as? List<*>)
                ?.filterIsInstance<Map<*, *>>()
                ?.firstOrNull()
                ?.get("msg") as? String
                ?: "Validation failed"
        } catch (_: Exception) {
            "Validation failed"
        }
    }

    /** The server flags an under-13 DOB with `detail.code = under_minimum_age` on a 400. */
    private fun isUnderMinimumAge(body: String?): Boolean =
        body?.contains("under_minimum_age", ignoreCase = true) == true
}

private const val UNDER_MIN_AGE_MESSAGE = "You must be at least 13 years old to use ArtRINX."
