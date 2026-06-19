package com.rinx.artRINXapp.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight "does the device currently have a network?" check. Used to tell a *transient* request
 * failure (e.g. DNS not yet resolvable in the moments right after the device wakes from sleep) apart
 * from a genuine offline state, so callers can retry the former without flashing an offline screen.
 *
 * Intentionally does NOT require [NetworkCapabilities.NET_CAPABILITY_VALIDATED]: right after wake a
 * real connection is often not yet "validated", and that's exactly the window we want to treat as
 * online-and-retry rather than offline. Returns `true` if the connectivity service is unavailable so
 * we never wrongly block a request.
 */
@Singleton
class ConnectivityChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) } ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
