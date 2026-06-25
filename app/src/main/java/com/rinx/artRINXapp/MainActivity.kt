package com.rinx.artRINXapp

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.rinx.artRINXapp.core.navigation.AppNavGraph
import com.rinx.artRINXapp.core.navigation.DeepLinkParser
import com.rinx.artRINXapp.core.navigation.DeepLinkRouter
import com.rinx.artRINXapp.core.navigation.DeepLinkTarget
import com.rinx.artRINXapp.core.push.NotificationChannels
import com.rinx.artRINXapp.core.push.PushTokenManager
import com.rinx.artRINXapp.core.push.RinxMessagingService
import com.rinx.artRINXapp.core.theme.ArtRinxTheme
import com.rinx.artRINXapp.feature.home.presentation.components.LocalUnreadNotificationCount
import com.rinx.artRINXapp.feature.notifications.domain.UnreadNotificationsStore
import com.rinx.artRINXapp.feature.notifications.domain.repository.NotificationsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    @Inject lateinit var pushTokenManager: PushTokenManager
    @Inject lateinit var notificationsRepository: NotificationsRepository
    @Inject lateinit var deepLinkRouter: DeepLinkRouter
    @Inject lateinit var unreadNotificationsStore: UnreadNotificationsStore

    /**
     * Render at the app's own fixed font + display scale, independent of the device's
     * Accessibility font-size and display-size settings: pin fontScale to 1.0 and density to the
     * device's stable (default) density. The whole Activity — Compose LocalConfiguration/LocalDensity,
     * ResponsiveDimens, sp text and dp layout — derives from this fixed config. A later system
     * font/display-size change recreates the Activity, which re-pins to the same constants.
     */
    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = 1f
        config.densityDpi = DisplayMetrics.DENSITY_DEVICE_STABLE
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            // Hold the splash until the start destination is known AND any cold-start push/deep-link
            // target has been consumed. The nav graph renders its start destination (Home) and then
            // navigates to the target UNDER the splash, so a tapped notification lands directly on the
            // relevant screen with no Home flash in between.
            mainViewModel.startDestination.value == null || hasPendingContentTarget()
        }

        enableEdgeToEdge()
        // Ensure IME insets are dispatched to Compose so imePadding() works correctly.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Block screenshots & screen recording app-wide (parity with iOS — captures come out blank).
        // Temporarily disabled per request.
        // window.setFlags(
        //     WindowManager.LayoutParams.FLAG_SECURE,
        //     WindowManager.LayoutParams.FLAG_SECURE,
        // )

        // Push notifications: ensure the channel exists and register the FCM token (if signed in).
        // The POST_NOTIFICATIONS prompt is requested on the Home screen — i.e. after login/registration.
        NotificationChannels.ensureDefaultChannel(this)
        pushTokenManager.registerCurrentToken()
        // Seed the bell-tab unread badge (no-op count if not signed in).
        unreadNotificationsStore.refresh()
        handleIntentDeepLink(intent)

        setContent {
            ArtRinxTheme {
                val startDestination by mainViewModel.startDestination.collectAsState()
                val unreadCount by unreadNotificationsStore.count.collectAsState()

                startDestination?.let { destination ->
                    CompositionLocalProvider(LocalUnreadNotificationCount provides unreadCount) {
                        AppNavGraph(startDestination = destination, deepLinkRouter = deepLinkRouter)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentDeepLink(intent)
    }

    /**
     * On launch / push tap: fire-and-forget the notification read-clear (handout §Push tap), then
     * resolve any deep-link target (invite link, push route/url, or gallery_enterprise_notice) and
     * hand it to the nav graph via [DeepLinkRouter].
     */
    /**
     * True while a cold-start push/deep-link points at an in-app screen that the nav graph will route
     * to. Keeping the splash up until it's consumed avoids a Home-screen flash before the target.
     * Excludes `Invite` (it has its own signup flow) and null (a normal launch).
     */
    private fun hasPendingContentTarget(): Boolean = when (deepLinkRouter.target.value) {
        is DeepLinkTarget.Route,
        is DeepLinkTarget.Event,
        DeepLinkTarget.HomeOnly,
        DeepLinkTarget.Notifications -> true
        else -> false
    }

    private fun handleIntentDeepLink(intent: Intent?) {
        intent ?: return
        // A push tapped while the app is backgrounded/killed is shown by the SYSTEM (not our
        // onMessageReceived), which launches us with the raw FCM `data` entries as string extras
        // keyed by their own names ("url", "route", "type", …) — NOT the push_* keys our foreground
        // service sets. Read our keys first, then fall back to the raw data keys so taps route in
        // BOTH cases (otherwise a backgrounded-tap loses the target and lands on Home).
        fun extra(primary: String?, fallback: String): String? =
            (primary?.let { intent.getStringExtra(it) }) ?: intent.getStringExtra(fallback)

        val notificationId = extra(RinxMessagingService.EXTRA_NOTIFICATION_ID, "notification_id")
        notificationId?.let { id ->
            lifecycleScope.launch { runCatching { notificationsRepository.markRead(id) } }
        }
        var target = DeepLinkParser.fromViewUri(
            intent.data.takeIf { intent.action == Intent.ACTION_VIEW },
        ) ?: DeepLinkParser.fromPush(
            route = extra(RinxMessagingService.EXTRA_ROUTE, "route"),
            url = extra(RinxMessagingService.EXTRA_URL, "url"),
            kind = extra(RinxMessagingService.EXTRA_KIND, "kind"),
            type = extra(RinxMessagingService.EXTRA_TYPE, "type"),
            eventId = extra(RinxMessagingService.EXTRA_EVENT_ID, "event_id"),
            resourceType = extra(null, "resource_type"),
            resourceId = extra(null, "resource_id"),
            actorId = extra(null, "actor_id"),
        )
        // A tapped notification that didn't resolve to a specific screen should still open the
        // Notifications list — never silently land on Home. (ACTION_VIEW web links are excluded so
        // they keep resolving via fromViewUri.)
        if (target == null && intent.action != Intent.ACTION_VIEW && notificationId != null) {
            target = DeepLinkTarget.Notifications
        }
        deepLinkRouter.post(target)
    }
}
