package com.rinx.artRINXapp.feature.profile.presentation.steps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.profile.domain.model.Medium

private const val MAX_MEDIUMS = 3

@Composable
fun MediumSelectionStep(
    mediums: List<Medium>,
    isLoading: Boolean,
    error: String?,
    selectedMediumIds: Set<Int>,
    showTooltip: Boolean,
    onMediumToggle: (Int) -> Unit,
    onTooltipToggle: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalDimens.current
    val selectedCount = selectedMediumIds.size

    // Warm Coil's disk/memory cache for every medium image as soon as the list arrives, so the
    // circles are already decoded by the time the user scrolls — removes the visible load delay.
    val context = LocalContext.current
    LaunchedEffect(mediums) {
        val loader = context.imageLoader
        mediums.forEach { m ->
            if (m.pictureUrl.isNotBlank()) {
                loader.enqueue(ImageRequest.Builder(context).data(m.pictureUrl).build())
            }
        }
    }

    // No internal scroll — the host (onboarding flow / Change Medium screen) owns the scroll so the
    // info text + Get-Started button sit naturally at the bottom of one scroll.
    // The whole step is wrapped in a Box so the info tooltip can render as a layout-neutral
    // OVERLAY above the info row (see the AnimatedVisibility after the Column) instead of an
    // inline child that would grow the Column and push the CTA behind the fold.
    Box(modifier = modifier.fillMaxWidth()) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Spacer(Modifier.height(Spacing.xxl))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Select 3 Mediums",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$selectedCount/$MAX_MEDIUMS",
                style = MaterialTheme.typography.headlineSmall,
                color = if (selectedCount > 0) BrandPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Spacing.xs))

        Text(
            text = "We'll highlight art based on your preferences. You can edit this later in profile settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Spacing.xxl))

        when {
            isLoading -> {
                CircularProgressIndicator(
                    color = BrandPrimary,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(Spacing.xxxl))
            }
            error != null -> {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(Spacing.md))
                TextButton(onClick = onRetry) {
                    Text("Retry", color = BrandPrimary)
                }
                Spacer(Modifier.height(Spacing.xxxl))
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            // fixed height to prevent nested scroll conflict
                            (((mediums.size + 2) / 3) * (dimens.authButtonHeight.value * 2.5f)).dp,
                        ),
                ) {
                    items(mediums, key = { it.id }) { medium ->
                        MediumItem(
                            medium = medium,
                            selected = medium.id in selectedMediumIds,
                            disabled = selectedCount >= MAX_MEDIUMS && medium.id !in selectedMediumIds,
                            onToggle = { onMediumToggle(medium.id) },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))

        // ── Info link (at the bottom — the host scrolls, and the CTA sits just below this) ──────
        // The tooltip itself is rendered as an overlay after this Column (not inline here), so
        // toggling it never changes this Column's height.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs, Alignment.CenterHorizontally),
        ) {
            Text(
                text = "Why is my medium missing?",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(
                onClick = onTooltipToggle,
                modifier = Modifier.size(Spacing.xl),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_help),
                    contentDescription = "Medium info",
                    tint = if (showTooltip) BrandPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Spacing.lg),
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))
    }

        // ── Info tooltip overlay ────────────────────────────────────────────────
        // Floats ABOVE the info row without participating in the Column's layout, so the medium
        // grid, the info row, and the host's Save/Continue button stay put (no scroll needed).
        // Tail points down at the info button; bottom padding clears the info row + its Spacer.
        AnimatedVisibility(
            visible = showTooltip,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Spacing.xl + Spacing.lg),
        ) {
            InfoTooltip(
                text = "As our community expands, we'll be adding more. Let us know what you'd like to see: info@rinx.com",
                onClose = onTooltipToggle,
                tailAtBottom = true,
            )
        }
    }
}

@Composable
private fun MediumItem(
    medium: Medium,
    selected: Boolean,
    disabled: Boolean,
    onToggle: () -> Unit,
) {
    val dimens = LocalDimens.current
    val borderColor by animateColorAsState(
        targetValue = if (selected) BrandPrimary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(200),
        label = "mediumBorder",
    )
    val circleSize = dimens.authButtonHeight * 1.2f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = !disabled, onClick = onToggle),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .clip(CircleShape)
                    .border(
                        width = if (selected) 2.5.dp else 1.dp,
                        color = borderColor,
                        shape = CircleShape,
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                    ),
            ) {
                if (medium.pictureUrl.isNotBlank()) {
                    AsyncImage(
                        model = medium.pictureUrl,
                        contentDescription = medium.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }
                if (disabled && !selected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                CircleShape,
                            ),
                    )
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(circleSize * 0.35f)
                        .background(BrandPrimary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(Spacing.md),
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = medium.title,
            style = MaterialTheme.typography.labelSmall,
            color = if (disabled && !selected) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
