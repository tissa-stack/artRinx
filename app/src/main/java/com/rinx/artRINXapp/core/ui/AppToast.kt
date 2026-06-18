package com.rinx.artRINXapp.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.Spacing
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-wide lightweight toast.
 *
 * Replaces [android.widget.Toast] for in-app confirmations: from Android 12 (API 31) the OS stamps
 * the app's launcher icon into every text toast — it renders as an oversized logo on some OEM skins
 * and can't be sized, recolored, or removed via any API. Routing messages through [AppToastHost]
 * (mounted once at the top of the Compose tree) gives a fully themed pill: no system icon, correct
 * in light & dark, and rendered above transient surfaces (bottom sheets, dialogs).
 */
object AppToast {
    data class Message(val text: String, val long: Boolean, val seq: Long)

    private var counter = 0L
    private val _messages = MutableSharedFlow<Message>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<Message> = _messages.asSharedFlow()

    /** Show a short (or, with [long] = true, longer) toast. Blank messages are ignored. */
    fun show(text: String, long: Boolean = false) {
        if (text.isBlank()) return
        counter += 1
        _messages.tryEmit(Message(text, long, counter))
    }
}

/**
 * Renders [AppToast] messages as a themed pill near the bottom of the screen. Mount exactly ONCE,
 * overlaying the whole app (see MainActivity).
 *
 * The pill lives in a [Popup] composed only while a message is showing, so its window is created at
 * that moment and stacks ABOVE any modal bottom sheet or dialog already on screen — a toast raised
 * from inside a sheet (e.g. "Art already exists in the curation") stays visible instead of hiding
 * behind it. The content is given an explicit bounded width (screen width minus margins) so it
 * always measures and draws inside the popup window.
 */
@Composable
fun AppToastHost() {
    var current by remember { mutableStateOf<AppToast.Message?>(null) }

    LaunchedEffect(Unit) {
        AppToast.messages.collect { msg ->
            current = msg
            delay(if (msg.long) 3500L else 2000L)
            current = null
        }
    }

    val msg = current ?: return
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Popup(
        alignment = Alignment.BottomCenter,
        properties = PopupProperties(focusable = false),
    ) {
        // Re-key per message so each toast animates in fresh.
        val visibleState = remember(msg.seq) { MutableTransitionState(false) }
        visibleState.targetState = true

        Box(
            modifier = Modifier
                .width(screenWidth)
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.giant),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visibleState = visibleState,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut(),
            ) {
                Row(
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.inverseSurface)
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    // App brand mark, tinted to the text colour so it always contrasts with the
                    // pill (which is dark in light theme, light in dark theme).
                    Icon(
                        painter = painterResource(R.drawable.ic_logo_rinx),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.size(Spacing.xl),
                    )
                    Text(
                        text = msg.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
