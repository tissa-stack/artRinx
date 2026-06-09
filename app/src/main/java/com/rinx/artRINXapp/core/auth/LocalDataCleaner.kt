package com.rinx.artRINXapp.core.auth

import com.rinx.artRINXapp.core.offline.LiveMutationQueue
import com.rinx.artRINXapp.feature.auth.data.local.SessionDataSource
import com.rinx.artRINXapp.feature.home.data.local.CurationPreviewStore
import com.rinx.artRINXapp.feature.home.data.local.DetailCache
import com.rinx.artRINXapp.feature.home.domain.repository.HomeRepository
import com.rinx.artRINXapp.feature.profile.data.local.ProfileDraftDataSource
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import com.rinx.artRINXapp.feature.search.domain.repository.SearchRepository
import com.rinx.artRINXapp.feature.upload.domain.CurationManager
import com.rinx.artRINXapp.feature.upload.domain.UploadManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single chokepoint for wiping locally-held user data. Prevents one account's data from leaking into
 * the next session:
 *  - [clearCaches] drops the in-memory SWR caches + transient upload state. Safe on a fresh sign-in
 *    (keeps the just-saved session) so a new login never briefly shows the previous user's content.
 *  - [clearAll] additionally clears the session tokens, the profile-creation draft, and the offline
 *    like queue — called on logout and account deletion.
 *
 * App-wide, non-user prefs (onboarding-complete flag, home-tour flag) are intentionally NOT touched.
 */
@Singleton
class LocalDataCleaner @Inject constructor(
    private val session: SessionDataSource,
    private val profileDraft: ProfileDraftDataSource,
    private val liveMutationQueue: LiveMutationQueue,
    private val uploadManager: UploadManager,
    private val curationManager: CurationManager,
    private val homeRepository: HomeRepository,
    private val searchRepository: SearchRepository,
    private val profileRepository: ProfileRepository,
    private val detailCache: DetailCache,
    private val curationPreviewStore: CurationPreviewStore,
) {
    /** Wipe in-memory caches + transient upload/curation state. Keeps the session. */
    fun clearCaches() {
        uploadManager.dismiss()
        curationManager.dismiss()
        homeRepository.clearCache()
        searchRepository.clearCache()
        profileRepository.clearCache()
        detailCache.clear()
        curationPreviewStore.clear()
    }

    /** Wipe ALL user-scoped local state — tokens, draft, offline queue, and caches. Logout/delete. */
    suspend fun clearAll() {
        session.clearSession()
        profileDraft.clearDraft()
        liveMutationQueue.clear()
        clearCaches()
    }
}
