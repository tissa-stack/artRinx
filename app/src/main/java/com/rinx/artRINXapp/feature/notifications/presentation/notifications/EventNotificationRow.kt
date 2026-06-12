package com.rinx.artRINXapp.feature.notifications.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import coil.compose.AsyncImage
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.core.util.initialsOf
import com.rinx.artRINXapp.core.util.pastelColorFor
import com.rinx.artRINXapp.feature.notifications.domain.model.NotificationItem

/**
 * Event notification row (Figma 3600-9781). Square event banner on the left (falls back to the
 * organizer avatar), then composed text where the `@handle` is the only tappable span (→ profile);
 * tapping anywhere else on the row opens the event popup.
 */
@Composable
fun EventNotificationRow(
    item: NotificationItem,
    onOpenEvent: (Long) -> Unit,
    onOpenProfile: (Long) -> Unit,
) {
    val d = LocalDimens.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Tap anywhere that isn't the @handle link → open the event popup.
            .clickable(enabled = item.eventId != null) { item.eventId?.let(onOpenEvent) }
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        val bannerModel: Any? = item.eventImageUrl ?: item.thumbnailUrl ?: item.avatarUrl
        if (bannerModel != null) {
            AsyncImage(
                model = bannerModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(d.eventThumbnailSize)
                    .clip(RoundedCornerShape(Spacing.sm)),
            )
        } else {
            // No banner/avatar → show the organizer's initials on a pastel tint (square, to match
            // the event banner shape); fall back to a person icon only when no name is known.
            val orgInitials = initialsOf(item.organizerName)
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(d.eventThumbnailSize)
                    .clip(RoundedCornerShape(Spacing.sm))
                    .background(
                        if (orgInitials != null) pastelColorFor(item.organizerName)
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (orgInitials != null) {
                    Text(
                        text = orgInitials,
                        color = Color(0xFF1D1D1D),
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(d.eventThumbnailSize * 0.55f),
                    )
                }
            }
        }

        Spacer(Modifier.width(Spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildEventText(item, onOpenProfile),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.timeAgo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun buildEventText(
    item: NotificationItem,
    onOpenProfile: (Long) -> Unit,
) = buildAnnotatedString {
    val name = item.organizerName?.trim().orEmpty()
    val handle = item.organizerHandle?.trim().orEmpty()
    val unreadWeight = if (item.isRead) FontWeight.Normal else FontWeight.SemiBold

    if (name.isNotEmpty()) {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(name) }
        append(" ")
    }
    if (handle.isNotEmpty()) {
        val display = if (handle.startsWith("@")) handle else "@$handle"
        val organizerId = item.organizerId
        if (organizerId != null) {
            withLink(
                LinkAnnotation.Clickable(
                    tag = "organizer",
                    linkInteractionListener = { onOpenProfile(organizerId) },
                ),
            ) {
                withStyle(SpanStyle(color = BrandPrimary, fontWeight = FontWeight.SemiBold)) {
                    append(display)
                }
            }
        } else {
            withStyle(SpanStyle(color = BrandPrimary, fontWeight = FontWeight.SemiBold)) {
                append(display)
            }
        }
        append(" ")
    }
    withStyle(SpanStyle(fontWeight = unreadWeight)) { append(item.message) }
}
