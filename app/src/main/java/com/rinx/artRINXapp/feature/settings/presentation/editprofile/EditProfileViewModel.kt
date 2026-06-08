package com.rinx.artRINXapp.feature.settings.presentation.editprofile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.profile.domain.model.EditableProfile
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileUpdate
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val age: String = "",
    val country: String = "",
    val state: String = "",
    val city: String = "",
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val isSaving: Boolean = false,
    val usernameError: String? = null,
    val saveStatus: SaveStatus? = null,
    val saveError: String? = null,
    val showUsernameTooltip: Boolean = false,
    val showFullNameTooltip: Boolean = false,
    val showDisplayNameTooltip: Boolean = false,
) {
    val canSave: Boolean
        get() = !isSaving && username.isNotBlank() && fullName.isNotBlank() && displayName.isNotBlank() &&
            age.isNotBlank() && country.isNotBlank() && state.isNotBlank() && city.isNotBlank()
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    /** Snapshot of the loaded profile, used to send only changed fields on save. */
    private var original: EditableProfile? = null

    init {
        load()
    }

    private fun load() {
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
                            age = p.age,
                            country = p.country,
                            state = p.state,
                            city = p.city,
                            pictureUrl = p.profilePictureUrl,
                            isLoading = false,
                            loadError = null,
                        )
                    }
                }
                is ApiResult.Error -> _state.update {
                    it.copy(isLoading = false, loadError = result.toLoadMessage())
                }
            }
        }
    }

    fun onRetryLoad() = load()

    fun onSave() {
        val o = original ?: return
        val s = _state.value
        val changes = ProfileUpdate(
            username = s.username.trim().takeIf { it != o.username },
            fullName = s.fullName.trim().takeIf { it != o.fullName },
            displayName = s.displayName.trim().takeIf { it != o.displayName },
            bio = s.bio.trim().takeIf { it != o.bio },
            age = s.age.takeIf { it != o.age },
            country = s.country.trim().takeIf { it != o.country },
            state = s.state.trim().takeIf { it != o.state },
            city = s.city.trim().takeIf { it != o.city },
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
                                it.copy(isSaving = false, usernameError = "Username is taken")
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

    fun onUsernameChange(v: String) = _state.update { it.copy(username = v, usernameError = null) }
    fun onFullNameChange(v: String) = _state.update { it.copy(fullName = v) }
    fun onBioChange(v: String) = _state.update { it.copy(bio = v) }
    fun onDisplayNameChange(v: String) = _state.update { it.copy(displayName = v) }
    fun onAgeChange(v: String) = _state.update { it.copy(age = v) }
    fun onCountryChange(v: String) = _state.update { it.copy(country = v) }
    fun onStateChange(v: String) = _state.update { it.copy(state = v) }
    fun onCityChange(v: String) = _state.update { it.copy(city = v) }
    fun onPictureSelected(uri: Uri?) = _state.update { it.copy(pictureUri = uri) }

    fun onUsernameTooltipToggle() = _state.update { it.copy(showUsernameTooltip = !it.showUsernameTooltip) }
    fun onFullNameTooltipToggle() = _state.update { it.copy(showFullNameTooltip = !it.showFullNameTooltip) }
    fun onDisplayNameTooltipToggle() = _state.update { it.copy(showDisplayNameTooltip = !it.showDisplayNameTooltip) }
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
