package com.example.artrinx.feature.auth.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ApiErrorResponse(
    val detail: List<ErrorDetail>,
)

data class ErrorDetail(
    val loc: List<String>,
    val msg: String,
    val type: String,
)

data class ApiMessageResponse(
    @SerializedName("message") val message: String?,
)
