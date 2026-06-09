package com.rinx.artRINXapp.core.location

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads bundled country/state data from assets/locations.json. All countries are listed; states are
 * populated for major countries and empty for the rest (callers should fall back to free text when
 * [statesOf] is empty). The JSON is the single source of truth — drop in a fuller dataset (same
 * schema) to expand coverage without code changes.
 */
@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private data class LocationData(@SerializedName("countries") val countries: List<CountryEntry> = emptyList())
    private data class CountryEntry(
        @SerializedName("name") val name: String = "",
        @SerializedName("states") val states: List<String> = emptyList(),
    )

    private val countries: List<CountryEntry> by lazy { load() }

    /** All country names, in the order they appear in the dataset (alphabetical). */
    fun countryNames(): List<String> = countries.map { it.name }

    /** States/provinces for [country], or empty if none are bundled (→ caller uses free text). */
    fun statesOf(country: String): List<String> =
        countries.firstOrNull { it.name.equals(country.trim(), ignoreCase = true) }?.states ?: emptyList()

    private fun load(): List<CountryEntry> = try {
        context.assets.open("locations.json").bufferedReader().use { reader ->
            Gson().fromJson(reader, LocationData::class.java)?.countries ?: emptyList()
        }
    } catch (_: Exception) {
        emptyList()
    }
}
