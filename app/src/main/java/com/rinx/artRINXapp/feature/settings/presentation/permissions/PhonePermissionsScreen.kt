package com.rinx.artRINXapp.feature.settings.presentation.permissions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.InactiveButton
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.ToggleTrackOff
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.core.util.LegalLinks
import com.rinx.artRINXapp.core.util.appendLegalLink

@Composable
fun PhonePermissionsScreen(
    onBack: () -> Unit,
    viewModel: PhonePermissionsViewModel = hiltViewModel(),
) {
    val dimens = LocalDimens.current
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // ── Push-notification OS permission state ──────────────────────────────────
    // Pre-Android-13 there's no runtime permission → notifications are on by default.
    fun pushGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    var pushEnabled by remember { mutableStateOf(pushGranted()) }
    // Re-read on resume so returning from system settings reflects the new state.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) pushEnabled = pushGranted()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> pushEnabled = granted }

    fun openAppNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        runCatching { context.startActivity(intent) }
    }

    // Marketing confirm popup ("Enable/Disable marketing" before toggling).
    var pendingMarketing by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(state.error) {
        // Errors are reverted in the VM; nothing to surface beyond the switch snapping back.
        if (state.error != null) viewModel.consumeError()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
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
                text = "Phone Permissions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = dimens.screenPaddingHorizontal),
        )

        Spacer(Modifier.size(Spacing.lg))

        ToggleRow(
            title = "Marketing SMS Communications",
            subtitle = "Opt-in for events, promotions, and news. Reply HELP for help and STOP " +
                "to stop SMS at any time.",
            checked = state.marketingSmsConsent,
            enabled = !state.isLoading && !state.isSaving,
            onToggle = { desired -> pendingMarketing = desired },
        )

        ToggleRow(
            title = "Push Notifications",
            subtitle = "Opt-in to alerts for likes, messages, events, and updates.",
            checked = pushEnabled,
            enabled = true,
            onToggle = { desired ->
                when {
                    // Turning ON, pre-13 or already grantable → request the runtime permission.
                    desired && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !pushEnabled ->
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    // Turning OFF, or a permanently-denied re-enable → send the user to system settings.
                    else -> openAppNotificationSettings()
                }
            },
        )
    }

    pendingMarketing?.let { desired ->
        MarketingConsentDialog(
            enable = desired,
            onConfirm = {
                viewModel.setMarketingSmsConsent(desired)
                pendingMarketing = null
            },
            onCancel = { pendingMarketing = null },
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val dimens = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.screenPaddingHorizontal, vertical = Spacing.lg),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.size(Spacing.xs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(Spacing.md))
        Switch(
            checked = checked,
            onCheckedChange = { onToggle(it) },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BrandPrimary,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = ToggleTrackOff,
                uncheckedBorderColor = Color.Transparent,
                disabledCheckedThumbColor = Color.White,
                disabledCheckedTrackColor = BrandPrimary.copy(alpha = 0.5f),
                disabledUncheckedThumbColor = Color.White,
                disabledUncheckedTrackColor = ToggleTrackOff.copy(alpha = 0.5f),
            ),
        )
    }
}

/**
 * Branded confirm popup for the marketing-SMS toggle (Figma M/Popup-1 & M/Popup-2). Theme-aware
 * (surface + on-surface tokens) so it works in light and dark. Enable shows the full SMS-consent
 * disclosure with Privacy Policy / Terms links; Disable shows a short re-enable note below the
 * buttons. Renders in its own [Dialog] window (above tab/nav chrome).
 */
@Composable
private fun MarketingConsentDialog(
    enable: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val d = LocalDimens.current
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(d.cardCornerRadius),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(horizontal = Spacing.xl)
                .fillMaxWidth()
                .widthIn(max = d.eventPopupMaxWidth)
                .wrapContentHeight(),
        ) {
            Box(Modifier.fillMaxWidth().padding(Spacing.lg)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(Spacing.xl)
                        .clickable { onCancel() },
                )

                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_large_speaker),
                        contentDescription = null,
                        tint = BrandPrimary,
                        // Asset is 60×46 — keep that ratio so the megaphone isn't squished.
                        modifier = Modifier
                            .width(d.eventPopupIconSize)
                            .height(d.eventPopupIconSize * 46f / 60f),
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        text = if (enable) "Enable Marketing\nNotifications?" else "Disable Marketing\nNotifications?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )

                    if (enable) {
                        Spacer(Modifier.height(Spacing.md))
                        Text(
                            text = buildAnnotatedString {
                                append(
                                    "I consent to receive promotional marketing recurring SMS " +
                                        "communications from artRinx at the number provided. Message " +
                                        "frequency varies. Msg & data rates may apply. Reply HELP for " +
                                        "help and STOP to opt-out at any time. ",
                                )
                                val linkStyle = SpanStyle(
                                    color = BrandPrimary,
                                    textDecoration = TextDecoration.Underline,
                                )
                                appendLegalLink("Privacy Policy", LegalLinks.PRIVACY_POLICY, linkStyle)
                                append(" & ")
                                appendLegalLink("Terms and Conditions", LegalLinks.TERMS_OF_USE, linkStyle)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Spacer(Modifier.height(Spacing.lg))
                    PillButton(text = "Confirm", filled = true, onClick = onConfirm)
                    Spacer(Modifier.height(Spacing.sm))
                    PillButton(text = "Cancel", filled = false, onClick = onCancel)

                    if (!enable) {
                        Spacer(Modifier.height(Spacing.lg))
                        Text(
                            text = "You will no longer receive promotional messages via SMS from " +
                                "artRinx. You can re-enable this at any time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PillButton(text: String, filled: Boolean, onClick: () -> Unit) {
    val d = LocalDimens.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(d.authButtonHeight)
            .clip(RoundedCornerShape(50))
            .background(if (filled) BrandPrimary else InactiveButton)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}
