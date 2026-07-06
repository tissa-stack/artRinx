package com.rinx.artRINXapp.feature.settings.presentation.titleplan.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.settings.domain.model.PlanCardCta
import com.rinx.artRINXapp.feature.settings.domain.model.PlanOption

@Composable
fun PlanCard(
    plan: PlanOption,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cta: PlanCardCta = PlanCardCta.NONE,
    onCta: () -> Unit = {},
    // When true (pager usage) the card stretches to the pager's height and scrolls its content
    // internally so tall cards never clip on small screens / large font scales. Off by default so
    // stacked-card callers are unaffected.
    fillHeight: Boolean = false,
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) BrandPrimary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(200),
        label = "planBorder_${plan.id}",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
            // Lighter card surface so the card lifts off the screen background.
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(Spacing.md))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(Spacing.md),
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .then(if (fillHeight) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
    ) {
        // Header: crown + name + price
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = if (selected) BrandPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(Spacing.xl),
            )
            Spacer(Modifier.size(Spacing.sm))
            Text(
                text = plan.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = plan.price,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) BrandPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Spacing.md))

        // Feature list
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            plan.features.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Spacing.lg),
                    )
                    Spacer(Modifier.size(Spacing.sm))
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // ── CTA (handout §Plan card CTA behavior) ──────────────────────────────
        when (cta) {
            PlanCardCta.CURRENT_PLAN -> {
                Spacer(Modifier.height(Spacing.md))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        // Pill uses `surface` (not `surfaceVariant`) so it stays visible on the card fill.
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                ) {
                    Text(
                        text = "Current Plan",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            PlanCardCta.UPGRADE -> {
                Spacer(Modifier.height(Spacing.md))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(BrandPrimary)
                        .clickable(onClick = onCta)
                        .padding(vertical = Spacing.md),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Upgrade",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                // Required auto-renewable subscription disclosure (Play / App Store 3.1.3 equivalent).
                Text(
                    text = "${plan.price} · Auto-renews monthly until cancelled. " +
                        "By upgrading you agree to the Terms of Service and Privacy Policy.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PlanCardCta.MANAGED_ON_WEB -> {
                Spacer(Modifier.height(Spacing.md))
                // Anti-steering: account-management language only, no purchase verb / button.
                Text(
                    text = "Managed on artrinx.com",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PlanCardCta.NONE -> Unit
        }
    }
}