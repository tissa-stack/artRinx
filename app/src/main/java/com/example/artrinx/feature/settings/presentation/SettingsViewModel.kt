package com.example.artrinx.feature.settings.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Settings is a static menu for now — no remote state. The ViewModel exists to
 * keep the screen consistent with the app's MVVM pattern and to host any future
 * logout / account state.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel()