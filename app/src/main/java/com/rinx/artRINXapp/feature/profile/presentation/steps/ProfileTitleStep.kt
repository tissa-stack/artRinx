package com.rinx.artRINXapp.feature.profile.presentation.steps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.profile.domain.model.ProfileType

@Composable
fun ProfileTitleStep(
    profileTypes: List<ProfileType>,
    isLoading: Boolean,
    error: String?,
    selectedTypeId: Int?,
    onTypeSelected: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(Spacing.xxl))

        Text(
            text = "Profile title",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "The title on your profile tells others how you use artRinx. You can change this later in profile settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Spacing.xxxl))

        when {
            isLoading -> CircularProgressIndicator(
                color = BrandPrimary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            error != null -> {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(Spacing.md))
                TextButton(onClick = onRetry) {
                    Text("Retry", color = BrandPrimary)
                }
            }

            else -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                profileTypes.forEach { type ->
                    ProfileTypeCard(
                        type = type,
                        selected = type.id == selectedTypeId,
                        onSelect = { onTypeSelected(type.id) },
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.xxxl))
    }
}

@Composable
private fun ProfileTypeCard(
    type: ProfileType,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) BrandPrimary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(200),
        label = "cardBorder_${type.id}",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) BrandPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200),
        label = "cardText_${type.id}",
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) BrandPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(200),
        label = "cardIcon_${type.id}",
    )

    val description = profileTypeDescription(type.name)
    val badge = profileTypeTitleBadge(type.name)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.RadioButton }
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(Spacing.md),
        border = BorderStroke(
            // 1 dp border at all times — slightly thicker when selected
            width = if (selected) 1.5.dp else 1.dp,
            color = borderColor,
        ),
        colors = CardDefaults.cardColors(
            // Background matches screen — cards look like white/dark cards with only a border
            containerColor = MaterialTheme.colorScheme.background,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
        ) {
            // Always-visible row: icon + name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Icon(
                    painter = painterResource(profileTypeIconRes(type.name)),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(Spacing.xl),
                )
                Text(
                    text = type.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = textColor,
                )
            }

            // Description + badge — only visible when this card is selected
            AnimatedVisibility(
                visible = selected,
                enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(200)),
            ) {
                Column {
                    if (description.isNotBlank()) {
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (badge != null) {
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = "Your profile title will be $badge",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontStyle = FontStyle.Italic,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun profileTypeIconRes(name: String): Int = when {
    name.contains("Artist", ignoreCase = true) -> R.drawable.ic_edit_photo
    name.contains("Collector", ignoreCase = true) -> R.drawable.ic_collector
    name.contains("Curious", ignoreCase = true) -> R.drawable.ic_art_curious
    else -> R.drawable.ic_gallery
}

private fun profileTypeDescription(name: String): String = when {
    name.contains("Artist", ignoreCase = true) ->
        "Promote your work, create connections and be inspired."
    name.contains("Collector", ignoreCase = true) ->
        "Connect with new artists, grow and refine your collection."
    name.contains("Curious", ignoreCase = true) ->
        "View art, share art and be inspired, no strings attached!"
    else ->
        "Attract collectors, exhibit art, and promote artists."
}

private fun profileTypeTitleBadge(name: String): String? = when {
    name.contains("Artist", ignoreCase = true) -> "\"Artist\""
    name.contains("Collector", ignoreCase = true) -> null
    name.contains("Curious", ignoreCase = true) -> null
    else -> "\"Gallery\""
}
