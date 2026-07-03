package com.rinx.artRINXapp.core.navigation

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import com.rinx.artRINXapp.core.tour.TourHost
import com.rinx.artRINXapp.core.util.LegalLinks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rinx.artRINXapp.feature.auth.presentation.invite.InviteCodeScreen
import com.rinx.artRINXapp.feature.auth.domain.model.ContactType
import com.rinx.artRINXapp.feature.auth.presentation.login.LoginOptionsScreen
import com.rinx.artRINXapp.feature.auth.presentation.login.LoginScreen
import com.rinx.artRINXapp.feature.auth.presentation.otp.OtpScreen
import com.rinx.artRINXapp.feature.profile.presentation.ProfileCreationScreen
import com.rinx.artRINXapp.feature.profile.presentation.posttutorial.PostTutorialPlansScreen
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
import com.rinx.artRINXapp.feature.settings.presentation.blocked.BlockedArtworksScreen
import com.rinx.artRINXapp.feature.settings.presentation.changeemail.ChangeEmailScreen
import com.rinx.artRINXapp.feature.settings.presentation.addphone.AddPhoneScreen
import com.rinx.artRINXapp.feature.settings.presentation.changemediums.ChangeMediumsScreen
import com.rinx.artRINXapp.feature.settings.presentation.changephone.ChangePhoneScreen
import com.rinx.artRINXapp.feature.settings.presentation.role.ChangeRoleScreen
import com.rinx.artRINXapp.feature.settings.presentation.subscription.SubscriptionScreen
import com.rinx.artRINXapp.feature.settings.presentation.editprofile.EditProfileScreen
import com.rinx.artRINXapp.feature.settings.presentation.editprofile.EditProfileViewModel
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
import androidx.compose.ui.platform.LocalContext
import com.rinx.artRINXapp.feature.upload.domain.model.CurationProgress
import com.rinx.artRINXapp.feature.upload.domain.model.UploadProgress
import com.rinx.artRINXapp.feature.upload.presentation.UploadStatusViewModel

@Composable
fun AppNavGraph(
    startDestination: String,
    deepLinkRouter: DeepLinkRouter,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    // Forced logout: the session was authoritatively invalidated (refresh token rejected). Wipe any
    // remaining local data, drop any parked push target (non-invite) so it can't fire after we land
    // on login, and return to login with a cleared back stack (same as a manual logout).
    val sessionWatcher: SessionWatcherViewModel = hiltViewModel()
    LaunchedEffect(Unit) {
        sessionWatcher.forceLogout.collect {
            sessionWatcher.onForcedLogout()
            if (deepLinkRouter.target.value !is DeepLinkTarget.Invite) deepLinkRouter.consume()
            deepLinkRouter.consumeEventId()
            navController.navigate(NavRoutes.AUTH) { popUpTo(0) { inclusive = true } }
        }
    }

    // Re-validate the session each time the app comes to the foreground (handout: proactive refresh on
    // foreground). If our refresh token was revoked by "sign out of all devices" elsewhere, the refresh
    // 401s and the force-logout collector above routes to auth. No-op when signed out / offline.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) sessionWatcher.onAppForegrounded()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Push-tap / universal-link routing. Invite codes are consumed by the invite screen itself;
    // here we only act on content routes and the gallery_enterprise_notice home-only case.
    val pendingDeepLink by deepLinkRouter.target.collectAsState()
    LaunchedEffect(pendingDeepLink) {
        val t = pendingDeepLink ?: return@LaunchedEffect
        // Auth gate. The start destination is fixed for this composition's life, so it can't be
        // trusted after a mid-session forced logout (token expired / refresh reused). Read LIVE
        // session validity at tap time: a signed-out user may only act on an invite target; content
        // routes (chat / art / curation / profile / event / home) are dropped so the login screen
        // stands and we never navigate into a screen that immediately 403s.
        if (!sessionWatcher.isLoggedIn()) {
            if (t !is DeepLinkTarget.Invite) deepLinkRouter.consume()
            return@LaunchedEffect
        }
        when (t) {
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
            DeepLinkTarget.Notifications -> {
                // Unresolved notification push → at least open the Notifications list (never Home).
                navController.navigateToTab(NavRoutes.NOTIFICATIONS); deepLinkRouter.consume()
            }
            else -> Unit // Invite handled by InviteCodeViewModel; null = nothing pending.
        }
    }
    // Surface a PUBLIC upload/curation FAILURE that happens while the user is away from Home. The
    // in-feed retry row only renders on Home, so an off-Home failure would otherwise be silent. Gated
    // on route != HOME so we never double up with that row (which still offers retry once back on Home).
    val uploadStatus: UploadStatusViewModel = hiltViewModel()
    val appContext = LocalContext.current
    LaunchedEffect(Unit) {
        uploadStatus.uploadProgress.collect { p ->
            if (p is UploadProgress.Failed && !p.isPrivate &&
                navController.currentDestination?.route != NavRoutes.HOME
            ) {
                Toast.makeText(appContext, p.message, Toast.LENGTH_LONG).show()
            }
        }
    }
    LaunchedEffect(Unit) {
        uploadStatus.curationProgress.collect { p ->
            if (p is CurationProgress.Failed && !p.isPrivate &&
                navController.currentDestination?.route != NavRoutes.HOME
            ) {
                Toast.makeText(appContext, p.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // App-wide: tapping anywhere outside a focused text field dismisses the keyboard. Children that
    // consume taps (buttons, fields, clickables, scrolls) still get them; only taps that fall through
    // to the background reach here.
    val focusManager = LocalFocusManager.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) },
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

        composable(
            NavRoutes.ONBOARDING,
            // iOS uses a 300ms ease-in-out swap from onboarding into the invite/auth flow.
            exitTransition = { fadeOut(tween(durationMillis = 300, easing = EaseInOut)) },
        ) {
            OnboardingScreen(
                onNavigateToAuth = {
                    navController.navigate(NavRoutes.AUTH) {
                        popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(
            NavRoutes.AUTH,
            enterTransition = { fadeIn(tween(durationMillis = 300, easing = EaseInOut)) },
        ) {
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
            LoginOptionsScreen(
                onBack              = { navController.popBackStack() },
                onContinueWithEmail = { navController.navigate(NavRoutes.loginEntry(ContactType.EMAIL.name)) },
                onContinueWithPhone = { navController.navigate(NavRoutes.loginEntry(ContactType.PHONE.name)) },
                onNavigateToHome = {
                    navController.navigate(NavRoutes.HOME) { popUpTo(0) { inclusive = true } }
                },
                onNavigateToProfileCompletion = {
                    navController.navigate(NavRoutes.PROFILE_COMPLETION) { popUpTo(0) { inclusive = true } }
                },
            )
        }

        composable(
            route     = NavRoutes.LOGIN_ENTRY,
            arguments = listOf(
                navArgument("contactType") {
                    type         = NavType.StringType
                    defaultValue = ContactType.EMAIL.name
                },
            ),
        ) { backStackEntry ->
            val contactType = backStackEntry.arguments?.getString("contactType")
                ?.let { runCatching { ContactType.valueOf(it) }.getOrNull() }
                ?: ContactType.EMAIL
            LoginScreen(
                contactType     = contactType,
                onBack          = { navController.popBackStack() },
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
                onNavigateToHome = {
                    navController.navigate(NavRoutes.HOME) { popUpTo(0) { inclusive = true } }
                },
                onNavigateToProfileCompletion = {
                    navController.navigate(NavRoutes.PROFILE_COMPLETION) { popUpTo(0) { inclusive = true } }
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

        // Plans finale shown once, right after the first-launch tutorial.
        composable(NavRoutes.POST_TUTORIAL_PLANS) {
            PostTutorialPlansScreen(
                onContinue = {
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
                onNavigateToProfile       = { navController.navigateToTab(NavRoutes.PROFILE) },
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
            val targetUserId = entry.arguments?.getString("userId")?.toIntOrNull()

            // If the opened profile is the logged-in user's own, show the OWN-profile layout
            // (Liked tab, settings/invite/feedback, edit) as a pushed screen — Back returns to the
            // originating screen and the bottom nav keeps the source tab highlighted. Otherwise show
            // the stranger view. The decision waits for the current-user id to resolve (usually warm).
            val routeVm: com.rinx.artRINXapp.feature.profile.presentation.ProfileRouteViewModel = hiltViewModel()
            val myId by routeVm.currentUserId.collectAsState()

            when {
                myId == null -> Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = com.rinx.artRINXapp.core.theme.BrandPrimary,
                    )
                }
                myId == targetUserId -> UserProfileScreen(
                    onBack                    = { navController.popBackStack() },
                    activeRoute               = source,
                    onNavigateToHome          = { navController.navigateToTab(NavRoutes.HOME) },
                    onNavigateToSearch        = { navController.navigateToTab(NavRoutes.SEARCH) },
                    onNavigateToCreate        = { navController.navigateToTab(NavRoutes.CREATE) },
                    onNavigateToNotifications = { navController.navigateToTab(NavRoutes.NOTIFICATIONS) },
                    onNavigateToProfile       = { navController.navigateToTab(NavRoutes.PROFILE) },
                    onNavigateToSettings      = { navController.navigate(NavRoutes.SETTINGS) },
                    onNavigateToInviteFriends = { navController.navigate(NavRoutes.INVITE_FRIENDS) },
                    onNavigateToDetail        = { id -> navController.navigate(NavRoutes.artDetail(id, source)) },
                    onNavigateToCurationDetail = { id -> navController.navigate(NavRoutes.curationDetail(id, source)) },
                    onOpenFollowers           = { navController.navigate(NavRoutes.followList("followers")) },
                    onOpenFollowing           = { navController.navigate(NavRoutes.followList("following")) },
                )
                else -> OtherProfileScreen(
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
                onAddPhone            = { navController.navigate(NavRoutes.ADD_PHONE) },
                onChangeRole          = { navController.navigate(NavRoutes.CHANGE_ROLE) },
                onSubscription        = { navController.navigate(NavRoutes.SUBSCRIPTION) },
                onInviteFriends       = { navController.navigate(NavRoutes.INVITE_FRIENDS) },
                onAppTutorial         = { tour.restart(); navController.navigateToTab(NavRoutes.HOME) },
                onBlockedAccounts     = { navController.navigate(NavRoutes.BLOCKED_ACCOUNTS) },
                onBlockedArtworks     = { navController.navigate(NavRoutes.BLOCKED_ARTWORKS) },
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
                onChangeMedium = { navController.navigate(NavRoutes.CHANGE_MEDIUMS) },
            )
        }

        composable(NavRoutes.CHANGE_MEDIUMS) { entry ->
            // Share the EDIT_PROFILE screen's ViewModel so medium edits are held there and persisted
            // only when the overall profile is saved (same parent-scoped pattern as NEW_ART flows).
            val parentEntry = remember(entry) { navController.getBackStackEntry(NavRoutes.EDIT_PROFILE) }
            val editViewModel: EditProfileViewModel = hiltViewModel(parentEntry)
            ChangeMediumsScreen(
                onBack = { navController.popBackStack() },
                viewModel = editViewModel,
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

        composable(NavRoutes.ADD_PHONE) {
            AddPhoneScreen(
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }

        composable(NavRoutes.CHANGE_ROLE) {
            ChangeRoleScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable(NavRoutes.SUBSCRIPTION) {
            SubscriptionScreen(onBack = { navController.popBackStack() })
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

        composable(NavRoutes.BLOCKED_ARTWORKS) {
            BlockedArtworksScreen(onBack = { navController.popBackStack() })
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
