package com.rinx.artRINXapp.feature.profile.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileDraftDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        val KEY_STEP = intPreferencesKey("profile_draft_step")
        val KEY_GROUND_RULES = booleanPreferencesKey("ground_rules_accepted")
        val KEY_TYPE_ID = intPreferencesKey("profile_draft_type_id")
        val KEY_FULL_NAME = stringPreferencesKey("profile_draft_full_name")
        val KEY_USERNAME = stringPreferencesKey("profile_draft_username")
        val KEY_DISPLAY_NAME = stringPreferencesKey("profile_draft_display_name")
        val KEY_BIO = stringPreferencesKey("profile_draft_bio")
        val KEY_DOB = stringPreferencesKey("profile_draft_dob")
        val KEY_COUNTRY = stringPreferencesKey("profile_draft_country")
        val KEY_STATE = stringPreferencesKey("profile_draft_state")
        val KEY_CITY = stringPreferencesKey("profile_draft_city")
        val KEY_MEDIUM_IDS = stringPreferencesKey("profile_draft_medium_ids")
    }

    suspend fun getDraft(): ProfileDraft = dataStore.data.map { prefs ->
        ProfileDraft(
            step = prefs[KEY_STEP] ?: 0,
            groundRulesAccepted = prefs[KEY_GROUND_RULES] ?: false,
            profileTypeId = prefs[KEY_TYPE_ID],
            fullName = prefs[KEY_FULL_NAME] ?: "",
            username = prefs[KEY_USERNAME] ?: "",
            displayName = prefs[KEY_DISPLAY_NAME] ?: "",
            bio = prefs[KEY_BIO] ?: "",
            dob = prefs[KEY_DOB] ?: "",
            country = prefs[KEY_COUNTRY] ?: "",
            state = prefs[KEY_STATE] ?: "",
            city = prefs[KEY_CITY] ?: "",
            mediumIds = prefs[KEY_MEDIUM_IDS]
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?.toSet()
                ?: emptySet(),
        )
    }.first()

    suspend fun saveStep(step: Int) = dataStore.edit { it[KEY_STEP] = step }

    suspend fun saveGroundRulesAccepted() = dataStore.edit { it[KEY_GROUND_RULES] = true }

    suspend fun saveProfileTypeId(id: Int) = dataStore.edit { it[KEY_TYPE_ID] = id }

    suspend fun saveFullName(value: String) = dataStore.edit { it[KEY_FULL_NAME] = value }

    suspend fun saveUsername(value: String) = dataStore.edit { it[KEY_USERNAME] = value }

    suspend fun saveDisplayName(value: String) = dataStore.edit { it[KEY_DISPLAY_NAME] = value }

    suspend fun saveBio(value: String) = dataStore.edit { it[KEY_BIO] = value }

    suspend fun saveDob(value: String) = dataStore.edit { it[KEY_DOB] = value }

    suspend fun saveCountry(value: String) = dataStore.edit { it[KEY_COUNTRY] = value }

    suspend fun saveState(value: String) = dataStore.edit { it[KEY_STATE] = value }

    suspend fun saveCity(value: String) = dataStore.edit { it[KEY_CITY] = value }

    suspend fun saveMediumIds(ids: Set<Int>) = dataStore.edit {
        it[KEY_MEDIUM_IDS] = ids.joinToString(",")
    }

    suspend fun clearDraft() = dataStore.edit { prefs ->
        listOf(
            KEY_STEP, KEY_GROUND_RULES, KEY_TYPE_ID, KEY_FULL_NAME, KEY_USERNAME,
            KEY_DISPLAY_NAME, KEY_BIO, KEY_DOB, KEY_COUNTRY, KEY_STATE, KEY_CITY, KEY_MEDIUM_IDS,
        ).forEach { prefs.remove(it) }
    }
}
