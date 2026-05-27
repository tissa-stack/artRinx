package com.example.artrinx.feature.onboarding.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.artrinx.ONBOARDING_COMPLETE_KEY

class OnboardingRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : OnboardingRepository {

    override suspend fun markOnboardingComplete() {
        dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETE_KEY] = true
        }
    }
}
