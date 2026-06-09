package com.rinx.artRINXapp.core.tour

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persisted flag for the first-launch app tour. Mirrors the onboarding-complete pattern
 * ([com.rinx.artRINXapp.ONBOARDING_COMPLETE_KEY]) — a single boolean in the app preferences
 * DataStore, set once the user finishes or skips the tour so it never shows again.
 */
@Singleton
class TourPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun isCompleted(): Boolean =
        dataStore.data.map { it[KEY] ?: false }.first()

    suspend fun markCompleted() {
        dataStore.edit { it[KEY] = true }
    }

    /** Clear the flag so the tour shows again — used when a brand-new account is created. */
    suspend fun reset() {
        dataStore.edit { it[KEY] = false }
    }

    private companion object {
        val KEY = booleanPreferencesKey("home_tour_completed")
    }
}
