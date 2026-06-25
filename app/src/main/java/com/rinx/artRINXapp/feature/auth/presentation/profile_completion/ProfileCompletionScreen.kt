package com.rinx.artRINXapp.feature.auth.presentation.profile_completion

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.ArtRinxTheme
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing

@Composable
fun ProfileCompletionScreen() {
    ArtRinxTheme {
        val dimens = LocalDimens.current
        val isDark = isSystemInDarkTheme()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(
                    if (isDark) R.drawable.artrinx_logo_dark_theme else R.drawable.artrinx_logo_light_theme,
                ),
                contentDescription = "artRinx logo",
                modifier = Modifier
                    .height(dimens.logoHeight),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.height(Spacing.xxxl))
            Text(
                text = "Complete your profile",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "Coming soon",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
