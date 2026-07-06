package com.rinx.artRINXapp.feature.onboarding.presentation.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.feature.onboarding.domain.model.CardSize
import com.rinx.artRINXapp.feature.onboarding.presentation.OnboardingAnim
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Layout constants ────────────────────────────────────────────────────────

/** Within a row, narrow vs wide tile width (must sum to 1). */
private const val NARROW_FRAC = 0.40f
private const val WIDE_FRAC   = 0.60f

/** Column split used inside the block section (tall portrait vs stacked pair). */
private const val BLOCK_TALL_FRAC  = 0.50f
private const val BLOCK_STACK_FRAC = 0.50f

/** Fixed row heights as a fraction of total grid width — keeps the grid compact. */
private const val TOP_ROW_H_RATIO     = 0.30f
private const val BOTTOM_BLOCK_H_RATIO = 0.76f

// ── Shuffle timing ──────────────────────────────────────────────────────────
// Motion is spring-based (OnboardingAnim.pageFloatSpec, damping 0.8 / stiffness 110)
// to match iOS; only the stagger/tilt/scale-from shaping constants remain.
private const val SHUFFLE_PAIR_STAGGER_MS = 90L
private const val SHUFFLE_TALL_DELAY_MS   = 50L
private const val SHUFFLE_TILT_DEG        = 6f
private const val SHUFFLE_SCALE_FROM      = 0.93f

private data class SlotOffset(val dx: Float, val dy: Float, val rotateDeg: Float)

/** Absolute placement of one tile in dp (top-left origin). */
private data class SlotPlacement(
    val slot: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

private data class GridLayout(
    val placements: List<SlotPlacement>,
    val totalHeightDp: Float,
    val gapDp: Float,
)

// ─── Bento placement (row-aligned) ───────────────────────────────────────────

/**
 * Three fixed bento patterns matching the reference alignment:
 *  - Cards in the same *row* share the same height.
 *  - One tile per row is wider (40 / 60), not taller.
 *  - The tall portrait fills the remaining block height beside stacked tiles.
 */
private fun computeBentoLayout(
    cardSizes: List<CardSize>,
    flipLayout: Boolean,
    totalWidthDp: Float,
    gapDp: Float,
): GridLayout {
    // The three bento patterns below are defined for exactly 5 cards. Any other count (future
    // content/config drift) degrades to a uniform grid rather than crashing the very first screen a
    // new user sees.
    if (cardSizes.size != 5) return computeFallbackLayout(cardSizes.size, totalWidthDp, gapDp)
    val tallIndex = cardSizes.indexOf(CardSize.TALL_PORTRAIT)

    val avail = (totalWidthDp - gapDp).coerceAtLeast(1f)
    val narrowW     = avail * NARROW_FRAC
    val wideW       = avail * WIDE_FRAC
    val blockTallW  = avail * BLOCK_TALL_FRAC
    val blockStackW = avail * BLOCK_STACK_FRAC
    val rowH    = totalWidthDp * TOP_ROW_H_RATIO
    val blockH  = totalWidthDp * BOTTOM_BLOCK_H_RATIO
    val halfH   = (blockH - gapDp) / 2f

    val p = mutableListOf<SlotPlacement>()

    when {
        // Page 1 — row1: narrow|wide, block: tall left | two stacked right
        !flipLayout && tallIndex == 2 -> {
            p += SlotPlacement(0, 0f, 0f, narrowW, rowH)
            p += SlotPlacement(1, narrowW + gapDp, 0f, wideW, rowH)
            val y1 = rowH + gapDp
            p += SlotPlacement(2, 0f, y1, blockTallW, blockH)
            p += SlotPlacement(3, blockTallW + gapDp, y1, blockStackW, halfH)
            p += SlotPlacement(4, blockTallW + gapDp, y1 + halfH + gapDp, blockStackW, halfH)
        }
        // Page 2 — row1: wide|narrow, block: two stacked left | tall right
        flipLayout && tallIndex == 3 -> {
            p += SlotPlacement(0, 0f, 0f, wideW, rowH)
            p += SlotPlacement(1, wideW + gapDp, 0f, narrowW, rowH)
            val y1 = rowH + gapDp
            p += SlotPlacement(2, 0f, y1, blockStackW, halfH)
            p += SlotPlacement(4, 0f, y1 + halfH + gapDp, blockStackW, halfH)
            p += SlotPlacement(3, blockStackW + gapDp, y1, blockTallW, blockH)
        }
        // Page 3 — block: tall left | two stacked right, row2: narrow|wide
        !flipLayout && tallIndex == 0 -> {
            p += SlotPlacement(0, 0f, 0f, blockTallW, blockH)
            p += SlotPlacement(1, blockTallW + gapDp, 0f, blockStackW, halfH)
            p += SlotPlacement(3, blockTallW + gapDp, halfH + gapDp, blockStackW, halfH)
            val y1 = blockH + gapDp
            p += SlotPlacement(2, 0f, y1, narrowW, rowH)
            p += SlotPlacement(4, narrowW + gapDp, y1, wideW, rowH)
        }
        // Unrecognised flip/tall combination → uniform grid instead of a hard crash.
        else -> return computeFallbackLayout(cardSizes.size, totalWidthDp, gapDp)
    }

    val totalH = p.maxOf { it.y + it.height }
    return GridLayout(p, totalH, gapDp)
}

/**
 * Crash-proof fallback used whenever the card set doesn't match one of the three hand-tuned bento
 * patterns (wrong count, or an unrecognised flip/tall combination). Lays every card out in a simple
 * uniform grid — visually plainer than the bento, but it never throws, so a content/config change
 * can't crash the first-launch onboarding screen.
 */
private fun computeFallbackLayout(
    count: Int,
    totalWidthDp: Float,
    gapDp: Float,
): GridLayout {
    if (count <= 0) return GridLayout(emptyList(), 0f, gapDp)
    val cols = if (count == 1) 1 else 2
    val colW = ((totalWidthDp - gapDp * (cols - 1)) / cols).coerceAtLeast(1f)
    val tileH = colW // square-ish tiles keep it tidy for any count
    val p = (0 until count).map { i ->
        val row = i / cols
        val col = i % cols
        SlotPlacement(
            slot = i,
            x = col * (colW + gapDp),
            y = row * (tileH + gapDp),
            width = colW,
            height = tileH,
        )
    }
    val totalH = p.maxOf { it.y + it.height }
    return GridLayout(p, totalH, gapDp)
}

// ─── Shuffle (pair-swap, row-aware) ──────────────────────────────────────────

private fun buildPairSwap(
    placements: List<SlotPlacement>,
    tallIndex: Int,
): Pair<IntArray, List<Pair<Int, Int>>> {
    val bySlot = placements.associateBy { it.slot }
    val nonTall = (0..4).filter { it != tallIndex }
        .sortedWith(compareBy({ bySlot[it]?.y ?: 0f }, { bySlot[it]?.x ?: 0f }))

    val pairs = mutableListOf<Pair<Int, Int>>()
    var i = 0
    while (i + 1 <= nonTall.lastIndex) {
        pairs += nonTall[i] to nonTall[i + 1]
        i += 2
    }

    val from = IntArray(5) { it }
    pairs.forEach { (a, b) ->
        from[a] = b
        from[b] = a
    }
    return from to pairs
}

private fun computeShuffleOffsets(
    placements: List<SlotPlacement>,
    tallIndex: Int,
    shuffleFrom: IntArray,
): List<SlotOffset> {
    fun cx(p: SlotPlacement) = p.x + p.width / 2f
    fun cy(p: SlotPlacement) = p.y + p.height / 2f
    val bySlot = placements.associateBy { it.slot }

    return (0 until 5).map { slot ->
        val here = bySlot[slot] ?: return@map SlotOffset(0f, 0f, 0f)
        if (slot == tallIndex) {
            SlotOffset(0f, 0f, 0f)
        } else {
            val from = bySlot[shuffleFrom[slot]] ?: here
            val pairTilt = if (shuffleFrom[slot] > slot) SHUFFLE_TILT_DEG else -SHUFFLE_TILT_DEG
            SlotOffset(
                dx        = cx(from) - cx(here),
                dy        = cy(from) - cy(here),
                rotateDeg = pairTilt,
            )
        }
    }
}

private fun staggerForSlot(
    slot: Int,
    tallIndex: Int,
    pairs: List<Pair<Int, Int>>,
): Long {
    if (slot == tallIndex) return SHUFFLE_TALL_DELAY_MS
    pairs.forEachIndexed { idx, (a, b) ->
        if (slot == a || slot == b) return idx * SHUFFLE_PAIR_STAGGER_MS
    }
    return 0L
}

// ─── Composables ─────────────────────────────────────────────────────────────

@Composable
fun ArtMosaicGrid(
    images: List<Int>,
    cardSizes: List<CardSize>,
    modifier: Modifier = Modifier,
    flipLayout: Boolean = false,
    @Suppress("UNUSED_PARAMETER") landscapeHeightScale: Float = 1f,
    isSettled: Boolean = true,
) {
    val dimens = LocalDimens.current
    val gapDp = dimens.gridCellGap

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val totalWidthDp = maxWidth.value

        val layout = remember(cardSizes, flipLayout, totalWidthDp, gapDp) {
            computeBentoLayout(cardSizes, flipLayout, totalWidthDp, gapDp.value)
        }
        val tallIndex = remember(cardSizes) {
            cardSizes.indexOf(CardSize.TALL_PORTRAIT)
        }
        val (shuffleFrom, pairs) = remember(layout.placements, tallIndex) {
            buildPairSwap(layout.placements, tallIndex)
        }
        val offsets = remember(layout.placements, tallIndex, shuffleFrom) {
            computeShuffleOffsets(layout.placements, tallIndex, shuffleFrom)
        }
        val staggers = remember(pairs, tallIndex) {
            (0..4).map { staggerForSlot(it, tallIndex, pairs) }
        }

        val resources = LocalContext.current.resources

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.totalHeightDp.dp),
        ) {
            layout.placements.forEach { place ->
                val imageRes = images.getOrNull(place.slot) ?: return@forEach
                MosaicImage(
                    imageRes      = imageRes,
                    width         = place.width.dp,
                    height        = place.height.dp,
                    cornerRadius  = dimens.gridCornerRadius,
                    isSettled     = isSettled,
                    shuffleOffset = offsets.getOrElse(place.slot) { SlotOffset(0f, 0f, 0f) },
                    staggerMs     = staggers.getOrElse(place.slot) { 0L },
                    resources     = resources,
                    modifier      = Modifier.offset(x = place.x.dp, y = place.y.dp),
                )
            }
        }
    }
}

@Composable
private fun MosaicImage(
    imageRes: Int,
    width: Dp,
    height: Dp,
    cornerRadius: Dp,
    isSettled: Boolean,
    shuffleOffset: SlotOffset,
    staggerMs: Long,
    resources: android.content.res.Resources,
    modifier: Modifier = Modifier,
) {
    val alpha = remember { Animatable(0f) }
    val tx    = remember { Animatable(shuffleOffset.dx) }
    val ty    = remember { Animatable(shuffleOffset.dy) }
    val rot   = remember { Animatable(shuffleOffset.rotateDeg) }
    val scale = remember { Animatable(SHUFFLE_SCALE_FROM) }

    LaunchedEffect(isSettled, shuffleOffset) {
        if (isSettled) {
            alpha.snapTo(0f)
            tx.snapTo(shuffleOffset.dx)
            ty.snapTo(shuffleOffset.dy)
            rot.snapTo(shuffleOffset.rotateDeg)
            scale.snapTo(SHUFFLE_SCALE_FROM)

            delay(staggerMs)

            launch { alpha.animateTo(1f, OnboardingAnim.pageFloatSpec()) }
            launch { tx.animateTo(0f, OnboardingAnim.pageFloatSpec()) }
            launch { ty.animateTo(0f, OnboardingAnim.pageFloatSpec()) }
            launch { rot.animateTo(0f, OnboardingAnim.pageFloatSpec()) }
            scale.animateTo(1f, OnboardingAnim.pageFloatSpec())
        }
    }

    // decodeResource can return null for a missing/corrupt drawable; guard so the first-launch
    // screen skips that tile instead of NPE-ing on .asImageBitmap().
    val painter = remember(imageRes) {
        runCatching { BitmapFactory.decodeResource(resources, imageRes) }.getOrNull()
            ?.asImageBitmap()
            ?.let { BitmapPainter(image = it, filterQuality = FilterQuality.High) }
    } ?: return

    Image(
        painter = painter,
        contentDescription = null,
        modifier = modifier
            .width(width)
            .height(height)
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
