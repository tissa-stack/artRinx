package com.rinx.artRINXapp.feature.settings.presentation.changemediums

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.profile.presentation.steps.MediumSelectionStep
import com.rinx.artRINXapp.feature.settings.presentation.editprofile.EditProfileViewModel

/**
 * Edit-profile "Change Medium" screen. It shares the [EditProfileViewModel] (scoped to the
 * EDIT_PROFILE back-stack entry), so the medium selection is held as a working copy and is only
 * persisted when the user taps Save on the Edit Profile screen — there is intentionally NO save
 * button here. Navigating back retains the edited selection (it lives in the shared VM).
 */
@Composable
fun ChangeMediumsScreen(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xs),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = "Change Medium",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // The step no longer scrolls internally (the onboarding flow owns its scroll), so provide
        // one here — the info text then sits at the bottom of the scroll, same as onboarding.
        MediumSelectionStep(
            mediums = state.mediums,
            isLoading = state.mediumsLoading,
            error = state.mediumsError,
            selectedMediumIds = state.selectedMediumIds,
            showTooltip = state.showMediumsTooltip,
            onMediumToggle = viewModel::onMediumToggle,
            onTooltipToggle = viewModel::onMediumsTooltipToggle,
            onRetry = viewModel::retryLoadMediums,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LocalDimens.current.screenPaddingHorizontal),
        )
    }
}
