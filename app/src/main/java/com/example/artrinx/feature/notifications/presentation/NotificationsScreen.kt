package com.example.artrinx.feature.notifications.presentation

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.home.presentation.components.BottomNavBar
import com.example.artrinx.feature.notifications.domain.model.ConversationItem
import com.example.artrinx.feature.notifications.domain.model.NotifTab
import com.example.artrinx.feature.notifications.presentation.messages.MessagesContent
import com.example.artrinx.feature.notifications.presentation.notifications.NotificationsContent

@Composable
fun NotificationsScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToCreate: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToChat: (ConversationItem) -> Unit = {},
    onNavigateToNewMessage: () -> Unit = {},
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val d     = LocalDimens.current

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
                        modifier           = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private val NotifTab.label: String
    get() = when (this) {
        NotifTab.NOTIFICATIONS -> "Notifications"
        NotifTab.MESSAGES      -> "Messages"
    }
