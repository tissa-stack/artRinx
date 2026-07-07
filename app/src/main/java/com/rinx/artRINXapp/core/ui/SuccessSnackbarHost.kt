package com.rinx.artRINXapp.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.rinx.artRINXapp.core.theme.SuccessDark
import com.rinx.artRINXapp.core.theme.SuccessLight
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * App-standard success banner: a green, bottom-anchored [Snackbar] with a leading check icon and a
 * close (X) action, driven by a nullable [message]. When [message] becomes non-null it's shown until
 * dismissed (or the Long timeout), then [onShown] is invoked so the caller can clear its state —
 * invoked only AFTER dismissal, so the same message can't immediately re-trigger.
 *
 * Mirrors [ErrorSnackbarHost]; drop into a bottom-aligned overlay (or `Scaffold(snackbarHost = …)`).
 * Material3 `colorScheme` has no success slot, so the green comes directly from the [SuccessLight] /
 * [SuccessDark] theme tokens (per the app theme system).
 */
@Composable
fun SuccessSnackbarHost(
    message: String?,
    onShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        val m = message ?: return@LaunchedEffect
        hostState.showSnackbar(message = m, withDismissAction = true, duration = SnackbarDuration.Long)
        onShown()
    }
    val container = if (isSystemInDarkTheme()) SuccessDark else SuccessLight
    val content = Color.White
    SnackbarHost(hostState, modifier = modifier) { data ->
        Snackbar(
            containerColor = container,
            contentColor = content,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(Spacing.lg),
                )
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = content,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = Spacing.md),
                )
                IconButton(onClick = { data.dismiss() }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = content,
                        modifier = Modifier.size(Spacing.lg),
                    )
                }
            }
        }
    }
}
