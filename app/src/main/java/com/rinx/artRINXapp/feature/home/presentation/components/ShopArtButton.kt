package com.rinx.artRINXapp.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.Spacing
import kotlin.math.roundToLong

/**
 * Primary-blue "Shop Art" button with the listed [price] shown in a small rounded chip stuck to its
 * top-right corner (per design). The caller is responsible for only rendering this when a shop link
 * actually exists.
 */
@Composable
fun ShopArtButton(
    price: Double?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Outer box is not clipped, so the price chip can overhang the button's top-right corner.
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(Spacing.md))
                .background(BrandPrimary)
                .clickable { onClick() }
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Shop Art",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }

        price?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = Spacing.xs, y = -Spacing.sm)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            ) {
                Text(
                    text = formatPrice(it),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandPrimary,
                )
            }
        }
    }
}

/** Compact money formatting: 120000 → "$120K", 1_500_000 → "$1.5M", 120.5 → "$120.5". */
private fun formatPrice(value: Double): String {
    val sign = if (value < 0) "-" else ""
    val abs = kotlin.math.abs(value)
    return when {
        abs >= 1_000_000 -> "$sign$${trimZeros(abs / 1_000_000)}M"
        abs >= 1_000 -> "$sign$${trimZeros(abs / 1_000)}K"
        abs == abs.roundToLong().toDouble() -> "$sign$${abs.roundToLong()}"
        else -> "$sign$${trimZeros(abs)}"
    }
}

/** One-decimal value with a trailing ".0" stripped (1.0 → "1", 1.5 → "1.5"). */
private fun trimZeros(v: Double): String {
    val oneDp = (v * 10).roundToLong() / 10.0
    return if (oneDp == oneDp.roundToLong().toDouble()) oneDp.roundToLong().toString()
    else oneDp.toString()
}
