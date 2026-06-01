package com.example.artrinx.core.navigation

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.artrinx.feature.auth.presentation.invite.InviteCodeScreen
import com.example.artrinx.feature.auth.presentation.login.LoginScreen
import com.example.artrinx.feature.auth.presentation.otp.OtpScreen
import com.example.artrinx.feature.profile.presentation.ProfileCreationScreen
import com.example.artrinx.feature.auth.presentation.signup.SignupScreen
import com.example.artrinx.feature.auth.presentation.waitlist.WaitlistScreen
import com.example.artrinx.feature.home.presentation.HomeScreen
import com.example.artrinx.feature.home.presentation.detail.ArtDetailScreen
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
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
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
                onNavigateToSignup = { inviteCode ->
                    navController.navigate(NavRoutes.signup(inviteCode))
                },
                onJoinWaitlist = { navController.navigate(NavRoutes.WAITLIST) },
                onNavigateToLogin = { navController.navigate(NavRoutes.LOGIN) },
            )
        }

        composable(NavRoutes.WAITLIST) {
            WaitlistScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.LOGIN) {
            LoginScreen(
                onBack = { navController.popBackStack() },
                onNavigateToOtp = { args -> navController.navigateToOtp(args) },
            )
        }

        composable(
            route = NavRoutes.SIGNUP,
            arguments = listOf(
                navArgument("inviteCode") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            SignupScreen(
                onBack = { navController.popBackStack() },
                onNavigateToOtp = { args -> navController.navigateToOtp(args) },
                onNavigateToLogin = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = NavRoutes.OTP,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("contactType") { type = NavType.StringType },
                navArgument("contactValue") { type = NavType.StringType },
                navArgument("inviteCode") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) {
            OtpScreen(
                onBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToProfileCompletion = {
                    navController.navigate(NavRoutes.PROFILE_COMPLETION) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(NavRoutes.PROFILE_COMPLETION) {
            ProfileCreationScreen(
                onNavigateToHome = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(NavRoutes.HOME) {
            HomeScreen(
                onNavigateToDetail = { postId ->
                    navController.navigate(NavRoutes.artDetail(postId))
                },
            )
        }

        composable(
            route = NavRoutes.ART_DETAIL,
            arguments = listOf(
                navArgument("postId") { type = NavType.StringType },
            ),
        ) {
            ArtDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { postId ->
                    navController.navigate(NavRoutes.artDetail(postId))
                },
                onNavigateHome = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}

private fun NavHostController.navigateToOtp(args: OtpArgs) {
    val encodedValue = Uri.encode(args.contactValue)
    navigate(
        "otp?mode=${args.mode}&contactType=${args.contactType}&contactValue=${encodedValue}&inviteCode=${Uri.encode(args.inviteCode)}"
    )
}
