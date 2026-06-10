package com.rinx.artRINXapp

import android.content.Intent
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            mainViewModel.startDestination.value == null
        }

        enableEdgeToEdge()
        // Ensure IME insets are dispatched to Compose so imePadding() works correctly.
        WindowCompat.setDecorFitsSystemWindows(window, false)

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
    private fun handleIntentDeepLink(intent: Intent?) {
        intent ?: return
        intent.getStringExtra(RinxMessagingService.EXTRA_NOTIFICATION_ID)?.let { id ->
            lifecycleScope.launch { runCatching { notificationsRepository.markRead(id) } }
        }
        val target = DeepLinkParser.fromViewUri(
            intent.data.takeIf { intent.action == Intent.ACTION_VIEW },
        ) ?: DeepLinkParser.fromPush(
            route = intent.getStringExtra(RinxMessagingService.EXTRA_ROUTE),
            url = intent.getStringExtra(RinxMessagingService.EXTRA_URL),
            kind = intent.getStringExtra(RinxMessagingService.EXTRA_KIND),
            type = intent.getStringExtra(RinxMessagingService.EXTRA_TYPE),
            eventId = intent.getStringExtra(RinxMessagingService.EXTRA_EVENT_ID),
        )
        deepLinkRouter.post(target)
    }
}
