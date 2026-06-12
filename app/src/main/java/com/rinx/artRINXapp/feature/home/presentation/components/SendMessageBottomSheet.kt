package com.rinx.artRINXapp.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.feature.home.domain.model.SendMode
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.InactiveButton
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.RinxAvatar
import androidx.compose.foundation.clickable

private const val MAX_CHARS = 1000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendMessageBottomSheet(
    artistName: String,
    artistRole: String,
    artistAvatarUrl: String?,
    artworkTitle: String,
    artworkImageUrl: String,
    invitationsLeft: Int?,
    isSending: Boolean,
    sent: Boolean,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit,
    mode: SendMode = SendMode.INVITE,
    ready: Boolean = true,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var message by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface,
    ) {
        when {
            // Wait for the conversation state before choosing a layout — avoids an invite→message flash.
            !ready && !sent -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Spacing.giant + Spacing.huge)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = BrandPrimary, modifier = Modifier.size(Spacing.xl))
            }
            sent -> InvitationSent(
                isInvite        = mode == SendMode.INVITE,
                artistName      = artistName,
                invitationsLeft = invitationsLeft,
                onDismiss       = onDismiss,
            )
            !mode.canSend -> CantSend(mode = mode, artistName = artistName)
            else -> InvitationForm(
                mode            = mode,
                artistName      = artistName,
                artistRole      = artistRole,
                artistAvatarUrl = artistAvatarUrl,
                artworkTitle    = artworkTitle,
                artworkImageUrl = artworkImageUrl,
                message         = message,
                invitationsLeft = invitationsLeft,
                isSending       = isSending,
                onMessageChange = { if (it.length <= MAX_CHARS) message = it },
                onSend          = { onSend(message.trim()) },
            )
        }
    }
}

/** Disabled state shown when sending isn't allowed (pending invite / blocked / rate-limited). */
@Composable
private fun CantSend(mode: SendMode, artistName: String) {
    val (title, body) = when (mode) {
        SendMode.PENDING -> "Invitation pending" to
            "You can send another message once $artistName responds."
        SendMode.BLOCKED_BY_ME -> "You blocked this user" to "Unblock them to send messages."
        SendMode.BLOCKED_BY_THEM -> "Messaging unavailable" to "You can't message this user."
        else -> "Messaging unavailable" to "You can't message this user right now."
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl, vertical = Spacing.xxl)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.lg))
    }
}

// ── Invitation form (state 1 + 2) ─────────────────────────────────────────────

@Composable
private fun InvitationForm(
    mode: SendMode,
    artistName: String,
    artistRole: String,
    artistAvatarUrl: String?,
    artworkTitle: String,
    artworkImageUrl: String,
    message: String,
    invitationsLeft: Int?,
    isSending: Boolean,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val d = LocalDimens.current
    val isInvite = mode == SendMode.INVITE
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Scroll + ime padding so the Send button stays reachable above the keyboard.
            .verticalScroll(rememberScrollState())
            // Tap anywhere outside the field dismisses the keyboard.
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
            .padding(horizontal = Spacing.lg)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // Title
        Text(
            text      = if (isInvite) "Invite $artistName to chat" else "Message $artistName",
            style     = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color     = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.md))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(Spacing.lg))

        // Artist row
        Row(verticalAlignment = Alignment.CenterVertically) {
            RinxAvatar(
                url                = artistAvatarUrl,
                contentDescription = artistName,
                size               = d.avatarSize,
                name               = artistName,
            )
            Spacer(Modifier.width(Spacing.sm))
            Column {
                Text(
                    text       = artistName,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text  = artistRole,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))

        // Artwork row
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model              = artworkImageUrl,
                contentDescription = artworkTitle,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .size(Spacing.giant + Spacing.huge)   // 88dp thumbnail
                    .clip(RoundedCornerShape(Spacing.sm))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text(
                    text       = "\"$artworkTitle\"",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text  = "by $artistName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        // Message input — rounded surfaceVariant container matching the reference
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Spacing.sm))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                BasicTextField(
                    value          = message,
                    onValueChange  = onMessageChange,
                    modifier       = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Spacing.huge + Spacing.md),   // min ~52dp
                    textStyle      = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    cursorBrush    = SolidColor(BrandPrimary),
                    decorationBox  = { innerTextField ->
                        Box {
                            if (message.isEmpty()) {
                                Text(
                                    text  = "Write your message",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                // Character counter right-aligned inside box — always shown when typing
                if (message.isNotEmpty()) {
                    Text(
                        text      = "${message.length}/$MAX_CHARS characters",
                        style     = MaterialTheme.typography.labelSmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier  = Modifier.fillMaxWidth().padding(top = Spacing.xs),
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        // Info section — only for a brand-new chat (the invitation). Active chats skip it.
        if (isInvite) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    painter            = painterResource(R.drawable.ic_forward_inbox),
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onBackground,
                    modifier           = Modifier.size(Spacing.xl).padding(top = Spacing.xs),
                )
                Spacer(Modifier.width(Spacing.sm))
                Column {
                    Text(
                        text       = "Invite $artistName to chat",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text  = "This message will act as your invitation to chat for the first time " +
                                "with this profile. You can only send one message in this invite until they accept.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))
        }

        // Send message button — blue when message is typed, dark/disabled when empty or sending
        val canSend = message.isNotBlank() && !isSending
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(if (canSend) BrandPrimary else InactiveButton)
                .then(if (canSend) Modifier.clickable { onSend() } else Modifier)
                .padding(vertical = Spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = if (isSending) "Sending…" else "Send message",
                style      = MaterialTheme.typography.labelLarge,
                color      = Color.White.copy(alpha = if (canSend) 1f else 0.85f),
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.height(Spacing.sm))

        // New-chats-this-month count — only for a fresh invite (active chats don't spend quota).
        if (isInvite) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Text(
                    text  = invitationsLeft?.let { "You have $it new chats this month" }
                        ?: "New chats are limited each month",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(Spacing.xs))
                Icon(
                    painter            = painterResource(R.drawable.ic_help),
                    contentDescription = "Info",
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(Spacing.md),
                )
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

// ── Invitation sent (state 3) ──────────────────────────────────────────────────

@Composable
private fun InvitationSent(
    isInvite: Boolean,
    artistName: String,
    invitationsLeft: Int?,
    onDismiss: () -> Unit,
) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl, vertical = Spacing.xxl)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter            = painterResource(R.drawable.ic_send),
            contentDescription = null,
            tint               = BrandPrimary,
            modifier           = Modifier.size(Spacing.giant + Spacing.xxl),   // 72dp
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text       = "Message sent!",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onBackground,
            textAlign  = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.md))
        val context = LocalContext.current
        // Only nudge about notifications when they're actually OFF.
        val notifsEnabled = remember { NotificationManagerCompat.from(context).areNotificationsEnabled() }
        Text(
            text      = when {
                !isInvite -> "Your message is on its way to $artistName."
                else -> "Your message is on its way to $artistName" +
                    (invitationsLeft?.let { " — you have $it new chats left this month" } ?: "") +
                    if (notifsEnabled) "." else
                        ".\n\nGet notified when they respond by turning on notifications."
            },
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xl))
        // Notifications already on → plain "Done"; otherwise the CTA opens system notification settings.
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(BrandPrimary)
                .clickable {
                    if (!notifsEnabled) openAppNotificationSettings(context)
                    onDismiss()
                }
                .padding(vertical = Spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = if (notifsEnabled) "Done" else "Turn on notifications",
                style      = MaterialTheme.typography.labelLarge,
                color      = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(Spacing.lg))
    }
}

/** Open the system's per-app notification settings (falls back to app details on API < 26). */
private fun openAppNotificationSettings(context: android.content.Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
    }
    runCatching { context.startActivity(intent) }
}
