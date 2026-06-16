package com.rinx.artRINXapp.core.auth

import com.rinx.artRINXapp.core.offline.LiveMutationQueue
import com.rinx.artRINXapp.core.push.PushTokenManager
import com.rinx.artRINXapp.core.util.BlockedUsersStore
import com.rinx.artRINXapp.feature.auth.data.local.SessionDataSource
import com.rinx.artRINXapp.feature.home.data.local.CurationPreviewStore
import com.rinx.artRINXapp.feature.home.data.local.DetailCache
import com.rinx.artRINXapp.feature.home.domain.repository.HomeRepository
import com.rinx.artRINXapp.feature.notifications.data.local.ChatCache
import com.rinx.artRINXapp.feature.notifications.domain.OutgoingMessageStore
import com.rinx.artRINXapp.feature.notifications.domain.UnreadNotificationsStore
import com.rinx.artRINXapp.feature.profile.data.local.ProfileDraftDataSource
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import com.rinx.artRINXapp.feature.search.domain.repository.SearchRepository
import com.rinx.artRINXapp.feature.upload.domain.CurationManager
import com.rinx.artRINXapp.feature.upload.domain.UploadManager
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * [LocalDataCleaner] is the single chokepoint for wiping user data. Verifies the choreography:
 *  - [LocalDataCleaner.clearCaches] drops in-memory caches + transient upload state, KEEPS the session.
 *  - [LocalDataCleaner.clearAll] additionally clears tokens, profile draft, the offline queue and FCM.
 * All collaborators are relaxed mocks (no real I/O); we assert which ones are invoked.
 */
class LocalDataCleanerTest {

    private val session: SessionDataSource = mockk(relaxed = true)
    private val profileDraft: ProfileDraftDataSource = mockk(relaxed = true)
    private val liveMutationQueue: LiveMutationQueue = mockk(relaxed = true)
    private val uploadManager: UploadManager = mockk(relaxed = true)
    private val curationManager: CurationManager = mockk(relaxed = true)
    private val homeRepository: HomeRepository = mockk(relaxed = true)
    private val searchRepository: SearchRepository = mockk(relaxed = true)
    private val profileRepository: ProfileRepository = mockk(relaxed = true)
    private val detailCache: DetailCache = mockk(relaxed = true)
    private val curationPreviewStore: CurationPreviewStore = mockk(relaxed = true)
    private val chatCache: ChatCache = mockk(relaxed = true)
    private val outgoingMessageStore: OutgoingMessageStore = mockk(relaxed = true)
    private val blockedUsersStore: BlockedUsersStore = mockk(relaxed = true)
    private val unreadNotificationsStore: UnreadNotificationsStore = mockk(relaxed = true)
    private val pushTokenManager: PushTokenManager = mockk(relaxed = true)

    private lateinit var cleaner: LocalDataCleaner

    @Before
    fun setUp() {
        cleaner = LocalDataCleaner(
            session, profileDraft, liveMutationQueue, uploadManager, curationManager,
            homeRepository, searchRepository, profileRepository, detailCache, curationPreviewStore,
            chatCache, outgoingMessageStore, blockedUsersStore, unreadNotificationsStore, pushTokenManager,
        )
    }

    @Test
    fun `clearCaches wipes every in-memory cache and transient upload state`() {
        cleaner.clearCaches()

        verify { uploadManager.dismiss() }
        verify { curationManager.dismiss() }
        verify { homeRepository.clearCache() }
        verify { searchRepository.clearCache() }
        verify { profileRepository.clearCache() }
        verify { detailCache.clear() }
        verify { curationPreviewStore.clear() }
        verify { chatCache.clear() }
        verify { outgoingMessageStore.clear() }
        verify { blockedUsersStore.clear() }
        verify { unreadNotificationsStore.reset() }
    }

    @Test
    fun `clearCaches keeps the session and other user-scoped persistence intact`() {
        cleaner.clearCaches()

        coVerify(exactly = 0) { session.clearSession() }
        coVerify(exactly = 0) { profileDraft.clearDraft() }
        coVerify(exactly = 0) { liveMutationQueue.clear() }
        verify(exactly = 0) { pushTokenManager.deleteToken() }
    }

    @Test
    fun `clearAll wipes session, draft, offline queue, FCM token and all caches`() = runTest {
        cleaner.clearAll()

        coVerify { session.clearSession() }
        coVerify { profileDraft.clearDraft() }
        coVerify { liveMutationQueue.clear() }
        verify { pushTokenManager.deleteToken() }
        // clearAll delegates to clearCaches — spot-check a couple of cache wipes happened too.
        verify { detailCache.clear() }
        verify { homeRepository.clearCache() }
    }
}
