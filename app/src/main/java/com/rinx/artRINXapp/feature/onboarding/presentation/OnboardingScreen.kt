package com.rinx.artRINXapp.feature.onboarding.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.content.res.Configuration
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.onboarding.presentation.components.ArtMosaicGrid
import com.rinx.artRINXapp.feature.onboarding.presentation.components.AutoResizeText
import com.rinx.artRINXapp.feature.onboarding.presentation.components.OnboardingControls

@Composable
fun OnboardingScreen(
    onNavigateToAuth: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val dimens = LocalDimens.current
    // Portrait centers content with flexible weights; landscape is short, so make each page scroll
    // with fixed spacing instead — otherwise the grid + headline get clipped with no way to reach them.
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

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
                    if (isDark) R.drawable.artrinx_logo_dark_theme else R.drawable.artrinx_logo_light_theme,
                ),
                contentDescription = "artRINX logo",
                modifier = Modifier
                    .height(dimens.logoHeight),
                contentScale = ContentScale.Fit,
            )
        }

        // ── CAROUSEL: single currentPage driven by tap zones + swipe ──────
        // iOS model: tap left half = back, right half = forward; horizontal swipe
        // is kept as an Android convenience. No auto-advance.
        if (uiState.pages.isNotEmpty()) {
            val currentPage = uiState.currentPage
            val page = uiState.pages[currentPage]

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            if (offset.x < size.width / 2f) viewModel.previousPage()
                            else viewModel.nextPage()
                        }
                    }
                    .pointerInput(uiState.pages.size) {
                        var dragTotal = 0f
                        val threshold = 40.dp.toPx()
                        detectHorizontalDragGestures(
                            onDragStart = { dragTotal = 0f },
                            onDragEnd = {
                                if (dragTotal <= -threshold) viewModel.nextPage()
                                else if (dragTotal >= threshold) viewModel.previousPage()
                            },
                        ) { _, dragAmount -> dragTotal += dragAmount }
                    },
            ) {
                Column(
                    modifier = if (isLandscape) {
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    } else {
                        Modifier.fillMaxSize()
                    },
                ) {
                    if (isLandscape) Spacer(Modifier.height(Spacing.lg))
                    else Spacer(modifier = Modifier.weight(0.3f))

                    // Re-key on the page so the collage entrance animation replays each advance.
                    key(currentPage) {
                        ArtMosaicGrid(
                            images = page.imageRes,
                            cardSizes = page.cardSizes,
                            flipLayout = page.flipLayout,
                            landscapeHeightScale = page.landscapeHeightScale,
                            isSettled = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimens.gridPaddingHorizontal),
                        )
                    }

                    if (isLandscape) Spacer(Modifier.height(Spacing.xl))
                    else Spacer(modifier = Modifier.weight(0.5f))

                    // Text (title + subtitle): slide up from bottom + fade in on enter,
                    // fade only (no move) on exit — spring, matching iOS.
                    AnimatedContent(
                        targetState = currentPage,
                        transitionSpec = {
                            (slideInVertically(OnboardingAnim.textOffsetSpec()) { it } +
                                fadeIn(OnboardingAnim.textFloatSpec()))
                                .togetherWith(fadeOut(OnboardingAnim.textFloatSpec()))
                        },
                        label = "onboarding_text",
                    ) { targetPage ->
                        val textPage = uiState.pages[targetPage]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimens.screenPaddingHorizontal),
                        ) {
                            Text(
                                text = textPage.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            // Shrink-to-fit so the headline is never clipped on small/narrow screens or
                            // under large font scales — it scales down instead of ellipsizing.
                            AutoResizeText(
                                text = textPage.headline,
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    if (isLandscape) Spacer(Modifier.height(Spacing.lg))
                    else Spacer(modifier = Modifier.weight(0.2f))
                }
            }
        }

        // ── FIXED: Progress indicators + next button ──────────────────────
        if (uiState.pages.isNotEmpty()) {
            OnboardingControls(
                pageCount = uiState.pages.size,
                currentPage = uiState.currentPage,
                onNext = viewModel::nextPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.screenPaddingHorizontal)
                    .padding(bottom = dimens.screenPaddingBottom),
            )
        }
    }
}
