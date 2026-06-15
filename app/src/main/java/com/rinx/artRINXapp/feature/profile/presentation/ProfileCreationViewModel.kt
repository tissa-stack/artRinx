package com.rinx.artRINXapp.feature.profile.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.tour.TourManager
import com.rinx.artRINXapp.feature.auth.domain.repository.AuthRepository
import com.rinx.artRINXapp.feature.profile.data.local.ProfileDraftDataSource
import com.rinx.artRINXapp.feature.profile.domain.model.Medium
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileDraft
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileType
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

sealed class UsernameCheckState {
    data object Idle : UsernameCheckState()
    data object Checking : UsernameCheckState()
    data class Available(val username: String) : UsernameCheckState()
    data class Taken(val username: String) : UsernameCheckState()
    data object Error : UsernameCheckState()
}

data class ProfileCreationUiState(
    val draftLoaded: Boolean = false,
    val currentStep: Int = 0,
    val showGroundRules: Boolean = false,
    val groundRulesChecked: Boolean = false,

    // Step 0 – Profile Title
    val profileTypes: List<ProfileType> = emptyList(),
    val profileTypesLoading: Boolean = false,
    val profileTypesError: String? = null,
    val selectedProfileTypeId: Int? = null,

    // Step 1 – Profile Info
    val profilePictureUri: Uri? = null,
    val showImageSourceSheet: Boolean = false,
    val fullName: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val usernameCheckState: UsernameCheckState = UsernameCheckState.Idle,
    val fullNameError: Boolean = false,
    val usernameError: Boolean = false,
    val displayNameError: Boolean = false,
    val showFullNameTooltip: Boolean = false,
    val showDisplayNameTooltip: Boolean = false,

    // Step 2 – Personal Info
    val dob: String = "", // ISO YYYY-MM-DD
    val country: String = "",
    val state: String = "",
    val city: String = "",
    val dobError: Boolean = false,
    val countryError: Boolean = false,
    val stateError: Boolean = false,
    val cityError: Boolean = false,
    val showPersonalInfoTooltip: Boolean = false,
    // Location pickers (master catalog APIs). Options are filtered display names; the selected ids
    // drive the cascade and gate "Next" (a typed-but-unpicked value has no id and can't advance).
    val countryOptions: List<String> = emptyList(),
    val stateOptions: List<String> = emptyList(),
    val cityOptions: List<String> = emptyList(),
    val selectedCountryIso2: String? = null,
    val selectedStateCode: String? = null,

    // Step 3 – Mediums
    val mediums: List<Medium> = emptyList(),
    val mediumsLoading: Boolean = false,
    val mediumsError: String? = null,
    val selectedMediumIds: Set<Int> = emptySet(),
    val showMediumsTooltip: Boolean = false,

    // Submission
    val isSubmitting: Boolean = false,
    val submissionError: String? = null,
    val navigateToHome: Boolean = false,
)

@HiltViewModel
class ProfileCreationViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val draftDataSource: ProfileDraftDataSource,
    private val authRepository: AuthRepository,
    private val masterLocationRepository: MasterLocationRepository,
    private val tourManager: TourManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileCreationUiState())
    val uiState: StateFlow<ProfileCreationUiState> = _uiState.asStateFlow()

    private var usernameCheckJob: Job? = null

    // Loaded master catalogs (full names + ids); the UI shows names, these resolve the cascade ids.
    private var countries: List<CountryOption> = emptyList()
    private var states: List<StateOption> = emptyList()
    // Cancels stale cascade lookups (a new country/state/city query supersedes the prior one).
    private var locationJob: Job? = null

    init {
        viewModelScope.launch {
            val draft = draftDataSource.getDraft()
            _uiState.update { state ->
                state.copy(
                    draftLoaded = true,
                    currentStep = draft.step,
                    showGroundRules = !draft.groundRulesAccepted,
                    selectedProfileTypeId = draft.profileTypeId,
                    fullName = draft.fullName,
                    username = draft.username,
                    displayName = draft.displayName,
                    bio = draft.bio,
                    dob = draft.dob,
                    country = draft.country,
                    state = draft.state,
                    city = draft.city,
                    selectedMediumIds = draft.mediumIds,
                )
            }
            loadCountriesThenRestore(draft.country, draft.state)
        }
        loadProfileTypes()
        loadMediums()
    }

    // ── Ground Rules ─────────────────────────────────────────────────────────

    fun onGroundRulesCheckedChange(checked: Boolean) {
        _uiState.update { it.copy(groundRulesChecked = checked) }
    }

    fun onGroundRulesContinue() {
        viewModelScope.launch {
            draftDataSource.saveGroundRulesAccepted()
            _uiState.update { it.copy(showGroundRules = false) }
        }
    }

    // ── Profile Types ─────────────────────────────────────────────────────────

    private fun loadProfileTypes() {
        viewModelScope.launch {
            _uiState.update { it.copy(profileTypesLoading = true, profileTypesError = null) }
            when (val result = profileRepository.getProfileTypes()) {
                is ApiResult.Success -> _uiState.update {
                    // Gallery is a web-only (Stripe) acquisition role — never selectable in the
                    // mobile signup wizard. Backend 400s on profile_type_id = Gallery from mobile.
                    val selectable = result.data.filterNot { type ->
                        type.name.contains("gallery", ignoreCase = true)
                    }
                    it.copy(profileTypes = selectable, profileTypesLoading = false)
                }
                else -> _uiState.update {
                    it.copy(
                        profileTypesLoading = false,
                        profileTypesError = "Failed to load profile types. Tap to retry.",
                    )
                }
            }
        }
    }

    fun retryLoadProfileTypes() = loadProfileTypes()

    fun onProfileTypeSelected(id: Int) {
        _uiState.update { it.copy(selectedProfileTypeId = id) }
        viewModelScope.launch { draftDataSource.saveProfileTypeId(id) }
    }

    // ── Profile Info ──────────────────────────────────────────────────────────

    fun onProfilePictureSelected(uri: Uri?) {
        _uiState.update { it.copy(profilePictureUri = uri, showImageSourceSheet = false) }
    }

    fun onAvatarTapped() {
        _uiState.update { it.copy(showImageSourceSheet = true) }
    }

    fun onImageSourceSheetDismiss() {
        _uiState.update { it.copy(showImageSourceSheet = false) }
    }

    fun onFullNameChange(value: String) {
        _uiState.update { it.copy(fullName = value, fullNameError = false) }
        viewModelScope.launch { draftDataSource.saveFullName(value) }
    }

    fun onUsernameChange(value: String) {
        _uiState.update {
            it.copy(
                username = value,
                usernameError = false,
                usernameCheckState = UsernameCheckState.Idle,
            )
        }
        viewModelScope.launch { draftDataSource.saveUsername(value) }
        usernameCheckJob?.cancel()
        // Spec: debounced 500ms, fired on every keystroke once the username is >= 5 chars.
        if (value.length >= 5) {
            usernameCheckJob = viewModelScope.launch {
                delay(500)
                checkUsernameAvailability(value)
            }
        }
    }

    private suspend fun checkUsernameAvailability(username: String) {
        _uiState.update { it.copy(usernameCheckState = UsernameCheckState.Checking) }
        when (val result = profileRepository.checkUsername(username)) {
            is ApiResult.Success -> _uiState.update {
                it.copy(
                    usernameCheckState = if (result.data)
                        UsernameCheckState.Available(username)
                    else
                        UsernameCheckState.Taken(username),
                )
            }
            else -> _uiState.update { it.copy(usernameCheckState = UsernameCheckState.Error) }
        }
    }

    fun onDisplayNameChange(value: String) {
        _uiState.update { it.copy(displayName = value, displayNameError = false) }
        viewModelScope.launch { draftDataSource.saveDisplayName(value) }
    }

    fun onBioChange(value: String) {
        _uiState.update { it.copy(bio = value) }
        viewModelScope.launch { draftDataSource.saveBio(value) }
    }

    fun onFullNameTooltipToggle() {
        _uiState.update { it.copy(showFullNameTooltip = !it.showFullNameTooltip, showDisplayNameTooltip = false) }
    }

    fun onDisplayNameTooltipToggle() {
        _uiState.update { it.copy(showDisplayNameTooltip = !it.showDisplayNameTooltip, showFullNameTooltip = false) }
    }

    // ── Personal Info ─────────────────────────────────────────────────────────

    fun onDobChange(value: String) {
        _uiState.update { it.copy(dob = value, dobError = false) }
        viewModelScope.launch { draftDataSource.saveDob(value) }
    }

    /** Loads the master country catalog, then best-effort restores the cascade from saved names. */
    private fun loadCountriesThenRestore(savedCountry: String, savedState: String) {
        viewModelScope.launch {
            val result = masterLocationRepository.getCountries()
            if (result !is ApiResult.Success) return@launch
            countries = result.data
            _uiState.update { it.copy(countryOptions = result.data.map { c -> c.name }) }

            val country = countries.firstOrNull { it.name.equals(savedCountry.trim(), ignoreCase = true) } ?: return@launch
            val statesResult = masterLocationRepository.getStates(country.iso2)
            if (statesResult !is ApiResult.Success) return@launch
            states = statesResult.data
            _uiState.update { it.copy(selectedCountryIso2 = country.iso2, stateOptions = statesResult.data.map { s -> s.name }) }

            val state = states.firstOrNull { it.name.equals(savedState.trim(), ignoreCase = true) } ?: return@launch
            val citiesResult = masterLocationRepository.getCities(country.iso2, state.stateCode, null)
            _uiState.update {
                it.copy(
                    selectedStateCode = state.stateCode,
                    cityOptions = (citiesResult as? ApiResult.Success)?.data.orEmpty(),
                )
            }
        }
    }

    /** Typing in Country: filter the catalog and invalidate any prior selection + dependents. */
    fun onCountryQuery(value: String) {
        states = emptyList()
        _uiState.update {
            it.copy(
                country = value,
                countryError = false,
                selectedCountryIso2 = null,
                state = "",
                stateError = false,
                stateOptions = emptyList(),
                city = "",
                cityError = false,
                cityOptions = emptyList(),
                countryOptions = countries.filterByName(value),
            )
        }
        viewModelScope.launch {
            draftDataSource.saveCountry(value)
            draftDataSource.saveState("")
            draftDataSource.saveCity("")
        }
    }

    /** Picked a real country → resolve its iso2 and load its states. */
    fun onCountrySelected(name: String) {
        val country = countries.firstOrNull { it.name == name } ?: return
        locationJob?.cancel()
        states = emptyList()
        _uiState.update {
            it.copy(
                country = country.name,
                countryError = false,
                selectedCountryIso2 = country.iso2,
                state = "",
                stateError = false,
                stateOptions = emptyList(),
                city = "",
                cityError = false,
                cityOptions = emptyList(),
            )
        }
        viewModelScope.launch {
            draftDataSource.saveCountry(country.name)
            draftDataSource.saveState("")
            draftDataSource.saveCity("")
        }
        locationJob = viewModelScope.launch {
            val result = masterLocationRepository.getStates(country.iso2)
            if (result is ApiResult.Success) {
                states = result.data
                _uiState.update { it.copy(stateOptions = result.data.map { s -> s.name }) }
            }
        }
    }

    /** Typing in State: filter loaded states and invalidate any prior selection + city. */
    fun onStateQuery(value: String) {
        _uiState.update {
            it.copy(
                state = value,
                stateError = false,
                selectedStateCode = null,
                city = "",
                cityError = false,
                cityOptions = emptyList(),
                stateOptions = states.map { s -> s.name }.filterByQuery(value),
            )
        }
        viewModelScope.launch {
            draftDataSource.saveState(value)
            draftDataSource.saveCity("")
        }
    }

    /** Picked a real state → resolve its code and load the first page of cities. */
    fun onStateSelected(name: String) {
        val iso2 = _uiState.value.selectedCountryIso2 ?: return
        val state = states.firstOrNull { it.name == name } ?: return
        locationJob?.cancel()
        _uiState.update {
            it.copy(
                state = state.name,
                stateError = false,
                selectedStateCode = state.stateCode,
                city = "",
                cityError = false,
                cityOptions = emptyList(),
            )
        }
        viewModelScope.launch {
            draftDataSource.saveState(state.name)
            draftDataSource.saveCity("")
        }
        locationJob = viewModelScope.launch {
            val result = masterLocationRepository.getCities(iso2, state.stateCode, null)
            if (result is ApiResult.Success) _uiState.update { it.copy(cityOptions = result.data) }
        }
    }

    /** Typing in City: debounced prefix search against the catalog (needs both ids). */
    fun onCityQuery(value: String) {
        _uiState.update { it.copy(city = value, cityError = false) }
        viewModelScope.launch { draftDataSource.saveCity(value) }
        val iso2 = _uiState.value.selectedCountryIso2
        val code = _uiState.value.selectedStateCode
        if (iso2 == null || code == null) return
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            delay(CITY_DEBOUNCE_MS)
            val result = masterLocationRepository.getCities(iso2, code, value)
            if (result is ApiResult.Success) _uiState.update { it.copy(cityOptions = result.data) }
        }
    }

    fun onCitySelected(name: String) {
        locationJob?.cancel()
        _uiState.update { it.copy(city = name, cityError = false) }
        viewModelScope.launch { draftDataSource.saveCity(name) }
    }

    private fun List<CountryOption>.filterByName(query: String): List<String> =
        map { it.name }.filterByQuery(query)

    private fun List<String>.filterByQuery(query: String): List<String> {
        val q = query.trim()
        return if (q.isBlank()) this else filter { it.contains(q, ignoreCase = true) }
    }

    fun onPersonalInfoTooltipToggle() {
        _uiState.update { it.copy(showPersonalInfoTooltip = !it.showPersonalInfoTooltip) }
    }

    // ── Mediums ───────────────────────────────────────────────────────────────

    private fun loadMediums() {
        viewModelScope.launch {
            _uiState.update { it.copy(mediumsLoading = true, mediumsError = null) }
            when (val result = profileRepository.getMediums()) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(mediums = result.data, mediumsLoading = false)
                }
                else -> _uiState.update {
                    it.copy(
                        mediumsLoading = false,
                        mediumsError = "Failed to load mediums. Tap to retry.",
                    )
                }
            }
        }
    }

    fun retryLoadMediums() = loadMediums()

    fun onMediumToggle(id: Int) {
        val current = _uiState.value.selectedMediumIds
        val updated = if (current.contains(id)) {
            current - id
        } else {
            // Spec: the user picks EXACTLY 3 mediums — block additional selections past 3.
            if (current.size >= REQUIRED_MEDIUM_COUNT) current else current + id
        }
        _uiState.update { it.copy(selectedMediumIds = updated) }
        viewModelScope.launch { draftDataSource.saveMediumIds(updated) }
    }

    fun onMediumsTooltipToggle() {
        _uiState.update { it.copy(showMediumsTooltip = !it.showMediumsTooltip) }
    }

    // ── Step Navigation ───────────────────────────────────────────────────────

    fun onNextFromProfileTitle() {
        val state = _uiState.value
        if (state.selectedProfileTypeId == null) return
        goToStep(1)
    }

    fun onNextFromProfileInfo(): Boolean {
        val state = _uiState.value
        val nameOk = state.fullName.isNotBlank()
        val usernameOk = state.username.isNotBlank() &&
            state.usernameCheckState is UsernameCheckState.Available
        val displayOk = state.displayName.isNotBlank()
        _uiState.update {
            it.copy(
                fullNameError = !nameOk,
                usernameError = !usernameOk,
                displayNameError = !displayOk,
            )
        }
        if (!nameOk || !usernameOk || !displayOk) return false
        goToStep(2)
        return true
    }

    fun onNextFromPersonalInfo(): Boolean {
        val state = _uiState.value
        val dobOk = state.dob.isNotBlank()
        // Country/state must be real picks from the catalog (resolved ids), not just typed text.
        val countryOk = state.selectedCountryIso2 != null
        val stateOk = state.selectedStateCode != null
        val cityOk = state.city.isNotBlank()
        _uiState.update {
            it.copy(
                dobError = !dobOk,
                countryError = !countryOk,
                stateError = !stateOk,
                cityError = !cityOk,
            )
        }
        if (!dobOk || !countryOk || !stateOk || !cityOk) return false
        goToStep(3)
        return true
    }

    fun onBack() {
        val current = _uiState.value.currentStep
        // Don't allow stepping back into the wizard from the post-submit plan step.
        if (current in 1 until PLAN_STEP) goToStep(current - 1)
    }

    private fun goToStep(step: Int) {
        _uiState.update { it.copy(currentStep = step) }
        viewModelScope.launch { draftDataSource.saveStep(step) }
    }

    // ── Submission ────────────────────────────────────────────────────────────

    fun onSubmit(pictureUri: Uri?) {
        val state = _uiState.value
        if (state.selectedMediumIds.size != REQUIRED_MEDIUM_COUNT) return
        _uiState.update { it.copy(isSubmitting = true, submissionError = null) }
        viewModelScope.launch {
            val draft = ProfileDraft(
                step = 3,
                groundRulesAccepted = true,
                profileTypeId = state.selectedProfileTypeId,
                fullName = state.fullName,
                username = state.username,
                displayName = state.displayName,
                bio = state.bio,
                dob = state.dob,
                country = state.country,
                state = state.state,
                city = state.city,
                mediumIds = state.selectedMediumIds,
            )
            when (val result = profileRepository.createProfile(draft, pictureUri)) {
                is ApiResult.Success -> {
                    authRepository.saveProfileCompleted(true)
                    // Brand-new account → re-arm the first-launch tour (the completed flag is
                    // device-global, so a 2nd account on the same device otherwise never sees it).
                    tourManager.prepareForNewUser()
                    draftDataSource.clearDraft()
                    // POST fires at the mediums step; advance to the informational plan step (step 5).
                    _uiState.update { it.copy(isSubmitting = false, currentStep = PLAN_STEP) }
                    draftDataSource.saveStep(PLAN_STEP)
                }
                is ApiResult.Error.Validation -> _uiState.update {
                    it.copy(isSubmitting = false, submissionError = result.message)
                }
                is ApiResult.Error.Network -> _uiState.update {
                    it.copy(isSubmitting = false, submissionError = "No internet connection. Please try again.")
                }
                else -> _uiState.update {
                    it.copy(isSubmitting = false, submissionError = "Something went wrong. Please try again.")
                }
            }
        }
    }

    /** Step 5 "Explore RINX" exit. Profile is already created + marked complete; just route Home. */
    fun onExploreRinx() {
        _uiState.update { it.copy(navigateToHome = true) }
    }

    fun onNavigatedToHome() {
        _uiState.update { it.copy(navigateToHome = false) }
    }

    fun onDismissError() {
        _uiState.update { it.copy(submissionError = null) }
    }

    companion object {
        /** Spec: the mediums step requires the user to pick exactly this many. */
        const val REQUIRED_MEDIUM_COUNT = 3
        /** Index of the informational plan step (step 5), shown after the profile POST succeeds. */
        const val PLAN_STEP = 4
        /** Debounce for the city prefix-search query. */
        const val CITY_DEBOUNCE_MS = 350L
    }
}
