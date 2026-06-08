package com.example.artrinx.core.network

import android.content.Context
import android.content.pm.PackageManager
import com.example.artrinx.core.di.ApplicationScope
import com.example.artrinx.feature.auth.data.local.SessionDataSource
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Single, app-scoped chat WebSocket (API §12). Connects while the app is foregrounded, fans decoded
 * [ChatEvent]s into a bounded [events] flow, and reconnects with backoff + close-code handling.
 *
 * Lifecycle is driven by [onAppForeground]/[onAppBackground] (called from the Application's activity
 * callbacks) — Android suspends WS work in the background anyway (§12.6).
 */
@Singleton
class ChatWebSocketManager @Inject constructor(
    @Named("ws") private val client: OkHttpClient,
    private val session: SessionDataSource,
    private val tokenRefreshCoordinator: TokenRefreshCoordinator,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val gson = Gson()

    private val _connectionState = MutableStateFlow(WsConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    // Bounded, drop-oldest (§12.7) — never leak when a subscriber is paused.
    private val _events = MutableSharedFlow<ChatEvent>(
        replay = 0,
        extraBufferCapacity = 100,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events = _events.asSharedFlow()

    @Volatile private var webSocket: WebSocket? = null
    @Volatile private var foreground = false
    @Volatile private var manualClose = false
    @Volatile private var attempt = 0
    @Volatile private var consecutive4401 = 0
    @Volatile private var lastInboundAt = 0L

    private var reconnectJob: Job? = null
    private var watchdogJob: Job? = null

    // ── Lifecycle entry points ───────────────────────────────────────────────────

    fun onAppForeground() {
        foreground = true
        manualClose = false
        connect()
    }

    fun onAppBackground() {
        foreground = false
        manualClose = true
        teardown()
    }

    // ── Connection ───────────────────────────────────────────────────────────────

    @Synchronized
    private fun connect() {
        if (!foreground || webSocket != null) return
        val token = session.getAccessToken()
        if (token.isNullOrBlank()) {
            // No usable session yet (e.g. cold launch before sign-in completes) — retry softly.
            scheduleReconnect()
            return
        }
        manualClose = false
        _connectionState.value = WsConnectionState.CONNECTING
        lastInboundAt = System.currentTimeMillis()
        val request = Request.Builder()
            .url(WS_URL)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("X-App-Version", appVersion())
            .build()
        webSocket = client.newWebSocket(request, listener)
        startWatchdog()
    }

    @Synchronized
    private fun teardown() {
        reconnectJob?.cancel(); reconnectJob = null
        watchdogJob?.cancel(); watchdogJob = null
        webSocket?.close(NORMAL_CLOSURE, "lifecycle")
        webSocket = null
        _connectionState.value = WsConnectionState.DISCONNECTED
    }

    private fun scheduleReconnect() {
        if (!foreground || manualClose) return
        if (reconnectJob?.isActive == true) return
        val backoffMs = min(MAX_BACKOFF_MS, (BASE_BACKOFF_MS * 2.0.pow(attempt)).toLong())
        val jitter = Random.nextLong(0, JITTER_MS)
        attempt++
        reconnectJob = scope.launch {
            delay(backoffMs + jitter)
            connect()
        }
    }

    private fun forceReconnect() {
        synchronized(this) {
            webSocket?.cancel()
            webSocket = null
            _connectionState.value = WsConnectionState.DISCONNECTED
        }
        scheduleReconnect()
    }

    /** Passive inbound-silence watchdog (§12.5): no frame for 45s ⇒ reconnect. */
    private fun startWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            while (isActive) {
                delay(WATCHDOG_INTERVAL_MS)
                if (!foreground) break
                if (System.currentTimeMillis() - lastInboundAt > SILENCE_LIMIT_MS) {
                    forceReconnect()
                    break
                }
            }
        }
    }

    // ── Socket listener ─────────────────────────────────────────────────────────

    private val listener = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            lastInboundAt = System.currentTimeMillis()
            handleFrame(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(NORMAL_CLOSURE, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            handleDisconnect(code)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            handleDisconnect(response?.code ?: ABNORMAL_CLOSURE)
        }
    }

    private fun handleDisconnect(code: Int) {
        synchronized(this) {
            webSocket = null
            _connectionState.value = WsConnectionState.DISCONNECTED
        }
        if (manualClose || !foreground) return
        when (code) {
            // Auth expired on an established socket → refresh once, reconnect. Second 4401 → give up.
            4401, 1008 -> {
                if (consecutive4401 >= 2) return
                consecutive4401++
                scope.launch {
                    tokenRefreshCoordinator.refresh()
                    scheduleReconnect()
                }
            }
            // Terminal: ambiguous handshake / banned / protocol errors (§12.4).
            4400, 4403, 1002, 1003, 1007, 1009, 1010 -> Unit
            // 1000 / 1001 / 1006 / 1011 + unknown → reconnect with backoff.
            else -> scheduleReconnect()
        }
    }

    // ── Frame decoding (§12.3) ────────────────────────────────────────────────────

    private fun handleFrame(text: String) {
        val obj = try {
            gson.fromJson(text, JsonObject::class.java)
        } catch (_: Exception) {
            return
        } ?: return
        val type = obj.get("type")?.takeIf { !it.isJsonNull }?.asString ?: return
        val data = obj.get("data")?.takeIf { it.isJsonObject }?.asJsonObject

        when (type) {
            "ready" -> {
                attempt = 0
                consecutive4401 = 0
                _connectionState.value = WsConnectionState.CONNECTED
            }
            "ping" -> Unit // app-level heartbeat; lastInboundAt already bumped. Do NOT pong (§12.3).
            "chat" -> data?.let { emit(ChatEvent.NewMessage(it)) }
            "chat_edit" -> data?.let {
                emit(
                    ChatEvent.Edit(
                        messageId = it.string("message_id") ?: return,
                        chatroomId = it.string("chatroom_id"),
                        text = it.string("text"),
                        isEdited = it.bool("is_edited") ?: true,
                        editedAt = it.string("edited_at"),
                    ),
                )
            }
            "chat_delete" -> data?.let {
                emit(
                    ChatEvent.Delete(
                        messageId = it.string("message_id") ?: return,
                        chatroomId = it.string("chatroom_id"),
                    ),
                )
            }
            "chat_read" -> data?.let {
                emit(
                    ChatEvent.Read(
                        messageId = it.string("message_id") ?: return,
                        chatroomId = it.string("chatroom_id"),
                        readerId = it.int("reader_id"),
                    ),
                )
            }
            "notification" -> data?.let { emit(ChatEvent.IncomingNotification(it)) }
            else -> Unit // unknown event types are silently dropped (§12.3).
        }
    }

    private fun emit(event: ChatEvent) {
        _events.tryEmit(event)
    }

    // ── Small JSON helpers ────────────────────────────────────────────────────────

    private fun JsonObject.string(key: String): String? =
        get(key)?.takeIf { !it.isJsonNull }?.asString

    private fun JsonObject.bool(key: String): Boolean? =
        get(key)?.takeIf { !it.isJsonNull }?.asBoolean

    private fun JsonObject.int(key: String): Int? =
        get(key)?.takeIf { !it.isJsonNull }?.asInt

    private fun appVersion(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
    } catch (_: PackageManager.NameNotFoundException) {
        "1.0"
    }

    private companion object {
        const val WS_URL = "wss://apifargate.rinx.com/ws"
        const val NORMAL_CLOSURE = 1000
        const val ABNORMAL_CLOSURE = 1006
        const val BASE_BACKOFF_MS = 1_000L
        const val MAX_BACKOFF_MS = 60_000L
        const val JITTER_MS = 1_000L
        const val WATCHDOG_INTERVAL_MS = 15_000L
        const val SILENCE_LIMIT_MS = 45_000L
    }
}
