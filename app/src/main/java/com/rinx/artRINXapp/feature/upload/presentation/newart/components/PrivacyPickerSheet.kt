package com.rinx.artRINXapp.feature.upload.presentation.newart.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.upload.domain.model.PrivacyOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPickerSheet(
    selected: PrivacyOption,
    onPrivacySelected: (PrivacyOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .navigationBarsPadding(),
        ) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text       = "Who can see this?",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.md))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

            PrivacyRow(
                icon        = { Icon(painterResource(R.drawable.ic_globe), null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(Spacing.xxl)) },
                title       = "Public",
                subtitle    = "Visible to everyone on artRINX",
                isSelected  = selected == PrivacyOption.PUBLIC,
                onClick     = { onPrivacySelected(PrivacyOption.PUBLIC); onDismiss() },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            PrivacyRow(
                icon        = { Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(Spacing.xxl)) },
                title       = "Private",
                subtitle    = "Visible to just you",
                isSelected  = selected == PrivacyOption.PRIVATE,
                onClick     = { onPrivacySelected(PrivacyOption.PRIVATE); onDismiss() },
            )
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun PrivacyRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = title,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isSelected) {
            Icon(
                imageVector        = Icons.Default.Check,
                contentDescription = null,
                tint               = BrandPrimary,
                modifier           = Modifier.size(Spacing.xxl),
            )
        }
    }
}
