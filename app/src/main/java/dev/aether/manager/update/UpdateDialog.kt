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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
// Entry point
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
// Download state
// ─────────────────────────────────────────────────────────────

private sealed class DownloadState {
    object Idle                          : DownloadState()
    data class Progress(val percent: Int): DownloadState()
    data class Done(val apkFile: File)   : DownloadState()
    data class Failed(val reason: String): DownloadState()
}

// ─────────────────────────────────────────────────────────────
// Dialog utama
// ─────────────────────────────────────────────────────────────

@Composable
fun UpdateDialog(
    info: ReleaseInfo,
    onDismiss: () -> Unit,
) {
    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()
    var dlState   by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }

    // Tab: "Deskripsi" atau "Changelog"
    var selectedTab by remember { mutableIntStateOf(0) }
    // Changelog yang di-fetch langsung dari GitHub Release API
    var changelog by remember { mutableStateOf(info.releaseNotes) }
    var fetchingChangelog by remember { mutableStateOf(false) }

    // Fetch changelog segar dari repo saat dialog dibuka
    LaunchedEffect(info.releasePageUrl) {
        fetchingChangelog = true
        val fresh = fetchChangelogFromGitHub(info.releasePageUrl)
        if (fresh != null) changelog = fresh
        fetchingChangelog = false
    }

    Dialog(
        onDismissRequest = {
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
            shape          = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            color          = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {

                // ── Header icon ───────────────────────────────────
                UpdateHeaderBadge(isForce = info.isForceUpdate)

                // ── Title ─────────────────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = if (info.isForceUpdate) "Update Wajib Tersedia" else "Update Tersedia",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Aether Manager",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // ── Version chip ──────────────────────────────────
                VersionChip(version = info.latestVersion)

                // ── Tab: Deskripsi / Changelog ────────────────────
                val tabs = listOf("Deskripsi", "Changelog")
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    indicator = {},
                    divider   = {},
                ) {
                    tabs.forEachIndexed { idx, label ->
                        val selected = selectedTab == idx
                        Tab(
                            selected = selected,
                            onClick  = { selectedTab = idx },
                            modifier = Modifier
                                .padding(3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.surface
                                    else androidx.compose.ui.graphics.Color.Transparent
                                ),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                }

                // ── Tab content ───────────────────────────────────
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label = "tab_content",
                ) { tab ->
                    when (tab) {
                        0 -> DescriptionBox(isForce = info.isForceUpdate)
                        1 -> ChangelogBox(notes = changelog, loading = fetchingChangelog)
                        else -> Unit
                    }
                }

                // ── Force notice ──────────────────────────────────
                if (info.isForceUpdate) {
                    ForceUpdateNotice()
                }

                // ── Buttons ───────────────────────────────────────
                when (val dl = dlState) {
                    is DownloadState.Idle -> {
                        Column(
                            Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        downloadAndInstall(
                                            context    = context,
                                            url        = info.downloadUrl,
                                            onProgress = { dlState = DownloadState.Progress(it) },
                                            onDone     = { dlState = DownloadState.Done(it) },
                                            onFailed   = { dlState = DownloadState.Failed(it) },
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape    = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Outlined.Download, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Download & Install  v${info.latestVersion}",
                                    style = MaterialTheme.typography.labelLarge)
                            }

                            // Tombol "Nanti Saja" hanya untuk soft update
                            if (!info.isForceUpdate) {
                                OutlinedButton(
                                    onClick  = onDismiss,
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape    = RoundedCornerShape(14.dp),
                                ) {
                                    Text("Nanti Saja", style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    is DownloadState.Progress -> {
                        DownloadProgressBar(percent = dl.percent)
                    }

                    is DownloadState.Done -> {
                        LaunchedEffect(dl.apkFile) { installApk(context, dl.apkFile) }
                        DownloadProgressBar(percent = 100)
                        Text("Membuka installer…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    is DownloadState.Failed -> {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(10.dp),
                                color    = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            ) {
                                Text(
                                    "Gagal: ${dl.reason}",
                                    modifier = Modifier.padding(12.dp),
                                    style    = MaterialTheme.typography.bodySmall,
                                    color    = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            OutlinedButton(
                                onClick  = { dlState = DownloadState.Idle },
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(12.dp),
                            ) { Text("Coba Lagi") }
                            TextButton(
                                onClick  = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, info.releasePageUrl.toUri()))
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Buka di Browser") }
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
    val color = if (isForce) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.primaryContainer
    val iconColor = if (isForce) MaterialTheme.colorScheme.onErrorContainer
                   else MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isForce) Icons.Outlined.Warning else Icons.Outlined.NewReleases,
            contentDescription = null,
            tint     = iconColor,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
private fun VersionChip(version: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(
            text     = "v$version",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            style    = MaterialTheme.typography.labelLarge,
            color    = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DescriptionBox(isForce: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Info, null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Text("Tentang Update Ini",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = if (isForce)
                    "Update ini wajib dipasang untuk melanjutkan penggunaan Aether Manager. " +
                    "Versi ini mengandung perbaikan penting atau perubahan yang tidak kompatibel dengan versi lama."
                else
                    "Versi baru Aether Manager telah tersedia. Update direkomendasikan untuk mendapatkan " +
                    "fitur terbaru, perbaikan bug, dan peningkatan performa.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun ChangelogBox(notes: String, loading: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        if (loading) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary)
                    Text("Memuat changelog…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .heightIn(min = 80.dp, max = 200.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (notes.isBlank()) {
                    Text("Tidak ada changelog tersedia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(
                        text  = notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ForceUpdateNotice() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        color    = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Warning, null,
                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
            Text(
                "Update ini wajib dipasang untuk melanjutkan menggunakan aplikasi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun DownloadProgressBar(percent: Int) {
    val progress by animateFloatAsState(
        targetValue = percent / 100f, animationSpec = tween(300), label = "dl_progress")
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Mengunduh APK…", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$percent%", style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Fetch changelog segar dari GitHub Releases API
// Ambil body release yang sesuai dengan releasePageUrl
// ─────────────────────────────────────────────────────────────

private suspend fun fetchChangelogFromGitHub(releasePageUrl: String): String? =
    withContext(Dispatchers.IO) {
        try {
            // Konversi html_url ke API URL:
            // https://github.com/owner/repo/releases/tag/vX.Y
            // → https://api.github.com/repos/owner/repo/releases/tags/vX.Y
            val apiUrl = releasePageUrl
                .replace("https://github.com/", "https://api.github.com/repos/")
                .replace("/releases/tag/", "/releases/tags/")

            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "AetherManager-Android")
                connectTimeout = 10_000
                readTimeout    = 10_000
            }

            if (conn.responseCode != 200) return@withContext null

            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = org.json.JSONObject(body)
            val releaseBody = json.optString("body", "")

            // Bersihkan baris kontrol internal (force_update: true)
            releaseBody.lines()
                .filterNot { it.trim().startsWith("force_update:", ignoreCase = true) }
                .joinToString("\n")
                .trim()
                .takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
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
            context, "${context.packageName}.provider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, apkFile.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
