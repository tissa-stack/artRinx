package com.rinx.artRINXapp.feature.settings.presentation

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
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.settings.presentation.components.DeleteAccountDialog
import com.rinx.artRINXapp.feature.settings.presentation.components.LogoutDialog

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onChangeEmail: () -> Unit,
    onChangePhone: () -> Unit,
    onProfileTitleAndPlan: () -> Unit,
    onInviteFriends: () -> Unit,
    onAppTutorial: () -> Unit,
    onBlockedAccounts: () -> Unit,
    onBlockedArtworks: () -> Unit,
    onPhonePermissions: () -> Unit,
    onTermsAndConditions: () -> Unit,
    onCommunityGuidelines: () -> Unit,
    onAboutUs: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val dimens = LocalDimens.current
    val state by viewModel.state.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Account deletion succeeded → drop to the auth flow (same exit as logout).
    LaunchedEffect(state.deleted) { if (state.deleted) onLogout() }
    // Shown when a phone-login user with no email taps "Change phone number": adding an email is
    // required first, so an email stays as a login/recovery path once phone login is retired.
    var showAddEmailPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(state.loggedOut) {
        if (state.loggedOut) onLogout()
    }

    // Re-check email presence on resume so the account row flips "Add email" → "Change email"
    // right after a phone-only user adds one and returns from the Change Email screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshContactState()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

        // ── Scrollable section list (logout + version scroll along, not anchored) ─
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader("Account")
            SettingsRow(painter = R.drawable.ic_navigation_profile, label = "Edit profile", onClick = onEditProfile)
            SettingsRow(
                imageVector = Icons.Outlined.MailOutline,
                label = if (state.hasEmail) "Change email" else "Add email",
                onClick = onChangeEmail,
            )
            // Phone change is offered only to accounts that have a phone (email-only users don't see it).
            // Require an email first so the user keeps a login/recovery path when phone login is retired.
            if (state.hasPhone) {
                SettingsRow(
                    imageVector = Icons.Outlined.PhoneAndroid,
                    label = "Change phone number",
                    onClick = { if (state.hasEmail) onChangePhone() else showAddEmailPrompt = true },
                )
            }
            SettingsRow(painter = R.drawable.ic_profile_title_and_plan, label = "Profile title and plan", onClick = onProfileTitleAndPlan)
            SettingsRow(painter = R.drawable.ic_invite_friends, label = "Invite Friends", onClick = onInviteFriends)
            SettingsRow(imageVector = Icons.Outlined.Info, label = "App tutorial", onClick = onAppTutorial)

            SectionHeader("Privacy")
            SettingsRow(painter = R.drawable.ic_blocked_accounts, label = "Blocked accounts", onClick = onBlockedAccounts)
            SettingsRow(imageVector = Icons.Outlined.Block, label = "Blocked artworks", onClick = onBlockedArtworks)
            SettingsRow(imageVector = Icons.Outlined.PhoneAndroid, label = "Phone Permissions", onClick = onPhonePermissions)
            SettingsRow(imageVector = Icons.Outlined.DeleteOutline, label = "Delete account", onClick = { showDeleteDialog = true })

            SectionHeader("Resources")
            SettingsRow(painter = R.drawable.ic_terms_and_conditions, label = "Terms and conditions", onClick = onTermsAndConditions)
            SettingsRow(painter = R.drawable.ic_guidelines, label = "Community Guidelines", onClick = onCommunityGuidelines)
            SettingsRow(painter = R.drawable.ic_about_us, label = "About us", onClick = onAboutUs)
            SettingsRow(painter = R.drawable.ic_privacy_policy, label = "Privacy policy", onClick = onPrivacyPolicy)

            Spacer(Modifier.height(Spacing.xxl))

            // ── Logout pill + app version (scroll along with the list) ──────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.screenPaddingHorizontal)
                    .padding(top = Spacing.lg, bottom = Spacing.md)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Row(
                    modifier = Modifier
                        .border(1.dp, BrandPrimary, RoundedCornerShape(50))
                        .clickable { showLogoutDialog = true }
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

                // App version (handout §9)
                Text(
                    text = "App Version ${state.appVersion}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showLogoutDialog) {
        LogoutDialog(
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout()
            },
            onDismiss = { showLogoutDialog = false },
        )
    }

    if (showDeleteDialog) {
        DeleteAccountDialog(
            onConfirm = { viewModel.deleteAccount() },
            onDismiss = {
                if (!state.isDeleting) {
                    showDeleteDialog = false
                    viewModel.onDeleteErrorShown()
                }
            },
            isDeleting = state.isDeleting,
            errorText = state.deleteError,
        )
    }

    if (showAddEmailPrompt) {
        AlertDialog(
            onDismissRequest = { showAddEmailPrompt = false },
            title = { Text("Add an email first") },
            text = {
                Text(
                    "Please add an email before changing your phone. It keeps you able to sign in " +
                        "and recover your account if phone sign-in is unavailable.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showAddEmailPrompt = false
                    onChangeEmail()
                }) { Text("Add email") }
            },
            dismissButton = {
                TextButton(onClick = { showAddEmailPrompt = false }) { Text("Not now") }
            },
        )
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