package com.example.artrinx.feature.create.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.artrinx.R
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.home.presentation.components.BottomNavBar

private val UploadArtColor     = Color(0xFF45B1E8)   // BrandPrimary
private val NewCollectionColor = Color(0xFF9C5CF8)   // Violet

@Composable
fun CreateScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
) {
    val d = LocalDimens.current

    Scaffold(
        bottomBar = {
            BottomNavBar(
                activeRoute = "create",
                onNavigate  = { route ->
                    when (route) {
                        "home"    -> onNavigateToHome()
                        "search"  -> onNavigateToSearch()
                        "profile" -> onNavigateToProfile()
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .padding(horizontal = Spacing.lg),
        ) {
            Spacer(Modifier.height(Spacing.xl))

            // ── Title ────────────────────────────────────────────────────
            Text(
                text       = "Create",
                style      = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(Spacing.xl))

            // ── Option cards ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                CreateOptionCard(
                    iconRes    = R.drawable.ic_upload_art,
                    iconColor  = UploadArtColor,
                    title      = "Upload Art",
                    subtitle   = "Share your work",
                    modifier   = Modifier.weight(1f),
                    onClick    = {},
                )
                Spacer(Modifier.width(Spacing.md))
                CreateOptionCard(
                    iconRes    = R.drawable.ic_create_curation,
                    iconColor  = NewCollectionColor,
                    title      = "New Collection",
                    subtitle   = "Curate pieces",
                    modifier   = Modifier.weight(1f),
                    onClick    = {},
                )
            }

            Spacer(Modifier.height(Spacing.md))

            // ── Upload limit card ─────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(d.cardCornerRadius))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = "Upload limit",
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text  = "Unlimited uploads",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier         = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, BrandPrimary, RoundedCornerShape(50))
                        .clickable { }
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = "Upgrade",
                        style      = MaterialTheme.typography.labelMedium,
                        color      = BrandPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

// ── Create option card ────────────────────────────────────────────────────────

@Composable
private fun CreateOptionCard(
    iconRes: Int,
    iconColor: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val d = LocalDimens.current

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(d.cardCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(Spacing.lg),
    ) {
        // Icon container
        Box(
            modifier         = Modifier
                .size(Spacing.giant)
                .clip(RoundedCornerShape(Spacing.sm))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter            = painterResource(iconRes),
                contentDescription = null,
                tint               = iconColor,
                modifier           = Modifier.size(Spacing.xxl),
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text       = title,
            style      = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text  = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
