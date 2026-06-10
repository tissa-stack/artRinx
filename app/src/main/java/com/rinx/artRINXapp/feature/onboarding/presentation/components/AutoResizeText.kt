package com.rinx.artRINXapp.feature.onboarding.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp

/**
 * A [Text] that shrinks its font until the content fits within [maxLines] without visual overflow,
 * down to [minFontSize]. Solves headlines being clipped/ellipsized on small or narrow screens
 * (and under large system font scales) while staying full-size on roomy screens.
 *
 * Compose-1.7 friendly: built-in `Text(autoSize = …)` only exists on Foundation 1.8+, so we step the
 * size down via [onTextLayout]/[androidx.compose.ui.text.TextLayoutResult.hasVisualOverflow] and gate
 * drawing until the size has settled (avoids a one-frame flash at the larger size).
 *
 * [minFontSize] defaults to 60% of the style's size, so the floor tracks the (already responsive)
 * base size rather than a fixed magic number.
 */
@Composable
fun AutoResizeText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    minFontSize: TextUnit = TextUnit.Unspecified,
) {
    val floor = if (minFontSize.isUnspecified) style.fontSize * 0.6f else minFontSize

    // Reset whenever the text or the (responsive) base style changes — e.g. paging to a longer
    // headline or a configuration change that rescales typography.
    var resized by remember(text, style) { mutableStateOf(style) }
    var settled by remember(text, style) { mutableStateOf(false) }

    Text(
        text = text,
        style = resized,
        color = color,
        maxLines = maxLines,
        softWrap = true,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.drawWithContent { if (settled) drawContent() },
        onTextLayout = { result ->
            if (result.hasVisualOverflow && resized.fontSize > floor) {
                val next = (resized.fontSize.value - 1f).coerceAtLeast(floor.value)
                resized = resized.copy(fontSize = next.sp)
            } else if (!settled) {
                settled = true
            }
        },
    )
}