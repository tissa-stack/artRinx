package com.rinx.artRINXapp.feature.events.domain.repository

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.events.domain.model.EventDetail

interface EventsRepository {
    /** Fetch a single event for the popup. NotFound is surfaced for legacy 404 event ids. */
    suspend fun getEvent(id: Long): ApiResult<EventDetail>
}
