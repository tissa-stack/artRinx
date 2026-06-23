package com.rinx.artRINXapp.feature.profile.data.repository

import com.rinx.artRINXapp.core.network.ApiResult
import com.rinx.artRINXapp.feature.profile.data.remote.LocationApiService
import com.rinx.artRINXapp.feature.profile.domain.repository.CountryOption
import com.rinx.artRINXapp.feature.profile.domain.repository.MasterLocationRepository
import com.rinx.artRINXapp.feature.profile.domain.repository.StateOption
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasterLocationRepositoryImpl @Inject constructor(
    private val apiService: LocationApiService,
) : MasterLocationRepository {

    // The world country list is static → cache it for the session. States/cities are param-scoped.
    @Volatile private var countriesCache: List<CountryOption>? = null

    override suspend fun getCountries(): ApiResult<List<CountryOption>> {
        countriesCache?.let { return ApiResult.Success(it) }
        return safeCall {
            val response = apiService.getCountries()
            if (response.isSuccessful) {
                val items = response.body()?.data?.items.orEmpty().mapNotNull { dto ->
                    val name = dto.name?.trim()?.ifBlank { null } ?: return@mapNotNull null
                    val iso2 = dto.iso2?.trim()?.ifBlank { null } ?: return@mapNotNull null
                    // Normalize the dial code to a leading "+" (the catalog ships it bare, e.g. "91").
                    val phoneCode = dto.phoneCode?.trim()?.ifBlank { null }
                        ?.let { "+" + it.removePrefix("+") }
                    val emoji = dto.emoji?.trim()?.ifBlank { null }
                    CountryOption(name, iso2, phoneCode, emoji)
                }
                countriesCache = items
                ApiResult.Success(items)
            } else {
                errorFor(response)
            }
        }
    }

    override suspend fun getStates(countryIso2: String): ApiResult<List<StateOption>> = safeCall {
        val response = apiService.getStates(countryIso2 = countryIso2)
        if (response.isSuccessful) {
            val items = response.body()?.data?.items.orEmpty().mapNotNull { dto ->
                val name = dto.name?.trim()?.ifBlank { null } ?: return@mapNotNull null
                val code = dto.stateCode?.trim()?.ifBlank { null } ?: return@mapNotNull null
                StateOption(name, code)
            }
            ApiResult.Success(items)
        } else {
            errorFor(response)
        }
    }

    override suspend fun getCities(countryIso2: String, stateCode: String, query: String?): ApiResult<List<String>> = safeCall {
        val response = apiService.getCities(
            countryIso2 = countryIso2,
            stateCode = stateCode,
            q = query?.ifBlank { null },
        )
        if (response.isSuccessful) {
            // distinct(): the catalog can contain repeated city names (e.g. two distinct "Amaravati"),
            // which would render as identical rows and previously crashed the picker's LazyColumn.
            ApiResult.Success(response.body()?.data?.items.orEmpty().mapNotNull { it.name?.trim()?.ifBlank { null } }.distinct())
        } else {
            errorFor(response)
        }
    }

    private inline fun <T> safeCall(block: () -> ApiResult<T>): ApiResult<T> = try {
        block()
    } catch (e: IOException) {
        ApiResult.Error.Network(e)
    } catch (e: Exception) {
        ApiResult.Error.Unknown(e)
    }

    private fun errorFor(response: Response<*>): ApiResult.Error = when (response.code()) {
        in 400..499 -> ApiResult.Error.Validation("Request failed (${response.code()})")
        in 500..599 -> ApiResult.Error.Server(response.code())
        else -> ApiResult.Error.Unknown(RuntimeException("HTTP ${response.code()}"))
    }
}
