package com.rinx.artRINXapp.core.network

import com.google.gson.Gson
import retrofit2.Response

private val errorGson = Gson()

/**
 * Build an [ApiResult.Error] from a failed Retrofit response, preferring the server's human-readable
 * message so the UI can show the real reason (e.g. "You can't report a blocked user or their
 * curation.") instead of a generic fallback. Understands both `{"message": "..."}` and FastAPI-style
 * `{"detail": "..."}` / `{"detail": [{"msg": "..."}]}` bodies.
 */
fun Response<*>.toApiError(): ApiResult.Error {
    val msg = parseServerMessage(runCatching { errorBody()?.string() }.getOrNull())
    return when (val code = code()) {
        429 -> ApiResult.Error.RateLimited(msg ?: "You're doing that too often. Please try again shortly.")
        409 -> ApiResult.Error.Conflict(msg ?: "That action conflicts with the current state.")
        404 -> ApiResult.Error.NotFound(msg ?: "Not found.")
        in 400..499 -> ApiResult.Error.Validation(msg ?: "Request failed ($code).")
        in 500..599 -> ApiResult.Error.Server(code)
        else -> ApiResult.Error.Unknown(RuntimeException("HTTP $code"))
    }
}

/** Public access to the server's human-readable message from a raw error body (null if none). */
fun serverMessageOrNull(body: String?): String? = parseServerMessage(body)

private fun parseServerMessage(body: String?): String? {
    if (body.isNullOrBlank()) return null
    return runCatching {
        val obj = errorGson.fromJson(body, Map::class.java) ?: return null
        (obj["message"] as? String)?.takeIf { it.isNotBlank() }?.let { return it }
        when (val detail = obj["detail"]) {
            is String -> detail.takeIf { it.isNotBlank() }
            is List<*> -> (detail.filterIsInstance<Map<*, *>>().firstOrNull()?.get("msg") as? String)?.takeIf { it.isNotBlank() }
            else -> null
        }
    }.getOrNull()
}

/**
 * A user-facing message for an error — the server's message when we have one, else [fallback].
 * Use this in ViewModels instead of hard-coding "Something went wrong" so real reasons surface.
 */
fun ApiResult.Error.userMessage(fallback: String = "Something went wrong. Please try again."): String = when (this) {
    is ApiResult.Error.Validation -> message.ifBlank { fallback }
    is ApiResult.Error.Blocked -> message.ifBlank { fallback }
    is ApiResult.Error.Conflict -> message.ifBlank { fallback }
    is ApiResult.Error.NotFound -> message.ifBlank { fallback }
    is ApiResult.Error.RateLimited -> message.ifBlank { fallback }
    is ApiResult.Error.Network -> "No connection. Please try again."
    is ApiResult.Error.Server -> fallback
    is ApiResult.Error.Unknown -> fallback
}
