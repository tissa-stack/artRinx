package com.rinx.artRINXapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.rinx.artRINXapp.core.navigation.AppNavGraph
import com.rinx.artRINXapp.core.push.NotificationChannels
import com.rinx.artRINXapp.core.push.PushTokenManager
import com.rinx.artRINXapp.core.push.RinxMessagingService
import com.rinx.artRINXapp.core.theme.ArtRinxTheme
import com.rinx.artRINXapp.feature.notifications.domain.repository.NotificationsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    @Inject lateinit var pushTokenManager: PushTokenManager
    @Inject lateinit var notificationsRepository: NotificationsRepository

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
        handlePushIntent(intent)

        setContent {
            ArtRinxTheme {
                val startDestination by mainViewModel.startDestination.collectAsState()

                startDestination?.let { destination ->
                    AppNavGraph(startDestination = destination)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushIntent(intent)
    }

    /** When opened from a push tap, clear its in-app notification read flag (handout §Push tap). */
    private fun handlePushIntent(intent: Intent?) {
        val notificationId = intent?.getStringExtra(RinxMessagingService.EXTRA_NOTIFICATION_ID) ?: return
        lifecycleScope.launch { runCatching { notificationsRepository.markRead(notificationId) } }
        // route / url extras are available for future deep-link navigation.
    }
}
