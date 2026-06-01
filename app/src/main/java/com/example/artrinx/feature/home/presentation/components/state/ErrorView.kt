package com.example.artrinx.feature.home.presentation.components.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SignalWifiOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.home.presentation.HomeError

@Composable
fun ErrorView(
    error: HomeError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (icon, title, subtitle) = when (error) {
        is HomeError.NoInternet -> Triple(
            Icons.Outlined.SignalWifiOff,
            "No internet connection",
            "Check your connection and try again.",
        )
        is HomeError.Generic -> Triple(
            Icons.Outlined.WarningAmber,
            "Something went wrong",
            error.message.ifBlank { "An unexpected error occurred." },
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xxl, horizontal = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Spacing.giant),
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.lg))
        OutlinedButton(onClick = onRetry) {
            Text(
                text = "Retry",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
