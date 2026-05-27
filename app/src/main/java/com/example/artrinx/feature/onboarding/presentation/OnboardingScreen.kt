package com.example.artrinx.feature.onboarding.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.artrinx.R
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.onboarding.presentation.components.ArtMosaicGrid
import com.example.artrinx.feature.onboarding.presentation.components.OnboardingControls

@Composable
fun OnboardingScreen(
    onNavigateToAuth: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { uiState.pages.size.coerceAtLeast(1) })
    val dimens = LocalDimens.current

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page -> viewModel.setPage(page) }
    }
    LaunchedEffect(uiState.currentPage) {
        if (pagerState.currentPage != uiState.currentPage) {
            pagerState.animateScrollToPage(uiState.currentPage)
        }
    }
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) onNavigateToAuth()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // ── FIXED: Logo ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.logoPaddingHorizontal, vertical = dimens.logoPaddingVertical),
            contentAlignment = Alignment.Center,
        ) {
            val isDark = isSystemInDarkTheme()
            Image(
                painter = painterResource(
                    if (isDark) R.drawable.ic_white_logo else R.drawable.ic_black_logo,
                ),
                contentDescription = "RiNX logo",
                modifier = Modifier
                    .height(dimens.logoHeight)
                    .aspectRatio(4f),
                contentScale = ContentScale.Fit,
            )
        }

        // ── SWIPEABLE: Grid + label + headline ────────────────────────────
        if (uiState.pages.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { pageIndex ->
                val page = uiState.pages[pageIndex]
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.weight(0.3f))

                    ArtMosaicGrid(
                        images = page.imageRes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimens.gridPaddingHorizontal),
                    )

                    Spacer(modifier = Modifier.weight(0.5f))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimens.screenPaddingHorizontal),
                    ) {
                        Text(
                            text = page.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = page.headline,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.weight(0.2f))
                }
            }
        }

        // ── FIXED: Progress indicators + next button ──────────────────────
        if (uiState.pages.isNotEmpty()) {
            OnboardingControls(
                pageCount = uiState.pages.size,
                currentPage = pagerState.currentPage,
                onNext = viewModel::nextPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.screenPaddingHorizontal)
                    .padding(bottom = dimens.screenPaddingBottom),
            )
        }
    }
}
