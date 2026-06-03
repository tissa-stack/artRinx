package com.example.artrinx.feature.settings.presentation.invite

import androidx.lifecycle.ViewModel
import com.example.artrinx.feature.settings.domain.model.Invitee
import com.example.artrinx.feature.settings.domain.model.MockSettingsData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class InviteFriendsUiState(
    val code: String,
    val invitees: List<Invitee>,
    val invitesPerMonth: Int,
)

@HiltViewModel
class InviteFriendsViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(
        InviteFriendsUiState(
            code = MockSettingsData.inviteCode,
            invitees = MockSettingsData.invitees,
            invitesPerMonth = MockSettingsData.invitesPerMonth,
        ),
    )
    val state: StateFlow<InviteFriendsUiState> = _state.asStateFlow()
}