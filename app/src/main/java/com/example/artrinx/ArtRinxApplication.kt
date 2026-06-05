package com.example.artrinx

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "artrinx_prefs")

@HiltAndroidApp
class ArtRinxApplication : Application(), ImageLoaderFactory {

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
