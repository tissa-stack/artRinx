package com.rinx.artRINXapp.core.network

import com.rinx.artRINXapp.core.auth.SessionEventBus
import com.rinx.artRINXapp.feature.auth.data.local.SessionDataSource
import com.rinx.artRINXapp.feature.auth.data.remote.dto.OtpVerifyResponse
import com.rinx.artRINXapp.feature.auth.data.remote.dto.UserDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/**
 * Single-flight token refresh (API §1.5): coalescing, the authoritative 401 sign-out, and the
 * "never sign out on a transient network error" guarantee. Collaborators are mocked; a fresh
 * coordinator per test keeps the recency cache clean.
 */
class TokenRefreshCoordinatorTest {

    private lateinit var session: SessionDataSource
    private lateinit var refreshApi: TokenRefreshApi
    private lateinit var bus: SessionEventBus
    private lateinit var coordinator: TokenRefreshCoordinator

    @Before
    fun setUp() {
        session = mockk(relaxed = true)
        refreshApi = mockk()
        bus = mockk(relaxed = true)
        coordinator = TokenRefreshCoordinator(session, refreshApi, bus)
    }

    private fun user() = UserDto(
        id = 1, authUserId = "auth", email = "e@x.com", phone = null, emailVerified = true,
        phoneVerified = false, isAdmin = false, role = "artist", profileExists = true,
        profileCompleted = false, consents = null,
    )

    private fun envelope() = OtpVerifyResponse(
        accessToken = "new-access", refreshToken = "new-refresh",
        accessExpiresIn = 900, refreshExpiresIn = 1_000_000, user = user(),
    )

    @Test
    fun `successful refresh saves the new session and returns true`() = runTest {
        every { session.getRefreshToken() } returns "rt"
        coEvery { refreshApi.refresh(any()) } returns Response.success(envelope())

        assertTrue(coordinator.refresh())
        coVerify(exactly = 1) { session.saveSession(any()) }
        verify(exactly = 0) { bus.signalSessionExpired() }
    }

    @Test
    fun `a burst coalesces onto a single refresh round-trip`() = runTest {
        every { session.getRefreshToken() } returns "rt"
        coEvery { refreshApi.refresh(any()) } returns Response.success(envelope())

        // Two immediate calls fall inside the success recency window → one network call.
        assertTrue(coordinator.refresh())
        assertTrue(coordinator.refresh())
        coVerify(exactly = 1) { refreshApi.refresh(any()) }
    }

    @Test
    fun `missing refresh token returns false without calling the network`() = runTest {
        every { session.getRefreshToken() } returns null

        assertFalse(coordinator.refresh())
        coVerify(exactly = 0) { refreshApi.refresh(any()) }
        coVerify(exactly = 0) { session.clearSession() }
    }

    @Test
    fun `a 401 is the authoritative sign-out - wipes session and signals expiry`() = runTest {
        every { session.getRefreshToken() } returns "rt"
        coEvery { refreshApi.refresh(any()) } returns Response.error(401, "".toResponseBody(null))

        assertFalse(coordinator.refresh())
        coVerify(exactly = 1) { session.clearSession() }
        verify(exactly = 1) { bus.signalSessionExpired() }
    }

    @Test
    fun `a network error never signs the user out`() = runTest {
        every { session.getRefreshToken() } returns "rt"
        coEvery { refreshApi.refresh(any()) } throws IOException("offline")

        assertFalse(coordinator.refresh())
        coVerify(exactly = 0) { session.clearSession() }
        verify(exactly = 0) { bus.signalSessionExpired() }
    }

    @Test
    fun `a non-401 server error returns false and keeps the session`() = runTest {
        every { session.getRefreshToken() } returns "rt"
        coEvery { refreshApi.refresh(any()) } returns Response.error(500, "".toResponseBody(null))

        assertFalse(coordinator.refresh())
        coVerify(exactly = 0) { session.clearSession() }
        verify(exactly = 0) { bus.signalSessionExpired() }
    }
}
