package com.rinx.artRINXapp.feature.home.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.DarkCardSurface
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.core.ui.LinkSafetyInfoIcon
import com.rinx.artRINXapp.core.ui.LinkSafetyInfoOverlay

@Composable
fun ShopLinkDialog(
    artistName: String,
    shopUrl: String,
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var urlExpanded by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(Spacing.xl),
            color  = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.xl, vertical = Spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ── Close button ─────────────────────────────────────────
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick  = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = "Close",
                            tint               = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }

                // ── Globe icon ───────────────────────────────────────────
                Icon(
                    painter            = painterResource(R.drawable.ic_shop_globe),
                    contentDescription = null,
                    tint               = BrandPrimary,
                    modifier           = Modifier.size(Spacing.giant + Spacing.xxl),
                )
                Spacer(Modifier.height(Spacing.lg))

                // ── Title ────────────────────────────────────────────────
                Text(
                    text       = "Go to shop",
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onBackground,
                    textAlign  = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.md))

                // ── Warning body ─────────────────────────────────────────
                Text(
                    text      = "Following this link will take you to a third party website, artRinx is not responsible for your safety.",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.md))

                // ── "Would you like to continue?" ────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text  = "Would you like to continue?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    LinkSafetyInfoIcon(onClick = { showInfo = true })
                }
                Spacer(Modifier.height(Spacing.lg))

                // ── Shop Link label + more/less toggle ───────────────────
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text       = "Shop Link",
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onBackground,
                        modifier   = Modifier.weight(1f),
                    )
                    Text(
                        text     = if (urlExpanded) "less" else "more",
                        style    = MaterialTheme.typography.labelMedium,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { urlExpanded = !urlExpanded },
                    )
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text     = shopUrl,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = BrandPrimary,
                    maxLines = if (urlExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                )

                Spacer(Modifier.height(Spacing.xl))

                // ── Go to shop button ────────────────────────────────────
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(BrandPrimary)
                        .clickable {
                            if (shopUrl.isNotEmpty()) uriHandler.openUri(shopUrl)
                            onDismiss()
                        }
                        .padding(vertical = Spacing.md),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = "Go to $artistName's shop",
                        style      = MaterialTheme.typography.labelLarge,
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.height(Spacing.md))

                // ── Go back button ───────────────────────────────────────
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(DarkCardSurface)
                        .clickable { onDismiss() }
                        .padding(vertical = Spacing.md),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = "Go back",
                        style      = MaterialTheme.typography.labelLarge,
                        color      = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.height(Spacing.lg))
            }

            if (showInfo) LinkSafetyInfoOverlay(onClose = { showInfo = false })
          }
        }
    }
}
