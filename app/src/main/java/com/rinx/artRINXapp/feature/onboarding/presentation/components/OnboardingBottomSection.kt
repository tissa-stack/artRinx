package com.rinx.artRINXapp.feature.onboarding.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.rinx.artRINXapp.core.theme.LocalDimens

private const val MAX_VISIBLE_PILLS = 5

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
        // Page the pills in groups of [MAX_VISIBLE_PILLS]: within a group the active pill advances,
        // and once the group is complete the next set restarts from the beginning — so the indicator
        // keeps showing progress for any page count instead of overflowing / sticking at the last pill.
        val groupStart = (currentPage / MAX_VISIBLE_PILLS) * MAX_VISIBLE_PILLS
        val groupEnd = minOf(groupStart + MAX_VISIBLE_PILLS, pageCount)
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(dimens.pillSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (index in groupStart until groupEnd) {
                // Fill every pill up to and including the current page so the row reads as a
                // cumulative progress bar; going back un-fills them (animated) instead of just
                // moving a single highlighted pill.
                val isFilled = index <= currentPage
                val pillWidth by animateDpAsState(
                    targetValue = if (index == currentPage) dimens.pillActiveWidth else dimens.pillInactiveWidth,
                    animationSpec = tween(durationMillis = 250),
                    label = "pill_width_$index",
                )
                val pillColor by animateColorAsState(
                    targetValue = if (isFilled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline,
                    animationSpec = tween(durationMillis = 250),
                    label = "pill_color_$index",
                )
                Box(
                    modifier = Modifier
                        .height(dimens.pillHeight)
                        .width(pillWidth)
                        .background(
                            color = pillColor,
                            shape = RoundedCornerShape(dimens.pillCornerRadius),
                        ),
                )
            }
        }

        val arcProgress by animateFloatAsState(
            targetValue = (currentPage + 1).toFloat() / pageCount.toFloat(),
            animationSpec = tween(durationMillis = 300),
            label = "arc_progress",
        )
        val primaryColor = MaterialTheme.colorScheme.primary

        Box(
            modifier = Modifier.size(dimens.buttonOuterSize),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(dimens.buttonOuterSize)) {
                val strokePx = dimens.buttonArcStroke.toPx()
                val inset = strokePx / 2f
                val arcRect = Size(size.width - strokePx, size.height - strokePx)
                val arcOffset = Offset(inset, inset)

                drawArc(
                    color = primaryColor.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = arcRect,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = arcProgress * 360f,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = arcRect,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }

            Box(
                modifier = Modifier
                    .size(dimens.buttonInnerSize)
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
}
