package com.rinx.artRINXapp.core.tour

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import kotlin.math.roundToInt

/**
 * Full-screen first-launch coaching overlay: dims everything, punches a spotlight hole over the
 * current step's target, blocks all touches except the tooltip's controls, and walks the user
 * through the steps. [targets] holds each element's window bounds (reported via
 * `Modifier.onGloballyPositioned` + `boundsInWindow()`).
 *
 * The overlay fills the window, so target window-bounds are used directly as local coordinates.
 */
@Composable
fun TourOverlay(
    stepIndex: Int,
    targets: Map<TourTarget, Rect>,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
) {
    val step = TourStep.ordered.getOrNull(stepIndex) ?: return
    val targetRect = targets[step.target]
    val density = LocalDensity.current
    val d = LocalDimens.current

    val scrim = Color.Black.copy(alpha = 0.78f)
    val padPx = with(density) { Spacing.sm.toPx() }
    val cornerPx = with(density) { Spacing.md.toPx() }
    val ringStrokePx = with(density) { 2.dp.toPx() }
    val sideMarginPx = with(density) { Spacing.lg.toPx() }
    val gapPx = padPx * 2f

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            // Swallow every gesture so the underlying UI is fully disabled during the tour.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            },
    ) {
        val screenWpx = constraints.maxWidth.toFloat()
        val screenHpx = constraints.maxHeight.toFloat()
        val targetIsTopHalf = targetRect != null && targetRect.center.y < screenHpx / 2f

        // ── Scrim with a spotlight cut-out over the target ───────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
        ) {
            drawRect(scrim)
            targetRect?.let { r ->
                val hole = Rect(
                    left = r.left - padPx,
                    top = r.top - padPx,
                    right = r.right + padPx,
                    bottom = r.bottom + padPx,
                )
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(hole.left, hole.top),
                    size = Size(hole.width, hole.height),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    blendMode = BlendMode.Clear,
                )
                drawRoundRect(
                    color = BrandPrimary,
                    topLeft = Offset(hole.left, hole.top),
                    size = Size(hole.width, hole.height),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = Stroke(width = ringStrokePx),
                )
            }
        }

        // ── Tooltip card, anchored relative to the target ────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .layout { measurable, c ->
                    val p = measurable.measure(c.copy(minWidth = 0, minHeight = 0))
                    val x = (
                        if (targetRect != null) targetRect.center.x - p.width / 2f
                        else (screenWpx - p.width) / 2f
                    ).coerceIn(
                        sideMarginPx,
                        (screenWpx - p.width - sideMarginPx).coerceAtLeast(sideMarginPx),
                    )
                    val y = when {
                        targetRect == null -> (screenHpx - p.height) / 2f
                        targetIsTopHalf -> targetRect.bottom + gapPx
                        else -> targetRect.top - gapPx - p.height
                    }.coerceIn(
                        sideMarginPx,
                        (screenHpx - p.height - sideMarginPx).coerceAtLeast(sideMarginPx),
                    )
                    layout(p.width, p.height) { p.place(x.roundToInt(), y.roundToInt()) }
                }
                .widthIn(max = d.chatBubbleMaxWidth + Spacing.huge),
        ) {
            TourCard(
                step = step,
                stepIndex = stepIndex,
                lastIndex = TourStep.ordered.lastIndex,
                caretAtTop = targetIsTopHalf,
                onNext = onNext,
                onBack = onBack,
                onSkip = onSkip,
            )
        }
    }
}

@Composable
private fun TourCard(
    step: TourStep,
    stepIndex: Int,
    lastIndex: Int,
    caretAtTop: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
) {
    val cardColor = MaterialTheme.colorScheme.inverseSurface
    val onCard = MaterialTheme.colorScheme.inverseOnSurface
    val d = LocalDimens.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (caretAtTop) Caret(pointsUp = true, color = cardColor)

        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(d.cardCornerRadius))
                .background(cardColor)
                .padding(Spacing.lg),
        ) {
            Text(
                text = step.body,
                style = MaterialTheme.typography.bodyMedium,
                color = onCard,
            )
            Spacer(Modifier.height(Spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Skip tour",
                    style = MaterialTheme.typography.labelLarge,
                    color = onCard.copy(alpha = 0.7f),
                    modifier = Modifier.clickable { onSkip() },
                )
                Spacer(Modifier.weight(1f))
                if (stepIndex > 0) {
                    Text(
                        text = "‹ Back",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = onCard,
                        modifier = Modifier
                            .clickable { onBack() }
                            .padding(end = Spacing.lg),
                    )
                }
                Text(
                    text = if (stepIndex == lastIndex) "Done" else "Next ›",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandPrimary,
                    modifier = Modifier.clickable { onNext() },
                )
            }

            Spacer(Modifier.height(Spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TourStep.ordered.forEachIndexed { i, _ ->
                    val active = i == stepIndex
                    Box(
                        modifier = Modifier
                            .size(if (active) d.indicatorDotActive else d.indicatorDotSmall)
                            .clip(CircleShape)
                            .background(if (active) BrandPrimary else onCard.copy(alpha = 0.35f)),
                    )
                }
            }
        }

        if (!caretAtTop) Caret(pointsUp = false, color = cardColor)
    }
}

@Composable
private fun Caret(pointsUp: Boolean, color: Color) {
    Canvas(modifier = Modifier.size(width = Spacing.md, height = Spacing.sm)) {
        val path = Path().apply {
            if (pointsUp) {
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
            } else {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
            }
            close()
        }
        drawPath(path, color)
    }
}
