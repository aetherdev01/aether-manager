package dev.aether.manager.update

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// ─────────────────────────────────────────────────────────────
// Entry point: tampilkan dialog jika ada update
// ─────────────────────────────────────────────────────────────

@Composable
fun UpdateDialogHost(viewModel: UpdateViewModel) {
    val state     by viewModel.state.collectAsState()
    val dismissed by viewModel.dismissed.collectAsState()

    val updateInfo = (state as? UpdateUiState.UpdateAvailable)?.info ?: return
    if (dismissed) return

    UpdateDialog(
        info      = updateInfo,
        onDismiss = { viewModel.dismiss() },
    )
}

// ─────────────────────────────────────────────────────────────
// Dialog utama
// ─────────────────────────────────────────────────────────────

private sealed class DownloadState {
    object Idle         : DownloadState()
    data class Progress(val percent: Int) : DownloadState()
    data class Done(val apkFile: File)   : DownloadState()
    data class Failed(val reason: String): DownloadState()
}

@Composable
fun UpdateDialog(
    info: ReleaseInfo,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    var dlState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }

    Dialog(
        onDismissRequest = {
            // Force update = tidak bisa dismiss dengan back/outside tap
            if (!info.isForceUpdate) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress    = !info.isForceUpdate,
            dismissOnClickOutside = !info.isForceUpdate,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header icon + gradient
                UpdateHeaderBadge(isForce = info.isForceUpdate)

                // Title
                Text(
                    text = if (info.isForceUpdate) "Update Wajib Tersedia" else "Update Tersedia",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                // Version chip
                VersionChip(version = info.latestVersion)

                // Release notes
                if (info.releaseNotes.isNotBlank()) {
                    ReleaseNotesBox(notes = info.releaseNotes)
                }

                // Force update notice
                if (info.isForceUpdate) {
                    ForceUpdateNotice()
                }

                Spacer(Modifier.height(4.dp))

                // Download / progress
                when (val dl = dlState) {
                    is DownloadState.Idle -> {
                        Button(
                            onClick = {
                                scope.launch {
                                    downloadAndInstall(
                                        context   = context,
                                        url       = info.downloadUrl,
                                        onProgress = { dlState = DownloadState.Progress(it) },
                                        onDone    = { dlState = DownloadState.Done(it) },
                                        onFailed  = { dlState = DownloadState.Failed(it) },
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Outlined.Download, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Download & Install  v${info.latestVersion}")
                        }

                        // Tombol skip hanya untuk soft update
                        if (!info.isForceUpdate) {
                            TextButton(
                                onClick  = onDismiss,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Nanti Saja", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    is DownloadState.Progress -> {
                        DownloadProgressBar(percent = dl.percent)
                    }

                    is DownloadState.Done -> {
                        // Langsung trigger install APK
                        LaunchedEffect(dl.apkFile) {
                            installApk(context, dl.apkFile)
                        }
                        DownloadProgressBar(percent = 100)
                        Text(
                            "Membuka installer...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    is DownloadState.Failed -> {
                        Text(
                            "Gagal: ${dl.reason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        OutlinedButton(
                            onClick  = { dlState = DownloadState.Idle },
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(12.dp),
                        ) {
                            Text("Coba Lagi")
                        }
                        // Buka browser sebagai fallback
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, info.releasePageUrl.toUri())
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Buka di Browser")
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────

@Composable
private fun UpdateHeaderBadge(isForce: Boolean) {
    val color = if (isForce)
        MaterialTheme.colorScheme.errorContainer
    else
        MaterialTheme.colorScheme.primaryContainer

    val iconColor = if (isForce)
        MaterialTheme.colorScheme.onErrorContainer
    else
        MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isForce) Icons.Outlined.Warning else Icons.Outlined.NewReleases,
            contentDescription = null,
            tint   = iconColor,
            modifier = Modifier.size(36.dp),
        )
    }
}

@Composable
private fun VersionChip(version: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text  = "v$version",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ReleaseNotesBox(notes: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 180.dp)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text  = "Changelog",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text  = notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun ForceUpdateNotice() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        color    = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Warning,
                null,
                tint     = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text  = "Update ini wajib dipasang untuk melanjutkan menggunakan aplikasi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun DownloadProgressBar(percent: Int) {
    val progress by animateFloatAsState(
        targetValue = percent / 100f,
        animationSpec = tween(300),
        label = "dl_progress",
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Mengunduh APK...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "$percent%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Download + Install helpers
// ─────────────────────────────────────────────────────────────

private suspend fun downloadAndInstall(
    context: Context,
    url: String,
    onProgress: (Int) -> Unit,
    onDone: (File) -> Unit,
    onFailed: (String) -> Unit,
) = withContext(Dispatchers.IO) {
    try {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout    = 60_000
            instanceFollowRedirects = true
        }
        conn.connect()

        if (conn.responseCode != 200) {
            withContext(Dispatchers.Main) { onFailed("HTTP ${conn.responseCode}") }
            return@withContext
        }

        val totalBytes = conn.contentLengthLong
        val outFile    = File(context.cacheDir, "aether-manager-update.apk")

        conn.inputStream.use { input ->
            outFile.outputStream().use { output ->
                val buf = ByteArray(8192)
                var downloaded = 0L
                var read: Int
                while (input.read(buf).also { read = it } != -1) {
                    output.write(buf, 0, read)
                    downloaded += read
                    if (totalBytes > 0) {
                        val pct = (downloaded * 100 / totalBytes).toInt()
                        withContext(Dispatchers.Main) { onProgress(pct) }
                    }
                }
            }
        }

        withContext(Dispatchers.Main) { onDone(outFile) }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) { onFailed(e.message ?: "Download gagal") }
    }
}

private fun installApk(context: Context, apkFile: File) {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback: buka browser
        val intent = Intent(Intent.ACTION_VIEW, apkFile.toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
