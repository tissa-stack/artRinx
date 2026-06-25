package com.rinx.artRINXapp.feature.settings.presentation.editprofile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.profile.domain.model.EditableProfile
import com.rinx.artRINXapp.feature.profile.domain.model.Medium
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileUpdate
import com.rinx.artRINXapp.feature.profile.domain.repository.CountryOption
import com.rinx.artRINXapp.feature.profile.domain.repository.MasterLocationRepository
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import com.rinx.artRINXapp.feature.profile.domain.repository.StateOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SaveStatus { SAVED, FAILED }

data class EditProfileUiState(
    val pictureUri: Uri? = null, // newly picked local image (overrides the remote one)
    val pictureUrl: String? = null, // remote profile picture
    val username: String = "",
    val fullName: String = "",
    val bio: String = "",
    val shopLink: String = "",
    val displayName: String = "",
    val dob: String = "", // ISO YYYY-MM-DD
    val country: String = "",
    val state: String = "",
    val city: String = "",
    // Location pickers (master catalog APIs). Options are filtered display names; the selected ids
    // drive the cascade (states need the country iso2, cities need iso2 + state code).
    val countryOptions: List<String> = emptyList(),
    val stateOptions: List<String> = emptyList(),
    val cityOptions: List<String> = emptyList(),
    val selectedCountryIso2: String? = null,
    val selectedStateCode: String? = null,
    // True once the selected country's master state list comes back empty (no subdivisions).
    val selectedCountryHasNoStates: Boolean = false,
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val isSaving: Boolean = false,
    val usernameError: String? = null,
    /** The exact username the server last reported as taken; gates re-submitting it unchanged. */
    val takenUsername: String? = null,
    val saveStatus: SaveStatus? = null,
    val saveError: String? = null,
    val showUsernameTooltip: Boolean = false,
    val showFullNameTooltip: Boolean = false,
    val showDisplayNameTooltip: Boolean = false,
    /** Full name may be changed at most twice (handout §9); false once the cap is reached. */
    val canEditFullName: Boolean = true,
    // Mediums — edited on the Change Medium screen (which shares this VM) but persisted only on Save.
    val mediums: List<Medium> = emptyList(),
    val selectedMediumIds: Set<Int> = emptySet(),
    val mediumsLoading: Boolean = false,
    val mediumsError: String? = null,
    val showMediumsTooltip: Boolean = false,
    // Standalone medium save (Change Medium screen's own Save button → PUT /api/profile/mediums).
    val mediumsSaving: Boolean = false,
    val mediumsSaveStatus: SaveStatus? = null,
    val mediumsSaveError: String? = null,
) {
    val canSave: Boolean
        // Country, State & City are all optional (the bundled catalog only covers major countries,
        // and the API treats them as nullable) — type to send, leave blank to clear.
        get() = !isSaving && username.isNotBlank() && fullName.isNotBlank() && displayName.isNotBlank() &&
            dob.isNotBlank() &&
            // Can't re-submit a username the server just said is taken (until it's edited).
            username.trim() != takenUsername
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val masterLocationRepository: MasterLocationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    /** Snapshot of the loaded profile, used to send only changed fields on save. */
    private var original: EditableProfile? = null

    /** The user's medium ids at load time — compared on save so mediums are sent only when changed. */
    private var originalMediumIds: Set<Int> = emptySet()

    // Loaded master catalogs (full names + ids); the UI shows names, these resolve the cascade ids.
    private var countries: List<CountryOption> = emptyList()
    private var states: List<StateOption> = emptyList()
    // Cancels stale cascade lookups (a new country/state/city query supersedes the prior one).
    private var locationJob: Job? = null

    init {
        load()
    }

    private fun load() {
        userEdited = false // a fresh prefill is not a user edit
        _state.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            when (val result = repository.getEditableProfile()) {
                is ApiResult.Success -> {
                    val p = result.data
                    original = p
                    _state.update {
                        it.copy(
                            username = p.username,
                            fullName = p.fullName,
                            displayName = p.displayName,
                            bio = p.bio,
                            dob = p.dob,
                            country = p.country,
                            state = p.state,
                            city = p.city,
                            pictureUrl = p.profilePictureUrl,
                            canEditFullName = p.fullNameEditCount < MAX_FULL_NAME_EDITS,
                            isLoading = false,
                            loadError = null,
                        )
                    }
                    loadCountriesThenRestore(p.country, p.state)
                }
                is ApiResult.Error -> _state.update {
                    it.copy(isLoading = false, loadError = result.toLoadMessage())
                }
            }
        }
        loadMediums()
    }

    fun onRetryLoad() = load()

    // ── Mediums ─────────────────────────────────────────────────────────────────
    // State lives here (not in a separate VM) so the Change Medium screen — which shares this VM via
    // the EDIT_PROFILE back-stack entry — edits a working copy that's only persisted on profile Save.

    fun retryLoadMediums() = loadMediums()

    private fun loadMediums() {
        _state.update { it.copy(mediumsLoading = true, mediumsError = null) }
        viewModelScope.launch {
            // Master catalog drives the grid; the user's current mediums pre-select it (matched by id).
            val all = repository.getMediums()
            val current = repository.getUserMediums()
            if (all is ApiResult.Success) {
                val selected = (current as? ApiResult.Success)?.data?.map { it.id }?.toSet().orEmpty()
                originalMediumIds = selected
                _state.update {
                    it.copy(mediums = all.data, selectedMediumIds = selected, mediumsLoading = false, mediumsError = null)
                }
            } else {
                _state.update { it.copy(mediumsLoading = false, mediumsError = "Couldn't load mediums. Tap to retry.") }
            }
        }
    }

    fun onMediumToggle(id: Int) {
        // Mediums save on their own (Change Medium screen's Save), so a toggle is NOT a profile-form
        // edit. Their dirtiness is tracked separately via [isDirty] (selection vs. originalMediumIds).
        _state.update { s ->
            val updated = s.selectedMediumIds.toMutableSet().apply {
                when {
                    contains(id) -> remove(id)
                    size < MAX_MEDIUMS -> add(id)
                    else -> {} // at the cap → ignore (the grid also disables unselected items)
                }
            }
            s.copy(selectedMediumIds = updated)
        }
    }

    fun onMediumsTooltipToggle() = _state.update { it.copy(showMediumsTooltip = !it.showMediumsTooltip) }

    /** Persist the medium selection on its own (Change Medium screen Save) via PUT /api/profile/mediums. */
    fun saveMediums() {
        val s = _state.value
        if (s.mediumsSaving || s.selectedMediumIds.isEmpty()) return
        _state.update { it.copy(mediumsSaving = true, mediumsSaveError = null) }
        viewModelScope.launch {
            when (val res = repository.updateUserMediums(s.selectedMediumIds.toList())) {
                is ApiResult.Success -> {
                    // Persisted standalone → move the baseline so a later profile Save won't redundantly resend.
                    originalMediumIds = s.selectedMediumIds
                    _state.update { it.copy(mediumsSaving = false, mediumsSaveStatus = SaveStatus.SAVED) }
                }
                is ApiResult.Error -> _state.update {
                    it.copy(mediumsSaving = false, mediumsSaveStatus = SaveStatus.FAILED, mediumsSaveError = res.toSaveMessage())
                }
            }
        }
    }

    /** Clears the one-time medium-save result after the screen has consumed it. */
    fun onMediumsSaveHandled() = _state.update { it.copy(mediumsSaveStatus = null, mediumsSaveError = null) }

    fun onSave() {
        val o = original ?: return
        val s = _state.value
        val changes = ProfileUpdate(
            username = s.username.trim().takeIf { it != o.username },
            fullName = s.fullName.trim().takeIf { it != o.fullName },
            displayName = s.displayName.trim().takeIf { it != o.displayName },
            bio = s.bio.trim().takeIf { it != o.bio },
            dob = s.dob.takeIf { it != o.dob },
            country = s.country.trim().takeIf { it != o.country },
            state = s.state.trim().takeIf { it != o.state },
            city = s.city.trim().takeIf { it != o.city },
            // Send mediums only when the working selection differs from what was loaded.
            preferredMediumIds = s.selectedMediumIds.toList().takeIf { s.selectedMediumIds != originalMediumIds },
        )

        // Nothing changed and no new picture — just close the screen.
        if (!changes.hasAnyField && s.pictureUri == null) {
            _state.update { it.copy(saveStatus = SaveStatus.SAVED) }
            return
        }

        _state.update { it.copy(isSaving = true, saveError = null, usernameError = null) }
        viewModelScope.launch {
            // Pre-flight: a changed username must be available before we attempt the update.
            changes.username?.let { newUsername ->
                when (val check = repository.checkUsername(newUsername)) {
                    is ApiResult.Success ->
                        if (!check.data) {
                            _state.update {
                                it.copy(isSaving = false, usernameError = "Username is taken", takenUsername = newUsername)
                            }
                            return@launch
                        }
                    is ApiResult.Error -> {
                        _state.update {
                            it.copy(
                                isSaving = false,
                                saveStatus = SaveStatus.FAILED,
                                saveError = check.toSaveMessage(),
                            )
                        }
                        return@launch
                    }
                }
            }

            when (val result = repository.updateProfile(changes, s.pictureUri)) {
                is ApiResult.Success -> _state.update {
                    it.copy(isSaving = false, saveStatus = SaveStatus.SAVED)
                }
                is ApiResult.Error -> _state.update {
                    it.copy(
                        isSaving = false,
                        saveStatus = SaveStatus.FAILED,
                        saveError = result.toSaveMessage(),
                    )
                }
            }
        }
    }

    /** Clears the one-time save result after the screen has consumed it. */
    fun onSaveHandled() = _state.update { it.copy(saveStatus = null, saveError = null) }

    /**
     * True once the user has actually edited any field / picked a new picture. We track real user
     * interaction rather than diffing loaded values — none of the inputs (text fields, DOB picker,
     * dropdowns) emit on composition, so this can't false-positive on a fresh load the way a value
     * diff did (loaded representation vs. picker-normalized form). Reset to false after a load.
     */
    private var userEdited = false
    // Dirty when a profile field changed, OR mediums differ from their saved baseline (unsaved medium
    // edits still warn; a standalone medium Save moves the baseline, so saved mediums don't prompt).
    val isDirty: Boolean get() = userEdited || _state.value.selectedMediumIds != originalMediumIds

    fun onUsernameChange(v: String) {
        userEdited = true
        _state.update { it.copy(username = v, usernameError = null) }
    }
    fun onFullNameChange(v: String) = _state.update {
        // Guard: ignore edits once the 2-change cap is reached (the field is also disabled in the UI).
        if (!it.canEditFullName) it else { userEdited = true; it.copy(fullName = v) }
    }
    fun onBioChange(v: String) {
        userEdited = true
        _state.update { it.copy(bio = v) }
    }
    fun onDisplayNameChange(v: String) {
        userEdited = true
        _state.update { it.copy(displayName = v) }
    }
    fun onDobChange(v: String) {
        userEdited = true
        _state.update { it.copy(dob = v) }
    }
    // ── Location cascade (master catalog APIs) ─────────────────────────────────
    // Mirrors ProfileCreationViewModel: Country → State → City type-to-search. State & City are
    // optional here; a typed-but-unpicked value is kept as free text and sent verbatim on save.

    /** Loads the master country catalog, then best-effort restores the cascade from saved names. */
    private fun loadCountriesThenRestore(savedCountry: String, savedState: String) {
        viewModelScope.launch {
            val result = masterLocationRepository.getCountries()
            if (result !is ApiResult.Success) return@launch
            countries = result.data
            _state.update { it.copy(countryOptions = result.data.map { c -> c.name }) }

            val country = countries.firstOrNull { it.name.equals(savedCountry.trim(), ignoreCase = true) } ?: return@launch
            val statesResult = masterLocationRepository.getStates(country.iso2)
            if (statesResult !is ApiResult.Success) return@launch
            states = statesResult.data
            _state.update {
                it.copy(
                    selectedCountryIso2 = country.iso2,
                    stateOptions = statesResult.data.map { s -> s.name },
                    selectedCountryHasNoStates = statesResult.data.isEmpty(),
                )
            }

            val state = states.firstOrNull { it.name.equals(savedState.trim(), ignoreCase = true) } ?: return@launch
            val citiesResult = masterLocationRepository.getCities(country.iso2, state.stateCode, null)
            _state.update {
                it.copy(
                    selectedStateCode = state.stateCode,
                    cityOptions = (citiesResult as? ApiResult.Success)?.data.orEmpty(),
                )
            }
        }
    }

    /** Typing in Country: filter the catalog and invalidate any prior selection + dependents. */
    fun onCountryQuery(v: String) {
        userEdited = true
        states = emptyList()
        _state.update {
            it.copy(
                country = v,
                selectedCountryIso2 = null,
                selectedCountryHasNoStates = false,
                state = "",
                stateOptions = emptyList(),
                city = "",
                cityOptions = emptyList(),
                countryOptions = countries.map { c -> c.name }.filterByQuery(v),
            )
        }
    }

    /** Picked a real country → resolve its iso2 and load its states. */
    fun onCountrySelected(name: String) {
        val country = countries.firstOrNull { it.name == name } ?: return
        userEdited = true
        locationJob?.cancel()
        states = emptyList()
        _state.update {
            it.copy(
                country = country.name,
                selectedCountryIso2 = country.iso2,
                selectedCountryHasNoStates = false,
                state = "",
                stateOptions = emptyList(),
                city = "",
                cityOptions = emptyList(),
            )
        }
        locationJob = viewModelScope.launch {
            val result = masterLocationRepository.getStates(country.iso2)
            if (result is ApiResult.Success) {
                states = result.data
                _state.update {
                    it.copy(
                        stateOptions = result.data.map { s -> s.name },
                        selectedCountryHasNoStates = result.data.isEmpty(),
                    )
                }
            }
        }
    }

    /** Typing in State: filter loaded states and invalidate any prior selection + city. */
    fun onStateQuery(v: String) {
        userEdited = true
        _state.update {
            it.copy(
                state = v,
                selectedStateCode = null,
                city = "",
                cityOptions = emptyList(),
                stateOptions = states.map { s -> s.name }.filterByQuery(v),
            )
        }
    }

    /** Picked a real state → resolve its code and load the first page of cities. */
    fun onStateSelected(name: String) {
        val iso2 = _state.value.selectedCountryIso2 ?: return
        val state = states.firstOrNull { it.name == name } ?: return
        userEdited = true
        locationJob?.cancel()
        _state.update {
            it.copy(
                state = state.name,
                selectedStateCode = state.stateCode,
                city = "",
                cityOptions = emptyList(),
            )
        }
        locationJob = viewModelScope.launch {
            val result = masterLocationRepository.getCities(iso2, state.stateCode, null)
            if (result is ApiResult.Success) _state.update { it.copy(cityOptions = result.data) }
        }
    }

    /** Typing in City: debounced prefix search against the catalog (needs both ids). */
    fun onCityQuery(v: String) {
        userEdited = true
        _state.update { it.copy(city = v) }
        val iso2 = _state.value.selectedCountryIso2
        val code = _state.value.selectedStateCode
        if (iso2 == null || code == null) return
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            delay(CITY_DEBOUNCE_MS)
            val result = masterLocationRepository.getCities(iso2, code, v)
            if (result is ApiResult.Success) _state.update { it.copy(cityOptions = result.data) }
        }
    }

    fun onCitySelected(name: String) {
        userEdited = true
        locationJob?.cancel()
        _state.update { it.copy(city = name) }
    }

    private fun List<String>.filterByQuery(query: String): List<String> {
        val q = query.trim()
        return if (q.isBlank()) this else filter { it.contains(q, ignoreCase = true) }
    }

    fun onPictureSelected(uri: Uri?) {
        if (uri != null) userEdited = true // null = the user cancelled the picker → not an edit
        _state.update { it.copy(pictureUri = uri) }
    }

    fun onUsernameTooltipToggle() = _state.update { it.copy(showUsernameTooltip = !it.showUsernameTooltip) }
    fun onFullNameTooltipToggle() = _state.update { it.copy(showFullNameTooltip = !it.showFullNameTooltip) }
    fun onDisplayNameTooltipToggle() = _state.update { it.copy(showDisplayNameTooltip = !it.showDisplayNameTooltip) }

    private companion object {
        const val MAX_FULL_NAME_EDITS = 2
        const val CITY_DEBOUNCE_MS = 350L
        const val MAX_MEDIUMS = 3
    }
}

private fun ApiResult.Error.toLoadMessage(): String = when (this) {
    is ApiResult.Error.Network -> "No connection. Please try again."
    is ApiResult.Error.Server -> "Something went wrong. Please try again."
    else -> "Couldn't load your profile. Please try again."
}

private fun ApiResult.Error.toSaveMessage(): String = when (this) {
    is ApiResult.Error.Network -> "No connection. Please try again."
    is ApiResult.Error.Validation -> message
    is ApiResult.Error.Conflict -> message
    is ApiResult.Error.RateLimited -> message
    is ApiResult.Error.Server -> "Something went wrong. Please try again."
    else -> "Couldn't save changes — please try again."
}
