package com.rinx.artRINXapp.feature.settings.presentation.subscription

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.settings.presentation.titleplan.ProfileTitleAndPlanEditViewModel
import com.rinx.artRINXapp.feature.settings.presentation.titleplan.components.PlanPager

/**
 * Read-only Subscription screen split out of the old combined flow. Shows the available plans with
 * the user's current plan reflected in each card's CTA. Android has no in-app billing, so the CTAs
 * surface "coming soon" and management is on the web (Apple anti-steering / Play parity).
 */
@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    viewModel: ProfileTitleAndPlanEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val dimens = LocalDimens.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = "Subscription",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (state.isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPrimary)
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = dimens.screenPaddingHorizontal),
        ) {
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = "Plan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Choose our default basic plan for free, or upgrade to access premium features.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xl))

            PlanPager(
                plans = state.plans,
                currentPlanId = state.currentPlanId,
                role = state.role,
                isPaid = state.isPaid,
                onCta = {
                    Toast.makeText(context, "Subscriptions are coming soon.", Toast.LENGTH_SHORT).show()
                },
                // Both cards carry the blue border; per-card CTAs (Upgrade / Current Plan pill) stay.
                highlightAll = true,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )

            // Artists-only paywall footer (Restore always; Manage when subscribed → web).
            if (state.role.contains("artist", ignoreCase = true)) {
                Spacer(Modifier.height(Spacing.lg))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
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
            Spacer(Modifier.height(Spacing.lg))
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
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = Spacing.xs),
    )
}
