package com.rinx.artRINXapp.feature.profile.presentation.posttutorial

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.settings.domain.model.PlanCatalog
import com.rinx.artRINXapp.feature.settings.presentation.titleplan.components.PlanPager

/**
 * Informational plans screen shown once, immediately after the first-launch app tutorial. Lists the
 * plans available for the user's role; tapping "Continue" dismisses it and proceeds to Home (where the
 * notification-permission prompt then appears). This is NOT part of the signup wizard.
 */
@Composable
fun PostTutorialPlansScreen(
    onContinue: () -> Unit,
    viewModel: PostTutorialPlansViewModel = hiltViewModel(),
) {
    val roleName by viewModel.roleName.collectAsState()
    val dimens = LocalDimens.current
    val context = LocalContext.current

    // Finale gate — back shouldn't drop the user into the tour/profile tab behind it.
    BackHandler { }

    // A fresh post-signup user is never paid yet → Basic is the current plan; Artist Pro shows Upgrade.
    val plans = PlanCatalog.availablePlans(roleName)
    val currentPlanId = PlanCatalog.currentPlan(roleName, isPaid = false).id

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = dimens.screenPaddingHorizontal),
    ) {
        Spacer(Modifier.height(Spacing.xxl))
        Text(
            text = "You're all set!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "Here are the plans available for your ${roleName.ifBlank { "profile" }}. " +
                "You can change your plan anytime from Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.lg))

        PlanPager(
            plans = plans,
            currentPlanId = currentPlanId,
            role = roleName,
            isPaid = false,
            onCta = {
                Toast.makeText(context, "Subscriptions are coming soon.", Toast.LENGTH_SHORT).show()
            },
            // Both cards carry the blue border; the shared Continue button below is the only action,
            // so cards drop their per-card CTA here.
            highlightAll = true,
            showCta = false,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )

        Button(
            onClick = {
                viewModel.onContinue()
                onContinue()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.lg)
                .height(dimens.authButtonHeight),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(text = "Continue", style = MaterialTheme.typography.labelLarge)
        }
    }
}
