package com.rinx.artRINXapp.feature.home.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.DarkCardSurface
import com.rinx.artRINXapp.core.theme.Spacing

private val REPORT_REASONS = listOf(
    "Violence or inciting violence",
    "Nudity or sexual activity",
    "Hate speech or symbols",
    "Bullying or harassment",
    "Account my have been hacked",
    "False information",
    "Scam or fraud",
    "Something else",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportBottomSheet(
    artTitle: String,
    profileName: String,
    onDismiss: () -> Unit,
    subjectLabel: String = "art",
    isReporting: Boolean = false,
    reportSent: Boolean = false,
    isBlocking: Boolean = false,
    onSubmitReport: (message: String) -> Unit = {},
    onBlockArt: (() -> Unit)? = null,
    onBlockUser: () -> Unit = {},
    onUnfollowUser: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedReason by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        // Phase is driven by the report result, not local state.
        if (!reportSent) {
            ReportForm(
                selectedReason = selectedReason,
                subjectLabel = subjectLabel,
                isReporting = isReporting,
                onSelectReason = { selectedReason = it },
                onSubmit = { selectedReason?.let { onSubmitReport(it) } },
            )
        } else {
            ReportSent(
                artTitle = artTitle,
                profileName = profileName,
                isBlocking = isBlocking,
                onBlockArt = onBlockArt,
                onBlockUser = onBlockUser,
                onUnfollowUser = onUnfollowUser,
                onDismiss = onDismiss,
            )
        }
    }
}

// ── Report form (step 1 → 2) ──────────────────────────────────────────────────

@Composable
private fun ReportForm(
    selectedReason: String?,
    subjectLabel: String,
    isReporting: Boolean,
    onSelectReason: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val canReport = selectedReason != null && !isReporting
    val buttonColor by animateColorAsState(
        targetValue = if (canReport) BrandPrimary else DarkCardSurface,
        label       = "reportButton",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Keep content below the status bar — a tall reason list expands the sheet to full
            // height, so without this the title draws under the status bar on some devices.
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg)
            .navigationBarsPadding(),
    ) {
        Text(
            text      = "Report this $subjectLabel",
            style     = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color     = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.md))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(Spacing.md))

        Text(
            text      = "Why are you reporting this $subjectLabel?",
            style     = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color     = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.sm))

        REPORT_REASONS.forEach { reason ->
            val selected = reason == selectedReason
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectReason(reason) }
                    .padding(vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text     = reason,
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                RadioButton(
                    selected = selected,
                    onClick  = { onSelectReason(reason) },
                    colors   = RadioButtonDefaults.colors(
                        selectedColor   = BrandPrimary,
                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        }

        Spacer(Modifier.height(Spacing.lg))

        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(buttonColor)
                .then(if (canReport) Modifier.clickable { onSubmit() } else Modifier)
                .padding(vertical = Spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = if (isReporting) "Reporting…" else "Report",
                style      = MaterialTheme.typography.labelLarge,
                color      = Color.White.copy(alpha = if (canReport) 1f else 0.5f),
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.height(Spacing.sm))

        Text(
            text      = "We will look into this matter further.",
            style     = MaterialTheme.typography.bodySmall,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Spacing.lg))
    }
}

// ── Report sent confirmation (step 3) ─────────────────────────────────────────

@Composable
private fun ReportSent(
    artTitle: String,
    profileName: String,
    isBlocking: Boolean,
    onBlockArt: (() -> Unit)?,
    onBlockUser: () -> Unit,
    onUnfollowUser: (() -> Unit)?,
    onDismiss: () -> Unit,
) {

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Spacing.xl)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick  = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    imageVector        = Icons.Default.Close,
                    contentDescription = "Close",
                    tint               = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        Icon(
            painter            = painterResource(R.drawable.ic_send),
            contentDescription = null,
            tint               = BrandPrimary,
            modifier           = Modifier.size(Spacing.giant + Spacing.xxl),
        )
        Spacer(Modifier.height(Spacing.lg))

        Text(
            text       = "Report sent",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onBackground,
            textAlign  = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.md))

        Text(
            text      = "Thank you for working to keep artRINX a safe space. We will look into this matter further.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))

        Text(
            text      = "In the meantime, you can block this art or all art from \"$profileName\"",
            style     = MaterialTheme.typography.bodySmall,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Spacing.xl))

        if (onBlockArt != null) {
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(BrandPrimary)
                    .clickable(enabled = !isBlocking, onClick = onBlockArt)
                    .padding(horizontal = Spacing.xl, vertical = Spacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = if (isBlocking) "Blocking…" else "Block \"$artTitle\"",
                    style      = MaterialTheme.typography.labelLarge,
                    color      = Color.White.copy(alpha = if (isBlocking) 0.6f else 1f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(Spacing.md))
        }

        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(BrandPrimary)
                .clickable(enabled = !isBlocking, onClick = onBlockUser)
                .padding(horizontal = Spacing.xl, vertical = Spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = if (isBlocking) "Blocking…" else "Block \"$profileName\"",
                style      = MaterialTheme.typography.labelLarge,
                color      = Color.White.copy(alpha = if (isBlocking) 0.6f else 1f),
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
        }

        if (onUnfollowUser != null) {
            Spacer(Modifier.height(Spacing.md))
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(BrandPrimary)
                    .clickable(enabled = !isBlocking, onClick = onUnfollowUser)
                    .padding(horizontal = Spacing.xl, vertical = Spacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = "Unfollow \"$profileName\"",
                    style      = MaterialTheme.typography.labelLarge,
                    color      = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.height(Spacing.xxl))
    }
}
