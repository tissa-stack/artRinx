package com.rinx.artRINXapp.feature.notifications.presentation.messages.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.window.Dialog
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.Spacing

@Composable
fun BlockedDialog(
    userName: String,
    userHandle: String,
    onDismiss: () -> Unit,
    onReportProfile: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape    = RoundedCornerShape(Spacing.xl),
            color    = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(Icons.Default.Close, "Close",
                            tint = MaterialTheme.colorScheme.onBackground)
                    }
                }

                Icon(
                    painter            = painterResource(R.drawable.ic_block),
                    contentDescription = null,
                    tint               = BrandPrimary,
                    modifier           = Modifier.size(Spacing.giant + Spacing.xxl),
                )
                Spacer(Modifier.height(Spacing.lg))

                Text("$userName is blocked.",
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onBackground,
                    textAlign  = TextAlign.Center)
                Spacer(Modifier.height(Spacing.md))

                Text(
                    text      = "At artRINX, we're committed to fostering a safe space to share art. You will no longer see content from this profile.",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.sm))

                Text(
                    text = buildAnnotatedString {
                        append("If you believe ")
                        withStyle(SpanStyle(color = BrandPrimary)) {
                            append("@$userHandle")
                        }
                        append(" has violated our ")
                        withStyle(SpanStyle(color = BrandPrimary)) {
                            append("community guidelines")
                        }
                        append(", please report them.")
                    },
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.xl))

                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(BrandPrimary)
                        .clickable { onReportProfile(); onDismiss() }
                        .padding(vertical = Spacing.md),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Report profile",
                        style      = MaterialTheme.typography.labelLarge,
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(Spacing.md))
            }
        }
    }
}
