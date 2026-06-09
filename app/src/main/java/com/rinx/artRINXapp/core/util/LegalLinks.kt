package com.rinx.artRINXapp.core.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.withLink

/**
 * Single source of truth for the public artRINX web pages we link out to from the app
 * (registration screen, Ground Rules pop-up, Settings → Resources).
 */
object LegalLinks {
    const val PRIVACY_POLICY = "https://www.artrinx.com/privacy-policy"
    const val TERMS_OF_USE = "https://www.artrinx.com/terms-of-use"
    const val ABOUT_US = "https://www.artrinx.com/about-us"
    const val COMMUNITY_GUIDELINES = "https://www.artrinx.com/community-guidelines"
}

/**
 * Appends [label] as a tappable web link to [url] inside a [buildAnnotatedString] block.
 *
 * Compose 1.7's Text renders the [LinkAnnotation.Url] and routes the tap through
 * [androidx.compose.ui.platform.LocalUriHandler] automatically — no ClickableText or manual
 * offset handling needed, so only the linked words are tappable (not the surrounding sentence).
 */
fun AnnotatedString.Builder.appendLegalLink(
    label: String,
    url: String,
    style: SpanStyle,
) {
    withLink(LinkAnnotation.Url(url = url, styles = TextLinkStyles(style = style))) {
        append(label)
    }
}
