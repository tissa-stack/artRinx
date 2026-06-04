package com.example.artrinx.feature.profile.domain.model

data class ProfileType(
    val id: Int,
    val name: String,
)

/** The current logged-in user, used to attribute uploads to "myself". */
data class CurrentUser(
    val id: Int,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
)

data class Medium(
    val id: Int,
    val title: String,
    val pictureUrl: String,
)

data class ProfileDraft(
    val step: Int = 0,
    val groundRulesAccepted: Boolean = false,
    val profileTypeId: Int? = null,
    val fullName: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val age: String = "",
    val country: String = "",
    val state: String = "",
    val city: String = "",
    val mediumIds: Set<Int> = emptySet(),
)
