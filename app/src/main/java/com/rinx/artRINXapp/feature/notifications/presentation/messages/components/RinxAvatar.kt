package com.rinx.artRINXapp.feature.notifications.presentation.messages.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Circular profile avatar with a Coil cache key that is stable across re-signed CDN URLs.
 *
 * Profile-picture URLs are signed (a fresh `?signature` is handed back on every API response).
 * Coil's default cache key is the whole URL, so a new signature each refresh would miss the
 * cache and re-download — visible as a flicker on the Messages list every time it re-fetches.
 * We keep the full signed URL as the request data (needed to fetch) but derive the cache key
 * from the stable path (without the query), and reuse it as the placeholder key so any reload
 * serves the already-cached bitmap instantly.
 */
@Composable
fun RinxAvatar(
    url: String?,
    contentDescription: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    @DrawableRes fallbackRes: Int? = null,
    /** When set, the no-picture fallback shows the person's initials (e.g. "SK") instead of an icon. */
    name: String? = null,
) {
    val shaped = modifier.size(size).clip(CircleShape)
    val initials = remember(name) { com.rinx.artRINXapp.core.util.initialsOf(name) }
    // Track a load/decode failure so a 404 / corrupt CDN image falls back to the icon, not a
    // broken-image placeholder (handout §Chat list row → avatar).
    var loadFailed by remember(url) { mutableStateOf(false) }
    when {
        !url.isNullOrBlank() && !loadFailed -> {
            val key = remember(url) { url.substringBefore("?") }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .memoryCacheKey(key)
                    .diskCacheKey(key)
                    .placeholderMemoryCacheKey(key)
                    .crossfade(false)
                    .build(),
                contentDescription = contentDescription,
                contentScale       = ContentScale.Crop,
                onError            = { loadFailed = true },
                modifier           = shaped,
            )
        }
        fallbackRes != null -> Image(
            painter            = painterResource(fallbackRes),
            contentDescription = contentDescription,
            contentScale       = ContentScale.Crop,
            modifier           = shaped,
        )
        initials != null -> Box(
            modifier         = shaped.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = initials,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                fontSize   = with(LocalDensity.current) { (size * 0.38f).toSp() },
            )
        }
        else -> Box(
            modifier         = shaped.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(size * 0.6f),
            )
        }
    }
}
