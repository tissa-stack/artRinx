package com.example.artrinx.feature.auth.data.remote.dto

data class ApiErrorResponse(
    val detail: List<ErrorDetail>,
)

data class ErrorDetail(
    val loc: List<String>,
    val msg: String,
    val type: String,
)
