package com.rinx.artRINXapp.feature.settings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.InactiveButton
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * Confirmation for Settings → "Sign out of all devices" — revokes every session server-side and wipes
 * the local one. Distinct from the normal logout (single device).
 */
@Composable
fun SignOutAllDevicesDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(Spacing.xl),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_logout),
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(Spacing.giant),
                )
                Spacer(Modifier.height(Spacing.lg))

                Text(
                    text = "Sign out of all devices?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = "You'll be signed out of every device you've used, including this one. " +
                        "You'll need to sign in again on each.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.xl))

                PillButton(
                    text = "Sign out everywhere",
                    containerColor = BrandPrimary,
                    textColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = onConfirm,
                )
                Spacer(Modifier.height(Spacing.md))
                PillButton(
                    text = "Cancel",
                    containerColor = InactiveButton,
                    textColor = MaterialTheme.colorScheme.onSecondary,
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun PillButton(
    text: String,
    containerColor: Color,
    textColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
        )
    }
}
