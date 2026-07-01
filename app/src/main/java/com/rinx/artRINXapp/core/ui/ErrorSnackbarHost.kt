package com.rinx.artRINXapp.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * App-standard error banner: a red, bottom-anchored [Snackbar] with a close (X) action, driven by a
 * nullable [message]. When [message] becomes non-null it's shown until dismissed (or the Long
 * timeout), then [onShown] is invoked so the caller can clear its error state — invoked only AFTER
 * dismissal, so the same message can't immediately re-trigger.
 *
 * Drop into a `Scaffold(snackbarHost = { ErrorSnackbarHost(uiState.error, viewModel::onErrorShown) })`.
 * Colors use the [error]/[onError] theme tokens (no hardcoded colors).
 */
@Composable
fun ErrorSnackbarHost(
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
    SnackbarHost(hostState, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            dismissActionContentColor = MaterialTheme.colorScheme.onError,
        )
    }
}
