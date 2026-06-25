package com.rinx.artRINXapp.feature.settings.presentation.changemediums

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.InactiveButton
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.profile.presentation.steps.MediumSelectionStep
import com.rinx.artRINXapp.feature.settings.presentation.editprofile.EditProfileViewModel
import com.rinx.artRINXapp.feature.settings.presentation.editprofile.SaveStatus

/**
 * Edit-profile "Change Medium" screen. It shares the [EditProfileViewModel] (scoped to the
 * EDIT_PROFILE back-stack entry). Tapping Save here persists the selection on its own via
 * PUT /api/profile/mediums and returns; the selection also rides along on the overall profile Save
 * (preferred_medium_ids) when the user later saves the Edit Profile screen.
 */
@Composable
fun ChangeMediumsScreen(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel,
) {
    val state by viewModel.state.collectAsState()
    val dimens = LocalDimens.current

    // Standalone save succeeded → consume the one-time result and close the screen.
    LaunchedEffect(state.mediumsSaveStatus) {
        if (state.mediumsSaveStatus == SaveStatus.SAVED) {
            viewModel.onMediumsSaveHandled()
            onBack()
        }
    }

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
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenPaddingHorizontal),
        )

        // ── Save ──────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.screenPaddingHorizontal, vertical = Spacing.lg),
        ) {
            if (state.mediumsSaveError != null) {
                Text(
                    text = state.mediumsSaveError!!,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.sm),
                )
            }
            Button(
                onClick = viewModel::saveMediums,
                enabled = !state.mediumsSaving && state.selectedMediumIds.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.authButtonHeight),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    disabledContainerColor = InactiveButton,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                ),
            ) {
                if (state.mediumsSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Spacing.xl),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Save", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
