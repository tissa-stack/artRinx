package com.example.artrinx.feature.home.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onSeeAll: (() -> Unit)? = null,
) {
    val d = LocalDimens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = Spacing.md,
                end = if (onSeeAll != null) Spacing.xs else Spacing.md,
                top = Spacing.lg,
                bottom = Spacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandPrimary,
                )
            }
        }
    }
}
