package com.rinx.artRINXapp.feature.profile.presentation.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.ui.TextLimits
import com.rinx.artRINXapp.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedbackUiState(
    val text: String = "",
    val isSending: Boolean = false,
)

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    // One-shot toast (success / failure) and a "close the dialog" signal.
    private val _message = Channel<String>(Channel.BUFFERED)
    val message = _message.receiveAsFlow()
    private val _closed = Channel<Unit>(Channel.BUFFERED)
    val closed = _closed.receiveAsFlow()

    fun onTextChange(value: String) = _uiState.update { it.copy(text = value.take(TextLimits.FEEDBACK)) }

    fun submit() {
        val review = _uiState.value.text.trim()
        if (review.isEmpty() || _uiState.value.isSending) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            when (profileRepository.submitFeedback(review)) {
                is ApiResult.Success -> {
                    _uiState.update { FeedbackUiState() }
                    _message.send("Thanks for your feedback!")
                    _closed.send(Unit)
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isSending = false) }
                    _message.send("Couldn't send feedback — try again")
                }
            }
        }
    }
}
