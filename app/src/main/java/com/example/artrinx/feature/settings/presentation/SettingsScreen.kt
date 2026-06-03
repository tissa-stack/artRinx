package com.example.artrinx.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.artrinx.R
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onChangeEmail: () -> Unit,
    onProfileTitleAndPlan: () -> Unit,
    onInviteFriends: () -> Unit,
    onBlockedAccounts: () -> Unit,
    onTermsAndConditions: () -> Unit,
    onFaqs: () -> Unit,
    onAboutUs: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onLogout: () -> Unit,
    @Suppress("UNUSED_PARAMETER") viewModel: SettingsViewModel = hiltViewModel(),
) {
    val dimens = LocalDimens.current

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
                .padding(end = dimens.screenPaddingHorizontal, top = Spacing.xs, bottom = Spacing.xs),
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
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // ── Scrollable section list ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("Account")
            SettingsRow(painter = R.drawable.ic_navigation_profile, label = "Edit profile", onClick = onEditProfile)
            SettingsRow(imageVector = Icons.Outlined.MailOutline, label = "Change email", onClick = onChangeEmail)
            SettingsRow(painter = R.drawable.ic_profile_title_and_plan, label = "Profile title and plan", onClick = onProfileTitleAndPlan)
            SettingsRow(painter = R.drawable.ic_invite_friends, label = "Invite Friends", onClick = onInviteFriends)

            SectionHeader("Privacy")
            SettingsRow(painter = R.drawable.ic_blocked_accounts, label = "Blocked accounts", onClick = onBlockedAccounts)

            SectionHeader("Resources")
            SettingsRow(painter = R.drawable.ic_terms_and_conditions, label = "Terms and conditions", onClick = onTermsAndConditions)
            SettingsRow(painter = R.drawable.ic_faq, label = "FAQS", onClick = onFaqs)
            SettingsRow(painter = R.drawable.ic_about_us, label = "About us", onClick = onAboutUs)
            SettingsRow(painter = R.drawable.ic_privacy_policy, label = "Privacy policy", onClick = onPrivacyPolicy)

            Spacer(Modifier.height(Spacing.xxxl))
        }

        // ── Logout pill ─────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = dimens.screenPaddingHorizontal, vertical = Spacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .border(1.dp, BrandPrimary, RoundedCornerShape(50))
                    .clickable(onClick = onLogout)
                    .padding(horizontal = Spacing.xxxl, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    text = "Logout",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandPrimary,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_logout),
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(Spacing.lg),
                )
            }
        }
    }
}

// ── Section header band ───────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    val dimens = LocalDimens.current
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = dimens.screenPaddingHorizontal, vertical = Spacing.md),
    )
}

// ── Single settings row ─────────────────────────────────────────────────────────

@Composable
private fun SettingsRow(
    label: String,
    onClick: () -> Unit,
    painter: Int? = null,
    imageVector: ImageVector? = null,
) {
    val dimens = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = dimens.screenPaddingHorizontal, vertical = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            painter != null -> Icon(
                painter = painterResource(painter),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(Spacing.xl),
            )
            imageVector != null -> Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(Spacing.xl),
            )
        }
        Spacer(Modifier.size(Spacing.lg))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.height(Spacing.lg),
        )
    }
}