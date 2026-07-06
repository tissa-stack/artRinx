package com.rinx.artRINXapp.core.tour

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.rinx.artRINXapp.core.navigation.NavRoutes

/**
 * Global tour host: rendered above the NavHost so the coaching overlay persists across the
 * Home→Search→Home walk. On each step it navigates to the step's destination (if not already
 * there), then draws the overlay. Does nothing until the tour is active.
 */
@Composable
fun TourHost(
    navController: NavHostController,
    viewModel: TourViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // When the first-launch tour finishes, show the post-tour Plans finale — but only for Artists.
    // Collector / Art Curious skip straight to Home (mirroring the finale's own Continue nav), so the
    // Home notification-permission prompt fires just the same. Fires once; the Plans screen clears
    // `plansPending` via markPlansShown() for Artists, and we clear it here for everyone else.
    LaunchedEffect(state.plansPending) {
        if (!state.plansPending) return@LaunchedEffect
        if (viewModel.isArtistUser()) {
            navController.navigate(NavRoutes.POST_TUTORIAL_PLANS) { launchSingleTop = true }
        } else {
            viewModel.markPlansShown()
            navController.navigate(NavRoutes.HOME) { popUpTo(0) { inclusive = true } }
        }
    }

    if (!state.active) return

    // Device back mirrors the in-card "‹ Back": step back through cards, and at the first card it's
    // a consumed no-op so back can't pop the tab out from under the overlay or exit mid-tour.
    BackHandler { viewModel.back() }

    val route = TourStep.ordered[state.step].route
    LaunchedEffect(state.step, route) {
        if (navController.currentDestination?.route != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    TourOverlay(
        stepIndex = state.step,
        targets = viewModel.bounds,
        onNext = viewModel::next,
        onBack = viewModel::back,
    )
}
