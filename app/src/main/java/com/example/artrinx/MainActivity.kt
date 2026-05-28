package com.example.artrinx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.artrinx.core.navigation.AppNavGraph
import com.example.artrinx.core.navigation.NavRoutes
import com.example.artrinx.core.theme.ArtRinxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            mainViewModel.hasSeenOnboarding.value == null
        }

        enableEdgeToEdge()

        setContent {
            ArtRinxTheme {
                val hasSeenOnboarding by mainViewModel.hasSeenOnboarding.collectAsState()

                hasSeenOnboarding?.let { seen ->
                    AppNavGraph(
                        startDestination = if (seen) NavRoutes.AUTH else NavRoutes.ONBOARDING
                    )
                }
            }
        }
    }
}
