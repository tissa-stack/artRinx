package com.rinx.artRINXapp.feature.notifications.presentation.messages.components

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.Spacing

@Composable
fun ChatMenuScreen(
    onBack: () -> Unit,
    onViewProfile: () -> Unit,
    onChatDeleted: () -> Unit,
    viewModel: ChatMenuViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val userName = state.name
    val userRole = state.role
    val userHandle = state.handle

    // Two-step report flow: reason → confirmation
    var showReasonSheet   by remember { mutableStateOf(false) }
    var showReportSheet   by remember { mutableStateOf(false) }
    var showBlockedDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title        = "Delete messages?",
            message      = "This will delete all messages with $userName. " +
                           "They'll stay in your inbox, but the conversation can't be recovered.",
            confirmLabel = "Delete",
            onConfirm    = {
                showDeleteConfirm = false
                viewModel.deleteChat()
                onChatDeleted()
            },
            onDismiss    = { showDeleteConfirm = false },
        )
    }

    if (showReasonSheet) {
        ReportReasonSheet(
            onDismiss = { showReasonSheet = false },
            onSubmit  = {
                viewModel.reportUser()
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
                viewModel.blockUser()
                showReportSheet = false
                showBlockedDialog = true
            },
            onUnfollow = {
                viewModel.unfollowUser()
                showReportSheet = false
            },
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
                Triple(null,                                   "Delete messages", { showDeleteConfirm = true }),
                Triple(painterResource(R.drawable.ic_report), "Report profile", { showReasonSheet = true }),
                Triple(painterResource(R.drawable.ic_block),  "Block profile",  { viewModel.blockUser(); showBlockedDialog = true }),
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
