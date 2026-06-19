package com.rinx.artRINXapp.feature.upload.presentation.tags

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.upload.presentation.newart.NewArtViewModel

// Tag chips are always dark regardless of theme — intentional per design
private val TagChipBackground = Color(0xFF3D3D3D)

@Composable
fun AddTagsScreen(
    viewModel: NewArtViewModel,
    onBack: () -> Unit,
) {
    val state          by viewModel.state.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val focusManager   = LocalFocusManager.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Tap anywhere outside the input → drop focus and hide the keyboard.
            .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // ── Header: back + "Add tags" pill input ──────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.xs, end = Spacing.md,
                         top = Spacing.xs, bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter            = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint               = MaterialTheme.colorScheme.onBackground,
                )
            }

            // Pill input
            Box(
                modifier         = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value           = state.currentTagInput,
                    onValueChange   = viewModel::onTagInputChange,
                    modifier        = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle       = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    cursorBrush     = SolidColor(BrandPrimary),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        viewModel.onAddTag()
                        focusManager.clearFocus()
                    }),
                    decorationBox   = { inner ->
                        Box {
                            if (state.currentTagInput.isEmpty()) {
                                Text(
                                    text  = "Add tags",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
        }

        // ── Trending-tag suggestions (free-form; tap to add) ──────────────
        val suggestions = state.tagSuggestions.filter { it.lowercase() !in state.tags }
        if (suggestions.isNotEmpty()) {
            Text(
                text     = "Suggested",
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                suggestions.chunked(3).forEach { rowTags ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        rowTags.forEach { tag ->
                            SuggestionChip(label = tag, onClick = { viewModel.onSuggestedTagTap(tag) })
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.md))
        }

        // ── Tag chips area — wraps into rows of 3 ─────────────────────────
        if (state.tags.isNotEmpty()) {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.md, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                state.tags.chunked(3).forEach { rowTags ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        rowTags.forEach { tag ->
                            TagChip(
                                label    = tag,
                                onRemove = { viewModel.onRemoveTag(tag) },
                            )
                        }
                    }
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

// ── Suggestion chip (tap to add) ────────────────────────────────────────────

@Composable
private fun SuggestionChip(label: String, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

// ── Tag chip ──────────────────────────────────────────────────────────────────

@Composable
private fun TagChip(label: String, onRemove: () -> Unit) {
    Row(
        modifier          = Modifier
            .clip(RoundedCornerShape(50))
            .background(TagChipBackground)
            .padding(start = Spacing.md, end = Spacing.sm,
                     top = Spacing.sm, bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text       = label,
            style      = MaterialTheme.typography.bodySmall,
            color      = Color.White,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(Spacing.sm))
        Box(
            modifier         = Modifier
                .size(Spacing.xl)
                .clip(RoundedCornerShape(50))
                .clickable { onRemove() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Default.Close,
                contentDescription = "Remove $label",
                tint               = Color.White.copy(alpha = 0.7f),
                modifier           = Modifier.size(Spacing.md),
            )
        }
    }
}
