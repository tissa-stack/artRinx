package com.rinx.artRINXapp.feature.events.data.repository

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.core.network.serverMessageOrNull
import com.rinx.artRINXapp.feature.events.data.remote.EventsApiService
import com.rinx.artRINXapp.feature.events.data.remote.dto.EventDto
import com.rinx.artRINXapp.feature.events.domain.EventFormatting
import com.rinx.artRINXapp.feature.events.domain.model.EventDetail
import com.rinx.artRINXapp.feature.events.domain.repository.EventsRepository
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class EventsRepositoryImpl @Inject constructor(
    private val apiService: EventsApiService,
) : EventsRepository {

    override suspend fun getEvent(id: Long): ApiResult<EventDetail> = safeCall {
        val response = apiService.getEvent(id)
        val dto = response.body()?.data
        if (response.isSuccessful && dto != null) {
            ApiResult.Success(dto.toDetail(id))
        } else {
            errorFor(response)
        }
    }

    private fun EventDto.toDetail(fallbackId: Long): EventDetail = EventDetail(
        id = id ?: fallbackId,
        name = eventName.orEmpty(),
        imageUrl = imageUrl,
        dateText = EventFormatting.formatDate(startAtUtc, eventTz),
        timeText = EventFormatting.formatTimeRange(startAtUtc, endAtUtc, eventTz),
        locationLines = EventFormatting.locationLines(
            venueName = venueName,
            streetAddress = streetAddress,
            city = city,
            state = state,
            postalCode = postalCode,
            country = country,
        ),
    )

    private inline fun <T> safeCall(block: () -> ApiResult<T>): ApiResult<T> = try {
        block()
    } catch (e: IOException) {
        ApiResult.Error.Network(e)
    } catch (e: Exception) {
        ApiResult.Error.Unknown(e)
    }

    private fun errorFor(response: Response<*>): ApiResult.Error {
        val body = runCatching { response.errorBody()?.string() }.getOrNull()
        return when (response.code()) {
            404 -> ApiResult.Error.NotFound("Event not found")
            in 400..499 -> ApiResult.Error.Validation(
                serverMessageOrNull(body) ?: "Request failed (${response.code()})",
            )
            in 500..599 -> ApiResult.Error.Server(response.code())
            else -> ApiResult.Error.Unknown(RuntimeException("HTTP ${response.code()}"))
        }
    }
}
