package com.rinx.artRINXapp.feature.events.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * `GET /api/events/{id}` → data (V1.9 popup contract).
 *
 * Every field is nullable: the backend can return partially-filled rows and legacy aliases
 * (date/start_time/zipcode/…) which we intentionally ignore. Times are ISO-8601 UTC; render
 * them in [eventTz] (IANA), falling back to UTC when it's missing/unparseable.
 */
data class EventDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("event_name") val eventName: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("venue_name") val venueName: String? = null,
    @SerializedName("street_address") val streetAddress: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("postal_code") val postalCode: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("start_at_utc") val startAtUtc: String? = null,
    @SerializedName("end_at_utc") val endAtUtc: String? = null,
    @SerializedName("event_tz") val eventTz: String? = null,
)
