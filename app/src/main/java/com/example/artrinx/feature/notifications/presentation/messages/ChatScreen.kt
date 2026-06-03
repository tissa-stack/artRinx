package com.example.artrinx.feature.notifications.presentation.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.artrinx.R
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.notifications.domain.model.ChatMessage
import com.example.artrinx.feature.notifications.domain.model.ConversationState

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

    val chatBg         = if (isDark) Color(0xFF0A0A0A) else MaterialTheme.colorScheme.background
    val bubbleReceived = if (isDark) Color(0xFF2C2C2C) else MaterialTheme.colorScheme.surfaceVariant
    val bubbleSent     = if (isDark) Color(0xFF3D3D3D) else Color(0xFFE0E0E0)
    val textColor      = if (isDark) Color.White      else MaterialTheme.colorScheme.onBackground
    val inputBg        = if (isDark) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.surfaceVariant
    val hintColor      = if (isDark) Color.White.copy(alpha = 0.4f)
                         else MaterialTheme.colorScheme.onSurfaceVariant
    val iconColor      = if (isDark) Color.White else MaterialTheme.colorScheme.onBackground

    LaunchedEffect(userId) { viewModel.loadConversation(userId) }
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.scrollToItem(state.messages.size - 1)
    }

    val conv = state.conversation

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
                text       = if (conv != null) "${conv.userName}, ${conv.userRole}" else "Chat",
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
        LazyColumn(
            modifier       = Modifier.weight(1f),
            state          = listState,
            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.md),
        ) {
            items(state.messages, key = { it.id }) { msg ->
                // Centered timestamp above every message
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
                    bubbleReceived = bubbleReceived,
                    bubbleSent     = bubbleSent,
                    textColor      = textColor,
                    maxWidth       = dimens.chatBubbleMaxWidth,
                    avatarSize     = dimens.chatAvatarSize,
                )
                Spacer(Modifier.height(Spacing.sm))
            }
        }

        // ── Invitation hint ────────────────────────────────────────────
        if (conv?.state == ConversationState.INVITATION_PENDING) {
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
                    "Reply to accept the invitation",
                    style = MaterialTheme.typography.labelSmall,
                    color = iconColor.copy(alpha = 0.5f),
                )
            }
        }

        // ── Input bar ─────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .background(inputBg)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value         = state.inputText,
                onValueChange = viewModel::onInputChange,
                modifier      = Modifier.weight(1f),
                textStyle     = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                cursorBrush   = SolidColor(BrandPrimary),
                decorationBox = { inner ->
                    Box {
                        if (state.inputText.isEmpty()) {
                            val hint = if (conv?.state == ConversationState.INVITATION_PENDING)
                                "Send message" else "Write your message"
                            Text(hint, style = MaterialTheme.typography.bodyMedium, color = hintColor)
                        }
                        inner()
                    }
                },
            )
            Spacer(Modifier.width(Spacing.sm))
            Icon(
                painter            = painterResource(R.drawable.ic_send),
                contentDescription = "Send",
                tint               = if (state.inputText.isNotEmpty()) BrandPrimary
                                     else iconColor.copy(alpha = 0.3f),
                modifier           = Modifier
                    .size(Spacing.xxl)
                    .clickable(enabled = state.inputText.isNotEmpty()) { viewModel.onSend() },
            )
        }
        // Space below the input bar showing the chat background (nav bar height).
        // With adjustNothing + imePadding, navBar insets → 0 when keyboard is visible,
        // so this spacer vanishes and the input bar sits flush against the keyboard.
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .background(chatBg)
                .navigationBarsPadding()
        )
    }
}

// ── Chat bubble ───────────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(
    message: ChatMessage,
    bubbleReceived: Color,
    bubbleSent: Color,
    textColor: Color,
    maxWidth: Dp,
    avatarSize: Dp,
) {
    if (message.isSent) {
        // Sent: right-aligned bubble + tail at bottom-right
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
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                ) {
                    Text(
                        message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                    )
                }
                // Right-angle at top-right (16,0) connects flush to bubble's sharp bottom-right corner
                Icon(
                    painter            = painterResource(R.drawable.ic_message_send),
                    contentDescription = null,
                    tint               = bubbleSent,
                    modifier           = Modifier.size(width = Spacing.lg, height = Spacing.md),
                )
            }
        }
    } else {
        // Received: avatar at bottom-left + bubble + tail at bottom-left
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
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(Spacing.lg),
                )
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
                    Text(
                        message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                    )
                }
                // Right-angle at top-left (0,0) connects flush to bubble's sharp bottom-left corner
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