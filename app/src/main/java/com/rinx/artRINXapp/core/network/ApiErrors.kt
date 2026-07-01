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
            // FastAPI/Pydantic validation: a list of field errors. Build friendly, field-aware copy
            // from the first item (loc + type + ctx) instead of dumping the raw "String should have…".
            is List<*> -> detail.filterIsInstance<Map<*, *>>().firstOrNull()?.let { friendlyFieldError(it) }
            else -> null
        }
    }.getOrNull()
}

/**
 * Turns one Pydantic error item into user-facing copy, e.g.
 * `{"type":"string_too_long","loc":["query","username"],"ctx":{"max_length":50}}` →
 * "Username must be at most 50 characters." Falls back to the raw `msg`, then null.
 */
private fun friendlyFieldError(item: Map<*, *>): String? {
    val loc = (item["loc"] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
    // The field is the last loc segment that isn't a request-location container.
    val field = loc.lastOrNull { it !in setOf("body", "query", "path", "header") }
        ?.replace('_', ' ')
        ?.replaceFirstChar { it.uppercase() }
    val label = field ?: "This field"
    val type = item["type"] as? String
    val ctx = item["ctx"] as? Map<*, *>
    val max = (ctx?.get("max_length") as? Number)?.toInt()
    val min = (ctx?.get("min_length") as? Number)?.toInt()
    val msg = (item["msg"] as? String)?.takeIf { it.isNotBlank() }
    return when {
        type == "string_too_long" && max != null -> "$label must be at most $max characters."
        type == "string_too_short" && min != null -> "$label must be at least $min characters."
        type in setOf("missing", "value_error.missing") -> "$label is required."
        field != null && msg != null -> "$label: $msg"
        else -> msg
    }
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
    // Distinguish a request timeout (server slow/cold but device IS online) from a real offline failure,
    // so a SocketTimeoutException isn't mislabeled "No connection".
    is ApiResult.Error.Network ->
        if (cause is java.net.SocketTimeoutException) "The server is taking longer than usual. Please try again."
        else "No connection. Please try again."
    is ApiResult.Error.Server -> fallback
    is ApiResult.Error.Unknown -> fallback
}
