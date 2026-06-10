package com.rinx.artRINXapp.feature.events.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.events.domain.model.EventDetail

/**
 * Event detail popup (Figma 3611-9993). Centered card with a celebration glyph, title, and
 * Date / Time / Location rows (fixed label column). A Compose [Dialog] renders in its own window
 * above the bottom nav and content. Theme-aware: surface + on-surface tokens so it works in both
 * light and dark.
 *
 * Shows a spinner while [isLoading] and no [event] is available yet; the caller surfaces fetch
 * errors as a toast and keeps the popup closed.
 */
@Composable
fun EventDetailPopup(
    event: EventDetail?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onLearnMore: () -> Unit,
) {
    val d = LocalDimens.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(d.cardCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(horizontal = Spacing.xxl)
                .fillMaxWidth()
                .widthIn(max = d.eventPopupMaxWidth)
                .wrapContentHeight(),
        ) {
            Box(Modifier.fillMaxWidth().padding(Spacing.lg)) {
                // Close (X) — top-right
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(Spacing.xl)
                        .clickable { onDismiss() },
                )

                if (event == null) {
                    Box(
                        Modifier.fillMaxWidth().height(d.eventPopupMaxWidth * 0.5f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isLoading) CircularProgressIndicator(color = BrandPrimary)
                    }
                } else {
                    EventContent(event = event, onLearnMore = onLearnMore)
                }
            }
        }
    }
}

@Composable
private fun EventContent(event: EventDetail, onLearnMore: () -> Unit) {
    val d = LocalDimens.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_event_speaker),
            contentDescription = null,
            tint = BrandPrimary,
            modifier = Modifier.size(d.eventPopupIconSize),
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = event.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.lg))

        if (event.dateText.isNotEmpty()) {
            LabeledRow(label = "Date", lines = listOf(event.dateText))
            Spacer(Modifier.height(Spacing.md))
        }
        if (event.timeText.isNotEmpty()) {
            LabeledRow(label = "Time", lines = listOf(event.timeText))
            Spacer(Modifier.height(Spacing.md))
        }
        if (event.locationLines.isNotEmpty()) {
            LabeledRow(label = "Location", lines = event.locationLines)
        }

        Spacer(Modifier.height(Spacing.xl))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(BrandPrimary)
                .clickable { onLearnMore() }
                .padding(horizontal = Spacing.xxl, vertical = Spacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Learn more",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun LabeledRow(label: String, lines: List<String>) {
    val d = LocalDimens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(d.eventPopupLabelColumn),
        )
        Spacer(Modifier.width(Spacing.sm))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            lines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
