package com.rinx.artRINXapp.testutil

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [DataStore] of [Preferences] for unit tests — no files, no Android runtime. Matches the
 * real DataStore contract closely enough for queue/persistence logic: `data` emits the current
 * snapshot and `updateData` applies the transform atomically and publishes the result. The
 * `edit { }` extension builds the mutable copy itself, so this just stores what the transform returns.
 */
class FakePreferencesDataStore(
    initial: Preferences = emptyPreferences(),
) : DataStore<Preferences> {

    private val state = MutableStateFlow(initial)

    override val data: Flow<Preferences> = state

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
