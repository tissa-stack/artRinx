package com.rinx.artRINXapp.feature.settings.presentation.titleplan

import android.widget.Toast
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Intent
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.InactiveButton
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.settings.domain.model.PlanCatalog
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
    val context = LocalContext.current

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { if (state.step == 1) viewModel.goToTitleStep() else onBack() },
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
                    subtitle = "The title on your profile tells others how you use artRINX. You can change this later in profile settings.",
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
                            // Not a radio group — CTAs drive actions (handout SelectPlanView).
                            // The user's active plan is highlighted with the blue selection border.
                            selected = plan.id == state.currentPlanId,
                            cta = PlanCatalog.ctaFor(plan.id, state.currentPlanId, state.role, state.isPaid),
                            // Stub until Play Billing lands (Play Console products + verify-google).
                            onCta = {
                                Toast.makeText(context, "Subscriptions are coming soon.", Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                }

                // Paywall footer (handout visibility matrix): artists only — Restore always,
                // Manage when subscribed. Collector / Art Curious / Gallery show nothing.
                if (state.role.contains("artist", ignoreCase = true)) {
                    Spacer(Modifier.height(Spacing.lg))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        PaywallAction("Restore Purchases") {
                            Toast.makeText(context, "No purchases to restore.", Toast.LENGTH_SHORT).show()
                        }
                        if (state.isPaid) {
                            Spacer(Modifier.width(Spacing.xl))
                            PaywallAction("Manage Subscription") {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            "https://play.google.com/store/account/subscriptions".toUri(),
                                        ),
                                    )
                                }
                            }
                        }
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
private fun PaywallAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = BrandPrimary,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.xs),
    )
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