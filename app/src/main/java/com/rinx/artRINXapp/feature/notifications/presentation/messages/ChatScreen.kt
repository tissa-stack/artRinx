package com.rinx.artRINXapp.feature.notifications.presentation.messages

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.ErrorDark
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.ChatMenuViewModel
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.ConfirmDialog
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.ReportReasonSheet
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.ReportSentSheet
import com.rinx.artRINXapp.feature.home.presentation.components.shimmer.rememberShimmerBrush
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.RinxAvatar
import com.rinx.artRINXapp.feature.profile.presentation.other.components.ConfirmActionDialog
import com.rinx.artRINXapp.feature.notifications.domain.model.ChatGate
import com.rinx.artRINXapp.feature.notifications.domain.model.ChatMessage
import com.rinx.artRINXapp.feature.notifications.domain.model.SendStatus

private const val EDIT_WINDOW_MS = 15 * 60 * 1000L

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    userId: String,
    onBack: () -> Unit,
    onViewProfile: () -> Unit,
    onChatDeleted: () -> Unit,
    onBlocked: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
    menuViewModel: ChatMenuViewModel = hiltViewModel(),
) {
    val state     by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val isDark    = isSystemInDarkTheme()
    val dimens    = LocalDimens.current
    val context   = LocalContext.current

    val chatBg         = if (isDark) Color(0xFF0A0A0A) else MaterialTheme.colorScheme.background
    // Dark theme: received bubbles are a lighter gray, sent bubbles a near-black (matches reference).
    val bubbleReceived = if (isDark) Color(0xFF2E2E2E) else MaterialTheme.colorScheme.surfaceVariant
    val bubbleSent     = if (isDark) Color(0xFF1C1C1C) else Color(0xFFE0E0E0)
    val textColor      = if (isDark) Color.White      else MaterialTheme.colorScheme.onBackground
    val inputBg        = if (isDark) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surfaceVariant
    val hintColor      = if (isDark) Color.White.copy(alpha = 0.4f)
                         else MaterialTheme.colorScheme.onSurfaceVariant
    val iconColor      = if (isDark) Color.White else MaterialTheme.colorScheme.onBackground

    var menuTarget by remember { mutableStateOf<ChatMessage?>(null) }

    // ── Side options menu (anchored dropdown, hosted here — no separate screen) ──
    val menuState by menuViewModel.state.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }
    var showReasonSheet by remember { mutableStateOf(false) }
    var showReportSent by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var blockConfirm by remember { mutableStateOf(false) }
    var unblockConfirm by remember { mutableStateOf(false) }
    var unfollowConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(menuState.actionError) {
        menuState.actionError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            menuViewModel.onActionErrorShown()
        }
    }
    LaunchedEffect(menuState.blockedSuccess) {
        // Blocked → leave the chat for a safe screen (you can no longer message this user).
        if (menuState.blockedSuccess) {
            Toast.makeText(context, "Blocked ${menuState.name}", Toast.LENGTH_SHORT).show()
            blockConfirm = false
            menuViewModel.onBlockedHandled()
            onBlocked()
        }
    }
    LaunchedEffect(menuState.unblockedSuccess) {
        // Unblocked → close the confirm; the menu's full option set returns.
        if (menuState.unblockedSuccess) {
            Toast.makeText(context, "Unblocked ${menuState.name}", Toast.LENGTH_SHORT).show()
            unblockConfirm = false
            menuViewModel.onUnblockedHandled()
            // Refresh the chat so the gate flips back to ACTIVE — footer + menu return to normal.
            viewModel.loadConversation()
        }
    }
    LaunchedEffect(menuState.unfollowedSuccess) {
        if (menuState.unfollowedSuccess) {
            Toast.makeText(context, "Unfollowed ${menuState.name}", Toast.LENGTH_SHORT).show()
            unfollowConfirm = false
            menuViewModel.onUnfollowedHandled()
        }
    }

    if (blockConfirm) {
        ConfirmActionDialog(
            title = "Are you sure want\nto block \"${menuState.name}\"?",
            confirmLabel = "Block",
            confirmColor = com.rinx.artRINXapp.core.theme.DangerRed,
            iconRes = R.drawable.ic_block,
            isLoading = menuState.isActioning,
            onConfirm = { menuViewModel.blockUser() },
            onDismiss = { blockConfirm = false },
        )
    }
    if (unblockConfirm) {
        ConfirmActionDialog(
            title = "Are you sure want\nto unblock \"${menuState.name}\"?",
            confirmLabel = "Unblock",
            iconRes = R.drawable.ic_block,
            isLoading = menuState.isActioning,
            onConfirm = { menuViewModel.unblockUser() },
            onDismiss = { unblockConfirm = false },
        )
    }
    if (unfollowConfirm) {
        ConfirmActionDialog(
            title = "Are you sure want\nto unfollow \"${menuState.name}\"?",
            confirmLabel = "Unfollow",
            iconRes = R.drawable.ic_navigation_profile,
            isLoading = menuState.isActioning,
            onConfirm = { menuViewModel.unfollowUser() },
            onDismiss = { unfollowConfirm = false },
        )
    }
    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete messages?",
            message = "This will delete all messages with ${menuState.name}. " +
                "They'll stay in your inbox, but the conversation can't be recovered.",
            confirmLabel = "Delete",
            onConfirm = {
                showDeleteConfirm = false
                menuViewModel.deleteChat()
                onChatDeleted()
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }
    if (showReasonSheet) {
        ReportReasonSheet(
            onDismiss = { showReasonSheet = false },
            onSubmit = {
                menuViewModel.reportUser()
                showReasonSheet = false
                showReportSent = true
            },
        )
    }
    if (showReportSent) {
        ReportSentSheet(
            userName = menuState.name,
            onDismiss = { showReportSent = false },
            onBlock = { showReportSent = false; blockConfirm = true },
            onUnfollow = { showReportSent = false; unfollowConfirm = true },
        )
    }

    LaunchedEffect(Unit) { viewModel.loadConversation() }
    LaunchedEffect(Unit) {
        viewModel.toasts.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    // Auto-scroll to the newest message only when a message is APPENDED (last id changes) — not
    // when older messages are prepended by pagination.
    val lastMessageId = state.messages.lastOrNull()?.id
    LaunchedEffect(lastMessageId) {
        if (state.messages.isNotEmpty()) listState.scrollToItem(state.messages.size - 1)
    }
    // Scroll-to-top → load the next older page.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { if (it == 0) viewModel.loadEarlier() }
    }
    // Mark received messages read as they enter the viewport (handout §on view appear).
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }.toSet() }
            .collect { viewModel.onMessagesVisible(it) }
    }

    // Index of the last message the current user sent — used to show "Invite sent!".
    val lastSentIndex = state.messages.indexOfLast { it.isSent }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(chatBg)
            .statusBarsPadding()
            .imePadding(),
    ) {
        // ── Header ────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.xs, end = Spacing.md,
                         top = Spacing.xs, bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), "Back", tint = iconColor)
            }
            Text(
                text       = if (state.partnerName.isNotBlank())
                                 "${state.partnerName}, ${state.partnerRole}" else "Chat",
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color      = iconColor,
                modifier   = Modifier.weight(1f),
                textAlign  = TextAlign.Center,
            )
            // Hidden while the chat genuinely couldn't load — its actions would only error out.
            // (A blocked chat resolves with error=false, so its Unblock/Delete menu still shows.)
            if (!state.error) Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, "More", tint = iconColor)
                }
                // Side-anchored, wrap-content dropdown (replaces the old full-screen menu).
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    when (state.gate) {
                        // I blocked them → only Unblock + Delete; their profile is inaccessible.
                        ChatGate.BLOCKED_BY_ME -> {
                            DropdownMenuItem(
                                text = { Text("Unblock profile") },
                                onClick = { menuExpanded = false; unblockConfirm = true },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete messages") },
                                onClick = { menuExpanded = false; showDeleteConfirm = true },
                            )
                        }
                        // They blocked me → their profile is inaccessible (would 500), so no
                        // "View profile"; I can still delete, report, or block them back.
                        ChatGate.BLOCKED_BY_THEM -> {
                            DropdownMenuItem(
                                text = { Text("Delete messages") },
                                onClick = { menuExpanded = false; showDeleteConfirm = true },
                            )
                            DropdownMenuItem(
                                text = { Text("Report profile") },
                                onClick = { menuExpanded = false; showReasonSheet = true },
                            )
                            DropdownMenuItem(
                                text = { Text("Block profile") },
                                onClick = { menuExpanded = false; blockConfirm = true },
                            )
                        }
                        else -> {
                            DropdownMenuItem(
                                text = { Text("View profile") },
                                onClick = { menuExpanded = false; onViewProfile() },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete messages") },
                                onClick = { menuExpanded = false; showDeleteConfirm = true },
                            )
                            DropdownMenuItem(
                                text = { Text("Report profile") },
                                onClick = { menuExpanded = false; showReasonSheet = true },
                            )
                            DropdownMenuItem(
                                text = { Text("Block profile") },
                                onClick = { menuExpanded = false; blockConfirm = true },
                            )
                        }
                    }
                }
            }
        }

        // ── Messages (pull down to refresh) ───────────────────────────
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
        when {
            state.isLoading -> CircularProgressIndicator(
                color    = BrandPrimary,
                modifier = Modifier.align(Alignment.Center).size(Spacing.xxxl),
            )
            state.error -> Column(
                modifier            = Modifier.align(Alignment.Center).padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Couldn't load this chat.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Retry",
                    style    = MaterialTheme.typography.labelLarge,
                    color    = BrandPrimary,
                    modifier = Modifier.clickable { viewModel.loadConversation() },
                )
            }
            else -> LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            state          = listState,
            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.md),
        ) {
            if (state.isLoadingEarlier) {
                item(key = "load_earlier") {
                    Box(Modifier.fillMaxWidth().padding(Spacing.md), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandPrimary, modifier = Modifier.size(Spacing.xl))
                    }
                }
            }
            itemsIndexed(state.messages, key = { _, m -> m.id }) { index, msg ->
                Text(
                    text      = msg.timestamp,
                    style     = MaterialTheme.typography.labelSmall,
                    color     = textColor.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.sm),
                )
                ChatBubble(
                    message        = msg,
                    partnerAvatarUrl = state.partnerAvatarUrl,
                    bubbleReceived = bubbleReceived,
                    bubbleSent     = bubbleSent,
                    textColor      = textColor,
                    maxWidth       = dimens.chatBubbleMaxWidth,
                    avatarSize     = dimens.chatAvatarSize,
                    showInviteSent = msg.isSent && index == lastSentIndex &&
                                     state.gate == ChatGate.INVITE_SENT_WAITING,
                    onLongPress    = {
                        if (msg.isSent && !msg.isDeleted && msg.sendStatus == SendStatus.SENT) menuTarget = msg
                    },
                    onRetry        = { if (msg.sendStatus == SendStatus.FAILED) viewModel.retryMessage(msg) },
                )
                Spacer(Modifier.height(Spacing.sm))
            }
        }
        } // when
        } // Box(weight)

        // ── Compose-gate info banner (handout 5-state tree) ─────────────
        ChatGateBanner(
            gate             = state.gate,
            partnerName      = state.partnerName.ifBlank { "this user" },
            remainingInvites = state.remainingInvites,
            iconColor        = iconColor,
        )

        // ── Input bar ─────────────────────────────────────────────────
        // While the conversation/gate is still loading, show a shimmer placeholder and keep texting
        // disabled — we don't yet know whether sending is allowed.
        if (state.isLoading) {
            ChatInputShimmer(bg = chatBg)
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(chatBg)
                    .navigationBarsPadding(),
            )
            return@Column
        }
        val editing = state.editingMessageId != null
        // A genuine load error leaves no valid relationship data → keep the field disabled.
        val enabled = !state.error && (editing || state.canSend)
        val placeholder = when {
            editing  -> "Edit your message"
            !enabled -> "Messaging disabled"
            else     -> "Write your message"
        }
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .background(chatBg)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Edit mode: an X to the LEFT cancels the edit and clears the field.
            if (editing) {
                Icon(
                    imageVector        = Icons.Default.Close,
                    contentDescription = "Cancel edit",
                    tint               = iconColor,
                    modifier           = Modifier
                        .size(Spacing.xxl)
                        .clickable { viewModel.cancelEdit() },
                )
                Spacer(Modifier.width(Spacing.sm))
            }
            Row(
                modifier          = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Spacing.lg))
                    .background(inputBg)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
                verticalAlignment = Alignment.Bottom,
            ) {
                BasicTextField(
                    value         = state.inputText,
                    onValueChange = viewModel::onInputChange,
                    enabled       = enabled,
                    modifier      = Modifier.weight(1f),
                    textStyle     = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                    cursorBrush   = SolidColor(BrandPrimary),
                    // Grow up to 5 lines, then hold height and scroll the text inside the box.
                    maxLines      = 5,
                    decorationBox = { inner ->
                        Box {
                            if (state.inputText.isEmpty()) {
                                Text(
                                    placeholder,
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color      = hintColor,
                                )
                            }
                            inner()
                        }
                    },
                )
                Spacer(Modifier.width(Spacing.md))
                val canTapSend = enabled && state.inputText.isNotBlank()
                if (editing) {
                    Icon(
                        imageVector        = Icons.Default.Check,
                        contentDescription = "Save edit",
                        tint               = if (canTapSend) iconColor else iconColor.copy(alpha = 0.3f),
                        modifier           = Modifier
                            .size(Spacing.xxl)
                            .clickable(enabled = canTapSend) { viewModel.onSend() },
                    )
                } else {
                    Icon(
                        painter            = painterResource(R.drawable.ic_send),
                        contentDescription = "Send",
                        tint               = if (enabled) iconColor else iconColor.copy(alpha = 0.3f),
                        modifier           = Modifier
                            .size(Spacing.xxl)
                            .clickable(enabled = canTapSend) { viewModel.onSend() },
                    )
                }
            }
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .background(chatBg)
                .navigationBarsPadding()
        )
    }

    // ── Long-press menu (own messages) ──────────────────────────────────────────
    menuTarget?.let { target ->
        val canEdit = System.currentTimeMillis() - target.createdAtEpochMs in 0 until EDIT_WINDOW_MS
        ModalBottomSheet(
            onDismissRequest = { menuTarget = null },
            sheetState       = rememberModalBottomSheetState(),
            containerColor   = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                if (canEdit) {
                    // Inline edit: pre-fills the compose field with X / ✓ controls (handout §Edit mode).
                    MenuRow("Edit message") { viewModel.beginEdit(target); menuTarget = null }
                }
                MenuRow("Delete message") { viewModel.onDeleteMessage(target); menuTarget = null }
                Spacer(Modifier.height(Spacing.md))
            }
        }
    }
}

/** One shimmer bar standing in for the whole compose row while loading (texting disabled). */
@Composable
private fun ChatInputShimmer(bg: Color) {
    val d = LocalDimens.current
    val shimmer = rememberShimmerBrush()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(d.authButtonHeight)
                .clip(RoundedCornerShape(Spacing.lg))
                .background(shimmer),
        )
    }
}

/**
 * The info banner above the compose field, one per [ChatGate] state (handout 5-state tree).
 * ACTIVE renders nothing. FRESH_INVITE is a full invite card with the monthly-quota footnote;
 * the rest are a centered icon + title + body.
 */
@Composable
private fun ChatGateBanner(
    gate: ChatGate,
    partnerName: String,
    remainingInvites: Int?,
    iconColor: Color,
) {
    when (gate) {
        ChatGate.ACTIVE -> Unit

        ChatGate.FRESH_INVITE -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                .clip(RoundedCornerShape(Spacing.md))
                .background(iconColor.copy(alpha = 0.06f))
                .padding(Spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.MailOutline,
                    contentDescription = null,
                    tint = iconColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(Spacing.xl),
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    "Invite $partnerName to chat",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = iconColor.copy(alpha = 0.9f),
                )
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(
                "This message will act as your invitation to chat for the first time with this " +
                    "profile. You can only send one message in this invite until they accept.",
                style = MaterialTheme.typography.labelSmall,
                color = iconColor.copy(alpha = 0.6f),
            )
            if (remainingInvites != null) {
                Spacer(Modifier.height(Spacing.sm))
                if (remainingInvites <= 0) {
                    // Cap reached → existing chats stay open, but no new invites this month.
                    Text(
                        "You've reached your monthly limit for new chats. " +
                            "Unlimited messages within your active chats.",
                        style = MaterialTheme.typography.labelSmall,
                        color = ErrorDark,
                    )
                } else {
                    Text(
                        "You have $remainingInvites new chats this month",
                        style = MaterialTheme.typography.labelSmall,
                        color = iconColor.copy(alpha = 0.5f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        else -> {
            val title: String
            val body: String
            when (gate) {
                ChatGate.INVITE_RECEIVED -> {
                    title = "This is an invitation"; body = "Reply to accept the invitation"
                }
                ChatGate.INVITE_SENT_WAITING -> {
                    title = "Invitation sent"
                    body = "You can send another message when $partnerName responds."
                }
                ChatGate.BLOCKED_BY_ME -> {
                    title = "You blocked this user"; body = "Unblock to send messages."
                }
                ChatGate.BLOCKED_BY_THEM -> {
                    title = "Messaging unavailable"
                    body = "You can't send messages to this user."
                }
                else -> return
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Outlined.MailOutline,
                    contentDescription = null,
                    tint = iconColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(Spacing.xl),
                )
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = iconColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    body,
                    style = MaterialTheme.typography.labelSmall,
                    color = iconColor.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    Text(
        text     = label,
        style    = MaterialTheme.typography.bodyMedium,
        color    = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
    )
}

// ── Chat bubble ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubble(
    message: ChatMessage,
    partnerAvatarUrl: String?,
    bubbleReceived: Color,
    bubbleSent: Color,
    textColor: Color,
    maxWidth: Dp,
    avatarSize: Dp,
    showInviteSent: Boolean,
    onLongPress: () -> Unit,
    onRetry: () -> Unit,
) {
    if (message.isSent) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .widthIn(max = maxWidth)
                        .clip(RoundedCornerShape(
                            topStart    = Spacing.lg,
                            topEnd      = Spacing.lg,
                            bottomEnd   = 0.dp,
                            bottomStart = Spacing.lg,
                        ))
                        .background(bubbleSent)
                        .combinedClickable(
                            onClick     = { if (message.sendStatus == SendStatus.FAILED) onRetry() },
                            onLongClick = onLongPress,
                        )
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                ) {
                    BubbleContent(message, textColor)
                }
                Icon(
                    painter            = painterResource(R.drawable.ic_message_send),
                    contentDescription = null,
                    tint               = bubbleSent,
                    modifier           = Modifier.size(width = Spacing.lg, height = Spacing.md),
                )
                when {
                    message.sendStatus == SendStatus.SENDING -> StatusLabel("Sending…", textColor)
                    message.sendStatus == SendStatus.FAILED  -> StatusLabel("Failed — tap to retry", BrandPrimary)
                    showInviteSent                           -> StatusLabel("Invite sent!", textColor)
                    message.isRead                           -> StatusLabel("Read", textColor)
                }
            }
        }
    } else {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            RinxAvatar(
                url                = partnerAvatarUrl ?: message.sharedArtistAvatarUrl,
                contentDescription = null,
                size               = avatarSize,
            )
            Spacer(Modifier.width(Spacing.xs))
            Column(horizontalAlignment = Alignment.Start) {
                Box(
                    modifier = Modifier
                        .widthIn(max = maxWidth)
                        .clip(RoundedCornerShape(
                            topStart    = Spacing.lg,
                            topEnd      = Spacing.lg,
                            bottomEnd   = Spacing.lg,
                            bottomStart = 0.dp,
                        ))
                        .background(bubbleReceived)
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                ) {
                    BubbleContent(message, textColor)
                }
                Icon(
                    painter            = painterResource(R.drawable.ic_message_received),
                    contentDescription = null,
                    tint               = bubbleReceived,
                    modifier           = Modifier.size(width = Spacing.lg, height = Spacing.md),
                )
            }
        }
    }
}

@Composable
private fun BubbleContent(message: ChatMessage, textColor: Color) {
    Column {
        // Shared-artwork card (share-an-artwork message).
        if (!message.artworkImageUrl.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(Spacing.sm))
                    .background(textColor.copy(alpha = 0.06f))
                    .padding(Spacing.xs),
            ) {
                AsyncImage(
                    model = message.artworkImageUrl,
                    contentDescription = message.artworkTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(Spacing.giant)
                        .clip(RoundedCornerShape(Spacing.xs)),
                )
                Spacer(Modifier.width(Spacing.sm))
                Column {
                    Text(
                        text = message.artworkTitle?.let { "\"$it\"" } ?: "Artwork",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                    )
                    if (!message.sharedArtistName.isNullOrBlank()) {
                        Text(
                            text = "by ${message.sharedArtistName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.7f),
                        )
                    }
                }
            }
            if (message.content.isNotBlank() && !message.isDeleted) Spacer(Modifier.height(Spacing.xs))
        }

        when {
            message.isDeleted -> Text(
                "Message deleted",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor.copy(alpha = 0.5f),
                fontStyle = FontStyle.Italic,
            )
            else -> Text(
                message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
            )
        }
        if (message.isEdited && !message.isDeleted) {
            Text(
                "edited",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.45f),
            )
        }
    }
}

@Composable
private fun StatusLabel(text: String, color: Color) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.labelSmall,
        color    = color.copy(alpha = 0.6f),
        modifier = Modifier.padding(top = Spacing.xs),
    )
}
