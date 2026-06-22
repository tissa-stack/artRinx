package com.rinx.artRINXapp.feature.home.presentation.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.core.ui.PagingFooter
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileCurationItem
import com.rinx.artRINXapp.feature.profile.presentation.view.components.ProfileCurationCard
import com.rinx.artRINXapp.feature.upload.domain.model.CurationSource
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToCurationSheet(
    source: CurationSource,
    onDismiss: () -> Unit,
    onCreateNew: () -> Unit,
    viewModel: AddToCurationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var query by remember { mutableStateOf("") }
    var pendingTarget by remember { mutableStateOf<ProfileCurationItem?>(null) }
    val gridScrollState = rememberScrollState()

    // Fixed sheet height (status-bar-safe) so it doesn't fluctuate with content — the grid scrolls inside.
    val sheetHeight = (LocalConfiguration.current.screenHeightDp * 0.8f).dp

    // Load the next page of curations when the grid is scrolled near its bottom. Disabled while
    // searching (the visible list is a client-side filter over the loaded pages, so a short filtered
    // list must not trigger endless next-page loads).
    LaunchedEffect(gridScrollState, query) {
        if (query.isNotBlank()) return@LaunchedEffect
        snapshotFlow { gridScrollState.maxValue > 0 && gridScrollState.value >= gridScrollState.maxValue - 200 }
            .distinctUntilChanged()
            .collect { nearBottom -> if (nearBottom) viewModel.loadMore() }
    }

    // Toast one-shot messages (errors keep the sheet open).
    LaunchedEffect(state.message) {
        state.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeMessage()
        }
    }
    // Once an add succeeds, show the success toast *then* close the sheet (one-shot event — safe to
    // reopen for the same art). Toast fires synchronously here, before onDismiss disposes the sheet,
    // so the success message is never dropped to the dismissal race.
    LaunchedEffect(Unit) {
        viewModel.closeSheet.collect { successMessage ->
            Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    val filtered = remember(state.curations, query) {
        val q = query.trim()
        if (q.isEmpty()) state.curations
        else state.curations.filter {
            it.title.contains(q, ignoreCase = true) || it.handle.contains(q, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight)
                .padding(horizontal = Spacing.lg)
                .navigationBarsPadding()
                // Tap anywhere outside the search field collapses the keyboard.
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
        ) {
            // ── Header: title + close ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Add to curation",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(Spacing.xl)
                        .clickable { onDismiss() },
                )
            }

            Spacer(Modifier.height(Spacing.sm))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(Spacing.md))

            // ── Create Curation (right-aligned) ───────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.prepareCreate(source, onCreateNew) }
                    .padding(vertical = Spacing.xs),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_create_curation),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(Spacing.xl),
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = "Create Curation",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textDecoration = TextDecoration.Underline,
                )
            }

            Spacer(Modifier.height(Spacing.md))

            // ── Search pill ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Spacing.xl),
                )
                Spacer(Modifier.width(Spacing.sm))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    cursorBrush = SolidColor(BrandPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search curations",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        }
                    },
                )
                if (query.isNotEmpty()) {
                    Spacer(Modifier.width(Spacing.xs))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(Spacing.xl)
                            .clickable { query = "" },
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // ── Curations grid / states — fills the remaining fixed height ──
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = BrandPrimary)
                    }

                    state.curations.isEmpty() -> Text(
                        text = "You have no curations yet. Create one to get started.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.xl),
                    )

                    filtered.isEmpty() -> Text(
                        text = "No curations match \"$query\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.xl),
                    )

                    else -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(gridScrollState),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        filtered.chunked(2).forEach { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                rowItems.forEach { item ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        ProfileCurationCard(
                                            item = item,
                                            onClick = {
                                                focusManager.clearFocus()
                                                pendingTarget = item
                                            },
                                        )
                                    }
                                }
                                // Keep the last odd card half-width.
                                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                        // Loader / network-error+Retry footer for the next page (only when not searching).
                        if (query.isBlank()) {
                            PagingFooter(state.paging, onRetry = viewModel::retryLoadMore)
                        }
                        Spacer(Modifier.height(Spacing.lg))
                    }
                }
            }
        }
    }

    // ── Confirm before adding ─────────────────────────────────────────────
    pendingTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingTarget = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Add to curation") },
            text = { Text("Add this to \"${target.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addTo(target, source)
                    pendingTarget = null
                }) { Text("Add", color = BrandPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { pendingTarget = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}
