package com.rinx.artRINXapp.feature.auth

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.auth.data.repository.AuthRepositoryImpl
import io.mockk.mockk
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/**
 * Pure error-mapping logic in [AuthRepositoryImpl]: server error codes → user-facing messages and
 * HTTP status → typed [ApiResult.Error]. No network — the repo's collaborators are unused by these
 * helpers, so relaxed mocks suffice. Verifies the actual current behavior (incl. fallbacks).
 */
class AuthErrorMappingTest {

    private val repo = AuthRepositoryImpl(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))

    private fun errorBody(code: String) = """{"detail":{"code":"$code"}}"""
    private fun response(code: Int): Response<Unit> = Response.error(code, "".toResponseBody(null))

    // ── codeToMessage ─────────────────────────────────────────────────────────

    @Test
    fun `known codes map to their user-facing message`() {
        assertEquals("The code is invalid or has expired.", repo.codeToMessage("invalid_otp"))
        assertEquals("Your code has expired. Please request a new one.", repo.codeToMessage("expired_otp"))
        assertEquals("Your code has expired. Please request a new one.", repo.codeToMessage("otp_expired"))
        assertEquals("This invite code is not valid.", repo.codeToMessage("invite_invalid"))
        assertEquals("Your account has been disabled. Please contact support.", repo.codeToMessage("account_disabled"))
        assertEquals("Your session has expired. Please sign in again.", repo.codeToMessage("refresh_reused"))
        assertEquals("That contact is already in use by another account.", repo.codeToMessage("email_in_use"))
    }

    @Test
    fun `unknown or null code returns null`() {
        assertNull(repo.codeToMessage("some_brand_new_code"))
        assertNull(repo.codeToMessage(null))
    }

    // ── parseNativeError ──────────────────────────────────────────────────────

    @Test
    fun `parseNativeError maps a known code from a valid envelope`() {
        assertEquals(
            "The code is invalid or has expired.",
            repo.parseNativeError(errorBody("invalid_otp")),
        )
    }

    @Test
    fun `parseNativeError falls back for null, malformed, or unknown-code bodies`() {
        val fallback = "Something went wrong. Please try again."
        assertEquals(fallback, repo.parseNativeError(null))
        assertEquals(fallback, repo.parseNativeError("not json at all"))
        assertEquals(fallback, repo.parseNativeError(errorBody("totally_unknown_code")))
    }

    @Test
    fun `parseNativeError honours a custom fallback`() {
        assertEquals("custom", repo.parseNativeError(null, fallback = "custom"))
    }

    // ── nativeErrorResult: HTTP status → typed error ──────────────────────────

    @Test
    fun `http status maps to the correct ApiResult Error subtype`() {
        assertTrue(repo.nativeErrorResult(400, null, response(400)) is ApiResult.Error.Validation)
        assertTrue(repo.nativeErrorResult(403, null, response(403)) is ApiResult.Error.Blocked)
        assertTrue(repo.nativeErrorResult(404, null, response(404)) is ApiResult.Error.NotFound)
        assertTrue(repo.nativeErrorResult(409, null, response(409)) is ApiResult.Error.Conflict)
        assertTrue(repo.nativeErrorResult(422, null, response(422)) is ApiResult.Error.Validation)
        assertTrue(repo.nativeErrorResult(429, null, response(429)) is ApiResult.Error.RateLimited)
        assertTrue(repo.nativeErrorResult(500, null, response(500)) is ApiResult.Error.Server)
        assertTrue(repo.nativeErrorResult(503, null, response(503)) is ApiResult.Error.Server)
        assertTrue(repo.nativeErrorResult(418, null, response(418)) is ApiResult.Error.Unknown)
    }

    @Test
    fun `error subtype carries the parsed server message`() {
        val blocked = repo.nativeErrorResult(403, errorBody("account_disabled"), response(403))
        assertEquals(
            "Your account has been disabled. Please contact support.",
            (blocked as ApiResult.Error.Blocked).message,
        )
    }

    @Test
    fun `rate limited defaults to a 60 second retry-after when header is absent`() {
        val rl = repo.nativeErrorResult(429, null, response(429)) as ApiResult.Error.RateLimited
        assertEquals(60, rl.retryAfterSeconds)
    }

    @Test
    fun `server error preserves the status code`() {
        val server = repo.nativeErrorResult(502, null, response(502)) as ApiResult.Error.Server
        assertEquals(502, server.code)
    }
}
