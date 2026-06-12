package com.rinx.artRINXapp.feature.notifications.presentation.messages.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.rinx.artRINXapp.core.util.initialsOf
import com.rinx.artRINXapp.core.util.pastelColorFor
import com.rinx.artRINXapp.feature.home.presentation.components.shimmer.rememberShimmerBrush

/**
 * Circular profile avatar with a Coil cache key that is stable across re-signed CDN URLs, a
 * shimmer placeholder while the image is loading, and a graceful no-picture fallback:
 * the person's initials over a stable pastel tint (or a Person icon when no name is known).
 *
 * Profile-picture URLs are signed (a fresh `?signature` each response). We keep the full signed URL
 * as the request data but derive the cache key from the stable path (without the query) so reloads
 * serve the cached bitmap instantly — no flicker, and the shimmer only shows on a genuine first load.
 */
@Composable
fun RinxAvatar(
    url: String?,
    contentDescription: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    @DrawableRes fallbackRes: Int? = null,
    /** When set, the no-picture fallback shows the person's initials (e.g. "SK") on a pastel tint. */
    name: String? = null,
) {
    val shaped = modifier.size(size).clip(CircleShape)
    val initials = remember(name) { initialsOf(name) }

    if (url.isNullOrBlank()) {
        AvatarFallback(shaped, initials, name, fallbackRes, contentDescription, size)
        return
    }

    val key = remember(url) { url.substringBefore("?") }
    // Pin an explicit pixel size on the request. Without it, Coil derives the target size from the
    // painter's draw pass — but while loading we render a shimmer instead of the Image, so the painter
    // is never drawn and the request would hang in Loading forever (perpetual shimmer).
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .size(sizePx)
            .memoryCacheKey(key)
            .diskCacheKey(key)
            .placeholderMemoryCacheKey(key)
            .crossfade(false)
            .build(),
    )
    Box(modifier = shaped) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading ->
                Box(Modifier.fillMaxSize().background(rememberShimmerBrush()))
            is AsyncImagePainter.State.Error ->
                AvatarFallback(Modifier.fillMaxSize(), initials, name, fallbackRes, contentDescription, size)
            else ->
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
        }
    }
}

/** No-picture fallback: drawable → initials-on-pastel → person icon. [modifier] is pre-sized + clipped. */
@Composable
private fun AvatarFallback(
    modifier: Modifier,
    initials: String?,
    name: String?,
    @DrawableRes fallbackRes: Int?,
    contentDescription: String?,
    size: Dp,
) {
    when {
        fallbackRes != null -> Image(
            painter = painterResource(fallbackRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(CircleShape),
        )
        initials != null -> Box(
            modifier = modifier.clip(CircleShape).background(pastelColorFor(name)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                color = Color(0xFF1D1D1D),
                fontWeight = FontWeight.SemiBold,
                fontSize = with(LocalDensity.current) { (size * 0.38f).toSp() },
            )
        }
        else -> Box(
            modifier = modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.6f),
            )
        }
    }
}
