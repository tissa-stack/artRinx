package com.rinx.artRINXapp.feature.profile.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.location.LocationRepository
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.auth.domain.repository.AuthRepository
import com.rinx.artRINXapp.feature.profile.data.local.ProfileDraftDataSource
import com.rinx.artRINXapp.feature.profile.domain.model.Medium
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileDraft
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileType
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
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
    val age: String = "",
    val country: String = "",
    val state: String = "",
    val city: String = "",
    val ageError: Boolean = false,
    val countryError: Boolean = false,
    val stateError: Boolean = false,
    val cityError: Boolean = false,
    val showPersonalInfoTooltip: Boolean = false,
    // Location pickers (from bundled assets/locations.json).
    val countryOptions: List<String> = emptyList(),
    val stateOptions: List<String> = emptyList(),

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
    private val locationRepository: LocationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileCreationUiState())
    val uiState: StateFlow<ProfileCreationUiState> = _uiState.asStateFlow()

    private var usernameCheckJob: Job? = null

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
                    age = draft.age,
                    country = draft.country,
                    state = draft.state,
                    city = draft.city,
                    countryOptions = locationRepository.countryNames(),
                    stateOptions = locationRepository.statesOf(draft.country),
                    selectedMediumIds = draft.mediumIds,
                )
            }
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

    fun onAgeChange(value: String) {
        _uiState.update { it.copy(age = value, ageError = false) }
        viewModelScope.launch { draftDataSource.saveAge(value) }
    }

    fun onCountryChange(value: String) {
        // Country changed → refresh the state options and clear any previously-picked state.
        val states = locationRepository.statesOf(value)
        _uiState.update {
            it.copy(country = value, countryError = false, stateOptions = states, state = "", stateError = false)
        }
        viewModelScope.launch {
            draftDataSource.saveCountry(value)
            draftDataSource.saveState("")
        }
    }

    fun onStateChange(value: String) {
        _uiState.update { it.copy(state = value, stateError = false) }
        viewModelScope.launch { draftDataSource.saveState(value) }
    }

    fun onCityChange(value: String) {
        _uiState.update { it.copy(city = value, cityError = false) }
        viewModelScope.launch { draftDataSource.saveCity(value) }
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
        val ageOk = state.age.isNotBlank()
        val countryOk = state.country.isNotBlank()
        val stateOk = state.state.isNotBlank()
        val cityOk = state.city.isNotBlank()
        _uiState.update {
            it.copy(
                ageError = !ageOk,
                countryError = !countryOk,
                stateError = !stateOk,
                cityError = !cityOk,
            )
        }
        if (!ageOk || !countryOk || !stateOk || !cityOk) return false
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
                age = state.age,
                country = state.country,
                state = state.state,
                city = state.city,
                mediumIds = state.selectedMediumIds,
            )
            when (val result = profileRepository.createProfile(draft, pictureUri)) {
                is ApiResult.Success -> {
                    authRepository.saveProfileCompleted(true)
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
    }
}
