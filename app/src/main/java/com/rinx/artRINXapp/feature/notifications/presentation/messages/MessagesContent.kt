package com.rinx.artRINXapp.feature.notifications.presentation.messages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.feature.home.presentation.components.shimmer.ListShimmer
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.SearchOff
import com.rinx.artRINXapp.feature.notifications.domain.model.ConversationItem
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.ConfirmDialog
import com.rinx.artRINXapp.feature.search.presentation.components.SearchMessageView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesContent(
    conversations: List<ConversationItem>,
    messageQuery: String,
    invitationCount: Int,
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onQueryChange: (String) -> Unit,
    onConversationClick: (ConversationItem) -> Unit,
    onNewMessage: () -> Unit,
    onMarkRead: (ConversationItem) -> Unit = {},
    onDelete: (ConversationItem) -> Unit = {},
    error: String? = null,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // ── Search bar + compose icon ─────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier          = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter            = painterResource(R.drawable.ic_navigation_search),
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(Spacing.xl),
                )
                Spacer(Modifier.width(Spacing.sm))
                BasicTextField(
                    value         = messageQuery,
                    onValueChange = onQueryChange,
                    modifier      = Modifier.weight(1f),
                    textStyle     = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush   = SolidColor(BrandPrimary),
                    singleLine    = true,
                    decorationBox = { inner ->
                        Box {
                            if (messageQuery.isEmpty()) {
                                Text("Search",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            inner()
                        }
                    },
                )
                if (messageQuery.isNotEmpty()) {
                    Spacer(Modifier.width(Spacing.xs))
                    Icon(
                        imageVector        = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier
                            .size(Spacing.lg)
                            .clickable { onQueryChange("") },
                    )
                }
            }
            Spacer(Modifier.width(Spacing.sm))
            Icon(
                painter            = painterResource(R.drawable.ic_new_chat),
                contentDescription = "New message",
                tint               = MaterialTheme.colorScheme.onBackground,
                modifier           = Modifier
                    .size(Spacing.xxl)
                    .clickable { onNewMessage() },
            )
        }

        // ── Conversation list ─────────────────────────────────────────────
        val filtered = if (messageQuery.isEmpty()) conversations
                       else conversations.filter {
                           it.userName.contains(messageQuery, ignoreCase = true)
                       }

        // Long-press selects a row (anchored menu); delete asks for confirmation first.
        var menuTargetId by remember { mutableStateOf<String?>(null) }
        var pendingDelete by remember { mutableStateOf<ConversationItem?>(null) }
        pendingDelete?.let { target ->
            ConfirmDialog(
                title        = "Delete messages?",
                message      = "This will delete all messages with ${target.userName}. " +
                               "They'll stay in your inbox, but the conversation can't be recovered.",
                confirmLabel = "Delete",
                onConfirm    = { onDelete(target); pendingDelete = null },
                onDismiss    = { pendingDelete = null },
            )
        }

        // Network/server failure with no cached conversations to fall back on → show the reason
        // + Retry. A flaky refresh that still has rows keeps the list (handled by the else branch).
        if (conversations.isEmpty() && error != null) {
            SearchMessageView(
                title       = "Couldn't load messages",
                subtitle    = error,
                icon        = Icons.Outlined.CloudOff,
                actionLabel = "Retry",
                onAction    = onRetry,
                modifier    = Modifier.weight(1f),
            )
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh    = onRefresh,
                modifier     = Modifier.weight(1f),
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (filtered.isEmpty()) {
                        // Empty as a full-viewport item so pull-to-refresh still works.
                        item(key = "empty") {
                            when {
                                // First load still in flight — show a spinner, never the "Start a chat"
                                // empty state, which would otherwise flash before conversations arrive.
                                isLoading -> ListShimmer(
                                    modifier = Modifier.fillParentMaxSize(),
                                )
                                messageQuery.isNotEmpty() ->
                                    // Search returned nothing, but the inbox isn't actually empty.
                                    SearchMessageView(
                                        title    = "No results",
                                        subtitle = "No chats match \"$messageQuery\".",
                                        icon     = Icons.Outlined.SearchOff,
                                        modifier = Modifier.fillParentMaxSize(),
                                    )
                                else ->
                                    // Genuinely no conversations yet.
                                    SearchMessageView(
                                        title       = "No messages yet",
                                        subtitle    = "When you start a conversation, it'll show up here.",
                                        icon        = Icons.Outlined.ChatBubbleOutline,
                                        actionLabel = "Start a chat",
                                        onAction    = onNewMessage,
                                        modifier    = Modifier.fillParentMaxSize(),
                                    )
                            }
                        }
                    } else {
                        items(filtered, key = { it.id }) { conv ->
                            MessageRow(
                                item          = conv,
                                menuOpen      = menuTargetId == conv.id,
                                onLongPress   = { menuTargetId = conv.id },
                                onDismissMenu = { menuTargetId = null },
                                onClick       = { onConversationClick(conv) },
                                onMarkRead    = { onMarkRead(conv); menuTargetId = null },
                                onDelete      = { pendingDelete = conv; menuTargetId = null },
                            )
                            HorizontalDivider(
                                color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                modifier = Modifier.padding(horizontal = Spacing.lg),
                            )
                        }
                    }
                }
            }
        }

        // ── New-chats-this-month footer (hidden when none left) ──
        if (invitationCount > 0) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text  = "You have $invitationCount new chats this month",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(Spacing.xs))
                Box {
                    var showInfo by remember { mutableStateOf(false) }
                    Icon(
                        painter            = painterResource(R.drawable.ic_help),
                        contentDescription = "About invitations",
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier
                            .size(Spacing.lg)
                            .clickable { showInfo = true },
                    )
                    if (showInfo) InviteInfoTooltip(onClose = { showInfo = false })
                }
            }
        }
    }
}

/**
 * Dark informational bubble anchored above the help icon, with a downward caret and a "Close" action.
 * The monthly allotment isn't returned by the API, so the copy stays count-agnostic.
 */
@Composable
private fun InviteInfoTooltip(onClose: () -> Unit) {
    val bubbleColor = MaterialTheme.colorScheme.inverseSurface
    val onBubble    = MaterialTheme.colorScheme.inverseOnSurface
    val maxWidth    = LocalDimens.current.chatBubbleMaxWidth

    // Center the bubble horizontally over the help icon (clamped to stay on-screen).
    val positionProvider = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset = IntOffset(
                x = (anchorBounds.left + anchorBounds.width / 2 - popupContentSize.width / 2)
                    .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
                y = anchorBounds.top - popupContentSize.height,
            )
        }
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest      = onClose,
        properties            = PopupProperties(focusable = true),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(horizontal = Spacing.sm),
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .clip(RoundedCornerShape(Spacing.md))
                    .background(bubbleColor)
                    .padding(Spacing.lg),
            ) {
                Text(
                    text  = "You get a fresh allotment of new chats every month, " +
                            "starting on your initial sign-up date. Messages within your active chats are unlimited.",
                    style = MaterialTheme.typography.bodySmall,
                    color = onBubble,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text       = "Close",
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color      = onBubble,
                    modifier   = Modifier
                        .align(Alignment.End)
                        .clickable { onClose() },
                )
            }
            // Downward caret centered beneath the bubble, pointing at the help icon.
            Canvas(modifier = Modifier.size(width = Spacing.md, height = Spacing.sm)) {
                drawPath(
                    path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width / 2f, size.height)
                        close()
                    },
                    color = bubbleColor,
                )
            }
        }
    }
}
