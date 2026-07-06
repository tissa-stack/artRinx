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
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.ui.ErrorSnackbarHost
import com.rinx.artRINXapp.feature.home.presentation.components.shimmer.ChatShimmer
import com.rinx.artRINXapp.feature.search.presentation.components.SearchMessageView
import com.rinx.artRINXapp.core.theme.ChatBubbleReceived
import com.rinx.artRINXapp.core.theme.ChatBubbleReceivedText
import com.rinx.artRINXapp.core.theme.ChatBubbleSentText
import com.rinx.artRINXapp.core.theme.DangerRed
import com.rinx.artRINXapp.core.theme.ErrorDark
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.ChatMenuViewModel
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.ConfirmDialog
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.ReportReasonSheet
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.ReportSentSheet
import com.rinx.artRINXapp.feature.home.presentation.components.shimmer.rememberShimmerBrush
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.RinxAvatar
import com.rinx.artRINXapp.feature.profile.presentation.other.components.BlockConfirmDialog
import com.rinx.artRINXapp.feature.profile.presentation.other.components.ConfirmActionDialog
import com.rinx.artRINXapp.feature.notifications.domain.model.ChatGate
import com.rinx.artRINXapp.feature.notifications.domain.model.ChatMessage
import com.rinx.artRINXapp.feature.notifications.domain.model.SendStatus

private const val EDIT_WINDOW_MS = 15 * 60 * 1000L

/** Safety-net resync cadence while a chat is on screen (the WebSocket is the instant path). */
private const val CHAT_RESYNC_INTERVAL_MS = 10_000L

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    userId: String,
    onBack: () -> Unit,
    onViewProfile: () -> Unit,
    onChatDeleted: () -> Unit,
    onBlocked: () -> Unit,
    onOpenArtwork: (Int) -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
    menuViewModel: ChatMenuViewModel = hiltViewModel(),
) {
    val state     by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    // Kept hidden until the list is first scrolled to the newest message, so the user never sees it
    // paint from the top and jump to the bottom on open (the flicker). Flipped in the scroll effect.
    var initialScrollDone by remember { mutableStateOf(false) }
    val isDark    = isSystemInDarkTheme()
    val dimens    = LocalDimens.current
    val context   = LocalContext.current

    val chatBg         = if (isDark) Color(0xFF0A0A0A) else MaterialTheme.colorScheme.background
    // Both themes: sent bubbles are brand blue, received bubbles a light grey (matches reference).
    val bubbleSent     = BrandPrimary
    val bubbleReceived = ChatBubbleReceived
    val sentTextColor      = ChatBubbleSentText
    val receivedTextColor  = ChatBubbleReceivedText
    // Screen chrome (timestamps, status labels, errors) follows the active theme.
    val textColor      = if (isDark) Color.White      else MaterialTheme.colorScheme.onBackground
    val inputBg        = if (isDark) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surfaceVariant
    val hintColor      = if (isDark) Color.White.copy(alpha = 0.4f)
                         else MaterialTheme.colorScheme.onSurfaceVariant
    val iconColor      = if (isDark) Color.White else MaterialTheme.colorScheme.onBackground

    val clipboard = LocalClipboardManager.current
    var menuTarget by remember { mutableStateOf<ChatMessage?>(null) }
    // Tapping a message reveals a details line (date/time + read ticks) beneath it; tap again hides it.
    var detailsTarget by remember { mutableStateOf<String?>(null) }
    // Per-message delete confirmation (distinct from the whole-chat `showDeleteConfirm` below).
    var deleteTarget by remember { mutableStateOf<ChatMessage?>(null) }

    // ── Side options menu (anchored dropdown, hosted here — no separate screen) ──
    val menuState by menuViewModel.state.collectAsState()
    // Menu/dialog display data comes from the SAME load as the chat (ChatViewModel), so the 3-dot
    // menu is in sync — not a separate, racing fetch. menuState now only carries action results.
    val partnerName = state.partnerName.ifBlank { "User" }
    var menuExpanded by remember { mutableStateOf(false) }
    var showReasonSheet by remember { mutableStateOf(false) }
    var showReportSent by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var blockConfirm by remember { mutableStateOf(false) }
    var unblockConfirm by remember { mutableStateOf(false) }
    var unfollowConfirm by remember { mutableStateOf(false) }

    // Report succeeded → show the "Report sent" sheet. This is the ONLY place the success sheet
    // opens, so it can never appear alongside an error (which lands in actionError → red snackbar).
    LaunchedEffect(menuState.reportSuccess) {
        if (menuState.reportSuccess) {
            showReportSent = true
            menuViewModel.onReportSuccessHandled()
        }
    }
    LaunchedEffect(menuState.blockedSuccess) {
        // Blocked → stay in the chat; refresh so the gate flips to BLOCKED_BY_ME (footer banner +
        // "Unblock profile" menu). "View profile" now lands on the blocked-profile panel.
        if (menuState.blockedSuccess) {
            Toast.makeText(context, "Blocked ${partnerName}", Toast.LENGTH_SHORT).show()
            blockConfirm = false
            menuViewModel.onBlockedHandled()
            viewModel.loadConversation()
        }
    }
    LaunchedEffect(menuState.unblockedSuccess) {
        // Unblocked → close the confirm; the menu's full option set returns.
        if (menuState.unblockedSuccess) {
            Toast.makeText(context, "Unblocked ${partnerName}", Toast.LENGTH_SHORT).show()
            unblockConfirm = false
            menuViewModel.onUnblockedHandled()
            // Refresh the chat so the gate flips back to ACTIVE — footer + menu return to normal.
            viewModel.loadConversation()
        }
    }
    LaunchedEffect(menuState.unfollowedSuccess) {
        if (menuState.unfollowedSuccess) {
            Toast.makeText(context, "Unfollowed ${partnerName}", Toast.LENGTH_SHORT).show()
            unfollowConfirm = false
            menuViewModel.onUnfollowedHandled()
        }
    }

    if (blockConfirm) {
        BlockConfirmDialog(
            name = partnerName,
            isLoading = menuState.isActioning,
            onConfirm = { menuViewModel.blockUser() },
            onDismiss = { blockConfirm = false },
        )
    }
    if (unblockConfirm) {
        ConfirmActionDialog(
            title = "Are you sure want\nto unblock \"${partnerName}\"?",
            confirmLabel = "Unblock",
            iconRes = R.drawable.ic_block,
            isLoading = menuState.isActioning,
            onConfirm = { menuViewModel.unblockUser() },
            onDismiss = { unblockConfirm = false },
        )
    }
    if (unfollowConfirm) {
        ConfirmActionDialog(
            title = "Are you sure want\nto unfollow \"${partnerName}\"?",
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
            message = "This will delete all messages with ${partnerName}. " +
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
                // Fire the report and close the reason sheet. The "Report sent" sheet is opened
                // ONLY when the VM reports success (see LaunchedEffect(menuState.reportSuccess));
                // a failure surfaces in the red error snackbar instead — never both.
                menuViewModel.reportUser()
                showReasonSheet = false
            },
        )
    }
    if (showReportSent) {
        ReportSentSheet(
            userName = partnerName,
            isFollowing = state.isFollowing,
            onDismiss = { showReportSent = false },
            onBlock = { showReportSent = false; blockConfirm = true },
            onUnfollow = { showReportSent = false; unfollowConfirm = true },
        )
    }

    LaunchedEffect(Unit) { viewModel.loadConversation() }
    // Guarantee the on-screen user sees new messages without a manual refresh: reconcile with the
    // server on every resume and on a periodic tick while the chat is visible. The live WebSocket is
    // the instant path; this is the safety net for anything it misses. The loop is cancelled when the
    // screen leaves RESUMED, so there's no cost off-screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.resyncFromServer()
            while (true) {
                delay(CHAT_RESYNC_INTERVAL_MS)
                viewModel.resyncFromServer()
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.toasts.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    // Auto-scroll to the newest message only when a message is APPENDED (last id changes) — not
    // when older messages are prepended by pagination.
    val lastMessageId = state.messages.lastOrNull()?.id
    LaunchedEffect(lastMessageId) {
        if (state.messages.isNotEmpty()) {
            listState.scrollToItem(state.messages.size - 1)
            initialScrollDone = true
        }
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

    // Box wrapper hosts the red error snackbar as a bottom overlay (chat has no Scaffold). All chat
    // action errors — report / block / unblock / unfollow — flow through menuState.actionError and
    // surface here in red, matching the profile & art screens (never as a black toast).
    Box(modifier = Modifier.fillMaxSize()) {
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
            // Name + tap-to-open-profile are withheld until the chat data loads the first time:
            // before the gate is confirmed we don't have a real name (would show "Chat") and the
            // profile isn't resolved yet. A cached reopen (isLoading=false) shows it instantly.
            if (!state.isLoading) {
                Text(
                    text       = if (state.partnerName.isNotBlank())
                                     "${state.partnerName}, ${state.partnerRole}" else "Chat",
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color      = iconColor,
                    // Tapping the name opens the partner's profile (blocked or not).
                    modifier   = Modifier
                        .weight(1f)
                        .clickable { onViewProfile() },
                    textAlign  = TextAlign.Center,
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            // Hidden while the chat genuinely couldn't load — its actions would only error out.
            // (A blocked chat resolves with error=false, so its Unblock/Delete menu still shows.)
            // Also hidden during the first-open shimmer (isLoading): the gate isn't confirmed yet, so
            // showing it would flash the wrong "Block profile" for an already-blocked user. A cached
            // reopen has isLoading=false → the menu shows instantly with the last-confirmed gate.
            if (!state.error && !state.isLoading) Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, "More", tint = iconColor)
                }
                // Side-anchored, wrap-content dropdown (replaces the old full-screen menu).
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    // Light theme: a clean white menu (no tonal-elevation grey). Dark keeps its surface.
                    containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                ) {
                    // Always four options (iOS parity), in a FIXED order so rows never reshuffle when
                    // the block state changes: View profile · Delete messages · Report · Block/Unblock.
                    // Only the 4th row's label/action toggles (Block ↔ Unblock) — its position is stable.
                    // "View profile" is valid even when blocked: BLOCKED_BY_ME → the "Profile Blocked"
                    // panel; BLOCKED_BY_THEM → the "This profile isn't available" state.
                    val iBlockedThem = state.gate == ChatGate.BLOCKED_BY_ME
                    DropdownMenuItem(
                        text = { Text("View profile") },
                        leadingIcon = { MenuIcon(R.drawable.ic_eye) },
                        onClick = { menuExpanded = false; onViewProfile() },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete messages") },
                        leadingIcon = { MenuIcon(R.drawable.ic_delete_message, tint = DangerRed) },
                        colors = MenuDefaults.itemColors(textColor = DangerRed),
                        onClick = { menuExpanded = false; showDeleteConfirm = true },
                    )
                    DropdownMenuItem(
                        text = { Text("Report profile") },
                        leadingIcon = { MenuIcon(R.drawable.ic_report) },
                        onClick = { menuExpanded = false; showReasonSheet = true },
                    )
                    DropdownMenuItem(
                        text = { Text(if (iBlockedThem) "Unblock profile" else "Block profile") },
                        leadingIcon = { MenuIcon(R.drawable.ic_block) },
                        onClick = {
                            menuExpanded = false
                            if (iBlockedThem) unblockConfirm = true else blockConfirm = true
                        },
                    )
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
            state.isLoading -> ChatShimmer(modifier = Modifier.fillMaxSize())
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
            // Empty message area: show the friendly empty state ONLY for a totally fresh chat
            // (FRESH_INVITE, server-confirmed). Every other empty case — an invite sent/received whose
            // only message was deleted, or blocked — is already explained by the ChatGateBanner above the
            // input, so we render nothing extra here and let the area stay blank.
            state.messages.isEmpty() && state.gate == ChatGate.FRESH_INVITE && state.gateConfirmed ->
                SearchMessageView(
                    title    = "No messages yet",
                    subtitle = "Send a message to start the conversation.",
                    iconRes  = R.drawable.ic_no_chat,
                    modifier = Modifier.fillMaxSize(),
                )
            else -> LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                // Hide the list for the ~1 frame before it's scrolled to the newest message, so it's
                // never seen painting from the top and jumping. Empty lists aren't gated (nothing to
                // position); revealed at the bottom once positioned.
                .alpha(if (initialScrollDone || state.messages.isEmpty()) 1f else 0f),
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
                // Centered day header, shown once when the calendar day changes (messages ascend by time).
                val prev = state.messages.getOrNull(index - 1)
                val showHeader = prev == null ||
                    ChatDateTime.dayKey(prev.createdAtEpochMs) != ChatDateTime.dayKey(msg.createdAtEpochMs)
                if (showHeader) DayHeader(ChatDateTime.dayHeader(msg.createdAtEpochMs), textColor)
                // Edit is allowed only for your own messages within the 15-min window.
                val canEditMsg = msg.isSent &&
                    System.currentTimeMillis() - msg.createdAtEpochMs in 0 until EDIT_WINDOW_MS
                ChatBubble(
                    message            = msg,
                    partnerAvatarUrl   = state.partnerAvatarUrl,
                    partnerName        = state.partnerName,
                    bubbleReceived     = bubbleReceived,
                    bubbleSent         = bubbleSent,
                    sentTextColor      = sentTextColor,
                    receivedTextColor  = receivedTextColor,
                    statusColor        = textColor,
                    maxWidth           = dimens.chatBubbleMaxWidth,
                    avatarSize         = dimens.chatAvatarSize,
                    showInviteSent     = msg.isSent && index == lastSentIndex &&
                                         state.gate == ChatGate.INVITE_SENT_WAITING &&
                                         state.gateConfirmed,
                    // "Read" shows only under the most recent sent message, not every read one.
                    isLastSentMessage  = msg.isSent && index == lastSentIndex,
                    // Long-press opens the action menu ANCHORED to this bubble (not a bottom sheet).
                    menuOpen           = menuTarget?.id == msg.id,
                    canEdit            = canEditMsg,
                    onLongPress        = {
                        // Own and received messages both get a menu; skip deleted/in-flight bubbles.
                        if (!msg.isDeleted && msg.sendStatus == SendStatus.SENT) menuTarget = msg
                    },
                    onDismissMenu      = { menuTarget = null },
                    onCopy             = {
                        val text = msg.content.ifBlank { msg.artworkTitle.orEmpty() }
                        if (text.isNotBlank()) {
                            clipboard.setText(AnnotatedString(text))
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        }
                        menuTarget = null
                    },
                    onEdit             = { viewModel.beginEdit(msg); menuTarget = null },
                    onDelete           = { deleteTarget = msg; menuTarget = null },
                    onRetry            = { if (msg.sendStatus == SendStatus.FAILED) viewModel.retryMessage(msg) },
                    onOpenArtwork      = onOpenArtwork,
                    showDetails        = detailsTarget == msg.id,
                    onTap              = { detailsTarget = if (detailsTarget == msg.id) null else msg.id },
                )
                Spacer(Modifier.height(Spacing.sm))
            }
            if (state.partnerIsTyping) {
                item(key = "typing_indicator") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        RinxAvatar(
                            url = state.partnerAvatarUrl,
                            contentDescription = null,
                            size = dimens.chatAvatarSize,
                            name = state.partnerName,
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Spacing.lg))
                                .background(bubbleReceived)
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        ) {
                            Text(
                                text = "typing…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = receivedTextColor.copy(alpha = 0.6f),
                                fontStyle = FontStyle.Italic,
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                }
            }
        }
        } // when
        } // Box(weight)

        // ── Compose-gate info banner (handout 5-state tree) ─────────────
        ChatGateBanner(
            gate             = state.gate,
            confirmed        = state.gateConfirmed,
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
        // Auto-focus the input (opening the keyboard) the moment an edit begins, with the text ready.
        val editFocusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        LaunchedEffect(state.editingMessageId) {
            if (state.editingMessageId != null) {
                // Small delay so the field is attached; requestFocus alone doesn't reliably raise
                // the IME, so show() it explicitly to land in edit mode ready to type.
                delay(100)
                runCatching { editFocusRequester.requestFocus() }
                keyboardController?.show()
            }
        }
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
                    modifier      = Modifier.weight(1f).focusRequester(editFocusRequester),
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

        // Red error snackbar for any failed chat action (report/block/unblock/unfollow). Floats
        // above the input bar and keyboard; clears actionError once shown.
        ErrorSnackbarHost(
            message = menuState.actionError,
            onShown = menuViewModel::onActionErrorShown,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding(),
        )
    }

    // The long-press action menu is rendered inline, anchored to each ChatBubble (see ChatBubble).

    // Per-message delete confirmation. Own messages soft-delete for everyone; a received message
    // is removed from your own thread only.
    deleteTarget?.let { target ->
        ConfirmDialog(
            title        = "Delete message?",
            message      = if (target.isSent) {
                "This message will be deleted for everyone and can't be recovered."
            } else {
                "This message will be removed from your chat."
            },
            confirmLabel = "Delete",
            destructive  = true,
            onConfirm    = { viewModel.onDeleteMessage(target); deleteTarget = null },
            onDismiss    = { deleteTarget = null },
        )
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
    confirmed: Boolean,
    partnerName: String,
    remainingInvites: Int?,
    iconColor: Color,
) {
    // Invite-type gates flip as the relationship changes, so a cache-seeded reopen can briefly hold a
    // stale one (e.g. "Invite to chat" / "Invitation sent" for a chat that's long since active). Hold
    // these banners back until the server has confirmed the gate for this open. Blocked states are
    // seeded reliably (incl. the local blocked-users store), so they show immediately.
    val isInviteState = gate == ChatGate.FRESH_INVITE ||
        gate == ChatGate.INVITE_RECEIVED ||
        gate == ChatGate.INVITE_SENT_WAITING
    if (isInviteState && !confirmed) return

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

/** Leading icon for the chat overflow-menu rows (tinted to the current theme). */
@Composable
private fun MenuIcon(
    @androidx.annotation.DrawableRes res: Int,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Icon(
        painter = painterResource(res),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(Spacing.xl),
    )
}

/** Centered day separator ("Today" / "Jun 22") grouping the messages below it. */
@Composable
private fun DayHeader(label: String, textColor: Color) {
    if (label.isBlank()) return
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = textColor.copy(alpha = 0.7f),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        )
    }
}

/** Icon + label row for the long-press message menu. [tint] colors both icon and label. */
@Composable
private fun MessageActionsMenu(
    expanded: Boolean,
    canEdit: Boolean,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        DropdownMenuItem(
            text = { Text("Copy", color = onBg) },
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_copy_message), null, Modifier.size(Spacing.lg), tint = onBg)
            },
            onClick = onCopy,
        )
        if (canEdit) {
            // Inline edit: pre-fills the compose field with X / ✓ controls (handout §Edit mode).
            DropdownMenuItem(
                text = { Text("Edit", color = onBg) },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_edit_message), null, Modifier.size(Spacing.lg), tint = onBg)
                },
                onClick = onEdit,
            )
        }
        DropdownMenuItem(
            text = { Text("Delete", color = DangerRed) },
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_delete_message), null, Modifier.size(Spacing.lg), tint = DangerRed)
            },
            onClick = onDelete,
        )
    }
}

// ── Chat bubble ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatBubble(
    message: ChatMessage,
    partnerAvatarUrl: String?,
    partnerName: String,
    bubbleReceived: Color,
    bubbleSent: Color,
    sentTextColor: Color,
    receivedTextColor: Color,
    statusColor: Color,
    maxWidth: Dp,
    avatarSize: Dp,
    showInviteSent: Boolean,
    isLastSentMessage: Boolean,
    menuOpen: Boolean,
    canEdit: Boolean,
    onLongPress: () -> Unit,
    onDismissMenu: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onOpenArtwork: (Int) -> Unit,
    showDetails: Boolean,
    onTap: () -> Unit,
) {
    // Highlight the whole row while it's long-press-selected (the action menu is open for it).
    val rowHighlight = if (menuOpen) {
        Modifier
            .background(BrandPrimary.copy(alpha = 0.12f))
            .padding(vertical = Spacing.xs)
    } else {
        Modifier
    }
    if (message.isSent) {
        Row(
            modifier              = Modifier.fillMaxWidth().then(rowHighlight),
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
                            onClick     = { if (message.sendStatus == SendStatus.FAILED) onRetry() else onTap() },
                            onLongClick = onLongPress,
                        )
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                ) {
                    BubbleContent(message, sentTextColor, onOpenArtwork)
                    MessageActionsMenu(
                        expanded = menuOpen,
                        canEdit  = canEdit,
                        onCopy   = onCopy,
                        onEdit   = onEdit,
                        onDelete = onDelete,
                        onDismiss = onDismissMenu,
                    )
                }
                Icon(
                    painter            = painterResource(R.drawable.ic_message_send),
                    contentDescription = null,
                    tint               = bubbleSent,
                    modifier           = Modifier.size(width = Spacing.lg, height = Spacing.md),
                )
                when {
                    message.sendStatus == SendStatus.SENDING -> StatusLabel("Sending…", statusColor)
                    message.sendStatus == SendStatus.FAILED  -> StatusLabel("Failed — tap to retry", BrandPrimary)
                    showInviteSent                           -> StatusLabel("Invite sent!", statusColor)
                }
                // Tap-to-reveal: date/time + read receipt (two ticks, blue when read) below the bubble.
                if (showDetails) MessageDetailsLine(message, isSent = true, statusColor = statusColor)
            }
        }
    } else {
        Row(
            modifier          = Modifier.fillMaxWidth().then(rowHighlight),
            verticalAlignment = Alignment.Bottom,
        ) {
            RinxAvatar(
                url                = partnerAvatarUrl ?: message.sharedArtistAvatarUrl,
                contentDescription = null,
                size               = avatarSize,
                name               = partnerName.ifBlank { message.sharedArtistName },
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
                        .combinedClickable(onClick = { onTap() }, onLongClick = onLongPress)
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                ) {
                    BubbleContent(message, receivedTextColor, onOpenArtwork)
                    MessageActionsMenu(
                        expanded = menuOpen,
                        canEdit  = canEdit,
                        onCopy   = onCopy,
                        onEdit   = onEdit,
                        onDelete = onDelete,
                        onDismiss = onDismissMenu,
                    )
                }
                Icon(
                    painter            = painterResource(R.drawable.ic_message_received),
                    contentDescription = null,
                    tint               = bubbleReceived,
                    modifier           = Modifier.size(width = Spacing.lg, height = Spacing.md),
                )
                if (showDetails) MessageDetailsLine(message, isSent = false, statusColor = statusColor)
            }
        }
    }
}

/** Date/time (+ read receipt for sent messages) revealed under a message when it's tapped. */
@Composable
private fun MessageDetailsLine(message: ChatMessage, isSent: Boolean, statusColor: Color) {
    Spacer(Modifier.height(Spacing.xs))
    Row(verticalAlignment = Alignment.CenterVertically) {
        val label = if (message.isEdited && !message.isDeleted) "edited · ${message.timestamp}" else message.timestamp
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor.copy(alpha = 0.7f),
        )
        if (isSent) {
            Spacer(Modifier.width(Spacing.xs))
            Icon(
                imageVector        = Icons.Filled.DoneAll,
                contentDescription = if (message.isRead) "Read" else "Sent",
                tint               = if (message.isRead) BrandPrimary else statusColor.copy(alpha = 0.5f),
                modifier           = Modifier.size(Spacing.md),
            )
        }
    }
}

@Composable
private fun BubbleContent(
    message: ChatMessage,
    contentColor: Color,
    onOpenArtwork: (Int) -> Unit,
) {
    Column {
        // Message text first (matches reference layout: text above the shared item card).
        when {
            message.isDeleted -> Text(
                "Message deleted",
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.6f),
                fontStyle = FontStyle.Italic,
            )
            message.content.isNotBlank() -> Text(
                message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )
        }

        // Shared-artwork card (share-an-artwork message). Tapping opens its detail screen.
        if (!message.isDeleted && !message.artworkImageUrl.isNullOrBlank()) {
            if (message.content.isNotBlank()) Spacer(Modifier.height(Spacing.sm))
            val artworkId = message.sharedArtworkId
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(Spacing.sm))
                    .background(Color.Black.copy(alpha = 0.12f))
                    .then(
                        if (artworkId != null)
                            Modifier.clickable { onOpenArtwork(artworkId) }
                        else Modifier
                    )
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
                        text = message.artworkTitle ?: "Artwork",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                    )
                    if (!message.sharedArtistName.isNullOrBlank()) {
                        Text(
                            text = "By ${message.sharedArtistName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }

        // Time + edited/read are no longer shown inside the bubble — they appear on tap via
        // MessageDetailsLine below the message.
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
