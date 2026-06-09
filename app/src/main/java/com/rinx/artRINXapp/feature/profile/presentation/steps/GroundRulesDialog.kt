package com.rinx.artRINXapp.feature.profile.presentation.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.util.LegalLinks
import com.rinx.artRINXapp.core.util.appendLegalLink
import com.rinx.artRINXapp.core.theme.InactiveButton
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * Ground Rules overlay — rendered directly on top of the profile-title step
 * (not a system Dialog) so it looks exactly like the reference design.
 */
@Composable
fun GroundRulesDialog(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
) {
    val dimens = LocalDimens.current

    // Scrim — full screen semi-transparent overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        // Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.screenPaddingHorizontal)
                .clip(RoundedCornerShape(Spacing.xl))
                .background(MaterialTheme.colorScheme.surface)
                .padding(Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Icon with circular brand-tinted background
            Box(
                modifier = Modifier
                    .size(dimens.authButtonHeight)
                    .clip(CircleShape)
                    .background(BrandPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_guidelines),
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(Spacing.xxxl),
                )
            }

            Spacer(Modifier.height(Spacing.lg))

            Text(
                text = "Ground Rules",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Spacing.sm))

            Text(
                text = "Thank you for keeping our community safe.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Spacing.xl))

            // Checkbox row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = BrandPrimary,
                        uncheckedColor = MaterialTheme.colorScheme.outline,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
                Text(
                    text = buildAnnotatedString {
                        val linkStyle = SpanStyle(color = BrandPrimary, fontWeight = FontWeight.Medium)
                        append("By continuing, I agree to the ")
                        appendLegalLink("Terms and Conditions", LegalLinks.TERMS_OF_USE, linkStyle)
                        append(", ")
                        appendLegalLink("Privacy Policy", LegalLinks.PRIVACY_POLICY, linkStyle)
                        append(" and ")
                        appendLegalLink("Community Guidelines", LegalLinks.COMMUNITY_GUIDELINES, linkStyle)
                        append(".")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
            }

            Spacer(Modifier.height(Spacing.xxl))

            Button(
                onClick = onContinue,
                enabled = checked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimens.authButtonHeight),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    disabledContainerColor = InactiveButton,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                ),
            ) {
                Text(text = "Continue", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
