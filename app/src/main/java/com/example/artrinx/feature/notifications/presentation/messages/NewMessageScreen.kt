package com.example.artrinx.feature.notifications.presentation.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.artrinx.R
import com.example.artrinx.core.theme.BrandPrimary
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing
import com.example.artrinx.feature.notifications.domain.model.UserContact

@Composable
fun NewMessageScreen(
    onBack: () -> Unit,
    onUserSelected: (UserContact) -> Unit,
    viewModel: NewMessageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val query = state.query
    val d     = LocalDimens.current
    val filtered = state.results

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // ── Header ────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.xs, end = Spacing.md,
                         top = Spacing.xs, bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), "Back",
                    tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("New Message",
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onBackground,
                modifier   = Modifier.weight(1f),
                textAlign  = TextAlign.Center)
            Spacer(Modifier.width(Spacing.huge))
        }

        // ── Search pill ───────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_navigation_search), null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Spacing.xl))
            Spacer(Modifier.width(Spacing.sm))
            BasicTextField(
                value         = query,
                onValueChange = viewModel::onQueryChange,
                modifier      = Modifier.weight(1f),
                textStyle     = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground),
                cursorBrush   = SolidColor(BrandPrimary),
                singleLine    = true,
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) Text("Search user",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        inner()
                    }
                },
            )
        }

        // ── User list ─────────────────────────────────────────────────────
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filtered, key = { it.id }) { user ->
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable { onUserSelected(user) }
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val avatarModel: Any? = user.avatarUrl ?: user.avatarRes
                    if (avatarModel != null) {
                        AsyncImage(model = avatarModel, contentDescription = user.name,
                            contentScale = ContentScale.Crop,
                            modifier     = Modifier.size(d.avatarSizeLg).clip(CircleShape))
                    } else {
                        Box(
                            modifier         = Modifier
                                .size(d.avatarSizeLg)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Person, null,
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(d.avatarSizeLg * 0.6f))
                        }
                    }
                    Spacer(Modifier.width(Spacing.md))
                    Column {
                        Text(user.name,
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onBackground)
                        Text(user.handle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
