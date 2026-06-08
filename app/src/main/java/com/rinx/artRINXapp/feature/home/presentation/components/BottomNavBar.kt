package com.rinx.artRINXapp.feature.home.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing

private data class NavEntry(
    @DrawableRes val iconRes: Int,
    val label: String,
    val route: String,
)

private val startNavEntries = listOf(
    NavEntry(R.drawable.ic_navigation_home, "Home", "home"),
    NavEntry(R.drawable.ic_navigation_search, "Search", "search"),
)

private val endNavEntries = listOf(
    NavEntry(R.drawable.ic_navigation_bell, "Notifications", "notifications"),
    NavEntry(R.drawable.ic_navigation_profile, "Profile", "profile"),
)

@Composable
fun BottomNavBar(
    activeRoute: String,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit = {},
    onItemBounds: ((route: String, Rect) -> Unit)? = null,
) {
    val d = LocalDimens.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Icon row — always exactly bottomNavHeight tall ────────────
            // navigationBarsPadding() is NOT applied here to avoid squeezing
            // the icon area. System nav bar space is handled by the Spacer below.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(d.bottomNavHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                startNavEntries.forEach { entry ->
                    NavIconSlot(
                        entry = entry,
                        isActive = entry.route == activeRoute,
                        onNavigate = onNavigate,
                        onBounds = onItemBounds,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Centre FAB — always visible regardless of nav bar height
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(d.avatarSizeLg)
                            .clip(CircleShape)
                            .background(BrandPrimary)
                            .then(
                                if (onItemBounds != null) {
                                    Modifier.onGloballyPositioned { onItemBounds("create", it.boundsInWindow()) }
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { onNavigate("create") }
                            .semantics { role = Role.Button },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_navigation_plus),
                            contentDescription = "Create",
                            tint = Color.White,
                            modifier = Modifier.size(Spacing.xl),
                        )
                    }
                }

                endNavEntries.forEach { entry ->
                    NavIconSlot(
                        entry = entry,
                        isActive = entry.route == activeRoute,
                        onNavigate = onNavigate,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── System navigation bar spacer ──────────────────────────────
            // Expands to exactly the system navigation bar inset height so
            // the icon row is never pushed up or squeezed on any device.
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.navigationBars),
            )
        }
    }
}

@Composable
private fun NavIconSlot(
    entry: NavEntry,
    isActive: Boolean,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBounds: ((route: String, Rect) -> Unit)? = null,
) {
    val d = LocalDimens.current
    Box(
        modifier = modifier
            .height(d.bottomNavHeight)
            .then(
                if (onBounds != null) {
                    Modifier.onGloballyPositioned { onBounds(entry.route, it.boundsInWindow()) }
                } else {
                    Modifier
                },
            )
            .clickable { onNavigate(entry.route) }
            .padding(Spacing.xs)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(entry.iconRes),
            contentDescription = entry.label,
            tint = if (isActive) BrandPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Spacing.xxl),
        )
    }
}
