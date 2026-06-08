package com.example.artrinx

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.artrinx.core.network.ChatWebSocketManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "artrinx_prefs")

@HiltAndroidApp
class ArtRinxApplication : Application(), ImageLoaderFactory {

    @Inject lateinit var chatWebSocketManager: ChatWebSocketManager

    /** Drives the chat socket: connect while any activity is foregrounded, disconnect otherwise. */
    private var startedActivities = 0

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                if (startedActivities++ == 0) chatWebSocketManager.onAppForeground()
            }

            override fun onActivityStopped(activity: Activity) {
                if (--startedActivities <= 0) {
                    startedActivities = 0
                    chatWebSocketManager.onAppBackground()
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    /**
     * App-wide Coil loader. Without this Coil runs on defaults: a small memory cache and a disk
     * cache that obeys the CDN's Cache-Control headers — our signed CDN URLs send restrictive
     * headers, so avatars/artworks were re-downloaded at full size on every visit (slow first
     * paint on the larger avatars, e.g. Edit Profile). We size the memory cache, add a persistent
     * 100 MB disk cache, ignore cache headers so images actually persist, and crossfade.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
}
