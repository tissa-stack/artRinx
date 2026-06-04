package com.example.artrinx.feature.upload.domain.model

import android.net.Uri

/**
 * Cross-screen state of the currently-running (or just-finished) artwork upload.
 * Every state carries [localThumb] (the picked/compressed image) so the "Uploading" row and the
 * optimistic feed/profile items can render instantly, and [isPrivate] so Home (public) and
 * Profile (private) know whether the result belongs to them.
 */
sealed interface UploadProgress {
    val localThumb: Uri
    val isPrivate: Boolean

    data class Compressing(
        override val localThumb: Uri,
        override val isPrivate: Boolean,
    ) : UploadProgress

    data class Uploading(
        override val localThumb: Uri,
        override val isPrivate: Boolean,
        val percent: Int,
    ) : UploadProgress

    data class Finalizing(
        override val localThumb: Uri,
        override val isPrivate: Boolean,
    ) : UploadProgress

    data class Success(
        override val localThumb: Uri,
        override val isPrivate: Boolean,
        val artwork: CreatedArtwork,
        val title: String,
        val artistName: String,
        val artistHandle: String,
    ) : UploadProgress

    data class Failed(
        override val localThumb: Uri,
        override val isPrivate: Boolean,
        val retryable: Boolean,
        val message: String,
    ) : UploadProgress
}
