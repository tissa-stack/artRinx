package com.rinx.artRINXapp.feature.auth.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ConsentsDto(
    @SerializedName("accepted_terms") val acceptedTerms: Boolean = false,
    @SerializedName("sms_2fa_consent") val sms2faConsent: Boolean = false,
    @SerializedName("account_notification_sms") val accountNotificationSms: Boolean = false,
    @SerializedName("marketing_sms_consent") val marketingSmsConsent: Boolean = false,
)
