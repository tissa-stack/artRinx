package com.rinx.artRINXapp.core.push

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Requests the POST_NOTIFICATIONS runtime permission (Android 13+) once [enabled] becomes true.
 * Placed on the post-authentication landing (Home) so the prompt only appears AFTER the user has
 * logged in or finished registering — never on the auth/onboarding screens. No-op below API 33 or
 * when already granted (and the OS shows no UI once permanently denied).
 *
 * The ActivityResult launcher is registered unconditionally on first composition (NOT gated by
 * [enabled]) so registration is stable and ready before the request fires. Only the actual
 * `launch()` is gated by [enabled] — on Home that's the post-tour flag, so the dialog reliably
 * appears the instant the tutorial ends rather than racing launcher registration against the
 * frame where the effect first enters composition (which dropped the request on some OEM devices).
 */
@Composable
fun NotificationPermissionEffect(enabled: Boolean = true) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result ignored — token registration is independent of the OS prompt */ }
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
