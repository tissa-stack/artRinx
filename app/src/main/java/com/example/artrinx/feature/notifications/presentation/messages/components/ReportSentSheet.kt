package com.example.artrinx.feature.notifications.presentation.messages.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.artrinx.R
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportSentSheet(
    userName: String,
    onDismiss: () -> Unit,
    onBlock: () -> Unit,
    onUnfollow: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter            = painterResource(R.drawable.ic_send),
                contentDescription = null,
                tint               = BrandPrimary,
                modifier           = Modifier.size(Spacing.giant + Spacing.xxl),
            )
            Spacer(Modifier.height(Spacing.lg))
            Text("Report sent",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(Spacing.md))
            Text(
                text      = "Thank you for working to keep RINX a safe space. We will look into this matter further.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text      = "In the meantime, you can block this art or all art from \"$userName\"",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xl))

            // Block button
            ActionPill("Block this user", BrandPrimary) { onBlock(); onDismiss() }
            Spacer(Modifier.height(Spacing.md))
            // Unfollow button
            ActionPill("Unfollow this user", BrandPrimary) { onUnfollow(); onDismiss() }

            Spacer(Modifier.height(Spacing.md))
            Text(
                text  = "We will look into this matter further.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun ActionPill(label: String, bg: Color, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier         = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = Spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(label,
            style      = MaterialTheme.typography.labelLarge,
            color      = Color.White,
            fontWeight = FontWeight.SemiBold)
    }
}
