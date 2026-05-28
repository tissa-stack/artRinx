package com.example.artrinx.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.artrinx.feature.auth.presentation.invite.InviteCodeScreen
import com.example.artrinx.feature.auth.presentation.waitlist.WaitlistScreen
import com.example.artrinx.feature.onboarding.presentation.OnboardingScreen

@Composable
fun AppNavGraph(
    startDestination: String,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(NavRoutes.ONBOARDING) {
            OnboardingScreen(
                onNavigateToAuth = {
                    navController.navigate(NavRoutes.AUTH) {
                        popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.AUTH) {
            InviteCodeScreen(
                onNavigateToHome = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.AUTH) { inclusive = true }
                    }
                },
                onJoinWaitlist = { navController.navigate(NavRoutes.WAITLIST) },
                onNavigateToLogin = { /* TODO: Login flow */ },
            )
        }

        composable(NavRoutes.WAITLIST) {
            WaitlistScreen(onBack = { navController.popBackStack() })
        }
    }
}
