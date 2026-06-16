package com.rinx.artRINXapp.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.DarkElevatedSurface
import com.rinx.artRINXapp.core.theme.Spacing

/** Safety tips shown when the user taps the info button next to a third-party-link warning. */
private val BAD_LINK_TIPS = listOf(
    "Check the URL and make sure there's no weird spelling or extra characters.",
    "Check for unusual domain endings.",
    "Do not continue on a site if there are multiple redirects.",
    "Do not click anything that uses urgency to get you to click it.",
    "The link takes you somewhere that has no affiliation with where you're supposed to go.",
)

/**
 * Help icon placed right after the "Would you like to continue?" question in third-party-link
 * dialogs. Pair it with [LinkSafetyInfoOverlay], which renders the tips card over the dialog.
 *
 * Used by [com.rinx.artRINXapp.feature.home.presentation.components.ShopLinkDialog] and
 * [com.rinx.artRINXapp.feature.profile.presentation.components.PortfolioLinkDialog].
 */
@Composable
fun LinkSafetyInfoIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = Spacing.lg,
) {
    Icon(
        painter            = painterResource(R.drawable.ic_help),
        contentDescription = "Learn to identify bad links",
        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
        // Clip+padding give a comfortable tap area while the glyph stays compact.
        modifier           = modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(Spacing.xs)
            .size(iconSize),
    )
}

/**
 * Tips card overlaying the host dialog, anchored to the top-left and dimming the content behind it.
 * Call inside the dialog's root [Box] so it covers the dialog; tapping outside the card or "Close"
 * dismisses it. Colours come from the theme so it adapts to light/dark.
 */
@Composable
fun BoxScope.LinkSafetyInfoOverlay(onClose: () -> Unit) {
    // Scrim: blocks the dialog's buttons while the tips are open and dismisses on outside tap.
    Box(
        modifier = Modifier
            .matchParentSize()
            .clickable { onClose() },
    )

    // In dark theme use a lighter raised grey so the card clearly reads above the dialog surface.
    val cardColor = if (isSystemInDarkTheme()) DarkElevatedSurface
    else MaterialTheme.colorScheme.surfaceVariant

    Surface(
        shape           = RoundedCornerShape(Spacing.lg),
        color           = cardColor,
        shadowElevation = Spacing.sm,
        modifier        = Modifier
            .align(Alignment.TopStart)
            .padding(start = Spacing.lg, top = Spacing.giant, bottom = Spacing.md)
            .fillMaxWidth(0.78f)
            // No-op click consumes taps so they don't fall through to the dismissing scrim.
            .clickable {},
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
        ) {
            Text(
                text       = "Learn to identify bad links:",
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.sm))
            BAD_LINK_TIPS.forEachIndexed { index, tip ->
                if (index > 0) Spacer(Modifier.height(Spacing.sm))
                Row {
                    Text(
                        text     = "${index + 1}.",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = Spacing.sm),
                    )
                    Text(
                        text  = tip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Text(
                text       = "Close",
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onBackground,
                modifier   = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(Spacing.xs))
                    .clickable { onClose() }
                    .padding(Spacing.xs),
            )
        }
    }
}
