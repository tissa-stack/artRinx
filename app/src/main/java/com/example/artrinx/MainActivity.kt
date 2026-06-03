package com.example.artrinx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.example.artrinx.core.navigation.AppNavGraph
import com.example.artrinx.core.theme.ArtRinxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            mainViewModel.startDestination.value == null
        }

        enableEdgeToEdge()
        // Ensure IME insets are dispatched to Compose so imePadding() works correctly.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            ArtRinxTheme {
                val startDestination by mainViewModel.startDestination.collectAsState()

                startDestination?.let { destination ->
                    AppNavGraph(startDestination = destination)
                }
            }
        }
    }
}
