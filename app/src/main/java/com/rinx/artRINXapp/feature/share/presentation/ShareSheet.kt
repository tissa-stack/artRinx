package com.rinx.artRINXapp.feature.share.presentation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.core.util.shareEntity
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.RinxAvatar
import com.rinx.artRINXapp.feature.profile.domain.model.FollowUser
import com.rinx.artRINXapp.feature.share.domain.model.ShareTarget

/**
 * Bottom sheet that lets the user share [target] to selected followers (in-app, recipients get a
 * notification) and/or copy the link / open the system chooser. Mirrors the app's existing
 * ModalBottomSheet pattern (surface container, search pill, BrandPrimary action pill).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(
    target: ShareTarget,
    onDismiss: () -> Unit,
    viewModel: ShareViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<Int>()) }

    // Fixed, status-bar-safe height so the sheet doesn't jump as content/keyboard changes.
    val sheetHeight = (LocalConfiguration.current.screenHeightDp * 0.8f).dp

    LaunchedEffect(Unit) {
        viewModel.message.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    LaunchedEffect(Unit) {
        viewModel.closeSheet.collect { onDismiss() }
    }

    val filtered = remember(state.followers, query) {
        val q = query.trim()
        if (q.isEmpty()) state.followers
        else state.followers.filter {
            it.name.contains(q, ignoreCase = true) || it.handle.contains(q, ignoreCase = true)
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
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
        ) {
            // ── Header ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Share",
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

            // ── Search pill ─────────────────────────────────────────────────
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
                                    text = "Search",
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

            // ── Followers grid / states (fills the remaining height) ─────────
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(color = BrandPrimary) }

                    state.loadFailed -> CenteredHint(
                        text = "Couldn't load followers. Tap to retry.",
                        onClick = viewModel::retry,
                    )

                    state.followers.isEmpty() -> CenteredHint(text = "You have no followers yet.")

                    filtered.isEmpty() -> CenteredHint(text = "No matches for \"$query\".")

                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        items(filtered, key = { it.userId }) { follower ->
                            FollowerCell(
                                follower = follower,
                                selected = follower.userId in selected,
                                onToggle = {
                                    focusManager.clearFocus()
                                    selected = if (follower.userId in selected) {
                                        selected - follower.userId
                                    } else {
                                        selected + follower.userId
                                    }
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // ── Footer: Send (when selecting) + quick external actions ───────
            if (selected.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(BrandPrimary)
                        .clickable(enabled = !state.isSending) {
                            viewModel.send(target.kind, target.id, selected.toList())
                        }
                        .padding(vertical = Spacing.md),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isSending) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(Spacing.xl),
                        )
                    } else {
                        Text(
                            text = "Send (${selected.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.md))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                QuickAction(
                    iconRes = R.drawable.ic_copy,
                    label = "Copy link",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        clipboard.setText(AnnotatedString(target.webUrl))
                        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                    },
                )
                QuickAction(
                    iconRes = R.drawable.ic_send,
                    label = "Share to…",
                    modifier = Modifier.weight(1f),
                    onClick = { context.shareEntity(target.title, target.subtitle, target.webUrl) },
                )
            }
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

@Composable
private fun FollowerCell(
    follower: FollowUser,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val d = LocalDimens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Spacing.sm))
            .clickable { onToggle() }
            .padding(vertical = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            RinxAvatar(
                url = follower.avatarUrl,
                contentDescription = follower.name,
                size = d.avatarSizeLg,
                modifier = if (selected) {
                    Modifier.border(2.dp, BrandPrimary, CircleShape)
                } else {
                    Modifier
                },
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(d.avatarSizeLg)
                        .clip(CircleShape)
                        .background(BrandPrimary.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(d.avatarSizeLg * 0.5f),
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = follower.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun QuickAction(
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(50),
            )
            .clickable { onClick() }
            .padding(vertical = Spacing.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(Spacing.lg),
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
        )
    }
}

@Composable
private fun CenteredHint(text: String, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.xl),
        )
    }
}
