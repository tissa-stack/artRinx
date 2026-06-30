package com.rinx.artRINXapp.core.network

import com.rinx.artRINXapp.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the app-identity headers sent on every REST request and the WebSocket
 * upgrade. The backend uses these for per-platform feature gating (e.g. bidirectional blocking):
 *
 * - `X-App-Platform: android` — [platform] — an explicit platform signal so the backend never has
 *   to guess from the version string (build numbers aren't comparable across iOS/Android).
 * - `X-App-Version: artrinx/<major.minor>+build.<buildInt>` — [versionHeader] — the backend parser
 *   requires a **2-part** version (e.g. `1.9`); a 3-part `1.1.0` fails to parse and the request is
 *   treated as unknown/exempt. So we send only `major.minor` (derived from `versionName`), keeping
 *   the build integer it actually gates on.
 *
 * Read straight from gradle's generated [BuildConfig], so bumping `versionName` / `versionCode`
 * in `app/build.gradle.kts` flows through automatically with zero code change here.
 */
@Singleton
class AppVersionProvider @Inject constructor() {
    val platform: String = "android"

    val versionHeader: String =
        "artrinx/${BuildConfig.VERSION_NAME.split(".").take(2).joinToString(".")}+build.${BuildConfig.VERSION_CODE}"
}
