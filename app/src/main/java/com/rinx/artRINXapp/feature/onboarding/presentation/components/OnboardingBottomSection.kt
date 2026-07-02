package com.rinx.artRINXapp.feature.onboarding.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.OnboardingActiveProgress
import com.rinx.artRINXapp.feature.onboarding.presentation.OnboardingAnim

@Composable
fun OnboardingControls(
    pageCount: Int,
    currentPage: Int,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // iOS progress: exactly [pageCount] fixed-size pills. Only the active pill
        // scales 1.0 → 1.1 and takes the active colour; the rest stay secondary.
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(dimens.pillSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (index in 0 until pageCount) {
                val active = index == currentPage
                val scale by animateFloatAsState(
                    targetValue = if (active) 1.1f else 1.0f,
                    animationSpec = OnboardingAnim.pillFloatSpec(),
                    label = "pill_scale_$index",
                )
                val pillColor by animateColorAsState(
                    targetValue = if (active)
                        OnboardingActiveProgress
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = OnboardingAnim.pillColorSpec(),
                    label = "pill_color_$index",
                )
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .height(dimens.pillHeight)
                        .width(dimens.pillWidth)
                        .background(
                            color = pillColor,
                            shape = RoundedCornerShape(dimens.pillCornerRadius),
                        ),
                )
            }
        }

        // iOS next button: plain 44×44 circle, white chevron on #45B1E8.
        Box(
            modifier = Modifier
                .size(dimens.chevronButtonSize)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .clickable(onClick = onNext),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = if (currentPage == pageCount - 1) "Get Started" else "Next",
                tint = Color.White,
                modifier = Modifier.size(dimens.buttonIconSize),
            )
        }
    }
}
