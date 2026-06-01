package com.example.artrinx.feature.home.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.example.artrinx.R
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.DarkCardSurface
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import androidx.compose.foundation.clickable

private enum class SheetPhase { FORM, SENT }
private const val MAX_CHARS = 1000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendMessageBottomSheet(
    artistName: String,
    artistRole: String,
    @DrawableRes artistAvatarRes: Int?,
    artworkTitle: String,
    @DrawableRes artworkImageRes: Int,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var phase by remember { mutableStateOf(SheetPhase.FORM) }
    var message by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface,
    ) {
        when (phase) {
            SheetPhase.FORM -> InvitationForm(
                artistName     = artistName,
                artistRole     = artistRole,
                artistAvatarRes = artistAvatarRes,
                artworkTitle   = artworkTitle,
                artworkImageRes = artworkImageRes,
                message        = message,
                onMessageChange = { if (it.length <= MAX_CHARS) message = it },
                onSend         = { phase = SheetPhase.SENT },
            )
            SheetPhase.SENT -> InvitationSent(
                artistName = artistName,
                onDismiss  = onDismiss,
            )
        }
    }
}

// ── Invitation form (state 1 + 2) ─────────────────────────────────────────────

@Composable
private fun InvitationForm(
    artistName: String,
    artistRole: String,
    @DrawableRes artistAvatarRes: Int?,
    artworkTitle: String,
    @DrawableRes artworkImageRes: Int,
    message: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val d = LocalDimens.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg)
            .navigationBarsPadding(),
    ) {
        // Title
        Text(
            text      = "Invite $artistName to chat",
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
            Box(
                modifier         = Modifier
                    .size(d.avatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (artistAvatarRes != null) {
                    AsyncImage(
                        model              = artistAvatarRes,
                        contentDescription = artistName,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize().clip(CircleShape),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint        = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier    = Modifier.size(d.avatarSize * 0.6f),
                    )
                }
            }
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
                model              = artworkImageRes,
                contentDescription = artworkTitle,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .size(Spacing.huge + Spacing.xxl)   // 64dp thumbnail
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

        // Info section
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                painter            = painterResource(R.drawable.ic_message_send),
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

        // Send message button — blue when message is typed, dark/disabled when empty
        val canSend = message.isNotEmpty()
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(if (canSend) BrandPrimary else DarkCardSurface)
                .then(if (canSend) Modifier.clickable { onSend() } else Modifier)
                .padding(vertical = Spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = "Send message",
                style      = MaterialTheme.typography.labelLarge,
                color      = Color.White.copy(alpha = if (canSend) 1f else 0.5f),
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.height(Spacing.sm))

        // Invitations count
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            Text(
                text  = "You have 15 invitations left",
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

// ── Invitation sent (state 3) ──────────────────────────────────────────────────

@Composable
private fun InvitationSent(
    artistName: String,
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
        Text(
            text      = "Your invitation is in $artistName's inbox — you have 14 invitations " +
                    "left this month.\n\nGet notified when they respond by turning on notifications.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xl))
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(BrandPrimary)
                .clickable { onDismiss() }
                .padding(vertical = Spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = "Turn on notifications",
                style      = MaterialTheme.typography.labelLarge,
                color      = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(Spacing.lg))
    }
}
