package com.example.artrinx.feature.settings.presentation.editprofile

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.artrinx.feature.settings.domain.model.MockSettingsData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class EditProfileUiState(
    val pictureUri: Uri? = null,
    val username: String = "",
    val fullName: String = "",
    val bio: String = "",
    val shopLink: String = "",
    val displayName: String = "",
    val gender: String = "",
    val age: String = "",
    val country: String = "",
    val state: String = "",
    val city: String = "",
    val showUsernameTooltip: Boolean = false,
    val showFullNameTooltip: Boolean = false,
    val showDisplayNameTooltip: Boolean = false,
) {
    val canSave: Boolean
        get() = username.isNotBlank() && fullName.isNotBlank() && displayName.isNotBlank() &&
            gender.isNotBlank() && age.isNotBlank() &&
            country.isNotBlank() && state.isNotBlank() && city.isNotBlank()
}

@HiltViewModel
class EditProfileViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(loadInitial())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    private fun loadInitial(): EditProfileUiState {
        val d = MockSettingsData.editProfile
        return EditProfileUiState(
            username = d.username, fullName = d.fullName, bio = d.bio, shopLink = d.shopLink,
            displayName = d.displayName, gender = d.gender, age = d.age,
            country = d.country, state = d.state, city = d.city,
        )
    }

    fun onUsernameChange(v: String)    = _state.update { it.copy(username = v) }
    fun onFullNameChange(v: String)    = _state.update { it.copy(fullName = v) }
    fun onBioChange(v: String)         = _state.update { it.copy(bio = v) }
    fun onDisplayNameChange(v: String) = _state.update { it.copy(displayName = v) }
    fun onGenderChange(v: String)      = _state.update { it.copy(gender = v) }
    fun onAgeChange(v: String)         = _state.update { it.copy(age = v) }
    fun onCountryChange(v: String)     = _state.update { it.copy(country = v) }
    fun onStateChange(v: String)       = _state.update { it.copy(state = v) }
    fun onCityChange(v: String)        = _state.update { it.copy(city = v) }
    fun onPictureSelected(uri: Uri?)   = _state.update { it.copy(pictureUri = uri) }

    fun onUsernameTooltipToggle()    = _state.update { it.copy(showUsernameTooltip = !it.showUsernameTooltip) }
    fun onFullNameTooltipToggle()    = _state.update { it.copy(showFullNameTooltip = !it.showFullNameTooltip) }
    fun onDisplayNameTooltipToggle() = _state.update { it.copy(showDisplayNameTooltip = !it.showDisplayNameTooltip) }
}