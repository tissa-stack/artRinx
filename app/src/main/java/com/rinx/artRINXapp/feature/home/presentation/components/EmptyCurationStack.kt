package com.rinx.artRINXapp.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * The "create a collection" empty placeholder — three grey cards stacked BEHIND each other (front
 * card carries an add-photo glyph). Used on the New/Edit Collection editor and on an empty
 * collection's DETAIL screen, so an art-less collection reads the same everywhere it's opened.
 * (The small list/grid previews use the fanned [EmptyCurationPreview] instead.)
 */
@Composable
fun EmptyCurationStack(modifier: Modifier = Modifier) {
    val d = LocalDimens.current
    val cardHeight = d.uploadImageHeight * 0.80f
    val cardWidth = d.uploadImageHeight * 0.68f
    val cornerRadius = d.cardCornerRadius
    // Theme-aware stacked-card shades: onSurface overlays read as neutral grey on a light background
    // and a subtle lighter grey on a dark one. Front is the most prominent (highest alpha).
    val onSurface = MaterialTheme.colorScheme.onSurface
    val cardFront = onSurface.copy(alpha = 0.22f)
    val cardMiddle = onSurface.copy(alpha = 0.15f)
    val cardBack = onSurface.copy(alpha = 0.10f)

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Back card — rightmost, darkest
        Box(
            modifier = Modifier
                .width(cardWidth * 0.88f)
                .height(cardHeight * 0.88f)
                .offset(x = 22.dp, y = 8.dp)
                .zIndex(0f)
                .shadow(4.dp, RoundedCornerShape(cornerRadius))
                .clip(RoundedCornerShape(cornerRadius))
                .background(cardBack),
        )
        // Middle card
        Box(
            modifier = Modifier
                .width(cardWidth * 0.94f)
                .height(cardHeight * 0.94f)
                .offset(x = 11.dp, y = 4.dp)
                .zIndex(1f)
                .shadow(4.dp, RoundedCornerShape(cornerRadius))
                .clip(RoundedCornerShape(cornerRadius))
                .background(cardMiddle),
        )
        // Front card — has add photo icon
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .zIndex(2f)
                .shadow(6.dp, RoundedCornerShape(cornerRadius))
                .clip(RoundedCornerShape(cornerRadius))
                .background(cardFront),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add_photo),
                contentDescription = "Add art",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Spacing.giant),
            )
        }
    }
}
