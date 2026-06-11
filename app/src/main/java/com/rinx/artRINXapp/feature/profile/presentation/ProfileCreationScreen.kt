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
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.rinx.artRINXapp.feature.settings.domain.model.PlanCatalog
import com.rinx.artRINXapp.feature.settings.presentation.titleplan.components.PlanCard
import com.rinx.artRINXapp.feature.profile.presentation.steps.GroundRulesDialog
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
            onGroundRulesCheckedChange = viewModel::onGroundRulesCheckedChange,
            onGroundRulesContinue = viewModel::onGroundRulesContinue,
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
            onAgeChange = viewModel::onAgeChange,
            onCountryChange = viewModel::onCountryChange,
            onStateChange = viewModel::onStateChange,
            onCityChange = viewModel::onCityChange,
            onPersonalInfoTooltipToggle = viewModel::onPersonalInfoTooltipToggle,
            onMediumToggle = viewModel::onMediumToggle,
            onMediumsTooltipToggle = viewModel::onMediumsTooltipToggle,
            onRetryMediums = viewModel::retryLoadMediums,
            onNextFromProfileTitle = viewModel::onNextFromProfileTitle,
            onNextFromProfileInfo = viewModel::onNextFromProfileInfo,
            onNextFromPersonalInfo = viewModel::onNextFromPersonalInfo,
            onSubmit = viewModel::onSubmit,
            onExploreRinx = viewModel::onExploreRinx,
            onBack = viewModel::onBack,
            onDismissError = viewModel::onDismissError,
        )
    }
}

@Composable
private fun ProfileCreationContent(
    uiState: ProfileCreationUiState,
    onGroundRulesCheckedChange: (Boolean) -> Unit,
    onGroundRulesContinue: () -> Unit,
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
    onAgeChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onStateChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onPersonalInfoTooltipToggle: () -> Unit,
    onMediumToggle: (Int) -> Unit,
    onMediumsTooltipToggle: () -> Unit,
    onRetryMediums: () -> Unit,
    onNextFromProfileTitle: () -> Unit,
    onNextFromProfileInfo: () -> Boolean,
    onNextFromPersonalInfo: () -> Boolean,
    onSubmit: (Uri?) -> Unit,
    onExploreRinx: () -> Unit,
    onBack: () -> Unit,
    onDismissError: () -> Unit,
) {
    val dimens = LocalDimens.current
    val snackbarHostState = remember { SnackbarHostState() }
    val currentStep = uiState.currentStep
    val totalSteps = 5
    val isDark = isSystemInDarkTheme()
    // Back is only meaningful within the data-entry steps (not the post-submit plan step).
    val canGoBack = currentStep in 1 until ProfileCreationViewModel.PLAN_STEP

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
        2 -> uiState.age.isNotBlank() && uiState.country.isNotBlank() &&
            uiState.state.isNotBlank() && uiState.city.isNotBlank()
        3 -> uiState.selectedMediumIds.size == ProfileCreationViewModel.REQUIRED_MEDIUM_COUNT
        4 -> true // informational plan step — always proceedable
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
                    contentDescription = "RiNX logo",
                    modifier = Modifier
                        .height(dimens.logoHeight)
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit,
                )
            }

            // ── Step content — fills all space between top bar and bottom bar ─
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = dimens.screenPaddingHorizontal),
            ) {
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
                        age = uiState.age,
                        country = uiState.country,
                        state = uiState.state,
                        city = uiState.city,
                        ageError = uiState.ageError,
                        countryError = uiState.countryError,
                        stateError = uiState.stateError,
                        cityError = uiState.cityError,
                        showTooltip = uiState.showPersonalInfoTooltip,
                        onAgeChange = onAgeChange,
                        onCountryChange = onCountryChange,
                        onStateChange = onStateChange,
                        onCityChange = onCityChange,
                        onTooltipToggle = onPersonalInfoTooltipToggle,
                        countryOptions = uiState.countryOptions,
                        stateOptions = uiState.stateOptions,
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
                    4 -> PlanInfoStep(
                        roleName = uiState.profileTypes
                            .firstOrNull { it.id == uiState.selectedProfileTypeId }?.name.orEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ── Bottom bar — stays above keyboard, never scrolls away ─────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimens.screenPaddingHorizontal,
                        vertical = Spacing.lg,
                    ),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Step-progress dots — filled circles, active = BrandPrimary
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimens.pillSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(totalSteps) { index ->
                        val isActive = index == currentStep
                        val dotColor by animateColorAsState(
                            targetValue = if (isActive) BrandPrimary
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

                // Action button
                Button(
                    onClick = {
                        when (currentStep) {
                            0 -> onNextFromProfileTitle()
                            1 -> onNextFromProfileInfo()
                            2 -> onNextFromPersonalInfo()
                            3 -> onSubmit(uiState.profilePictureUri)
                            4 -> onExploreRinx()
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
                        val label = if (currentStep == ProfileCreationViewModel.PLAN_STEP) "Explore RINX >" else "Continue"
                        Text(text = label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
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

        // Ground Rules dialog overlay
        if (uiState.showGroundRules) {
            GroundRulesDialog(
                checked = uiState.groundRulesChecked,
                onCheckedChange = onGroundRulesCheckedChange,
                onContinue = onGroundRulesContinue,
            )
        }
    }
}

/**
 * Step 5 (informational only — no API). Confirms the profile is set up and shows the plans available
 * for the picked role. Exit via the "Explore RINX" button (handout §Profile Setup Wizard, step 5).
 */
@Composable
private fun PlanInfoStep(
    roleName: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "You're all set!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Here are the plans available for your ${roleName.ifBlank { "profile" }}. " +
                "You can change your plan anytime from Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PlanCatalog.availablePlans(roleName).forEach { plan ->
            PlanCard(plan = plan, selected = false)
        }
        Spacer(Modifier.height(Spacing.lg))
    }
}
