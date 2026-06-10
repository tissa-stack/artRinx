package com.rinx.artRINXapp.feature.events.data.remote

import com.rinx.artRINXapp.feature.events.data.remote.dto.EventDto
import com.rinx.artRINXapp.feature.home.data.remote.dto.EnvelopeDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/** Event endpoints (V1.9). */
interface EventsApiService {

    /** Event popup shape for `GET /api/events/{id}`. */
    @GET("api/events/{id}")
    suspend fun getEvent(@Path("id") id: Long): Response<EnvelopeDto<EventDto>>
}
