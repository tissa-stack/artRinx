package com.rinx.artRINXapp.feature.create.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.rinx.artRINXapp.R
import com.rinx.artRINXapp.core.theme.BrandPrimary
import com.rinx.artRINXapp.core.theme.LocalDimens
import com.rinx.artRINXapp.core.theme.Spacing
import com.rinx.artRINXapp.feature.home.presentation.components.BottomNavBar

private val UploadArtColor     = Color(0xFF45B1E8)
private val NewCollectionColor = Color(0xFF9C5CF8)

@Composable
fun CreateScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNewArt: (String) -> Unit = {},
    onNavigateToNewCuration: () -> Unit = {},
    viewModel: CreateViewModel = hiltViewModel(),
) {
    val d       = LocalDimens.current
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var dialog by remember { mutableStateOf<CreateViewModel.UploadAction?>(null) }

    // ── Photo picker (PickVisualMedia handles API 33+ natively; falls back on older) ──
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            // Persist read-URI permission so Coil can load it after navigation
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            onNavigateToNewArt(uri.toString())
        }
    }

    // ── Permission launcher (needed for Android < 13) ─────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) photoPickerLauncher.launch(PickVisualMediaRequest(ImageOnly))
    }

    fun launchPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: Photo Picker needs no permission
            photoPickerLauncher.launch(PickVisualMediaRequest(ImageOnly))
        } else {
            val perm = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED) {
                photoPickerLauncher.launch(PickVisualMediaRequest(ImageOnly))
            } else {
                permissionLauncher.launch(perm)
            }
        }
    }

    // Upload-Art gate (handout §Upload tap handler): paid+limit → alert; Gallery → web; else → upgrade.
    fun onUploadArtTap() {
        when (viewModel.resolveUploadAction()) {
            CreateViewModel.UploadAction.OPEN_PICKER,
            CreateViewModel.UploadAction.LOADING -> launchPicker()
            else -> dialog = viewModel.resolveUploadAction()
        }
    }

    // ── Upload-gate dialogs ───────────────────────────────────────────────────
    when (dialog) {
        CreateViewModel.UploadAction.LIMIT_REACHED -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Upload limit reached") },
            text = { Text("You've reached your plan's active-artwork limit. Remove an existing piece to upload a new one.") },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("OK") } },
        )
        CreateViewModel.UploadAction.GALLERY_WEB -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Gallery plan required") },
            text = {
                Text(
                    "Uploading art requires an active Gallery plan, which is managed on artrinx.com. " +
                        "Visit our website to view or change your plan.",
                )
            },
            // Anti-steering: "Open Website" carries no purchase verb; no in-app purchase/Restore.
            confirmButton = {
                TextButton(onClick = {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_VIEW, "https://artrinx.com".toUri()),
                        )
                    }
                    dialog = null
                }) { Text("Open Website") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Not Now") } },
        )
        CreateViewModel.UploadAction.UPGRADE_REQUIRED -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Upgrade required") },
            text = { Text("Uploading art requires an Artist Pro plan. You can upgrade from Settings → Profile title and plan.") },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("OK") } },
        )
        else -> Unit
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                activeRoute = "create",
                onNavigate  = { route ->
                    when (route) {
                        "home"          -> onNavigateToHome()
                        "search"        -> onNavigateToSearch()
                        "notifications" -> onNavigateToNotifications()
                        "profile"       -> onNavigateToProfile()
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .padding(horizontal = Spacing.lg),
        ) {
            Spacer(Modifier.height(Spacing.xl))

            Text(
                text       = "Create",
                style      = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(Spacing.xl))

            // ── Option cards ──────────────────────────────────────────────
            // IntrinsicSize.Max + fillMaxHeight keeps both cards the same height even when one
            // title wraps to two lines (e.g. "New Collection") and the other stays single-line.
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                CreateOptionCard(
                    iconRes  = R.drawable.ic_upload_art,
                    iconColor = UploadArtColor,
                    title    = "Upload Art",
                    subtitle = "Share your work",
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick  = { onUploadArtTap() },
                )
                Spacer(Modifier.width(Spacing.md))
                CreateOptionCard(
                    iconRes  = R.drawable.ic_create_curation,
                    iconColor = NewCollectionColor,
                    title    = "New Collection",
                    subtitle = "Curate pieces",
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick  = { onNavigateToNewCuration() },
                )
            }

            Spacer(Modifier.height(Spacing.md))

            // ── Upload limit card ─────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(d.cardCornerRadius))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = "Upload limit",
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text  = state.uploadLimitText.ifBlank { "Uploads" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier         = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, BrandPrimary, RoundedCornerShape(50))
                        .clickable {
                            Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = "Upgrade",
                        style      = MaterialTheme.typography.labelMedium,
                        color      = BrandPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

// ── Option card ───────────────────────────────────────────────────────────────

@Composable
private fun CreateOptionCard(
    iconRes: Int,
    iconColor: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val d = LocalDimens.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(d.cardCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(Spacing.lg),
    ) {
        Box(
            modifier         = Modifier
                .size(Spacing.giant)
                .clip(RoundedCornerShape(Spacing.sm))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter            = painterResource(iconRes),
                contentDescription = null,
                tint               = iconColor,
                modifier           = Modifier.size(Spacing.xxl),
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text       = title,
            style      = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text  = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
