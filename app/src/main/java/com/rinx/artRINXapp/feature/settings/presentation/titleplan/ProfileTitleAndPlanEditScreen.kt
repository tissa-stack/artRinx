package com.rinx.artRINXapp.feature.settings.presentation.titleplan

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.InactiveButton
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.settings.presentation.titleplan.components.PlanCard
import com.rinx.artRINXapp.feature.settings.presentation.titleplan.components.ProfileTitleCard

@Composable
fun ProfileTitleAndPlanEditScreen(
    initialStep: Int,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ProfileTitleAndPlanEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val dimens = LocalDimens.current

    LaunchedEffect(initialStep) { viewModel.setInitialStep(initialStep) }

    // Navigate back once the change is saved (or was a no-op).
    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.onSaveHandled()
            onSaved()
        }
    }

    // Step 1 back → step 0; step 0 back → exit
    BackHandler(enabled = state.step == 1) { viewModel.goToTitleStep() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xs),
        ) {
            IconButton(
                onClick = { if (state.step == 1) viewModel.goToTitleStep() else onBack() },
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = "Profile title and plan",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // ── Step content ──────────────────────────────────────────────────────────
        if (state.isLoading) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = BrandPrimary) }
            return@Column
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.screenPaddingHorizontal),
        ) {
            Spacer(Modifier.height(Spacing.lg))
            if (state.step == 0) {
                StepHeading(
                    title = "Profile title",
                    subtitle = "The title on your profile tells others how you use RINX. You can change this later in profile settings.",
                )
                Spacer(Modifier.height(Spacing.xl))
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    state.titles.forEach { option ->
                        ProfileTitleCard(
                            option = option,
                            selected = option.id == state.selectedTitleId,
                            expanded = option.id == state.selectedTitleId,
                            onClick = { viewModel.onTitleSelected(option.id) },
                        )
                    }
                }
            } else {
                StepHeading(
                    title = "Plan",
                    subtitle = "Choose our default basic plan for free, or upgrade to access premium features.",
                )
                Spacer(Modifier.height(Spacing.xl))
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    state.plans.forEach { plan ->
                        PlanCard(
                            plan = plan,
                            selected = plan.id == state.selectedPlanId,
                            onClick = { viewModel.onPlanSelected(plan.id) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.xxxl))
        }

        // ── Bottom: dots + action button ─────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = dimens.screenPaddingHorizontal, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.saveError != null) {
                Text(
                    text = state.saveError!!,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                )
            }
            val enabled = if (state.step == 0) state.canAdvanceTitle else state.canSavePlan
            Button(
                onClick = { if (state.step == 0) viewModel.goToPlanStep() else viewModel.onSave() },
                enabled = enabled,
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
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Spacing.xl),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = if (state.step == 0) "Next" else "Save changes",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            // 2-step dot indicator
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                repeat(2) { index ->
                    val active = index == state.step
                    val color by animateColorAsState(
                        targetValue = if (active) BrandPrimary else MaterialTheme.colorScheme.outline,
                        animationSpec = tween(250),
                        label = "stepDot_$index",
                    )
                    Box(
                        modifier = Modifier
                            .size(Spacing.sm)
                            .background(color, CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun StepHeading(title: String, subtitle: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(Spacing.xs))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}