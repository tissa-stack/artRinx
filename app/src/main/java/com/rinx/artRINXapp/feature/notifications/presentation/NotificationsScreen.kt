package com.rinx.artRINXapp.feature.notifications.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.events.presentation.EventDetailPopup
import com.rinx.artRINXapp.feature.home.presentation.components.BottomNavBar
import com.rinx.artRINXapp.feature.notifications.domain.model.ConversationItem
import com.rinx.artRINXapp.feature.notifications.domain.model.NotifTab
import com.rinx.artRINXapp.feature.notifications.presentation.messages.MessagesContent
import com.rinx.artRINXapp.feature.notifications.presentation.notifications.NotificationsContent

@Composable
fun NotificationsScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToChat: (ConversationItem) -> Unit = {},
    onNavigateToNewMessage: () -> Unit = {},
    onOpenUserProfile: (Long) -> Unit = {},
    onOpenArtDetail: (Long) -> Unit = {},
    onOpenCurationDetail: (Long) -> Unit = {},
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state   by viewModel.state.collectAsState()
    val d       = LocalDimens.current
    val context = LocalContext.current

    // One-shot toast for a failed/missing event fetch (e.g. legacy 404 events).
    LaunchedEffect(state.eventError) {
        state.eventError?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.consumeEventError()
        }
    }

    // Re-sync inbox previews / unread badges whenever the screen becomes visible (return from a
    // chat, tab switch back, app foreground). WS events alone don't cover the user's own read
    // action, so without this the list stays stale until a manual pull-to-refresh.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshConversations()
                viewModel.loadInvitationCount()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                activeRoute = "notifications",
                onNavigate  = { route ->
                    when (route) {
                        "home"    -> onNavigateToHome()
                        "search"  -> onNavigateToSearch()
                        "create"  -> onNavigateToCreate()
                        "profile" -> onNavigateToProfile()
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(bottom = innerPadding.calculateBottomPadding())
                .statusBarsPadding(),
        ) {
            androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
                // ── Tab bar: Notifications | Messages ─────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                        .height(d.tabPillHeight)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(Spacing.xs),
                ) {
                    NotifTab.entries.forEach { tab ->
                        val isActive = tab == state.activeTab
                        val bgColor by animateColorAsState(
                            targetValue  = if (isActive) BrandPrimary else Color.Transparent,
                            animationSpec = tween(220),
                            label        = "notif-tab-bg",
                        )
                        val textColor by animateColorAsState(
                            targetValue  = if (isActive) Color.White
                                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            animationSpec = tween(220),
                            label        = "notif-tab-text",
                        )
                        Box(
                            modifier         = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(bgColor)
                                .clickable { viewModel.onTabSelected(tab) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text  = tab.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = textColor,
                            )
                        }
                    }
                }

                // ── Tab content ───────────────────────────────────────────
                when (state.activeTab) {
                    NotifTab.NOTIFICATIONS -> NotificationsContent(
                        notifications = state.notifications,
                        isLoading     = state.isLoadingNotifications,
                        onDelete      = viewModel::onDeleteNotification,
                        onMarkRead    = viewModel::onMarkNotificationRead,
                        onOpenEvent    = viewModel::onOpenEvent,
                        onOpenProfile  = onOpenUserProfile,
                        onOpenArt      = onOpenArtDetail,
                        onOpenCuration = onOpenCurationDetail,
                        error          = state.notificationsError,
                        onRetry       = viewModel::retryNotifications,
                        modifier      = Modifier.weight(1f),
                    )
                    NotifTab.MESSAGES -> MessagesContent(
                        conversations      = state.conversations,
                        messageQuery       = state.messageQuery,
                        invitationCount    = state.invitationCount,
                        isRefreshing       = state.isRefreshing,
                        onRefresh          = { viewModel.refreshConversations(isUserRefresh = true) },
                        onQueryChange      = viewModel::onMessageQueryChange,
                        onConversationClick = { conv -> onNavigateToChat(conv) },
                        onNewMessage       = onNavigateToNewMessage,
                        onMarkRead         = viewModel::onMarkConversationRead,
                        onDelete           = viewModel::onDeleteConversation,
                        error              = state.conversationsError,
                        onRetry            = { viewModel.refreshConversations(isUserRefresh = true) },
                        modifier           = Modifier.weight(1f),
                    )
                }
            }

            // Event popup (Dialog → renders in its own window, above the bottom nav).
            if (state.isEventPopupOpen) {
                EventDetailPopup(
                    event = state.eventPopup,
                    isLoading = state.isEventLoading,
                    onDismiss = viewModel::dismissEventPopup,
                    onLearnMore = { /* Destination TBD by product/backend (handout §22). */ },
                )
            }
        }
    }
}

private val NotifTab.label: String
    get() = when (this) {
        NotifTab.NOTIFICATIONS -> "Notifications"
        NotifTab.MESSAGES      -> "Messages"
    }
