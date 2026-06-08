package com.rinx.artRINXapp.feature.auth.domain.model

sealed class ProfileType(val id: Int, val label: String) {
    data object Artist : ProfileType(1, "Artist")
    data object ArtCurious : ProfileType(3, "Art Curious")
    data object Collector : ProfileType(2, "Collector")
    data object Gallery : ProfileType(4, "Gallery")

    companion object {
        val all: List<ProfileType> = listOf(Artist, ArtCurious, Collector, Gallery)
    }
}
