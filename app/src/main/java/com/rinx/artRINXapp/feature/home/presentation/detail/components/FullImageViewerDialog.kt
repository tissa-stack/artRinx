package com.rinx.artRINXapp.feature.home.presentation.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rinx.artRINXapp.core.theme.Spacing

/**
 * Full-screen lightbox for an artwork: a black scrim with the WHOLE image fit to the screen
 * (nothing cropped), supporting pinch / double-tap / pan zoom via [ZoomableImage]. A close (X)
 * button (and system Back) dismisses. Used when the detail-page hero preview is tapped.
 */
@Composable
fun FullImageViewerDialog(
    imageUrl: String?,
    contentDescription: String?,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            ZoomableImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,        // whole image, never cropped
                backgroundColor = Color.Transparent,     // let the black scrim show in the margins
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(Spacing.sm),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                )
            }
        }
    }
}
