package com.rinx.artRINXapp.feature.auth.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WaitlistRequest(
    @SerializedName("email") val email: String,
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("profile_type_id") val profileTypeId: Int,
    // Optional per handout — omitted from the request when blank.
    @SerializedName("instagram_handle") val instagramHandle: String? = null,
    @SerializedName("accepted_terms") val acceptedTerms: Boolean,
    @SerializedName("sms_notifications_opt_in") val smsNotificationsOptIn: Boolean,
    @SerializedName("device_type") val deviceType: String = "android",
    @SerializedName("source_tag") val sourceTag: String? = null,
)
