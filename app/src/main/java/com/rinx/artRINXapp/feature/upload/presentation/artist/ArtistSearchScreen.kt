package com.rinx.artRINXapp.feature.upload.presentation.artist

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.RinxAvatar
import com.rinx.artRINXapp.feature.upload.domain.model.ArtistResult
import com.rinx.artRINXapp.feature.upload.presentation.newart.NewArtViewModel

@Composable
fun ArtistSearchScreen(
    viewModel: NewArtViewModel,
    onBack: () -> Unit,
) {
    val state          by viewModel.state.collectAsState()
    val focusRequester = remember { FocusRequester() }

    val query = state.artistSearchQuery
    // Empty query → show "Myself" at the top; typing → show live search results.
    val results: List<ArtistResult> = if (query.isBlank()) {
        listOfNotNull(state.selfArtist)
    } else {
        state.artistResults
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // ── Header row: back + search pill ───────────────────────────────
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

            Row(
                modifier          = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector        = Icons.Default.Search,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(Spacing.xl),
                )
                Spacer(Modifier.width(Spacing.sm))
                BasicTextField(
                    value           = query,
                    onValueChange   = viewModel::onArtistSearchQueryChange,
                    modifier        = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    textStyle       = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    cursorBrush     = SolidColor(BrandPrimary),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    decorationBox   = { inner ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    text  = "Search artist",
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
                        imageVector        = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier
                            .size(Spacing.xl)
                            .clickable { viewModel.onArtistSearchQueryChange("") },
                    )
                }
            }
        }

        // ── Results list ──────────────────────────────────────────────────
        if (results.isNotEmpty()) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                // userId is nullable; falling back to handle could collide (two profile-less
                // artists sharing a handle) and crash. Synthesize a unique key from the index.
                itemsIndexed(results, key = { index, a -> a.userId?.toString() ?: "artist-$index" }) { _, artist ->
                    ArtistRow(
                        artist  = artist,
                        onClick = {
                            viewModel.onArtistSelected(artist)
                            onBack()
                        },
                    )
                }
            }
        } else if (query.isNotEmpty()) {
            EmptyState(
                query    = query,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.xl),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }

        // ── Add an artist without a RINX profile (name only, no id) ───────
        if (query.isNotBlank()) {
            AddWithoutProfileButton(
                name     = query.trim(),
                modifier = Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.md),
                onClick  = {
                    viewModel.onArtistWithoutProfile(query)
                    onBack()
                },
            )
            Spacer(Modifier.height(Spacing.md))
        }
    }
}

// ── Artist result row ─────────────────────────────────────────────────────────

@Composable
private fun ArtistRow(artist: ArtistResult, onClick: () -> Unit) {
    val d = LocalDimens.current
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RinxAvatar(
            url                = artist.avatarUrl,
            fallbackRes        = artist.avatarRes,
            contentDescription = artist.displayName,
            size               = d.avatarSizeLg,
            name               = artist.displayName,
        )

        Spacer(Modifier.width(Spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = artist.displayName,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onBackground,
                maxLines   = 1,
            )
            Text(
                text      = artist.subtitle,
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic,
                maxLines  = 1,
            )
        }
    }
}

// ── "Add artist without artRinx profile" button ──────────────────────────────────

@Composable
private fun AddWithoutProfileButton(name: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier         = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(BrandPrimary)
            .clickable { onClick() }
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = "Add \"$name\" without artRinx profile",
            style      = MaterialTheme.typography.labelLarge,
            color      = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines   = 1,
            textAlign  = TextAlign.Center,
        )
    }
}

// ── Empty state (no RINX match) ───────────────────────────────────────────────

@Composable
private fun EmptyState(query: String, modifier: Modifier = Modifier) {
    val d = LocalDimens.current
    Column(
        modifier            = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier         = Modifier
                .size(d.avatarSizeLg * 1.6f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Default.Person,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier           = Modifier.size(d.avatarSizeLg),
            )
        }
        Spacer(Modifier.height(Spacing.xl))
        Text(
            text      = "There are no profile results on artRinx for\n\"$query\"",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
