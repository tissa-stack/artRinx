package com.example.artrinx.feature.notifications.presentation.messages.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.DarkCardSurface
import com.example.artrinx.core.theme.Spacing

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
fun ReportReasonSheet(
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
) {
    val sheetState      = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedReasons = remember { mutableStateListOf<String>() }
    val canSubmit       = selectedReasons.isNotEmpty()
    val btnBg by animateColorAsState(
        targetValue = if (canSubmit) BrandPrimary else DarkCardSurface,
        label       = "reportBtn",
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .navigationBarsPadding(),
        ) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text       = "Report profile",
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onBackground,
                    modifier   = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close",
                        tint = MaterialTheme.colorScheme.onBackground)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            Spacer(Modifier.height(Spacing.md))
            Text(
                text       = "Why are you reporting this profile?",
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.sm))

            REPORT_REASONS.forEach { reason ->
                val checked = reason in selectedReasons
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (checked) selectedReasons.remove(reason)
                            else selectedReasons.add(reason)
                        }
                        .padding(vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text     = reason,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    ReasonCheckbox(checked = checked)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }

            Spacer(Modifier.height(Spacing.lg))

            // Report button
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(btnBg)
                    .then(if (canSubmit) Modifier.clickable { onSubmit() } else Modifier)
                    .padding(vertical = Spacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = "Report",
                    style      = MaterialTheme.typography.labelLarge,
                    color      = Color.White.copy(alpha = if (canSubmit) 1f else 0.5f),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun ReasonCheckbox(checked: Boolean) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier         = Modifier
            .size(18.dp)
            .clip(shape)
            .background(if (checked) BrandPrimary else Color.Transparent)
            .then(
                if (!checked) Modifier.border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    shape = shape,
                ) else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector        = Icons.Default.Check,
                contentDescription = null,
                tint               = Color.White,
                modifier           = Modifier.size(12.dp),
            )
        }
    }
}
