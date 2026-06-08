package com.rinx.artRINXapp.feature.upload.data.remote.dto

import com.google.gson.annotations.SerializedName

// ── Step 1: prepare-upload → signed URL + file path ──────────────────────────
// NOTE: this response is camelCase on the wire (per the live swagger), unlike most endpoints.

data class PrepareUploadDto(
    @SerializedName("filePath") val filePath: String? = null,
    @SerializedName("uploadUrl") val uploadUrl: String? = null,
    @SerializedName("rekognitionTags") val rekognitionTags: String? = null,
)

// ── Step 3: create-artwork result ────────────────────────────────────────────

data class CreateArtworkResultDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
)
