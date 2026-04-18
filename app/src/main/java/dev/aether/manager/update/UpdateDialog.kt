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
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import dev.aether.manager.ads.InterstitialAdManager
import dev.aether.manager.i18n.AppStrings
import dev.aether.manager.i18n.LocalStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// ─────────────────────────────────────────────────────────────
// Entry point — dialog muncul otomatis jika ada update
// ─────────────────────────────────────────────────────────────

@Composable
fun UpdateDialogHost(viewModel: UpdateViewModel) {
    val state     by viewModel.state.collectAsState()
    val dismissed by viewModel.dismissed.collectAsState()

    val updateInfo = (state as? UpdateUiState.UpdateAvailable)?.info ?: return
    if (dismissed) return

    UpdateDialog(
        info           = updateInfo,
        currentVersion = viewModel.currentVersionName,
        onDismiss      = { viewModel.dismiss() },
    )
}

// ─────────────────────────────────────────────────────────────
// Download state
// ─────────────────────────────────────────────────────────────

private sealed class DownloadState {
    object Idle : DownloadState()
    data class Progress(
        val percent        : Int,
        val downloadedBytes: Long = 0L,
        val totalBytes     : Long = 0L,
    ) : DownloadState()
    data class Done(val apkFile: File)    : DownloadState()
    data class Failed(val reason: String) : DownloadState()
}

// ─────────────────────────────────────────────────────────────
// Dialog utama
// ─────────────────────────────────────────────────────────────

@Composable
fun UpdateDialog(
    info           : ReleaseInfo,
    currentVersion : String,
    onDismiss      : () -> Unit,
) {
    val context  = LocalContext.current
    val activity = LocalActivity.current
    val s        = LocalStrings.current
    val scope    = rememberCoroutineScope()
    var dlState  by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }

    var selectedTab      by remember { mutableIntStateOf(0) }
    var changelog        by remember { mutableStateOf(info.releaseNotes) }
    var fetchingChangelog by remember { mutableStateOf(false) }

    // Fetch changelog fresh dari GitHub saat dialog dibuka
    LaunchedEffect(info.releasePageUrl) {
        fetchingChangelog = true
        val fresh = fetchChangelogFromGitHub(info.releasePageUrl)
        if (fresh != null) changelog = fresh
        fetchingChangelog = false
    }

    Dialog(
        onDismissRequest = { /* biarkan user tutup lewat tombol */ },
        properties = DialogProperties(
            dismissOnBackPress      = true,
            dismissOnClickOutside   = false,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier       = Modifier.fillMaxWidth(0.92f).wrapContentHeight(),
            shape          = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            color          = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier            = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {

                // ── Header icon ───────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.NewReleases,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier           = Modifier.size(32.dp),
                    )
                }

                // ── Title ─────────────────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text       = s.updateAvailable,
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.Center,
                    )
                    Text(
                        text  = "Aether Manager",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // ── Version chip: "versi saat ini → versi baru" ──
                VersionArrowChip(currentVersion = currentVersion, newVersion = info.latestVersion)

                // ── Tab: Deskripsi / Changelog ────────────────────
                val tabs = listOf(s.updateTabDesc, s.updateTabChangelog)
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier         = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    containerColor   = MaterialTheme.colorScheme.surfaceVariant,
                    indicator        = {},
                    divider          = {},
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
                                    else          androidx.compose.ui.graphics.Color.Transparent
                                ),
                        ) {
                            Text(
                                text       = label,
                                style      = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color      = if (selected) MaterialTheme.colorScheme.primary
                                             else          MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier   = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                }

                // ── Tab content ───────────────────────────────────
                AnimatedContent(
                    targetState  = selectedTab,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label        = "tab_content",
                ) { tab ->
                    when (tab) {
                        0    -> DescriptionBox(s)
                        1    -> ChangelogBox(notes = changelog, loading = fetchingChangelog, s = s)
                        else -> Unit
                    }
                }

                // ── Buttons ───────────────────────────────────────
                when (val dl = dlState) {
                    is DownloadState.Idle -> {
                        Column(
                            Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick  = {
                                    // Tampilkan interstitial saat klik Download
                                    if (activity != null) {
                                        InterstitialAdManager.showIfReady(activity)
                                    }
                                    scope.launch {
                                        downloadAndInstall(
                                            context    = context,
                                            url        = info.downloadUrl,
                                            onProgress = { pct, dl, total ->
                                                dlState = DownloadState.Progress(pct, dl, total)
                                            },
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
                                Text(
                                    s.updateBtnDownload,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            OutlinedButton(
                                onClick  = onDismiss,
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape    = RoundedCornerShape(14.dp),
                            ) {
                                Text(
                                    s.updateBtnLater,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is DownloadState.Progress -> {
                        DownloadProgressBar(
                                percent         = dl.percent,
                                downloadedBytes = dl.downloadedBytes,
                                totalBytes      = dl.totalBytes,
                            )
                    }

                    is DownloadState.Done -> {
                        LaunchedEffect(dl.apkFile) {
                            // Tampilkan interstitial sebelum install
                            if (activity != null) {
                                InterstitialAdManager.showIfReady(activity)
                            }
                            installApk(context, dl.apkFile)
                        }
                        DownloadProgressBar(percent = 100, downloadedBytes = -1L, totalBytes = -1L)
                        Text(
                            s.updateInstalling,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    is DownloadState.Failed -> {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(10.dp),
                                color    = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            ) {
                                Text(
                                    s.updateFailed.format(dl.reason),
                                    modifier  = Modifier.padding(12.dp),
                                    style     = MaterialTheme.typography.bodySmall,
                                    color     = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            OutlinedButton(
                                onClick  = { dlState = DownloadState.Idle },
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(12.dp),
                            ) { Text(s.updateBtnRetry) }
                            TextButton(
                                onClick  = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, info.releasePageUrl.toUri())
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(s.updateBtnBrowser) }
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

/** Chip: "v1.2 → v1.5" — menunjukkan versi asal dan tujuan update */
@Composable
private fun VersionArrowChip(currentVersion: String, newVersion: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text       = currentVersion,
                style      = MaterialTheme.typography.labelLarge,
                color      = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f),
                fontWeight = FontWeight.Medium,
            )
            Icon(
                Icons.Outlined.ArrowForward, null,
                tint     = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text       = newVersion,
                style      = MaterialTheme.typography.labelLarge,
                color      = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DescriptionBox(s: AppStrings) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Column(
            modifier            = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Info, null,
                    modifier = Modifier.size(15.dp),
                    tint     = MaterialTheme.colorScheme.primary
                )
                Text(
                    s.updateAboutTitle,
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text      = s.updateAboutDesc,
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun ChangelogBox(notes: String, loading: Boolean, s: AppStrings) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        if (loading) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        s.updateChangelogLoading,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                Text(
                    text       = notes.ifBlank { s.updateChangelogEmpty },
                    style      = MaterialTheme.typography.bodySmall,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun DownloadProgressBar(
    percent        : Int,
    downloadedBytes: Long,
    totalBytes     : Long,
) {
    val s        = LocalStrings.current
    val progress by animateFloatAsState(
        targetValue   = percent / 100f,
        animationSpec = tween(300),
        label         = "dl_progress"
    )

    // Format bytes → "X.X MB"
    fun Long.toMb(): String = "%.1f MB".format(this / 1_048_576.0)

    val hasSizeInfo = totalBytes > 0L && downloadedBytes >= 0L
    val isComplete  = percent >= 100

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ── Row atas: label kiri, size kanan ──────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    if (isComplete) {
                        Icon(
                            Icons.Outlined.CheckCircle, null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            s.updateDownloadDone,
                            style  = MaterialTheme.typography.labelSmall,
                            color  = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color       = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            s.updateDownloading,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Size info: "1.5 / 45.5 MB"
                if (hasSizeInfo) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    ) {
                        Row(
                            modifier              = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                        ) {
                            Text(
                                text       = downloadedBytes.toMb(),
                                style      = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text  = "/",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text  = totalBytes.toMb(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ── Progress bar + persen ─────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text       = "$percent%",
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Fetch changelog dari GitHub Releases API
// ─────────────────────────────────────────────────────────────

private suspend fun fetchChangelogFromGitHub(releasePageUrl: String): String? =
    withContext(Dispatchers.IO) {
        try {
            // html_url: https://github.com/owner/repo/releases/tag/vX.Y
            // API url:  https://api.github.com/repos/owner/repo/releases/tags/vX.Y
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

            org.json.JSONObject(body).optString("body", "").trim().takeIf { it.isNotBlank() }
        } catch (_: Exception) { null }
    }

// ─────────────────────────────────────────────────────────────
// Download + Install helpers
// ─────────────────────────────────────────────────────────────

private suspend fun downloadAndInstall(
    context   : Context,
    url       : String,
    onProgress: (percent: Int, downloaded: Long, total: Long) -> Unit,
    onDone    : (File) -> Unit,
    onFailed  : (String) -> Unit,
) = withContext(Dispatchers.IO) {
    try {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout          = 15_000
            readTimeout             = 60_000
            instanceFollowRedirects = true
        }
        conn.connect()

        if (conn.responseCode != 200) {
            withContext(Dispatchers.Main) { onFailed("HTTP ${conn.responseCode}") }
            return@withContext
        }

        val totalBytes = conn.contentLengthLong
        val outFile    = File(context.cacheDir, "update.apk")

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
                        withContext(Dispatchers.Main) { onProgress(pct, downloaded, totalBytes) }
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
            context, "${context.packageName}.provider", apkFile
        )
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    } catch (_: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, apkFile.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
