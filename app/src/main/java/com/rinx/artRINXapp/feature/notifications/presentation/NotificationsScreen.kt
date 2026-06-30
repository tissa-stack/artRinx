package com.rinx.artRINXapp.feature.notifications.presentation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
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

    // Reliability backstop: also request the notifications runtime permission here. Some users reach
    // the app without passing through Home-after-tour (where it's first requested), so prompt on the
    // Notifications tab too — no-op when already granted or on Android < 13.
    com.rinx.artRINXapp.core.push.NotificationPermissionEffect()

    // One-shot toast for a failed/missing event fetch (e.g. legacy 404 events).
    LaunchedEffect(state.eventError) {
        state.eventError?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.consumeEventError()
        }
    }
    // One-shot toast when a pull-to-refresh fails while content is already on screen.
    LaunchedEffect(state.refreshError) {
        state.refreshError?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.consumeRefreshError()
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
            Column(Modifier.fillMaxSize()) {
                val tabCount = NotifTab.entries.size
                val pagerState = rememberPagerState(initialPage = state.activeTab.ordinal) { tabCount }
                val scope = rememberCoroutineScope()
                val density = LocalDensity.current

                // Pager is the single source of truth: swipe + tab tap drive it; the settled page
                // mirrors back to the VM. No activeTab→pager binding (that two-way loop is what left a
                // tab stuck mid-switch on fast taps).
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.settledPage }.collect { page ->
                        NotifTab.entries.getOrNull(page)?.let { if (it != state.activeTab) viewModel.onTabSelected(it) }
                    }
                }

                // ── Draggable pill tab header (sliding BrandPrimary pill follows the pager) ──
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                        .height(d.tabPillHeight)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(Spacing.xs),
                ) {
                    val cellWidth = maxWidth / tabCount
                    val cellWidthPx = with(density) { cellWidth.toPx() }
                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .fillMaxHeight()
                            .offset {
                                val pos = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                                    .coerceIn(0f, (tabCount - 1).toFloat())
                                IntOffset((pos * cellWidthPx).roundToInt(), 0)
                            }
                            .clip(RoundedCornerShape(50))
                            .background(BrandPrimary),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                    ) {
                        NotifTab.entries.forEach { tab ->
                            val pos = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                                .coerceIn(0f, (tabCount - 1).toFloat())
                            val dist = abs(pos - tab.ordinal).coerceIn(0f, 1f)
                            val textColor = lerp(
                                Color.White,
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                dist,
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { scope.launch { pagerState.animateScrollToPage(tab.ordinal) } },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(tab.label, style = MaterialTheme.typography.labelLarge, color = textColor)
                            }
                        }
                    }
                }

                // ── Swipeable pager: Notifications | Messages ──────────────
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    beyondViewportPageCount = 1,
                ) { page ->
                    when (page) {
                        NotifTab.NOTIFICATIONS.ordinal -> NotificationsContent(
                            notifications = state.notifications,
                            isLoading     = state.isLoadingNotifications,
                            onDelete      = viewModel::onDeleteNotification,
                            onMarkRead    = viewModel::onMarkNotificationRead,
                            onOpenEvent    = viewModel::onOpenEvent,
                            onOpenProfile  = onOpenUserProfile,
                            onOpenArt      = onOpenArtDetail,
                            onOpenCuration = onOpenCurationDetail,
                            previews       = state.previews,
                            onLoadPreview  = viewModel::loadPreview,
                            error          = state.notificationsError,
                            onRetry       = viewModel::retryNotifications,
                            isRefreshing  = state.isRefreshingNotifications,
                            onRefresh     = viewModel::refreshNotifications,
                            modifier      = Modifier.fillMaxSize(),
                        )
                        else -> MessagesContent(
                            conversations      = state.conversations,
                            messageQuery       = state.messageQuery,
                            invitationCount    = state.invitationCount,
                            isLoading          = state.isLoadingConversations,
                            isRefreshing       = state.isRefreshing,
                            onRefresh          = { viewModel.refreshConversations(isUserRefresh = true) },
                            onQueryChange      = viewModel::onMessageQueryChange,
                            onConversationClick = { conv -> onNavigateToChat(conv) },
                            onNewMessage       = onNavigateToNewMessage,
                            onMarkRead         = viewModel::onMarkConversationRead,
                            onDelete           = viewModel::onDeleteConversation,
                            error              = state.conversationsError,
                            onRetry            = { viewModel.refreshConversations(isUserRefresh = true) },
                            modifier           = Modifier.fillMaxSize(),
                        )
                    }
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
