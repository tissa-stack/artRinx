package com.example.artrinx.feature.search.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.search.domain.model.ResultTab

@Composable
fun ResultTypeTabs(
    selectedTab: ResultTab,
    onTabSelected: (ResultTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        ResultTab.entries.forEach { tab ->
            ResultPillTab(
                label      = tab.label,
                isSelected = tab == selectedTab,
                onClick    = { onTabSelected(tab) },
            )
        }
    }
}

@Composable
private fun ResultPillTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) BrandPrimary else MaterialTheme.colorScheme.surfaceVariant,
        label       = "tab-bg",
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
        label       = "tab-text",
    )

    Box(
        modifier         = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color      = textColor,
        )
    }
}

private val ResultTab.label: String
    get() = when (this) {
        ResultTab.ART       -> "Art"
        ResultTab.USERS     -> "Users"
        ResultTab.CURATIONS -> "Curations"
    }
