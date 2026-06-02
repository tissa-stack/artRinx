package com.example.artrinx.feature.upload.presentation.curation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.artrinx.R
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.upload.domain.model.ArtTab

@Composable
fun AddArtToCurationScreen(
    viewModel: NewCurationViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // ── Top bar ───────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.xs, end = Spacing.md,
                         top = Spacing.xs, bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter            = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint               = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text       = "Add to curation",
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onBackground,
                modifier   = Modifier.weight(1f),
            )
            Text(
                text     = "Done",
                style    = MaterialTheme.typography.labelLarge,
                color    = BrandPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(Spacing.md)
                    .clickable { onBack() },
            )
        }

        // ── Uploads / Liked tab bar ───────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth()) {
            ArtTab.entries.forEach { tab ->
                val isActive = tab == state.activeArtTab
                Column(
                    modifier            = Modifier
                        .weight(1f)
                        .clickable { viewModel.onTabSelected(tab) }
                        .padding(vertical = Spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text       = tab.label,
                        style      = MaterialTheme.typography.labelLarge,
                        color      = if (isActive) BrandPrimary
                                     else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    // Underline below each tab — visible only for active tab
                    Box(
                        modifier = Modifier
                            .padding(top = Spacing.sm)
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (isActive) BrandPrimary else Color.Transparent),
                    )
                }
            }
        }

        // ── 3-column art grid ─────────────────────────────────────────────
        LazyVerticalGrid(
            columns               = GridCells.Fixed(3),
            modifier              = Modifier.fillMaxSize(),
            verticalArrangement   = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(state.displayedArts, key = { it.id }) { art ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { viewModel.onToggleArtSelection(art) },
                ) {
                    AsyncImage(
                        model              = art.imageRes,
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize(),
                    )
                    if (art.isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                        )
                        Box(
                            modifier         = Modifier
                                .padding(6.dp)
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                                .background(BrandPrimary, CircleShape)
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Check,
                                contentDescription = null,
                                tint               = Color.White,
                                modifier           = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private val ArtTab.label: String
    get() = when (this) {
        ArtTab.UPLOADS -> "Uploads"
        ArtTab.LIKED   -> "Liked"
    }
