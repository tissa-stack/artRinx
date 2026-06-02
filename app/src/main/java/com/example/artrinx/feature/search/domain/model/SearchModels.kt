package com.example.artrinx.feature.search.domain.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.example.artrinx.R

enum class SearchPhase { IDLE, ACTIVE_EMPTY, RESULTS }
enum class SortOption { NEWEST, OLDEST, MOST_POPULAR }
enum class ResultTab { ART, USERS, CURATIONS }
enum class CardHeight { SHORT, MEDIUM, TALL }

@Immutable
data class SearchFilter(
    val country: Boolean = false,
    val state: Boolean = false,
    val city: Boolean = false,
    val shopArtOnly: Boolean = false,
    val styles: Set<String> = emptySet(),
) {
    val hasAnySelection: Boolean
        get() = country || state || city || shopArtOnly || styles.isNotEmpty()
}

@Immutable
data class SearchResultItem(
    val id: String,
    @param:DrawableRes val imageRes: Int,
    val title: String,
    val artistName: String,
    val cardHeight: CardHeight,
    val artId: String = "",   // corresponds to ShoppablePost.id for detail navigation
)

val STYLE_OPTIONS = listOf(
    "Digital", "Drawing", "Mixed Media", "Painting", "Photography", "Prints", "Sculpture",
)

val TRENDING_TAGS = listOf("reflections", "water", "lights", "city", "nightphoto")

object MockSearchData {
    val recommended = listOf(
        SearchResultItem("rec1", R.drawable.art_sample_street_poster,  "Foyer de la Danse",   "Edgar Degas",      CardHeight.TALL,   artId = "s1"),
        SearchResultItem("rec2", R.drawable.art_sample_cosmic_swirl,   "Untitled",             "Sophia Ahamed",    CardHeight.SHORT,  artId = "s3"),
        SearchResultItem("rec3", R.drawable.art_sample_fluid_purple,   "La Brioche",           "Édouard Manet",    CardHeight.MEDIUM, artId = "s4"),
        SearchResultItem("rec4", R.drawable.art_sample_painted_hands,  "Chrysanthemum Spider", "Boyan Govedarski", CardHeight.TALL,   artId = "s7"),
        SearchResultItem("rec5", R.drawable.art_sample_paint_brushes,  "Bauerngar",            "Gustav Klimt",     CardHeight.MEDIUM, artId = "s2"),
        SearchResultItem("rec6", R.drawable.art_heaven,                "Heaven's Gate",        "Aria",             CardHeight.SHORT,  artId = "s5"),
        SearchResultItem("rec7", R.drawable.art_sample_artist_outdoors,"Lando",                "Wade Huston",      CardHeight.MEDIUM, artId = "s1"),
        SearchResultItem("rec8", R.drawable.art_sample_neon_corridor,  "Neon Passage",         "Saketh",           CardHeight.SHORT,  artId = "s6"),
    )

    val artResults = listOf(
        SearchResultItem("res1",  R.drawable.art_sample_street_poster,  "Foyer de la Danse",   "Edgar Degas",      CardHeight.TALL,   artId = "s1"),
        SearchResultItem("res2",  R.drawable.art_sample_cosmic_swirl,   "Untitled",             "Sophia Ahamed",    CardHeight.SHORT,  artId = "s3"),
        SearchResultItem("res3",  R.drawable.art_sample_fluid_purple,   "La Brioche",           "Édouard Manet",    CardHeight.MEDIUM, artId = "s4"),
        SearchResultItem("res4",  R.drawable.art_sample_painted_hands,  "Chrysanthemum Spider", "Boyan Govedarski", CardHeight.TALL,   artId = "s7"),
        SearchResultItem("res5",  R.drawable.art_sample_paint_brushes,  "Bauerngar",            "Gustav Klimt",     CardHeight.MEDIUM, artId = "s2"),
        SearchResultItem("res6",  R.drawable.art_heaven,                "Heaven's Gate",        "Aria",             CardHeight.SHORT,  artId = "s5"),
        SearchResultItem("res7",  R.drawable.art_sample_artist_outdoors,"Lando",                "Wade Huston",      CardHeight.TALL,   artId = "s1"),
        SearchResultItem("res8",  R.drawable.art_sample_neon_corridor,  "Neon Passage",         "Saketh",           CardHeight.SHORT,  artId = "s6"),
        SearchResultItem("res9",  R.drawable.art_sample_brush_red,      "Red Study",            "Carlos V",         CardHeight.MEDIUM, artId = "s8"),
        SearchResultItem("res10", R.drawable.art_sample_typewriter,     "Typewriter No.5",      "Ana Lima",         CardHeight.SHORT,  artId = "s2"),
        SearchResultItem("res11", R.drawable.art_sample_pink_glitter,   "Pink Glitter",         "Mira Stone",       CardHeight.TALL,   artId = "s3"),
        SearchResultItem("res12", R.drawable.art_and_artist,            "Plumes",               "Wade Huston",      CardHeight.MEDIUM, artId = "s2"),
    )
}
