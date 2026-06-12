package com.rinx.artRINXapp.core.navigation

import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import com.rinx.artRINXapp.core.tour.TourHost
import com.rinx.artRINXapp.core.util.LegalLinks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rinx.artRINXapp.feature.auth.presentation.invite.InviteCodeScreen
import com.rinx.artRINXapp.feature.auth.presentation.login.LoginScreen
import com.rinx.artRINXapp.feature.auth.presentation.otp.OtpScreen
import com.rinx.artRINXapp.feature.profile.presentation.ProfileCreationScreen
import com.rinx.artRINXapp.feature.auth.presentation.signup.SignupScreen
import com.rinx.artRINXapp.feature.auth.presentation.waitlist.WaitlistScreen
import com.rinx.artRINXapp.feature.home.presentation.HomeScreen
import com.rinx.artRINXapp.feature.home.presentation.detail.ArtDetailScreen
import com.rinx.artRINXapp.feature.home.presentation.detail.CurationDetailScreen
import com.rinx.artRINXapp.feature.onboarding.presentation.OnboardingScreen
import com.rinx.artRINXapp.feature.profile.presentation.follow.FollowListScreen
import com.rinx.artRINXapp.feature.profile.presentation.artistarts.ArtByArtistScreen
import com.rinx.artRINXapp.feature.profile.presentation.other.OtherProfileScreen
import com.rinx.artRINXapp.feature.profile.presentation.view.UserProfileScreen
import com.rinx.artRINXapp.feature.create.presentation.CreateScreen
import com.rinx.artRINXapp.feature.notifications.presentation.NotificationsScreen
import com.rinx.artRINXapp.feature.notifications.presentation.messages.ChatScreen
import com.rinx.artRINXapp.feature.notifications.presentation.messages.NewMessageScreen
import com.rinx.artRINXapp.feature.search.presentation.SearchScreen
import com.rinx.artRINXapp.feature.settings.presentation.SettingsScreen
import com.rinx.artRINXapp.feature.settings.presentation.blocked.BlockedAccountsScreen
import com.rinx.artRINXapp.feature.settings.presentation.changeemail.ChangeEmailScreen
import com.rinx.artRINXapp.feature.settings.presentation.changephone.ChangePhoneScreen
import com.rinx.artRINXapp.feature.settings.presentation.editprofile.EditProfileScreen
import com.rinx.artRINXapp.feature.settings.presentation.permissions.PhonePermissionsScreen
import com.rinx.artRINXapp.feature.settings.presentation.invite.InviteFriendsScreen
import com.rinx.artRINXapp.feature.settings.presentation.titleplan.ProfileTitleAndPlanEditScreen
import com.rinx.artRINXapp.feature.settings.presentation.titleplan.ProfileTitleAndPlanScreen
import com.rinx.artRINXapp.feature.upload.presentation.artist.ArtistSearchScreen
import com.rinx.artRINXapp.feature.upload.presentation.curation.AddArtToCurationScreen
import com.rinx.artRINXapp.feature.upload.presentation.curation.NewCurationScreen
import com.rinx.artRINXapp.feature.upload.presentation.curation.NewCurationViewModel
import com.rinx.artRINXapp.feature.upload.presentation.newart.NewArtPreviewScreen
import com.rinx.artRINXapp.feature.upload.presentation.newart.NewArtScreen
import com.rinx.artRINXapp.feature.upload.presentation.newart.NewArtViewModel
import com.rinx.artRINXapp.feature.upload.presentation.tags.AddTagsScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.remember

@Composable
fun AppNavGraph(
    startDestination: String,
    deepLinkRouter: DeepLinkRouter,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    // Push-tap / universal-link routing. Invite codes are consumed by the invite screen itself;
    // here we only act on content routes and the gallery_enterprise_notice home-only case.
    val pendingDeepLink by deepLinkRouter.target.collectAsState()
    LaunchedEffect(pendingDeepLink) {
        when (val t = pendingDeepLink) {
            is DeepLinkTarget.Route -> {
                navController.navigate(t.navRoute); deepLinkRouter.consume()
            }
            is DeepLinkTarget.Event -> {
                // Park the id for NotificationsViewModel to fetch + popup, then switch tabs.
                deepLinkRouter.postEventId(t.id)
                navController.navigateToTab(NavRoutes.NOTIFICATIONS)
                deepLinkRouter.consume()
            }
            DeepLinkTarget.HomeOnly -> {
                navController.navigate(NavRoutes.HOME); deepLinkRouter.consume()
            }
            else -> Unit // Invite handled by InviteCodeViewModel; null = nothing pending.
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
    NavHost(
        navController    = navController,
        startDestination = startDestination,
        // Edge-to-edge is on, and no screen pads left/right. In landscape the display cutout and
        // 3-button nav bar move to the sides, so inset all content horizontally here (the full-bleed
        // background behind the bars stays via the Box above). Top/bottom stay per-screen.
        modifier         = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
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

        composable(NavRoutes.HOME) { entry ->
            val reselectTick by entry.savedStateHandle
                .getStateFlow(TAB_RESELECT_KEY, 0)
                .collectAsState()
            HomeScreen(
                reselectTick = reselectTick,
                onReselect                = { navController.navigateToTab(NavRoutes.HOME) },
                onNavigateToSearch        = { navController.navigateToTab(NavRoutes.SEARCH) },
                onNavigateToCreate        = { navController.navigateToTab(NavRoutes.CREATE) },
                onNavigateToNotifications = { navController.navigateToTab(NavRoutes.NOTIFICATIONS) },
                onNavigateToProfile       = { navController.navigateToTab(NavRoutes.PROFILE) },
                onNavigateToDetail  = { postId ->
                    navController.navigate(NavRoutes.artDetail(postId, NavRoutes.HOME))
                },
                onNavigateToCurationDetail = { curationId ->
                    navController.navigate(NavRoutes.curationDetail(curationId, NavRoutes.HOME))
                },
                onNavigateToNewCuration = { navController.navigate(NavRoutes.NEW_CURATION) },
                onOpenProfile = { id -> navController.navigate(NavRoutes.userProfile(id.toString(), NavRoutes.HOME)) },
            )
        }

        composable(NavRoutes.SEARCH) {
            SearchScreen(
                onNavigateToHome          = { navController.navigateToTab(NavRoutes.HOME) },
                onNavigateToCreate        = { navController.navigateToTab(NavRoutes.CREATE) },
                onNavigateToNotifications = { navController.navigateToTab(NavRoutes.NOTIFICATIONS) },
                onNavigateToProfile       = { navController.navigateToTab(NavRoutes.PROFILE) },
                onNavigateToDetail  = { postId ->
                    navController.navigate(NavRoutes.artDetail(postId, NavRoutes.SEARCH))
                },
                onNavigateToCurationDetail = { curationId ->
                    navController.navigate(NavRoutes.curationDetail(curationId, NavRoutes.SEARCH))
                },
                onOpenProfile = { userId ->
                    navController.navigate(NavRoutes.userProfile(userId, NavRoutes.SEARCH))
                },
            )
        }

        composable(NavRoutes.NOTIFICATIONS) {
            NotificationsScreen(
                onNavigateToHome       = { navController.navigateToTab(NavRoutes.HOME) },
                onNavigateToSearch     = { navController.navigateToTab(NavRoutes.SEARCH) },
                onNavigateToCreate     = { navController.navigateToTab(NavRoutes.CREATE) },
                onNavigateToProfile    = { navController.navigateToTab(NavRoutes.PROFILE) },
                onNavigateToChat       = { conv -> navController.navigate(NavRoutes.chat(conv.id)) },
                onNavigateToNewMessage = { navController.navigate(NavRoutes.NEW_MESSAGE) },
                onOpenUserProfile      = { id ->
                    navController.navigate(NavRoutes.userProfile(id.toString(), NavRoutes.NOTIFICATIONS))
                },
                onOpenArtDetail        = { id ->
                    navController.navigate(NavRoutes.artDetail(id.toString(), NavRoutes.NOTIFICATIONS))
                },
                onOpenCurationDetail   = { id ->
                    navController.navigate(NavRoutes.curationDetail(id.toString(), NavRoutes.NOTIFICATIONS))
                },
            )
        }

        composable(
            route     = NavRoutes.CHAT,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("source") { type = NavType.StringType; defaultValue = NavRoutes.NOTIFICATIONS },
            ),
        ) { entry ->
            val userId = entry.arguments?.getString("userId") ?: ""
            ChatScreen(
                userId        = userId,
                onBack        = { navController.popBackStack() },
                onViewProfile = { navController.navigate(NavRoutes.userProfile(userId, NavRoutes.NOTIFICATIONS)) },
                onChatDeleted = { navController.popBackStack() },
                onBlocked     = { navController.popBackStack() },
                onOpenArtwork = { artworkId ->
                    navController.navigate(NavRoutes.artDetail(artworkId.toString(), NavRoutes.NOTIFICATIONS))
                },
            )
        }

        composable(NavRoutes.NEW_MESSAGE) {
            NewMessageScreen(
                onBack         = { navController.popBackStack() },
                onUserSelected = { user -> navController.navigate(NavRoutes.chat(user.id)) },
            )
        }


        composable(NavRoutes.CREATE) {
            CreateScreen(
                onNavigateToHome      = { navController.navigateToTab(NavRoutes.HOME) },
                onNavigateToSearch    = { navController.navigateToTab(NavRoutes.SEARCH) },
                onNavigateToNotifications = { navController.navigateToTab(NavRoutes.NOTIFICATIONS) },
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
            val imageUri = if (rawUri.isNotEmpty() && rawUri != NavRoutes.NEW_ART_EDIT_SENTINEL) {
                android.net.Uri.parse(Uri.decode(rawUri))
            } else null
            NewArtScreen(
                imageUri             = imageUri,
                onBack               = { navController.popBackStack() },
                onNavigateToArtist   = { navController.navigate(NavRoutes.ARTIST_SEARCH) },
                onNavigateToTags     = { navController.navigate(NavRoutes.ADD_TAGS) },
                onUploadStarted      = { isPrivate -> navController.navigateAfterUpload(isPrivate) },
                onNavigateToPreview  = { navController.navigate(NavRoutes.ART_PREVIEW) },
                // Edit done → close the New Art screen AND the underlying detail so the user
                // lands on the (auto-refreshing) screen behind it with the change reflected.
                onEditDone           = { navController.popBackStack(NavRoutes.ART_DETAIL, inclusive = true) },
            )
        }

        composable(NavRoutes.ART_PREVIEW) { entry ->
            val parentEntry = remember(entry) { navController.getBackStackEntry(NavRoutes.NEW_ART) }
            val viewModel: NewArtViewModel = hiltViewModel(parentEntry)
            NewArtPreviewScreen(
                viewModel       = viewModel,
                onBack          = { navController.popBackStack() },
                onUploadStarted = { isPrivate -> navController.navigateAfterUpload(isPrivate) },
            )
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
                onCreateStarted    = { isPrivate -> navController.navigateAfterUpload(isPrivate) },
                // Edit done → close New Curation AND the underlying curation detail.
                onEditDone         = { navController.popBackStack(NavRoutes.CURATION_DETAIL, inclusive = true) },
            )
        }

        composable(NavRoutes.ADD_ART_TO_CURATION) { entry ->
            val parentEntry = remember(entry) { navController.getBackStackEntry(NavRoutes.NEW_CURATION) }
            val viewModel: NewCurationViewModel = hiltViewModel(parentEntry)
            AddArtToCurationScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.PROFILE) {
            UserProfileScreen(
                onNavigateToHome          = { navController.navigateToTab(NavRoutes.HOME) },
                onNavigateToSearch        = { navController.navigateToTab(NavRoutes.SEARCH) },
                onNavigateToCreate        = { navController.navigateToTab(NavRoutes.CREATE) },
                onNavigateToNotifications = { navController.navigateToTab(NavRoutes.NOTIFICATIONS) },
                onNavigateToSettings      = { navController.navigate(NavRoutes.SETTINGS) },
                onNavigateToInviteFriends = { navController.navigate(NavRoutes.INVITE_FRIENDS) },
                onNavigateToDetail        = { id -> navController.navigate(NavRoutes.artDetail(id, NavRoutes.PROFILE)) },
                onNavigateToCurationDetail = { id -> navController.navigate(NavRoutes.curationDetail(id, NavRoutes.PROFILE)) },
                onOpenFollowers           = { navController.navigate(NavRoutes.followList("followers")) },
                onOpenFollowing           = { navController.navigate(NavRoutes.followList("following")) },
            )
        }

        composable(
            route     = NavRoutes.USER_PROFILE,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("source") { type = NavType.StringType; defaultValue = NavRoutes.HOME },
            ),
        ) { entry ->
            val source = entry.arguments?.getString("source") ?: NavRoutes.HOME
            OtherProfileScreen(
                onBack                    = { navController.popBackStack() },
                onNavigateToDetail        = { id -> navController.navigate(NavRoutes.artDetail(id, source)) },
                onNavigateToCurationDetail = { id -> navController.navigate(NavRoutes.curationDetail(id, source)) },
                onMessage                 = { uid -> navController.navigate(NavRoutes.chat(uid.toString(), source)) },
                onNavigateToHome          = { navController.navigateToTab(NavRoutes.HOME) },
                onNavigateToSearch        = { navController.navigateToTab(NavRoutes.SEARCH) },
                onNavigateToCreate        = { navController.navigateToTab(NavRoutes.CREATE) },
                onNavigateToNotifications = { navController.navigateToTab(NavRoutes.NOTIFICATIONS) },
                onNavigateToProfile       = { navController.navigateToTab(NavRoutes.PROFILE) },
                activeRoute               = source,
            )
        }

        composable(
            route     = NavRoutes.FOLLOW_LIST,
            arguments = listOf(navArgument("tab") { type = NavType.StringType; defaultValue = "followers" }),
        ) {
            FollowListScreen(
                onBack = { navController.popBackStack() },
                onOpenProfile = { id -> navController.navigate(NavRoutes.userProfile(id.toString(), NavRoutes.PROFILE)) },
            )
        }

        // ── Settings flow ──────────────────────────────────────────────────────

        composable(NavRoutes.SETTINGS) {
            val uriHandler = LocalUriHandler.current
            val tour: com.rinx.artRINXapp.core.tour.TourViewModel = hiltViewModel()
            SettingsScreen(
                onBack                = { navController.popBackStack() },
                onEditProfile         = { navController.navigate(NavRoutes.EDIT_PROFILE) },
                onChangeEmail         = { navController.navigate(NavRoutes.CHANGE_EMAIL) },
                onChangePhone         = { navController.navigate(NavRoutes.CHANGE_PHONE) },
                onProfileTitleAndPlan = { navController.navigate(NavRoutes.PROFILE_TITLE_PLAN) },
                onInviteFriends       = { navController.navigate(NavRoutes.INVITE_FRIENDS) },
                onAppTutorial         = { tour.restart(); navController.navigateToTab(NavRoutes.HOME) },
                onBlockedAccounts     = { navController.navigate(NavRoutes.BLOCKED_ACCOUNTS) },
                onPhonePermissions    = { navController.navigate(NavRoutes.PHONE_PERMISSIONS) },
                onTermsAndConditions  = { runCatching { uriHandler.openUri(LegalLinks.TERMS_OF_USE) } },
                onCommunityGuidelines = { runCatching { uriHandler.openUri(LegalLinks.COMMUNITY_GUIDELINES) } },
                onAboutUs             = { runCatching { uriHandler.openUri(LegalLinks.ABOUT_US) } },
                onPrivacyPolicy       = { runCatching { uriHandler.openUri(LegalLinks.PRIVACY_POLICY) } },
                onLogout              = {
                    navController.navigate(NavRoutes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(NavRoutes.EDIT_PROFILE) {
            EditProfileScreen(
                onBack  = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable(NavRoutes.PHONE_PERMISSIONS) {
            PhonePermissionsScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.CHANGE_EMAIL) {
            ChangeEmailScreen(
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }

        composable(NavRoutes.CHANGE_PHONE) {
            ChangePhoneScreen(
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }

        composable(NavRoutes.PROFILE_TITLE_PLAN) {
            ProfileTitleAndPlanScreen(
                onBack          = { navController.popBackStack() },
                onEdit          = { navController.navigate(NavRoutes.profileTitlePlanEdit(0)) },
                onEditTitle     = { navController.navigate(NavRoutes.profileTitlePlanEdit(0)) },
                onEditPlan      = { navController.navigate(NavRoutes.profileTitlePlanEdit(1)) },
                onDeleteAccount = {
                    navController.navigate(NavRoutes.AUTH) { popUpTo(0) { inclusive = true } }
                },
            )
        }

        composable(
            route     = NavRoutes.PROFILE_TITLE_PLAN_EDIT,
            arguments = listOf(navArgument("step") { type = NavType.IntType; defaultValue = 0 }),
        ) { entry ->
            val step = entry.arguments?.getInt("step") ?: 0
            ProfileTitleAndPlanEditScreen(
                initialStep = step,
                onBack      = { navController.popBackStack() },
                onSaved     = { navController.popBackStack() },
            )
        }

        composable(NavRoutes.BLOCKED_ACCOUNTS) {
            BlockedAccountsScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.INVITE_FRIENDS) {
            InviteFriendsScreen(
                onBack = { navController.popBackStack() },
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
                onNavigateHome            = { navController.navigateToTab(NavRoutes.HOME) },
                onNavigateToSearch        = { navController.navigateToTab(NavRoutes.SEARCH) },
                onNavigateToCreate        = { navController.navigateToTab(NavRoutes.CREATE) },
                onNavigateToNotifications = { navController.navigateToTab(NavRoutes.NOTIFICATIONS) },
                onNavigateToProfile       = { navController.navigateToTab(NavRoutes.PROFILE) },
                onNavigateToNewCuration   = { navController.navigate(NavRoutes.NEW_CURATION) },
                onEditArt                 = { navController.navigate(NavRoutes.newArtForEdit()) },
                onOpenProfile             = { id -> navController.navigate(NavRoutes.userProfile(id.toString(), source)) },
                onOpenArtistArts          = { name, artistId ->
                    navController.navigate(NavRoutes.artByArtist(name, artistId, source))
                },
                activeRoute = source,
            )
        }

        composable(
            route     = NavRoutes.ART_BY_ARTIST,
            arguments = listOf(
                navArgument("artistName") { type = NavType.StringType },
                navArgument("artistId") { type = NavType.StringType; defaultValue = "" },
                navArgument("source") { type = NavType.StringType; defaultValue = NavRoutes.HOME },
            ),
        ) { backStackEntry ->
            val source = backStackEntry.arguments?.getString("source") ?: NavRoutes.HOME
            ArtByArtistScreen(
                onBack = { navController.popBackStack() },
                onOpenProfile = { id -> navController.navigate(NavRoutes.userProfile(id.toString(), source)) },
                onNavigateToDetail = { postId -> navController.navigate(NavRoutes.artDetail(postId, source)) },
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
                onNavigateToArtDetail = { artId ->
                    navController.navigate(NavRoutes.artDetail(artId, source))
                },
                onNavigateHome            = { navController.navigateToTab(NavRoutes.HOME) },
                onNavigateToSearch        = { navController.navigateToTab(NavRoutes.SEARCH) },
                onNavigateToCreate        = { navController.navigateToTab(NavRoutes.CREATE) },
                onNavigateToNotifications = { navController.navigateToTab(NavRoutes.NOTIFICATIONS) },
                onNavigateToProfile       = { navController.navigateToTab(NavRoutes.PROFILE) },
                onNavigateToNewCuration   = { navController.navigate(NavRoutes.NEW_CURATION) },
                onEditCuration            = { navController.navigate(NavRoutes.NEW_CURATION) },
                onOpenProfile             = { id -> navController.navigate(NavRoutes.userProfile(id.toString(), source)) },
                activeRoute = source,
            )
        }
    }
        TourHost(navController = navController)
    }
}

/** savedStateHandle key carrying an incrementing "tab re-selected" tick to the active tab screen. */
const val TAB_RESELECT_KEY = "tab_reselect_tick"

/**
 * Bottom-tab tap handler.
 *
 * - Tapping a DIFFERENT tab (or tapping from a deep/detail screen) pops everything back to that
 *   tab's root, so the user immediately returns to the tab screen.
 * - Tapping the tab you're ALREADY on bumps a re-select tick on the current entry's
 *   savedStateHandle, which the screen observes to scroll its content back to the top.
 */
private fun NavHostController.navigateToTab(route: String) {
    if (currentDestination?.route == route) {
        val handle = currentBackStackEntry?.savedStateHandle ?: return
        handle[TAB_RESELECT_KEY] = (handle.get<Int>(TAB_RESELECT_KEY) ?: 0) + 1
    } else {
        navigate(route) {
            // Pop back to the tab root (Home), clearing any detail/sub screens. No saveState/
            // restoreState — re-tapping a tab returns to its root, not a previously-deep state.
            popUpTo(graph.findStartDestination().id) { inclusive = false }
            launchSingleTop = true
        }
    }
}

/**
 * After kicking off a background upload, leave the upload sub-flow and land on the surface where
 * the result + progress will appear: Profile (Art tab) for private uploads, Home (Discover) for
 * public ones. popUpTo(start) clears NEW_ART / ART_PREVIEW / ADD_TAGS without recreating Home.
 */
private fun NavHostController.navigateAfterUpload(isPrivate: Boolean) {
    val target = if (isPrivate) NavRoutes.PROFILE else NavRoutes.HOME
    navigate(target) {
        popUpTo(graph.findStartDestination().id) { inclusive = false }
        launchSingleTop = true
    }
}

private fun NavHostController.navigateToOtp(args: OtpArgs) {
    val encodedValue = Uri.encode(args.contactValue)
    navigate(
        "otp?mode=${args.mode}&contactType=${args.contactType}" +
        "&contactValue=$encodedValue&inviteCode=${Uri.encode(args.inviteCode)}"
    )
}
