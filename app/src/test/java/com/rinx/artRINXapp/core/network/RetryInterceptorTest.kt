package com.rinx.artRINXapp.core.network

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class RetryInterceptorTest {

    // Online by default; individual tests override for the offline case.
    private val connectivity = mockk<ConnectivityChecker> { every { isOnline() } returns true }
    private val interceptor = RetryInterceptor(connectivity)

    private fun okResponse(req: Request): Response = Response.Builder()
        .request(req)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body("{}".toResponseBody("application/json".toMediaTypeOrNull()))
        .build()

    @Test
    fun `GET retries once after a timeout then succeeds`() {
        val req = Request.Builder().url("https://example.com/x").get().build()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns req
        every { chain.proceed(any()) } throws SocketTimeoutException("timeout") andThen okResponse(req)

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        verify(exactly = 2) { chain.proceed(any()) } // original + 1 retry
    }

    @Test
    fun `GET rethrows after the single timeout retry is exhausted`() {
        val req = Request.Builder().url("https://example.com/x").get().build()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns req
        every { chain.proceed(any()) } throws SocketTimeoutException("timeout")

        assertThrows(SocketTimeoutException::class.java) { interceptor.intercept(chain) }
        verify(exactly = 2) { chain.proceed(any()) } // original + 1 retry, then give up
    }

    @Test
    fun `GET retries UnknownHostException while online (wake-from-sleep DNS window)`() {
        every { connectivity.isOnline() } returns true
        val req = Request.Builder().url("https://example.com/x").get().build()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns req
        every { chain.proceed(any()) } throws UnknownHostException("dns") andThen okResponse(req)

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        verify(atLeast = 2) { chain.proceed(any()) } // original + retry recovered
    }

    @Test
    fun `GET surfaces UnknownHostException immediately when offline`() {
        every { connectivity.isOnline() } returns false
        val req = Request.Builder().url("https://example.com/x").get().build()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns req
        every { chain.proceed(any()) } throws UnknownHostException("dns")

        assertThrows(UnknownHostException::class.java) { interceptor.intercept(chain) }
        verify(exactly = 1) { chain.proceed(any()) } // no retry when genuinely offline
    }

    @Test
    fun `POST is never retried`() {
        val body = "x".toRequestBody("text/plain".toMediaTypeOrNull())
        val req = Request.Builder().url("https://example.com/x").post(body).build()
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns req
        every { chain.proceed(any()) } throws SocketTimeoutException("timeout")

        assertThrows(SocketTimeoutException::class.java) { interceptor.intercept(chain) }
        verify(exactly = 1) { chain.proceed(any()) } // no retry for a non-idempotent write
    }
}
