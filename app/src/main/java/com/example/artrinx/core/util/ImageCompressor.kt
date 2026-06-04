package com.example.artrinx.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Reads a picked content [Uri], fixes EXIF rotation, downsamples + scales the longest edge to at
 * most [DEFAULT_MAX_EDGE]px, and re-encodes to JPEG. The backend documents no compression rules;
 * we standardize on JPEG to match the `image/jpeg` signed-URL PUT and to keep uploads small.
 *
 * Also returns the post-rotation `aspect_ratio` (width / height) the create-artwork call expects.
 * Convention matches the feed: <1 = portrait (tall), >1 = landscape (short).
 */
@Singleton
class ImageCompressor @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    data class Encoded(
        val jpeg: ByteArray,
        /** width / height, formatted with 4 decimals as a string for the form field. */
        val aspectRatio: String,
    ) {
        // ByteArray needs identity-free equals/hashCode for data-class correctness.
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Encoded) return false
            return jpeg.contentEquals(other.jpeg) && aspectRatio == other.aspectRatio
        }

        override fun hashCode(): Int = 31 * jpeg.contentHashCode() + aspectRatio.hashCode()
    }

    suspend fun compress(
        uri: Uri,
        maxEdge: Int = DEFAULT_MAX_EDGE,
        quality: Int = DEFAULT_QUALITY,
    ): Encoded = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        // 1. Bounds-only pass to compute a power-of-two downsample factor.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unable to read image dimensions" }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = computeSampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
        }
        var bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: throw IllegalArgumentException("Unable to decode image")

        // 2. Apply EXIF orientation so the uploaded image is upright.
        bitmap = applyExifRotation(uri, bitmap)

        // 3. Hard-cap the longest edge after rotation.
        bitmap = scaleToMaxEdge(bitmap, maxEdge)

        val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()

        // 4. Encode to JPEG.
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), out)
        bitmap.recycle()

        Encoded(jpeg = out.toByteArray(), aspectRatio = String.format("%.4f", aspect))
    }

    private fun computeSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        var longest = max(width, height)
        // Halve until the longest edge is within ~2x of the target (final exact scale done later).
        while (longest / 2 >= maxEdge) {
            longest /= 2
            sample *= 2
        }
        return sample
    }

    private fun applyExifRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    private fun scaleToMaxEdge(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / longest
        val w = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
        if (scaled != bitmap) bitmap.recycle()
        return scaled
    }

    private companion object {
        const val DEFAULT_MAX_EDGE = 2048
        const val DEFAULT_QUALITY = 85
    }
}
