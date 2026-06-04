package com.example.artrinx.feature.upload.data.repository

import com.example.artrinx.core.network.ApiResult
import com.example.artrinx.feature.upload.data.remote.UploadApiService
import com.example.artrinx.feature.upload.domain.model.CreatedArtwork
import com.example.artrinx.feature.upload.domain.model.PreparedUpload
import com.example.artrinx.feature.upload.domain.model.UploadRequest
import com.example.artrinx.feature.upload.domain.repository.UploadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

class UploadRepositoryImpl @Inject constructor(
    private val apiService: UploadApiService,
    @param:Named("upload") private val uploadClient: OkHttpClient,
) : UploadRepository {

    override suspend fun prepareUpload(): ApiResult<PreparedUpload> = safeCall {
        val response = apiService.prepareUpload()
        val data = response.body()?.data
        if (response.isSuccessful && data?.filePath != null && data.uploadUrl != null) {
            ApiResult.Success(
                PreparedUpload(
                    filePath = data.filePath,
                    uploadUrl = data.uploadUrl,
                    rekognitionTags = data.rekognitionTags,
                ),
            )
        } else {
            errorFor(response)
        }
    }

    override suspend fun uploadBytes(
        uploadUrl: String,
        jpeg: ByteArray,
        onProgress: (sent: Long, total: Long) -> Unit,
    ): ApiResult<Unit> = safeCall {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(uploadUrl)
                .put(progressBody(jpeg, onProgress))
                // The signed URL is the credential — deliberately NO Authorization header.
                .build()
            uploadClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    ApiResult.Success(Unit)
                } else {
                    errorFor(resp.code)
                }
            }
        }
    }

    override suspend fun createArtwork(
        filePath: String,
        request: UploadRequest,
        aspectRatio: String,
        rekognitionTags: String?,
    ): ApiResult<CreatedArtwork> = safeCall {
        val response = apiService.createArtwork(
            imageUrl = filePath,
            title = request.title,
            description = request.description,
            artistId = request.artistId,
            artistName = request.artistName,
            tags = request.tags.joinToString(",").ifBlank { null },
            mediumId = request.mediumId,
            shopLink = request.shopLink,
            price = request.price,
            privacy = request.isPrivate,
            rekognitionTags = rekognitionTags,
            aspectRatio = aspectRatio,
        )
        val data = response.body()?.data
        if (response.isSuccessful && data?.id != null) {
            ApiResult.Success(CreatedArtwork(id = data.id, imageUrl = data.imageUrl.orEmpty()))
        } else {
            errorFor(response)
        }
    }

    // ── Signed-URL PUT body with byte-level progress ──────────────────────────

    private fun progressBody(
        jpeg: ByteArray,
        onProgress: (sent: Long, total: Long) -> Unit,
    ): RequestBody = object : RequestBody() {
        override fun contentType() = "image/jpeg".toMediaType()
        override fun contentLength(): Long = jpeg.size.toLong()
        override fun writeTo(sink: BufferedSink) {
            val total = jpeg.size.toLong()
            var offset = 0
            var sent = 0L
            while (offset < jpeg.size) {
                val count = minOf(CHUNK, jpeg.size - offset)
                sink.write(jpeg, offset, count)
                offset += count
                sent += count
                onProgress(sent, total)
            }
        }
    }

    // ── Error handling (mirrors HomeRepositoryImpl / SearchRepositoryImpl) ─────

    private inline fun <T> safeCall(block: () -> ApiResult<T>): ApiResult<T> = try {
        block()
    } catch (e: IOException) {
        ApiResult.Error.Network(e)
    } catch (e: Exception) {
        ApiResult.Error.Unknown(e)
    }

    private fun errorFor(response: Response<*>): ApiResult.Error = errorFor(response.code())

    private fun errorFor(code: Int): ApiResult.Error = when (code) {
        in 400..499 -> ApiResult.Error.Validation("Request failed ($code)")
        in 500..599 -> ApiResult.Error.Server(code)
        else -> ApiResult.Error.Unknown(RuntimeException("HTTP $code"))
    }

    private companion object {
        const val CHUNK = 8 * 1024
    }
}
