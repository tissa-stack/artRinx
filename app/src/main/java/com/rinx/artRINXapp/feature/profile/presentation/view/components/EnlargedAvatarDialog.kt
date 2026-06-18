package com.rinx.artRINXapp.feature.profile.presentation.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rinx.artRINXapp.feature.notifications.presentation.messages.components.RinxAvatar

/**
 * Full-screen viewer for a profile avatar: a dim scrim with the ACTUAL profile photo enlarged in the
 * centre (initials fallback via [RinxAvatar] when there's no picture). Tap anywhere (or Back) dismisses.
 */
@Composable
fun EnlargedAvatarDialog(
    url: String?,
    name: String?,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            RinxAvatar(
                url = url,
                contentDescription = name,
                size = 280.dp,
                name = name,
            )
        }
    }
}
