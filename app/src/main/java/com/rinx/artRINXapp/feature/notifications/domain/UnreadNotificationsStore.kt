package com.rinx.artRINXapp.feature.notifications.domain

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.network.ChatEvent
import com.rinx.artRINXapp.core.network.ChatWebSocketManager
import com.rinx.artRINXapp.feature.notifications.domain.repository.NotificationsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped unread-notification counter that drives the red badge on the bell tab (handout §3/§13).
 *
 * Seeded by [refresh] at app start, kept live by the WebSocket `notification` channel (re-fetches
 * the authoritative count on each push), and set exactly by the notifications screen as it loads
 * and marks rows read. Cleared on logout via [reset].
 */
@Singleton
class UnreadNotificationsStore @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
    webSocket: ChatWebSocketManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    init {
        scope.launch {
            webSocket.events.collect { event ->
                if (event is ChatEvent.IncomingNotification) refreshInternal()
            }
        }
    }

    /** Set the exact unread count (notifications screen, as it loads / marks rows read). */
    fun set(count: Int) {
        _count.value = count.coerceAtLeast(0)
    }

    /** Re-fetch notifications and recompute the unread count (app start, WS push). */
    fun refresh() {
        scope.launch { refreshInternal() }
    }

    /** Clear on logout / account deletion. */
    fun reset() {
        _count.value = 0
    }

    private suspend fun refreshInternal() {
        when (val res = notificationsRepository.getNotifications()) {
            is ApiResult.Success -> _count.value = res.data.count { !it.isRead }
            is ApiResult.Error -> Unit // keep the last known count on a transient failure
        }
    }
}
