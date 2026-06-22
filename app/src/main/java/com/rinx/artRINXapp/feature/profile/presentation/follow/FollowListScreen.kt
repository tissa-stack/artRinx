package com.rinx.artRINXapp.feature.profile.presentation.follow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.core.ui.PagingFooter
import com.rinx.artRINXapp.core.ui.ProfileHeaderTabsPager
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.RinxAvatar
import com.rinx.artRINXapp.feature.profile.domain.model.FollowUser
import com.rinx.artRINXapp.feature.search.presentation.components.SearchTopBar
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun FollowListScreen(
    onBack: () -> Unit,
    onOpenProfile: (Int) -> Unit,
    viewModel: FollowListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val dimens = LocalDimens.current
    val tabs = remember { listOf(FollowTab.FOLLOWERS, FollowTab.FOLLOWING) }

    // Swipeable pager synced two-way with the active tab (mirrors the profile screens).
    val pagerState = rememberPagerState(
        initialPage = tabs.indexOf(state.activeTab).coerceAtLeast(0),
    ) { tabs.size }
    // The pager is the single source of truth: taps AND swipes drive it directly (see FollowTabBar),
    // and we only mirror the settled page back into the VM. There is deliberately NO activeTab→pager
    // binding — that two-way loop is what can leave a tab "stuck" mid-switch when you tap quickly.
    LaunchedEffect(pagerState.settledPage) {
        val settled = tabs[pagerState.settledPage]
        if (settled != state.activeTab) viewModel.onTabSelected(settled)
    }

    // One scroll state per tab → the two lists scroll independently and keep their positions.
    val followersListState = rememberLazyListState()
    val followingListState = rememberLazyListState()

    // Infinite scroll for the active tab. Disabled while searching (the visible list is a client-side
    // filter over the loaded pages, so a short filtered list must not trigger endless next-page loads).
    LaunchedEffect(pagerState.currentPage, state.query) {
        if (state.query.isNotBlank()) return@LaunchedEffect
        val tab = tabs[pagerState.currentPage]
        val activeListState = if (pagerState.currentPage == 0) followersListState else followingListState
        snapshotFlow { activeListState.canScrollForward }.collect { canScroll ->
            if (!canScroll) viewModel.loadMore(tab)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // ── Pinned top bar (back stays reachable while the search header collapses) ─────────
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                // Driven by the pager (not VM state) so the title flips in lockstep with the swipe.
                text = if (pagerState.currentPage == 0) "Followers" else "Following",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Collapsing search header + pinned swipeable tabs over per-tab lists.
        ProfileHeaderTabsPager(
            pagerState = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            listStateFor = { page -> if (page == 0) followersListState else followingListState },
            header = {
                SearchTopBar(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    onClear = viewModel::onClearQuery,
                    onFocused = {},
                    placeholder = "Search",
                    modifier = Modifier.padding(
                        horizontal = dimens.screenPaddingHorizontal,
                        vertical = Spacing.sm,
                    ),
                )
            },
            tabBar = {
                FollowTabBar(
                    labels = listOf("Followers", "Following"),
                    pagerState = pagerState,
                )
            },
        ) { page ->
            val base = if (page == 0) state.followers else state.following
            val list = if (state.query.isBlank()) {
                base
            } else {
                base.filter {
                    it.name.contains(state.query, ignoreCase = true) ||
                        it.handle.contains(state.query, ignoreCase = true)
                }
            }
            when {
                state.isLoading -> item(key = "loading-$page") {
                    Box(
                        modifier = Modifier.fillParentMaxHeight(0.8f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(color = BrandPrimary) }
                }

                state.error != null -> item(key = "error-$page") {
                    Column(
                        modifier = Modifier
                            .fillParentMaxHeight(0.8f)
                            .fillMaxWidth()
                            .padding(horizontal = dimens.screenPaddingHorizontal),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = state.error!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(Spacing.md))
                        TextButton(onClick = viewModel::onRetry) {
                            Text("Retry", color = BrandPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                list.isEmpty() -> item(key = "empty-$page") {
                    Box(
                        modifier = Modifier.fillParentMaxHeight(0.8f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = when {
                                state.query.isNotBlank() -> "No results"
                                page == 0 -> "No followers yet"
                                else -> "Not following anyone yet"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    items(list, key = { it.userId }) { user ->
                        FollowUserRow(
                            user = user,
                            modifier = Modifier.padding(horizontal = dimens.screenPaddingHorizontal),
                            onClick = { onOpenProfile(user.userId) },
                        )
                    }
                    // Footer only on the unfiltered list (search filters the loaded set client-side).
                    if (state.query.isBlank()) {
                        item(key = "paging-footer-$page") {
                            PagingFooter(
                                state.pagingFor(tabs[page]),
                                onRetry = { viewModel.retryLoadMore(tabs[page]) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Two-tab pill bar with the same sliding-pill + draggable behaviour as [ProfileTabBar]/Home tabs:
 * a BrandPrimary pill tracks the [pagerState] position, the text cross-fades by proximity, and the
 * bar can be dragged horizontally to change tabs (the pager swipe does the same).
 */
@Composable
private fun FollowTabBar(
    labels: List<String>,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    val d = LocalDimens.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val tabCount = labels.size

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = d.screenPaddingHorizontal, vertical = Spacing.sm),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(d.tabPillHeight)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(Spacing.xs),
        ) {
            val cellWidth = maxWidth / tabCount
            val cellWidthPx = with(density) { cellWidth.toPx() }

            // Sliding pill behind the labels, positioned by the pager offset.
            Box(
                modifier = Modifier
                    .width(cellWidth)
                    .fillMaxHeight()
                    .offset {
                        val pos = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                            .coerceIn(0f, (tabCount - 1).toFloat())
                        IntOffset((pos * cellWidthPx).roundToInt(), 0)
                    }
                    .clip(RoundedCornerShape(50))
                    .background(BrandPrimary),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            val pageSize = pagerState.layoutInfo.pageSize.takeIf { it > 0 }
                                ?: return@rememberDraggableState
                            if (cellWidthPx <= 0f) return@rememberDraggableState
                            scope.launch { pagerState.scrollBy(delta * (pageSize / cellWidthPx)) }
                        },
                        onDragStopped = {
                            scope.launch {
                                val target = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                                    .roundToInt().coerceIn(0, tabCount - 1)
                                pagerState.animateScrollToPage(target)
                            }
                        },
                    ),
            ) {
                labels.forEachIndexed { index, label ->
                    val pos = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                        .coerceIn(0f, (tabCount - 1).toFloat())
                    val dist = abs(pos - index).coerceIn(0f, 1f)
                    val textColor = lerp(Color.White, MaterialTheme.colorScheme.onSurfaceVariant, dist)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            // Tap drives the pager directly; a new animation cleanly cancels any
                            // in-flight one (the pager's scroll mutex), so taps never get stuck.
                            .clickable { scope.launch { pagerState.animateScrollToPage(index) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowUserRow(
    user: FollowUser,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = LocalDimens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RinxAvatar(
            url = user.avatarUrl,
            contentDescription = user.name,
            size = d.avatarSize,
            name = user.name,
        )
        Spacer(Modifier.size(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (user.handle.isNotBlank()) {
                Text(
                    text = user.handle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
