package com.example.artrinx.feature.onboarding.presentation.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.example.artrinx.core.theme.LocalDimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Shuffle geometry ────────────────────────────────────────────────────────

// Each slot i enters from the position of SHUFFLE_FROM[i], so images appear
// to trade places within the grid rather than fly in from outside.
// Cycle: 0←3, 1←0, 2←4, 3←2, 4←1
private val SHUFFLE_FROM      = listOf(3, 0, 4, 2, 1)
private val SLOT_ROTATE_DEG   = listOf(-5f, 6f, -4f, 7f, -5f)

private data class SlotOffset(val dx: Float, val dy: Float, val rotateDeg: Float)

/** Y-center of every slot in a single column, keyed by original image index. */
private fun columnYCenters(
    colWidthDp: Float,
    indices: List<Int>,
    aspectRatios: List<Float>,
    gapDp: Float,
): Map<Int, Float> {
    var top = 0f
    return buildMap {
        indices.forEachIndexed { i, idx ->
            val h = colWidthDp / aspectRatios.getOrElse(idx) { 1.35f }
            put(idx, top + h / 2f)
            if (i < indices.size - 1) top += h + gapDp
        }
    }
}

/**
 * For each of the 5 image slots, compute the (dx, dy) offset from that slot's
 * real position to the position it should *start* the animation from.
 * All values are in dp.
 */
private fun computeShuffleOffsets(
    colWidthDp: Float,
    gapDp: Float,
    aspectRatios: List<Float>,
    flipLayout: Boolean,
): List<SlotOffset> {
    val leftIdx  = if (flipLayout) listOf(0, 2, 4) else listOf(0, 2)
    val rightIdx = if (flipLayout) listOf(1, 3)    else listOf(1, 3, 4)

    val leftCx  = colWidthDp / 2f
    val rightCx = colWidthDp + gapDp + colWidthDp / 2f

    val yL = columnYCenters(colWidthDp, leftIdx,  aspectRatios, gapDp)
    val yR = columnYCenters(colWidthDp, rightIdx, aspectRatios, gapDp)

    fun cx(i: Int) = if (i in leftIdx) leftCx  else rightCx
    fun cy(i: Int) = yL[i] ?: yR[i] ?: 0f

    return (0 until 5).map { i ->
        val from = SHUFFLE_FROM[i]
        SlotOffset(
            dx        = cx(from) - cx(i),   // positive = start to the right
            dy        = cy(from) - cy(i),   // positive = start below
            rotateDeg = SLOT_ROTATE_DEG[i],
        )
    }
}

// ─── Composables ─────────────────────────────────────────────────────────────

@Composable
fun ArtMosaicGrid(
    images: List<Int>,
    modifier: Modifier = Modifier,
    flipLayout: Boolean = false,
    aspectRatios: List<Float> = emptyList(),
    isSettled: Boolean = true,
) {
    val dimens = LocalDimens.current

    val defaultLeftRatio  = if (flipLayout) 1.35f else 0.9f
    val defaultRightRatio = if (flipLayout) 0.9f  else 1.35f

    fun ratioFor(i: Int, left: Boolean) =
        if (aspectRatios.size > i) aspectRatios[i] else if (left) defaultLeftRatio else defaultRightRatio

    // Build (imageRes, aspectRatio, originalSlotIndex) triples
    val leftTriples: List<Triple<Int, Float, Int>>
    val rightTriples: List<Triple<Int, Float, Int>>

    if (images.size % 2 == 0) {
        leftTriples  = images.filterIndexed { i, _ -> i % 2 == 0 }
            .mapIndexed { i, img -> Triple(img, ratioFor(i * 2, true), i * 2) }
        rightTriples = images.filterIndexed { i, _ -> i % 2 == 1 }
            .mapIndexed { i, img -> Triple(img, ratioFor(i * 2 + 1, false), i * 2 + 1) }
    } else if (flipLayout) {
        leftTriples  = listOf(0, 2, 4).map { i -> Triple(images[i], ratioFor(i, true),  i) }
        rightTriples = listOf(1, 3).map    { i -> Triple(images[i], ratioFor(i, false), i) }
    } else {
        leftTriples  = listOf(0, 2).map    { i -> Triple(images[i], ratioFor(i, true),  i) }
        rightTriples = listOf(1, 3, 4).map { i -> Triple(images[i], ratioFor(i, false), i) }
    }

    // Derive column width from screen width minus the horizontal padding the caller applies
    val screenWidthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.toFloat()
    val colWidthDp    = (screenWidthDp - 2 * dimens.gridPaddingHorizontal.value - dimens.gridCellGap.value) / 2f
    val gapDp         = dimens.gridCellGap.value

    val offsets = remember(colWidthDp, gapDp, flipLayout) {
        computeShuffleOffsets(colWidthDp, gapDp, aspectRatios, flipLayout)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimens.gridCellGap),
    ) {
        MosaicColumn(
            triples      = leftTriples,
            offsets      = offsets,
            isSettled    = isSettled,
            cornerRadius = dimens.gridCornerRadius,
            gap          = dimens.gridCellGap,
            modifier     = Modifier.weight(1f),
        )
        MosaicColumn(
            triples      = rightTriples,
            offsets      = offsets,
            isSettled    = isSettled,
            cornerRadius = dimens.gridCornerRadius,
            gap          = dimens.gridCellGap,
            modifier     = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MosaicColumn(
    triples: List<Triple<Int, Float, Int>>,
    offsets: List<SlotOffset>,
    isSettled: Boolean,
    cornerRadius: Dp,
    gap: Dp,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        triples.forEach { (imageRes, aspectRatio, slotIdx) ->
            MosaicImage(
                imageRes      = imageRes,
                aspectRatio   = aspectRatio,
                cornerRadius  = cornerRadius,
                isSettled     = isSettled,
                shuffleOffset = offsets.getOrElse(slotIdx) { SlotOffset(0f, 0f, 0f) },
                staggerMs     = slotIdx * 35L,
                resources     = resources,
            )
        }
    }
}

@Composable
private fun MosaicImage(
    imageRes: Int,
    aspectRatio: Float,
    cornerRadius: Dp,
    isSettled: Boolean,
    shuffleOffset: SlotOffset,
    staggerMs: Long,
    resources: android.content.res.Resources,
) {
    val alpha = remember { Animatable(0f) }
    val tx    = remember { Animatable(shuffleOffset.dx) }
    val ty    = remember { Animatable(shuffleOffset.dy) }
    val rot   = remember { Animatable(shuffleOffset.rotateDeg) }
    val scale = remember { Animatable(0.84f) }

    LaunchedEffect(isSettled) {
        if (isSettled) {
            alpha.snapTo(0f)
            tx.snapTo(shuffleOffset.dx)
            ty.snapTo(shuffleOffset.dy)
            rot.snapTo(shuffleOffset.rotateDeg)
            scale.snapTo(0.84f)

            delay(staggerMs)

            val spec = tween<Float>(durationMillis = 500, easing = FastOutSlowInEasing)
            launch { alpha.animateTo(1f, tween(260)) }
            launch { tx.animateTo(0f, spec) }
            launch { ty.animateTo(0f, spec) }
            launch { rot.animateTo(0f, spec) }
            scale.animateTo(1f, spec)
        }
    }

    val painter = remember(imageRes) {
        BitmapPainter(
            image = BitmapFactory.decodeResource(resources, imageRes).asImageBitmap(),
            filterQuality = FilterQuality.High,
        )
    }

    Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(cornerRadius))
            .graphicsLayer {
                this.alpha   = alpha.value
                translationX = tx.value * density
                translationY = ty.value * density
                rotationZ    = rot.value
                scaleX       = scale.value
                scaleY       = scale.value
            },
        contentScale = ContentScale.Crop,
    )
}