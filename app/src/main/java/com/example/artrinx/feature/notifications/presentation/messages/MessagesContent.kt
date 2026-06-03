package com.example.artrinx.feature.notifications.presentation.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.example.artrinx.R
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.notifications.domain.model.ConversationItem

@Composable
fun MessagesContent(
    conversations: List<ConversationItem>,
    messageQuery: String,
    invitationCount: Int,
    onQueryChange: (String) -> Unit,
    onConversationClick: (ConversationItem) -> Unit,
    onNewMessage: () -> Unit,
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
                                Text("Search messages",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            inner()
                        }
                    },
                )
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

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filtered, key = { it.id }) { conv ->
                SwipeableMessageItem(
                    item      = conv,
                    onClick   = { onConversationClick(conv) },
                    onMarkRead = {},   // mark as read
                    onDelete   = {},   // delete conversation
                )
                HorizontalDivider(
                    color    = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
        }

        // ── Invitation count footer ───────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text  = "You have $invitationCount invitations left",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(Spacing.xs))
            Icon(
                painter            = painterResource(R.drawable.ic_help),
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(Spacing.md),
            )
        }
    }
}
