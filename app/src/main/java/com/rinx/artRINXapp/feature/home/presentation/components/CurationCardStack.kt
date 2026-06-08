package com.rinx.artRINXapp.feature.home.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

private data class SlotConfig(
    val offsetX: Dp,
    val offsetY: Dp,
    val scale: Float,
    val alpha: Float,
)

// No rotation, no elevation. Alternating L/R offsets for both-side peek.
private val STACK_CONFIGS = listOf(
    SlotConfig(  0.dp,   0.dp, 1.000f, 1.00f),  // top — centered
    SlotConfig( 20.dp, (-4).dp, 0.960f, 1.00f),  // 2nd — peeks right
    SlotConfig((-22).dp, (-7).dp, 0.920f, 0.95f),  // 3rd — peeks left
    SlotConfig( 26.dp,(-10).dp, 0.880f, 0.85f),  // 4th — peeks right
)

private const val MAX_VISIBLE       = 4
private const val DISMISS_THRESHOLD = 90f    // dp
private const val DISMISS_VELOCITY  = 500f   // dp/s (rough)
private const val EXIT_DURATION_MS  = 190
private const val ADVANCE_AFTER_MS  = 160L

/**
 * Physical photo-stack carousel — all cards centered, layered by zIndex.
 * No navigation arrows: user swipes left or right to dismiss the top card.
 * Dismissed card re-enters at the back via snap() — zero visible delay.
 */
@Composable
fun CurationCardStack(
    // Accepts either image-URL strings (home/detail) or drawable-res Ints (upload preview);
    // Coil's AsyncImage model takes both.
    artworks: List<Any>,
    modifier: Modifier = Modifier,
    onTopIndexChanged: (Int) -> Unit = {},
) {
    if (artworks.isEmpty()) return

    val d       = LocalDimens.current
    val density = LocalDensity.current
    val scope   = rememberCoroutineScope()
    val count   = artworks.size

    var topIndex       by rememberSaveable { mutableIntStateOf(0) }
    var justDismissed  by remember { mutableStateOf(-1) }
    var isAnimatingOut by remember { mutableStateOf(false) }

    val dragX = remember { Animatable(0f) }
    val dragY = remember { Animatable(0f) }

    // Notify parent whenever the focused card changes
    androidx.compose.runtime.LaunchedEffect(topIndex) { onTopIndexChanged(topIndex % count) }

    val thresholdPx = with(density) { DISMISS_THRESHOLD.dp.toPx() }

    fun dismiss(direction: Int) {
        if (isAnimatingOut) return
        isAnimatingOut = true
        val departingIdx = topIndex % count
        scope.launch {
            launch {
                dragX.animateTo(
                    targetValue   = direction * with(density) { 800.dp.toPx() },
                    animationSpec = tween(EXIT_DURATION_MS, easing = FastOutLinearInEasing),
                )
            }
            launch {
                dragY.animateTo(
                    targetValue   = dragY.value + with(density) { 40.dp.toPx() },
                    animationSpec = tween(EXIT_DURATION_MS, easing = FastOutLinearInEasing),
                )
            }
            kotlinx.coroutines.delay(ADVANCE_AFTER_MS)

            // Both in the same snapshot → re-entering card sees snap() in one recomposition
            justDismissed = departingIdx
            topIndex      = (topIndex + 1) % count
            dragX.snapTo(0f)
            dragY.snapTo(0f)

            kotlinx.coroutines.delay(40)
            justDismissed  = -1
            isAnimatingOut = false
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val screenWidth = maxWidth
        val cardWidth   = screenWidth * 0.78f
        val cardHeight  = d.artDetailImageHeight * 1.08f

        Box(
            modifier         = Modifier
                .width(screenWidth)
                .height(cardHeight + Spacing.lg),   // small extra height for the offset peek edges
            contentAlignment = Alignment.Center,
        ) {
            val visibleSlots = minOf(MAX_VISIBLE, count)

            for (slot in visibleSlots - 1 downTo 0) {
                val artIndex = (topIndex + slot) % count
                val isTop    = (slot == 0)
                val cfg      = STACK_CONFIGS.getOrElse(slot) { STACK_CONFIGS.last() }

                key(artIndex) {
                    val useSnap = (artIndex == justDismissed)
                    val posSpec = if (useSnap) snap() else spring<Dp>(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMediumLow,
                    )
                    val fltSpec = if (useSnap) snap() else spring<Float>(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMediumLow,
                    )

                    val animOffX  by animateDpAsState(
                        targetValue = cfg.offsetX, animationSpec = posSpec, label = "offX-$artIndex")
                    val animOffY  by animateDpAsState(
                        targetValue = cfg.offsetY, animationSpec = posSpec, label = "offY-$artIndex")
                    val animScale by animateFloatAsState(
                        targetValue = cfg.scale, animationSpec = fltSpec, label = "scale-$artIndex")
                    val animAlpha by animateFloatAsState(
                        targetValue = cfg.alpha, animationSpec = fltSpec, label = "alpha-$artIndex")

                    Box(
                        modifier = Modifier
                            .width(cardWidth)
                            .height(cardHeight)
                            .zIndex(if (isTop) 100f else (MAX_VISIBLE - slot).toFloat() * 10f)
                            .graphicsLayer {
                                if (isTop) {
                                    translationX = dragX.value
                                    translationY = dragY.value
                                    rotationZ    = 0f
                                    // No shadowElevation — avoids pointed artifact shadows
                                } else {
                                    val dragProg = (abs(dragX.value) / thresholdPx).coerceIn(0f, 1f)
                                    val prevCfg  = STACK_CONFIGS.getOrElse(slot - 1) { STACK_CONFIGS[0] }
                                    translationX = with(density) { animOffX.toPx() } +
                                            dragProg * with(density) { (prevCfg.offsetX - cfg.offsetX).toPx() }
                                    translationY = with(density) { animOffY.toPx() } +
                                            dragProg * with(density) { (prevCfg.offsetY - cfg.offsetY).toPx() }
                                    rotationZ    = 0f
                                    scaleX       = animScale + dragProg * (prevCfg.scale - cfg.scale)
                                    scaleY       = animScale + dragProg * (prevCfg.scale - cfg.scale)
                                    alpha        = animAlpha + dragProg * (prevCfg.alpha - cfg.alpha)
                                    // No shadowElevation — clean edges
                                }
                            }
                            .clip(RoundedCornerShape(d.cardCornerRadius))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .then(
                                if (isTop && !isAnimatingOut) Modifier.pointerInput(topIndex) {
                                    var velX = 0f
                                    detectDragGestures(
                                        onDragStart = { velX = 0f },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            velX = amount.x
                                            scope.launch {
                                                dragX.snapTo(dragX.value + amount.x)
                                                dragY.snapTo(dragY.value + amount.y * 0.3f)
                                            }
                                        },
                                        onDragEnd = {
                                            val exceeded = abs(dragX.value) > thresholdPx ||
                                                    abs(velX) * 60f > DISMISS_VELOCITY
                                            if (exceeded) {
                                                dismiss(if (dragX.value != 0f) sign(dragX.value).toInt() else 1)
                                            } else {
                                                scope.launch {
                                                    launch { dragX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                                                    launch { dragY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                                                }
                                            }
                                        },
                                        onDragCancel = {
                                            scope.launch {
                                                launch { dragX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                                                launch { dragY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
                                            }
                                        },
                                    )
                                } else Modifier,
                            ),
                    ) {
                        AsyncImage(
                            model              = artworks[artIndex],
                            contentDescription = "Artwork ${slot + 1} of ${artworks.size}",
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
