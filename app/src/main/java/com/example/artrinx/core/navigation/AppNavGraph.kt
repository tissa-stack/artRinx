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
import com.example.artrinx.feature.home.presentation.detail.CurationDetailScreen
import com.example.artrinx.feature.onboarding.presentation.OnboardingScreen
import com.example.artrinx.feature.profile.presentation.view.UserProfileScreen
import com.example.artrinx.feature.create.presentation.CreateScreen
import com.example.artrinx.feature.search.presentation.SearchScreen
import com.example.artrinx.feature.upload.presentation.artist.ArtistSearchScreen
import com.example.artrinx.feature.upload.presentation.curation.AddArtToCurationScreen
import com.example.artrinx.feature.upload.presentation.curation.NewCurationScreen
import com.example.artrinx.feature.upload.presentation.curation.NewCurationViewModel
import com.example.artrinx.feature.upload.presentation.newart.NewArtPreviewScreen
import com.example.artrinx.feature.upload.presentation.newart.NewArtScreen
import com.example.artrinx.feature.upload.presentation.newart.NewArtViewModel
import com.example.artrinx.feature.upload.presentation.tags.AddTagsScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.remember

@Composable
fun AppNavGraph(
    startDestination: String,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController    = navController,
        startDestination = startDestination,
        modifier         = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        enterTransition    = { EnterTransition.None },
        exitTransition     = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition  = { ExitTransition.None },
    ) {

        // ── Auth / onboarding flow ────────────────────────────────────────────

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
                onNavigateToSignup  = { inviteCode -> navController.navigate(NavRoutes.signup(inviteCode)) },
                onJoinWaitlist      = { navController.navigate(NavRoutes.WAITLIST) },
                onNavigateToLogin   = { navController.navigate(NavRoutes.LOGIN) },
            )
        }

        composable(NavRoutes.WAITLIST) {
            WaitlistScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.LOGIN) {
            LoginScreen(
                onBack        = { navController.popBackStack() },
                onNavigateToOtp = { args -> navController.navigateToOtp(args) },
            )
        }

        composable(
            route     = NavRoutes.SIGNUP,
            arguments = listOf(
                navArgument("inviteCode") {
                    type         = NavType.StringType
                    nullable     = true
                    defaultValue = null
                },
            ),
        ) {
            SignupScreen(
                onBack            = { navController.popBackStack() },
                onNavigateToOtp   = { args -> navController.navigateToOtp(args) },
                onNavigateToLogin = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route     = NavRoutes.OTP,
            arguments = listOf(
                navArgument("mode")         { type = NavType.StringType },
                navArgument("contactType")  { type = NavType.StringType },
                navArgument("contactValue") { type = NavType.StringType },
                navArgument("inviteCode")   { type = NavType.StringType; defaultValue = "" },
            ),
        ) {
            OtpScreen(
                onBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(NavRoutes.HOME) { popUpTo(0) { inclusive = true } }
                },
                onNavigateToProfileCompletion = {
                    navController.navigate(NavRoutes.PROFILE_COMPLETION) { popUpTo(0) { inclusive = true } }
                },
            )
        }

        composable(NavRoutes.PROFILE_COMPLETION) {
            ProfileCreationScreen(
                onNavigateToHome = {
                    navController.navigate(NavRoutes.HOME) { popUpTo(0) { inclusive = true } }
                },
            )
        }

        // ── Main app tabs ─────────────────────────────────────────────────────

        composable(NavRoutes.HOME) {
            HomeScreen(
                onNavigateToSearch  = { navController.navigateToTab(NavRoutes.SEARCH) },
                onNavigateToCreate  = { navController.navigateToTab(NavRoutes.CREATE) },
                onNavigateToProfile = { navController.navigateToTab(NavRoutes.PROFILE) },
                onNavigateToDetail  = { postId ->
                    navController.navigate(NavRoutes.artDetail(postId, NavRoutes.HOME))
                },
                onNavigateToCurationDetail = { curationId ->
                    navController.navigate(NavRoutes.curationDetail(curationId, NavRoutes.HOME))
                },
            )
        }

        composable(NavRoutes.SEARCH) {
            SearchScreen(
                onNavigateToHome    = { navController.navigateToTab(NavRoutes.HOME) },
                onNavigateToCreate  = { navController.navigateToTab(NavRoutes.CREATE) },
                onNavigateToProfile = { navController.navigateToTab(NavRoutes.PROFILE) },
                onNavigateToDetail  = { postId ->
                    navController.navigate(NavRoutes.artDetail(postId, NavRoutes.SEARCH))
                },
            )
        }

        composable(NavRoutes.CREATE) {
            CreateScreen(
                onNavigateToHome      = { navController.navigateToTab(NavRoutes.HOME) },
                onNavigateToSearch    = { navController.navigateToTab(NavRoutes.SEARCH) },
                onNavigateToProfile   = { navController.navigateToTab(NavRoutes.PROFILE) },
                onNavigateToNewArt    = { uri -> navController.navigate(NavRoutes.newArt(uri)) },
                onNavigateToNewCuration = { navController.navigate(NavRoutes.NEW_CURATION) },
            )
        }

        // ── Upload art flow ───────────────────────────────────────────────────

        composable(
            route     = NavRoutes.NEW_ART,
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType }),
        ) { entry ->
            val rawUri = entry.arguments?.getString("imageUri") ?: ""
            val imageUri = if (rawUri.isNotEmpty()) android.net.Uri.parse(Uri.decode(rawUri)) else null
            NewArtScreen(
                imageUri             = imageUri,
                onBack               = { navController.popBackStack() },
                onNavigateToArtist   = { navController.navigate(NavRoutes.ARTIST_SEARCH) },
                onNavigateToTags     = { navController.navigate(NavRoutes.ADD_TAGS) },
                onNavigateToPreview  = { navController.navigate(NavRoutes.ART_PREVIEW) },
            )
        }

        composable(NavRoutes.ART_PREVIEW) { entry ->
            val parentEntry = remember(entry) { navController.getBackStackEntry(NavRoutes.NEW_ART) }
            val viewModel: NewArtViewModel = hiltViewModel(parentEntry)
            NewArtPreviewScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.ARTIST_SEARCH) { entry ->
            val parentEntry = remember(entry) { navController.getBackStackEntry(NavRoutes.NEW_ART) }
            val viewModel: NewArtViewModel = hiltViewModel(parentEntry)
            ArtistSearchScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.ADD_TAGS) { entry ->
            val parentEntry = remember(entry) { navController.getBackStackEntry(NavRoutes.NEW_ART) }
            val viewModel: NewArtViewModel = hiltViewModel(parentEntry)
            AddTagsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        // ── New curation flow ─────────────────────────────────────────────────

        composable(NavRoutes.NEW_CURATION) {
            NewCurationScreen(
                onBack             = { navController.popBackStack() },
                onNavigateToAddArt = { navController.navigate(NavRoutes.ADD_ART_TO_CURATION) },
            )
        }

        composable(NavRoutes.ADD_ART_TO_CURATION) { entry ->
            val parentEntry = remember(entry) { navController.getBackStackEntry(NavRoutes.NEW_CURATION) }
            val viewModel: NewCurationViewModel = hiltViewModel(parentEntry)
            AddArtToCurationScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.PROFILE) {
            UserProfileScreen(
                onNavigateToHome   = { navController.navigateToTab(NavRoutes.HOME) },
                onNavigateToSearch = { navController.navigateToTab(NavRoutes.SEARCH) },
                onNavigateToCreate = { navController.navigateToTab(NavRoutes.CREATE) },
                onNavigateToSettings = {},
            )
        }

        // ── Detail screens ────────────────────────────────────────────────────

        composable(
            route     = NavRoutes.ART_DETAIL,
            arguments = listOf(
                navArgument("postId") { type = NavType.StringType },
                navArgument("source") { type = NavType.StringType; defaultValue = NavRoutes.HOME },
            ),
        ) { backStackEntry ->
            val source = backStackEntry.arguments?.getString("source") ?: NavRoutes.HOME
            ArtDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { postId ->
                    navController.navigate(NavRoutes.artDetail(postId, source))
                },
                onNavigateHome      = { navController.navigateToTab(NavRoutes.HOME) },
                onNavigateToSearch  = { navController.navigateToTab(NavRoutes.SEARCH) },
                onNavigateToCreate  = { navController.navigateToTab(NavRoutes.CREATE) },
                onNavigateToProfile = { navController.navigateToTab(NavRoutes.PROFILE) },
                activeRoute = source,
            )
        }

        composable(
            route     = NavRoutes.CURATION_DETAIL,
            arguments = listOf(
                navArgument("curationId") { type = NavType.StringType },
                navArgument("source") { type = NavType.StringType; defaultValue = NavRoutes.HOME },
            ),
        ) { backStackEntry ->
            val source = backStackEntry.arguments?.getString("source") ?: NavRoutes.HOME
            CurationDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCuration = { curationId ->
                    navController.navigate(NavRoutes.curationDetail(curationId, source))
                },
                onNavigateHome      = { navController.navigateToTab(NavRoutes.HOME) },
                onNavigateToSearch  = { navController.navigateToTab(NavRoutes.SEARCH) },
                onNavigateToCreate  = { navController.navigateToTab(NavRoutes.CREATE) },
                onNavigateToProfile = { navController.navigateToTab(NavRoutes.PROFILE) },
                activeRoute = source,
            )
        }
    }
}

/**
 * Navigate to a root bottom-tab destination.
 *
 * Clears the entire back stack (with state saved) so every root tab is the
 * sole entry — pressing the system back button on any root tab closes the app.
 * `restoreState = true` brings back the tab's previous scroll/ViewModel state
 * when the user returns to it.
 */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.id) {
            saveState = true
            inclusive = false   // graph node itself stays; all destinations are cleared
        }
        launchSingleTop = true
        restoreState    = true
    }
}

private fun NavHostController.navigateToOtp(args: OtpArgs) {
    val encodedValue = Uri.encode(args.contactValue)
    navigate(
        "otp?mode=${args.mode}&contactType=${args.contactType}" +
        "&contactValue=$encodedValue&inviteCode=${Uri.encode(args.inviteCode)}"
    )
}
