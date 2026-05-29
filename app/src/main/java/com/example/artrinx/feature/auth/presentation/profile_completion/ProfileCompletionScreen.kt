package com.example.artrinx.feature.auth.presentation.profile_completion

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.example.artrinx.R
import com.example.artrinx.core.theme.ArtRinxTheme
import com.example.artrinx.core.theme.LocalDimens
import com.example.artrinx.core.theme.Spacing

@Composable
fun ProfileCompletionScreen() {
    ArtRinxTheme(darkTheme = true) {
        val dimens = LocalDimens.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_white_logo),
                contentDescription = "RiNX logo",
                modifier = Modifier
                    .height(dimens.logoHeight)
                    .aspectRatio(4f),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.height(Spacing.xxxl))
            Text(
                text = "Complete your profile",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "Coming soon",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
