package com.rinx.artRINXapp.core.tour

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

    // When the first-launch tour finishes, show the post-tour Plans finale — regardless of which tab
    // the tour ended on. Fires once; the Plans screen clears `plansPending` via markPlansShown().
    LaunchedEffect(state.plansPending) {
        if (state.plansPending) {
            navController.navigate(NavRoutes.POST_TUTORIAL_PLANS) { launchSingleTop = true }
        }
    }

    if (!state.active) return

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
