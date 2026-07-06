package com.rinx.artRINXapp.feature.settings.presentation.titleplan.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import androidx.compose.material3.MaterialTheme
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.settings.domain.model.PlanCardCta
import com.rinx.artRINXapp.feature.settings.domain.model.PlanCatalog
import com.rinx.artRINXapp.feature.settings.domain.model.PlanOption
import kotlin.math.absoluteValue

/**
 * Horizontal, swipeable pager of equal-sized [PlanCard]s (one page per plan available for the
 * user's role). Cards fill the pager's height so both pages read as the same size.
 *
 * The pager occupies its height with `weight(1f)`, so the CALLER must give this composable a bounded
 * height — place it in a non-scrolling column with `Modifier.weight(1f)`.
 *
 * [highlightAll] draws the blue selection border on every card (a card style, not a picker state).
 * [showCta] toggles the per-card CTA (Upgrade button / "Current Plan" pill / disclosure): keep it in
 * the settings surfaces, but turn it off in the create flow where a single shared Continue button
 * drives the screen.
 */
@Composable
fun PlanPager(
    plans: List<PlanOption>,
    currentPlanId: String,
    role: String,
    isPaid: Boolean,
    onCta: (PlanOption) -> Unit,
    modifier: Modifier = Modifier,
    highlightAll: Boolean = false,
    showCta: Boolean = true,
) {
    if (plans.isEmpty()) return

    // Single plan (e.g. a non-artist's Basic-only Subscription): no pager / no full-height stretch —
    // render the one card at its natural content height so it doesn't balloon to fill the screen.
    if (plans.size == 1) {
        val plan = plans.first()
        Column(modifier = modifier.fillMaxWidth()) {
            PlanCard(
                plan = plan,
                selected = highlightAll,
                cta = if (showCta) PlanCatalog.ctaFor(plan.id, currentPlanId, role, isPaid) else PlanCardCta.NONE,
                onCta = { onCta(plan) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { plans.size })

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            // Wider side inset so a clear slice of the neighbouring card peeks in — signals the pager
            // is horizontally scrollable.
            contentPadding = PaddingValues(horizontal = Spacing.xxxl),
            pageSpacing = Spacing.sm,
            beyondViewportPageCount = 1,
        ) { page ->
            val plan = plans[page]
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            PlanCard(
                plan = plan,
                selected = highlightAll,
                cta = if (showCta) PlanCatalog.ctaFor(plan.id, currentPlanId, role, isPaid) else PlanCardCta.NONE,
                onCta = { onCta(plan) },
                fillHeight = true,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = Spacing.xs)
                    .graphicsLayer {
                        val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1f)
                        scaleX = lerp(0.96f, 1f, 1f - absOffset)
                        scaleY = lerp(0.96f, 1f, 1f - absOffset)
                        alpha = lerp(0.78f, 1f, 1f - absOffset)
                    },
            )
        }

        PlanPagerIndicator(pageCount = plans.size, currentPage = pagerState.currentPage)
    }
}

/** Simple dot indicator (mirrors the wizard's step dots). Hidden for a single-plan role. */
@Composable
private fun PlanPagerIndicator(pageCount: Int, currentPage: Int) {
    if (pageCount <= 1) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
    ) {
        repeat(pageCount) { index ->
            val active = index == currentPage
            val color by animateColorAsState(
                targetValue = if (active) BrandPrimary else MaterialTheme.colorScheme.outline,
                animationSpec = tween(250),
                label = "planDot_$index",
            )
            Box(
                modifier = Modifier
                    .size(Spacing.sm)
                    .background(color, CircleShape),
            )
        }
    }
}
