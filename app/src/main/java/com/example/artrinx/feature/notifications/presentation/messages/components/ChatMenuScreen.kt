package com.example.artrinx.feature.notifications.presentation.messages.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.artrinx.R
import com.example.artrinx.core.theme.Spacing

@Composable
fun ChatMenuScreen(
    userName: String,
    userRole: String,
    userHandle: String,
    onBack: () -> Unit,
    onViewProfile: () -> Unit,
    onDeleteMessage: () -> Unit,
) {
    // Two-step report flow: reason → confirmation
    var showReasonSheet   by remember { mutableStateOf(false) }
    var showReportSheet   by remember { mutableStateOf(false) }
    var showBlockedDialog by remember { mutableStateOf(false) }

    if (showReasonSheet) {
        ReportReasonSheet(
            onDismiss = { showReasonSheet = false },
            onSubmit  = {
                showReasonSheet = false
                showReportSheet = true
            },
        )
    }
    if (showReportSheet) {
        ReportSentSheet(
            userName  = userName,
            onDismiss = { showReportSheet = false },
            onBlock   = {
                showReportSheet = false
                showBlockedDialog = true
            },
            onUnfollow = { showReportSheet = false },
        )
    }
    if (showBlockedDialog) {
        BlockedDialog(
            userName        = userName,
            userHandle      = userHandle,
            onDismiss       = { showBlockedDialog = false },
            onReportProfile = {
                showBlockedDialog = false
                showReasonSheet   = true
            },
        )
    }

    Scaffold(contentWindowInsets = WindowInsets(0)) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.xs, end = Spacing.md,
                             top = Spacing.xs, bottom = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), "Back",
                        tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    text       = "$userName, $userRole",
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onBackground,
                    modifier   = Modifier.weight(1f),
                    textAlign  = TextAlign.Center,
                )
                IconButton(onClick = {}) {
                    Icon(Icons.Default.MoreVert, "More",
                        tint = MaterialTheme.colorScheme.onBackground)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // ── Menu options ──────────────────────────────────────────────
            val options = listOf(
                Triple(painterResource(R.drawable.ic_eye),    "View profile",   { onViewProfile() }),
                Triple(null,                                   "Delete message", { onDeleteMessage(); onBack() }),
                Triple(painterResource(R.drawable.ic_report), "Report profile", { showReasonSheet = true }),
                Triple(painterResource(R.drawable.ic_block),  "Block profile",  { showBlockedDialog = true }),
            )

            options.forEach { (icon, label, action) ->
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable { action() }
                        .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (icon != null) {
                        Icon(painter = icon, contentDescription = null,
                            tint     = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(Spacing.xxl))
                    } else {
                        Icon(Icons.Filled.Delete, null,
                            tint     = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(Spacing.xxl))
                    }
                    Spacer(Modifier.width(Spacing.lg))
                    Text(
                        text  = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}
