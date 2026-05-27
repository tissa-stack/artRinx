package com.example.artrinx

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // null = still reading DataStore, Boolean = resolved
    private val _hasSeenOnboarding = MutableStateFlow<Boolean?>(null)
    val hasSeenOnboarding: StateFlow<Boolean?> = _hasSeenOnboarding.asStateFlow()

    init {
        viewModelScope.launch {
            application.appDataStore.data
                .map { prefs -> prefs[ONBOARDING_COMPLETE_KEY] ?: false }
                .collect { seen -> _hasSeenOnboarding.value = seen }
        }
    }
}
