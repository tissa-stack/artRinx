package com.example.artrinx.feature.notifications.presentation.messages

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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.example.artrinx.R
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.notifications.domain.model.ChatGate
import com.example.artrinx.feature.notifications.domain.model.ChatMessage
import com.example.artrinx.feature.notifications.domain.model.SendStatus

private const val EDIT_WINDOW_MS = 15 * 60 * 1000L

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    userId: String,
    onBack: () -> Unit,
    onNavigateToChatMenu: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
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
    var editTarget by remember { mutableStateOf<ChatMessage?>(null) }

    LaunchedEffect(Unit) { viewModel.loadConversation() }
    LaunchedEffect(Unit) {
        viewModel.toasts.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.scrollToItem(state.messages.size - 1)
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
            IconButton(onClick = { onNavigateToChatMenu(userId) }) {
                Icon(Icons.Default.MoreVert, "More", tint = iconColor)
            }
        }

        // ── Messages ──────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
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

        // ── Invitation hint ────────────────────────────────────────────
        val hintSubtitle = when (state.gate) {
            ChatGate.FRESH_INVITE -> "You can send one message until they accept"
            ChatGate.INVITE_RECEIVED -> "Reply to accept the invitation"
            else -> null
        }
        if (hintSubtitle != null) {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector        = Icons.Outlined.MailOutline,
                    contentDescription = null,
                    tint               = iconColor.copy(alpha = 0.6f),
                    modifier           = Modifier.size(Spacing.xl),
                )
                Text(
                    "This is an invitation",
                    style      = MaterialTheme.typography.bodySmall,
                    color      = iconColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    hintSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = iconColor.copy(alpha = 0.5f),
                )
            }
        }

        // ── Input bar ─────────────────────────────────────────────────
        val enabled = state.canSend
        val placeholder = when {
            !enabled                            -> "Messaging disabled"
            state.gate == ChatGate.FRESH_INVITE -> "Send message"
            else                                -> "Write your message"
        }
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .background(chatBg)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier          = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Spacing.lg))
                    .background(inputBg)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value         = state.inputText,
                    onValueChange = viewModel::onInputChange,
                    enabled       = enabled,
                    modifier      = Modifier.weight(1f),
                    textStyle     = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                    cursorBrush   = SolidColor(BrandPrimary),
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
                    MenuRow("Edit message") { editTarget = target; menuTarget = null }
                }
                MenuRow("Delete message") { viewModel.onDeleteMessage(target); menuTarget = null }
                Spacer(Modifier.height(Spacing.md))
            }
        }
    }

    // ── Edit dialog ─────────────────────────────────────────────────────────────
    editTarget?.let { target ->
        var draft by remember(target.id) { mutableStateOf(target.content) }
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("Edit message") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Spacing.sm))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(Spacing.md),
                ) {
                    BasicTextField(
                        value         = draft,
                        onValueChange = { draft = it },
                        modifier      = Modifier.fillMaxWidth(),
                        textStyle     = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground),
                        cursorBrush   = SolidColor(BrandPrimary),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onEditMessage(target, draft); editTarget = null }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null }) { Text("Cancel") }
            },
        )
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
            Box(
                modifier         = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                val avatarUrl = partnerAvatarUrl ?: message.sharedArtistAvatarUrl
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.size(Spacing.lg),
                    )
                }
            }
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
