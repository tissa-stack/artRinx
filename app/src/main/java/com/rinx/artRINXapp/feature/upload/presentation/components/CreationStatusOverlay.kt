package com.rinx.artRINXapp.feature.upload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.upload.domain.model.CreationStatus

/**
 * Full-screen dimmed overlay shown on the create screen while a PRIVATE artwork/curation is being
 * created. Loading → spinner; Created → check + "find it in your profile" + Done; Failed → retry/close.
 * [label] customizes the noun ("artwork" / "curation").
 */
@Composable
fun CreationStatusOverlay(
    status: CreationStatus,
    label: String,
    onDone: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    error: String? = null,
    /** Override the success title (defaults to "$label created"). */
    createdTitle: String? = null,
    /** Override the success subtitle. */
    createdSubtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    val d = LocalDimens.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            // Swallow taps so the form behind can't be interacted with.
            .pointerInput(Unit) {},
        contentAlignment = Alignment.Center,
    ) {
        when (status) {
            CreationStatus.LOADING -> CircularProgressIndicator(
                color = BrandPrimary,
                modifier = Modifier.size(d.buttonOuterSize),
                strokeWidth = 3.dp,
            )

            CreationStatus.CREATED -> Column(
                modifier = Modifier
                    .padding(horizontal = Spacing.xl)
                    .clip(RoundedCornerShape(d.cardCornerRadius))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(Spacing.giant),
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = createdTitle ?: "$label created",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = createdSubtitle ?: "You can find it in your profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.lg))
                OverlayButton(text = "Done", filled = true, onClick = onDone)
            }

            CreationStatus.FAILED -> Column(
                modifier = Modifier
                    .padding(horizontal = Spacing.xl)
                    .clip(RoundedCornerShape(d.cardCornerRadius))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = error ?: "Couldn't create — please try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.lg))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    OverlayButton(text = "Close", filled = false, onClick = onDismiss)
                    OverlayButton(text = "Retry", filled = true, onClick = onRetry)
                }
            }
        }
    }
}

@Composable
private fun OverlayButton(text: String, filled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) BrandPrimary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = Spacing.xl, vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (filled) Color.White else MaterialTheme.colorScheme.onBackground,
        )
    }
}
