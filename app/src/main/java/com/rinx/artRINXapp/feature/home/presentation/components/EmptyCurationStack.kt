package com.rinx.artRINXapp.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * The "create a collection" empty placeholder — a clean, balanced stack of three rounded cards.
 * When [showAddArt] is true the centred front card carries an add-photo glyph + "Add art" label;
 * this is the actionable prompt and is used ONLY in the New/Edit Collection editor (create a new
 * curation, or edit an empty one). Everywhere else — viewing an empty collection's DETAIL or grid
 * card — it stays false so the plain card stack reads as "empty" without an action prompt.
 * (The small list/grid previews use the fanned [EmptyCurationPreview].)
 *
 * Theme-aware: cards use solid surface fills with a 1dp outline border (so edges stay crisp on a
 * white background in light theme and on near-black in dark theme); the back cards blend subtly
 * toward [onSurface] so they recede; only the front card casts a shadow (no muddy overlapping halo).
 */
@Composable
fun EmptyCurationStack(modifier: Modifier = Modifier, showAddArt: Boolean = false) {
    val d = LocalDimens.current
    val cardHeight = d.uploadImageHeight * 0.80f
    val cardWidth = d.uploadImageHeight * 0.68f
    val shape = RoundedCornerShape(d.cardCornerRadius)

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline
    // Front is the cleanest surface; the two back cards step subtly darker so the stack reads as
    // layered without going muddy.
    val cardFront = surfaceVariant
    val cardBackNear = lerp(surfaceVariant, onSurface, 0.06f)
    val cardBackFar = lerp(surfaceVariant, onSurface, 0.12f)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Back-left card — fans out to the left, peeks behind the front.
        Box(
            modifier = Modifier
                .width(cardWidth * 0.90f)
                .height(cardHeight * 0.90f)
                .offset(x = (-20).dp, y = 6.dp)
                .zIndex(0f)
                .clip(shape)
                .background(cardBackFar)
                .border(1.dp, outline, shape),
        )
        // Back-right card — mirrors the left for a balanced fan.
        Box(
            modifier = Modifier
                .width(cardWidth * 0.94f)
                .height(cardHeight * 0.94f)
                .offset(x = 20.dp, y = 4.dp)
                .zIndex(1f)
                .clip(shape)
                .background(cardBackNear)
                .border(1.dp, outline, shape),
        )
        // Front card — centred, the only card with a shadow; carries the add-photo glyph + label.
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .zIndex(2f)
                .shadow(6.dp, shape)
                .clip(shape)
                .background(cardFront)
                .border(1.dp, outline, shape),
            contentAlignment = Alignment.Center,
        ) {
            if (showAddArt) {
                // Editor (create / edit): actionable plus glyph + "Add art" prompt.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_photo),
                        contentDescription = "Add art",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Spacing.giant),
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = "Add art",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                // Viewing an empty curation: plain photo glyph (no plus, no label).
                Icon(
                    painter = painterResource(R.drawable.ic_photo),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(Spacing.giant),
                )
            }
        }
    }
}
