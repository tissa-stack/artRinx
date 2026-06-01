package com.example.artrinx.feature.home.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import coil.compose.AsyncImage
import com.example.artrinx.R
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.home.presentation.HomeTab

@Composable
fun TopTabs(
    activeTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val d = LocalDimens.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(
                start = d.screenPaddingHorizontal,
                end = d.screenPaddingHorizontal,
                top = d.logoPaddingVertical,
                bottom = Spacing.sm,
            ),
    ) {
        // Logo centred
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = if (isDarkTheme) R.drawable.ic_white_logo else R.drawable.ic_black_logo,
                contentDescription = "ArtRinx",
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(d.logoHeight),
            )
        }

        // Tab container — outer pill with surfaceVariant background + 4dp internal padding
        // so the active pill visually floats inside, matching the reference
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(d.tabPillHeight)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(Spacing.xs),            // 4dp inset on all sides
        ) {
            HomeTab.entries.forEach { tab ->
                val isActive = tab == activeTab
                val bgColor by animateColorAsState(
                    targetValue = if (isActive) BrandPrimary else Color.Transparent,
                    animationSpec = tween(durationMillis = 220),
                    label = "tab-bg-${tab.name}",
                )
                val textColor by animateColorAsState(
                    targetValue = if (isActive) Color.White
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                    animationSpec = tween(durationMillis = 220),
                    label = "tab-text-${tab.name}",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()          // fills container height minus 4dp top+bottom inset
                        .clip(RoundedCornerShape(50))
                        .background(bgColor)
                        .clickable { onTabSelected(tab) }
                        .semantics {
                            role = Role.Tab
                            selected = isActive
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tab.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
