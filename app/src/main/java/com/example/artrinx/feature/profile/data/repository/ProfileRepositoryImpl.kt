package com.example.artrinx.feature.profile.data.repository

import android.content.Context
import android.net.Uri
import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.profile.data.remote.ProfileApiService
import com.example.artrinx.feature.profile.domain.model.Medium
import com.example.artrinx.feature.profile.domain.model.ProfileDraft
import com.example.artrinx.feature.profile.domain.model.ProfileType
import com.example.artrinx.feature.profile.domain.repository.ProfileRepository
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val apiService: ProfileApiService,
    @ApplicationContext private val context: Context,
) : ProfileRepository {

    private val gson = Gson()
    private val textPlain = "text/plain; charset=utf-8".toMediaType()

    override suspend fun getProfileTypes(): ApiResult<List<ProfileType>> {
        return try {
            val response = apiService.getProfileTypes()
            if (response.isSuccessful) {
                val types = response.body()?.data?.map { ProfileType(it.id, it.name) } ?: emptyList()
                ApiResult.Success(types)
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun checkUsername(username: String): ApiResult<Boolean> {
        return try {
            val response = apiService.checkUsername(username)
            if (response.isSuccessful) {
                ApiResult.Success(response.body()?.data ?: false)
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun getMediums(): ApiResult<List<Medium>> {
        return try {
            val response = apiService.getMediums()
            if (response.isSuccessful) {
                val mediums = response.body()?.data?.map {
                    Medium(it.id, it.title, it.picture)
                } ?: emptyList()
                ApiResult.Success(mediums)
            } else {
                profileError(response.code())
            }
        } catch (e: IOException) {
            ApiResult.Error.Network(e)
        } catch (e: Exception) {
            ApiResult.Error.Unknown(e)
        }
    }

    override suspend fun createProfile(draft: ProfileDraft, pictureUri: Uri?): ApiResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val parts = mutableMapOf<String, RequestBody>()
                parts["username"] = draft.username.toRequestBody(textPlain)
                parts["display_name"] = draft.displayName.toRequestBody(textPlain)
                parts["full_name"] = draft.fullName.toRequestBody(textPlain)
                parts["profile_type_id"] = (draft.profileTypeId?.toString() ?: "").toRequestBody(textPlain)
                parts["accepted_terms"] = "true".toRequestBody(textPlain)
                parts["sms_2fa_consent"] = "false".toRequestBody(textPlain)
                parts["account_notification_sms"] = "false".toRequestBody(textPlain)
                parts["marketing_sms_consent"] = "false".toRequestBody(textPlain)
                if (draft.bio.isNotBlank()) parts["bio"] = draft.bio.toRequestBody(textPlain)
                // Backend requires a numeric age; the UI collects a range ("18-25", "65+",
                // "Under 18"). Send the range's lower bound as digits only.
                ageToNumeric(draft.age)?.let { parts["age"] = it.toRequestBody(textPlain) }
                if (draft.country.isNotBlank()) parts["country"] = draft.country.toRequestBody(textPlain)
                if (draft.state.isNotBlank()) parts["state"] = draft.state.toRequestBody(textPlain)
                if (draft.city.isNotBlank()) parts["city"] = draft.city.toRequestBody(textPlain)
                if (draft.mediumIds.isNotEmpty()) {
                    parts["medium_ids"] = draft.mediumIds.joinToString(",").toRequestBody(textPlain)
                }

                val picturePart = pictureUri?.let { uri ->
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bytes = stream.readBytes()
                        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                        val body = bytes.toRequestBody(mimeType.toMediaType())
                        MultipartBody.Part.createFormData("profile_picture", "profile_picture.jpg", body)
                    }
                }

                val response = apiService.createProfile(parts, picturePart)
                if (response.isSuccessful) {
                    ApiResult.Success(Unit)
                } else {
                    val rawError = response.errorBody()?.string()
                    when (response.code()) {
                        422 -> ApiResult.Error.Validation(parseValidationError(rawError))
                        in 400..499 -> ApiResult.Error.Validation("Profile creation failed. Please check your details.")
                        in 500..599 -> ApiResult.Error.Server(response.code())
                        else -> ApiResult.Error.Unknown(RuntimeException("HTTP ${response.code()}"))
                    }
                }
            } catch (e: IOException) {
                ApiResult.Error.Network(e)
            } catch (e: Exception) {
                ApiResult.Error.Unknown(e)
            }
        }

    /**
     * Converts the UI's age-range label into the digits-only value the backend expects.
     * Sends the range's lower bound: "18-25" -> "18", "26-35" -> "26", "65+" -> "65",
     * "Under 18" -> "17". Returns null for a blank/unparseable value so the field is omitted.
     * If the input is already numeric (e.g. a future numeric input field) it passes through.
     */
    private fun ageToNumeric(age: String): String? {
        if (age.isBlank()) return null
        if (age.contains("under", ignoreCase = true)) return "17"
        val firstNumber = age.dropWhile { !it.isDigit() }.takeWhile { it.isDigit() }
        return firstNumber.ifBlank { null }
    }

    private fun profileError(code: Int): ApiResult.Error = when (code) {
        in 400..499 -> ApiResult.Error.Validation("Request failed ($code)")
        in 500..599 -> ApiResult.Error.Server(code)
        else -> ApiResult.Error.Unknown(RuntimeException("HTTP $code"))
    }

    private fun parseValidationError(body: String?): String {
        if (body == null) return "Validation failed"
        return try {
            val obj = gson.fromJson(body, Map::class.java)
            (obj["detail"] as? List<*>)
                ?.filterIsInstance<Map<*, *>>()
                ?.firstOrNull()
                ?.get("msg") as? String
                ?: "Validation failed"
        } catch (_: Exception) {
            "Validation failed"
        }
    }
}
