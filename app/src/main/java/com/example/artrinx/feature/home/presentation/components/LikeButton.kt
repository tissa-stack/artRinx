package com.example.artrinx.feature.home.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.example.artrinx.R
import com.example.artrinx.core.theme.LikeRed

/**
 * Like icon that, the moment it becomes liked, physically jumps up and falls back, and switches
 * to a solid red filled heart. Liked/unliked use the SAME heart silhouette (ic_like_filled is the
 * filled version of ic_like's outer path), so the icon size never changes between states. Unliking
 * has no animation, and the jump only fires on a real user-driven not-liked → liked transition.
 */
@Composable
fun LikeButton(
    isLiked: Boolean,
    onClick: () -> Unit,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val jumpPx = with(LocalDensity.current) { size.toPx() }   // hop ~ one icon height
    val offsetY = remember { Animatable(0f) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(isLiked) {
        if (!initialized) {
            initialized = true
            return@LaunchedEffect
        }
        if (isLiked) {
            offsetY.snapTo(0f)
            offsetY.animateTo(-jumpPx, tween(durationMillis = 180, easing = FastOutSlowInEasing))
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
        }
    }

    Icon(
        painter = painterResource(
            if (isLiked) R.drawable.ic_like_filled else R.drawable.ic_like,
        ),
        contentDescription = if (isLiked) "Liked" else "Like",
        tint = if (isLiked) LikeRed else MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .size(size)
            .graphicsLayer { translationY = offsetY.value }
            .clickable { onClick() },
    )
}
