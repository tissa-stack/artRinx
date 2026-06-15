package com.rinx.artRINXapp.feature.upload.presentation

import androidx.lifecycle.ViewModel
import com.rinx.artRINXapp.feature.upload.domain.CurationManager
import com.rinx.artRINXapp.feature.upload.domain.UploadManager
import com.rinx.artRINXapp.feature.upload.domain.model.CurationProgress
import com.rinx.artRINXapp.feature.upload.domain.model.UploadProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Re-exposes the app-scoped upload/curation progress so a host-level surface (above the NavHost) can
 * report a PUBLIC upload/curation FAILURE that happens while the user is away from the Home feed —
 * the in-feed progress row only renders on Home, so an off-Home failure would otherwise be silent.
 */
@HiltViewModel
class UploadStatusViewModel @Inject constructor(
    uploadManager: UploadManager,
    curationManager: CurationManager,
) : ViewModel() {
    val uploadProgress: StateFlow<UploadProgress?> = uploadManager.progress
    val curationProgress: StateFlow<CurationProgress?> = curationManager.progress
}
