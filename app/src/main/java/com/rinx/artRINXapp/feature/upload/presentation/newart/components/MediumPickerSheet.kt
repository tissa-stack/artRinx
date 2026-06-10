package com.rinx.artRINXapp.feature.upload.presentation.newart.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.upload.domain.model.MediumOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediumPickerSheet(
    mediums: List<MediumOption>,
    selectedMediumId: Int?,
    onMediumSelected: (MediumOption) -> Unit,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .navigationBarsPadding(),
        ) {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss() }
                    .padding(vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text       = "Medium",
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onBackground,
                    modifier   = Modifier.weight(1f),
                )
                Icon(
                    imageVector        = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onBackground,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            Spacer(Modifier.height(Spacing.xs))

            if (mediums.isEmpty()) {
                Text(
                    text     = "No mediums available",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Spacing.md),
                )
            }

            mediums.forEach { medium ->
                val isSelected = medium.id == selectedMediumId
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable { onMediumSelected(medium) }
                        .padding(vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text       = medium.title,
                        style      = MaterialTheme.typography.bodyMedium,
                        color      = MaterialTheme.colorScheme.onBackground,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier   = Modifier.weight(1f),
                    )
                    MediumRadioButton(selected = isSelected)
                }
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

/** Single-select indicator: only one medium can be chosen, so use a radio dot, not a checkbox. */
@Composable
private fun MediumRadioButton(selected: Boolean) {
    Box(
        modifier         = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .border(
                width = 1.5.dp,
                color = if (selected) BrandPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(BrandPrimary),
            )
        }
    }
}
