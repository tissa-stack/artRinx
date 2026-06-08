package com.rinx.artRINXapp.feature.search.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rinx.artRINXapp.core.theme.Spacing

@Composable
fun TrendingTagsSection(
    tags: List<String>,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text       = "Trending Tags",
            style      = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.md))
        // Wrap tags into rows of 3 — avoids FlowRow API version issues
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            tags.chunked(3).forEach { rowTags ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    rowTags.forEach { tag ->
                        TrendingChip(label = tag, onClick = { onTagClick(tag) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingChip(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
