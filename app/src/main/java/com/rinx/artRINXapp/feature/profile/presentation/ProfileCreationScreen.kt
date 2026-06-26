package com.rinx.artRINXapp.feature.profile.presentation

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.ArtRinxTheme
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.InactiveButton
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.profile.presentation.steps.MediumSelectionStep
import com.rinx.artRINXapp.feature.profile.presentation.steps.PersonalInfoStep
import com.rinx.artRINXapp.feature.profile.presentation.steps.ProfileInfoStep
import com.rinx.artRINXapp.feature.profile.presentation.steps.ProfileTitleStep

@Composable
fun ProfileCreationScreen(
    onNavigateToHome: () -> Unit,
    viewModel: ProfileCreationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.navigateToHome) {
        if (uiState.navigateToHome) {
            viewModel.onNavigatedToHome()
            onNavigateToHome()
        }
    }

    // Follow system theme — no forced dark mode
    ArtRinxTheme {
        ProfileCreationContent(
            uiState = uiState,
            onProfileTypeSelected = viewModel::onProfileTypeSelected,
            onRetryProfileTypes = viewModel::retryLoadProfileTypes,
            onAvatarTapped = viewModel::onAvatarTapped,
            onPictureSelected = viewModel::onProfilePictureSelected,
            onImageSourceSheetDismiss = viewModel::onImageSourceSheetDismiss,
            onFullNameChange = viewModel::onFullNameChange,
            onUsernameChange = viewModel::onUsernameChange,
            onDisplayNameChange = viewModel::onDisplayNameChange,
            onBioChange = viewModel::onBioChange,
            onFullNameTooltipToggle = viewModel::onFullNameTooltipToggle,
            onDisplayNameTooltipToggle = viewModel::onDisplayNameTooltipToggle,
            onDobChange = viewModel::onDobChange,
            onCountryQuery = viewModel::onCountryQuery,
            onCountrySelected = viewModel::onCountrySelected,
            onStateQuery = viewModel::onStateQuery,
            onStateSelected = viewModel::onStateSelected,
            onCityQuery = viewModel::onCityQuery,
            onCitySelected = viewModel::onCitySelected,
            onPersonalInfoTooltipToggle = viewModel::onPersonalInfoTooltipToggle,
            onMediumToggle = viewModel::onMediumToggle,
            onMediumsTooltipToggle = viewModel::onMediumsTooltipToggle,
            onRetryMediums = viewModel::retryLoadMediums,
            onNextFromProfileTitle = viewModel::onNextFromProfileTitle,
            onNextFromProfileInfo = viewModel::onNextFromProfileInfo,
            onNextFromPersonalInfo = viewModel::onNextFromPersonalInfo,
            onSubmit = viewModel::onSubmit,
            onBack = viewModel::onBack,
            onDismissError = viewModel::onDismissError,
        )
    }
}

@Composable
private fun ProfileCreationContent(
    uiState: ProfileCreationUiState,
    onProfileTypeSelected: (Int) -> Unit,
    onRetryProfileTypes: () -> Unit,
    onAvatarTapped: () -> Unit,
    onPictureSelected: (Uri?) -> Unit,
    onImageSourceSheetDismiss: () -> Unit,
    onFullNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onFullNameTooltipToggle: () -> Unit,
    onDisplayNameTooltipToggle: () -> Unit,
    onDobChange: (String) -> Unit,
    onCountryQuery: (String) -> Unit,
    onCountrySelected: (String) -> Unit,
    onStateQuery: (String) -> Unit,
    onStateSelected: (String) -> Unit,
    onCityQuery: (String) -> Unit,
    onCitySelected: (String) -> Unit,
    onPersonalInfoTooltipToggle: () -> Unit,
    onMediumToggle: (Int) -> Unit,
    onMediumsTooltipToggle: () -> Unit,
    onRetryMediums: () -> Unit,
    onNextFromProfileTitle: () -> Unit,
    onNextFromProfileInfo: () -> Boolean,
    onNextFromPersonalInfo: () -> Boolean,
    onSubmit: (Uri?) -> Unit,
    onBack: () -> Unit,
    onDismissError: () -> Unit,
) {
    val dimens = LocalDimens.current
    val snackbarHostState = remember { SnackbarHostState() }
    val currentStep = uiState.currentStep
    val totalSteps = 4
    val isDark = isSystemInDarkTheme()
    // Back is meaningful within the data-entry steps (0 is first, mediums is last).
    val canGoBack = currentStep in 1..ProfileCreationViewModel.LAST_STEP

    BackHandler(enabled = canGoBack) { onBack() }

    LaunchedEffect(uiState.submissionError) {
        uiState.submissionError?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onDismissError()
        }
    }

    val isContinueEnabled = when (currentStep) {
        0 -> uiState.selectedProfileTypeId != null
        1 -> uiState.fullName.isNotBlank() &&
            uiState.usernameCheckState is UsernameCheckState.Available &&
            uiState.displayName.isNotBlank()
        // Country/State/City are optional; only DOB is required. onNextFromPersonalInfo()
        // surfaces an inline error if a location field was typed but not picked from the catalog.
        2 -> uiState.dob.isNotBlank()
        3 -> uiState.selectedMediumIds.size == ProfileCreationViewModel.REQUIRED_MEDIUM_COUNT
        else -> false
    }

    val buttonColor by animateColorAsState(
        targetValue = if (isContinueEnabled) BrandPrimary else InactiveButton,
        animationSpec = tween(200),
        label = "continueButtonColor",
    )

    // Outer Box holds snackbar and dialog overlays
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Main column — imePadding shrinks it above the keyboard
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // ── Top bar ── no horizontal padding so back arrow reaches the edge ─
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimens.logoPaddingVertical),
            ) {
                if (canGoBack) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
                Image(
                    painter = painterResource(
                        if (isDark) R.drawable.artrinx_logo_dark_theme else R.drawable.artrinx_logo_light_theme,
                    ),
                    contentDescription = "artRINX logo",
                    modifier = Modifier
                        .height(dimens.logoHeight)
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit,
                )
            }

            // ── One scroll for the whole step. The column is forced to at least the viewport height
            //    and uses SpaceBetween so the content sits at the top and the button + progress dots
            //    sit at the BOTTOM of the screen (not pinned) — and everything scrolls when the
            //    content is taller than the viewport. ───────────────────────────────────────────
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .padding(horizontal = dimens.screenPaddingHorizontal),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  // ── Content group ──
                  Column(modifier = Modifier.fillMaxWidth()) {
                    when (currentStep) {
                    0 -> ProfileTitleStep(
                        profileTypes = uiState.profileTypes,
                        isLoading = uiState.profileTypesLoading,
                        error = uiState.profileTypesError,
                        selectedTypeId = uiState.selectedProfileTypeId,
                        onTypeSelected = onProfileTypeSelected,
                        onRetry = onRetryProfileTypes,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    1 -> ProfileInfoStep(
                        pictureUri = uiState.profilePictureUri,
                        googlePhotoUrl = uiState.googlePhotoUrl,
                        avatarPrefilling = uiState.avatarPrefilling,
                        showImageSourceSheet = uiState.showImageSourceSheet,
                        fullName = uiState.fullName,
                        username = uiState.username,
                        displayName = uiState.displayName,
                        bio = uiState.bio,
                        usernameCheckState = uiState.usernameCheckState,
                        fullNameError = uiState.fullNameError,
                        usernameError = uiState.usernameError,
                        displayNameError = uiState.displayNameError,
                        showFullNameTooltip = uiState.showFullNameTooltip,
                        showDisplayNameTooltip = uiState.showDisplayNameTooltip,
                        onAvatarTapped = onAvatarTapped,
                        onPictureSelected = onPictureSelected,
                        onImageSourceSheetDismiss = onImageSourceSheetDismiss,
                        onFullNameChange = onFullNameChange,
                        onUsernameChange = onUsernameChange,
                        onDisplayNameChange = onDisplayNameChange,
                        onBioChange = onBioChange,
                        onFullNameTooltipToggle = onFullNameTooltipToggle,
                        onDisplayNameTooltipToggle = onDisplayNameTooltipToggle,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    2 -> PersonalInfoStep(
                        dob = uiState.dob,
                        country = uiState.country,
                        state = uiState.state,
                        city = uiState.city,
                        dobError = uiState.dobError,
                        countryError = uiState.countryError,
                        stateError = uiState.stateError,
                        cityError = uiState.cityError,
                        showTooltip = uiState.showPersonalInfoTooltip,
                        onDobChange = onDobChange,
                        onCountryQuery = onCountryQuery,
                        onCountrySelected = onCountrySelected,
                        onStateQuery = onStateQuery,
                        onStateSelected = onStateSelected,
                        onCityQuery = onCityQuery,
                        onCitySelected = onCitySelected,
                        onTooltipToggle = onPersonalInfoTooltipToggle,
                        countryOptions = uiState.countryOptions,
                        stateOptions = uiState.stateOptions,
                        cityOptions = uiState.cityOptions,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    3 -> MediumSelectionStep(
                        mediums = uiState.mediums,
                        isLoading = uiState.mediumsLoading,
                        error = uiState.mediumsError,
                        selectedMediumIds = uiState.selectedMediumIds,
                        showTooltip = uiState.showMediumsTooltip,
                        onMediumToggle = onMediumToggle,
                        onTooltipToggle = onMediumsTooltipToggle,
                        onRetry = onRetryMediums,
                        modifier = Modifier.fillMaxWidth(),
                    )
                  }
                  }

                  // ── Footer group (button + dots) — pinned to the bottom via SpaceBetween ──
                  Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                  ) {
                Spacer(Modifier.height(Spacing.lg))

                // Action button. On the medium (last) step it's HIDDEN until 3 are selected, so
                // "Get Started" appears just below the info text once the selection is complete.
                // On earlier steps it's always shown (greyed until the step is valid).
                if (currentStep != ProfileCreationViewModel.LAST_STEP || isContinueEnabled) {
                    Button(
                        onClick = {
                            when (currentStep) {
                                0 -> onNextFromProfileTitle()
                                1 -> onNextFromProfileInfo()
                                2 -> onNextFromPersonalInfo()
                                3 -> onSubmit(uiState.profilePictureUri)
                            }
                        },
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimens.authButtonHeight),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonColor,
                            disabledContainerColor = InactiveButton,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                        ),
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(Spacing.xl),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = Spacing.xs / 2,
                            )
                        } else {
                            // Mediums is the last step → its CTA enters the app (and the tutorial).
                            val label = if (currentStep == ProfileCreationViewModel.LAST_STEP) "Get Started" else "Continue"
                            Text(text = label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Spacer(Modifier.height(Spacing.lg))
                }

                // Step-progress dots — filled circles, active = BrandPrimary. Shown below the button.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimens.pillSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(totalSteps) { index ->
                        // Progress style: every step up to and including the current one is filled, so
                        // going back/forward shows how far the user has progressed (not just one dot).
                        val reached = index <= currentStep
                        val dotColor by animateColorAsState(
                            targetValue = if (reached) BrandPrimary
                            else MaterialTheme.colorScheme.outline,
                            animationSpec = tween(250),
                            label = "dot_$index",
                        )
                        Box(
                            modifier = Modifier
                                .size(Spacing.sm)
                                .background(dotColor, CircleShape),
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.lg))
                  } // footer group
                } // scroll Column
            } // BoxWithConstraints
        }

        // Snackbar overlay
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding(),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

