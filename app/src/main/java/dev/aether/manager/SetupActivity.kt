package dev.aether.manager

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.aether.manager.i18n.AppStrings
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.i18n.ProvideStrings
import dev.aether.manager.ui.AetherTheme
import dev.aether.manager.util.RootManager
import kotlinx.coroutines.launch

class SetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AetherTheme {
                ProvideStrings {
                    SetupScreen(
                        onDone = {
                            val prefs = getSharedPreferences("aether_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("setup_done", true).apply()
                            // Root sudah granted oleh RootManager.requestRoot() saat klik tombol.
                            // markGranted() memastikan cache valid untuk MainActivity.
                            RootManager.markGranted()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}

private enum class PermState { IDLE, CHECKING, GRANTED, DENIED }

private data class SetupPage(
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val title: String,
    val desc: String,
    val permissionType: String? = null,
    val ctaLabel: String? = null,
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SetupScreen(onDone: () -> Unit) {
    val ctx   = LocalContext.current
    val scope = rememberCoroutineScope()
    val s     = LocalStrings.current

    // ── Permission states ──────────────────────────────────────────────────
    // Root mulai IDLE — tidak ada check otomatis sama sekali.
    // Grant hanya dipicu saat user klik tombol di halaman ROOT.
    var rootState    by remember { mutableStateOf(PermState.IDLE) }
    var notifState   by remember { mutableStateOf(PermState.IDLE) }
    var writeState   by remember { mutableStateOf(PermState.IDLE) }
    var storageState by remember { mutableStateOf(PermState.IDLE) }

    // ── Launchers ──────────────────────────────────────────────────────────
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifState = if (granted) PermState.GRANTED else PermState.DENIED
    }

    val writeSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        writeState = if (android.provider.Settings.System.canWrite(ctx))
            PermState.GRANTED else PermState.DENIED
    }

    val allPermsGranted = rootState    == PermState.GRANTED &&
                          notifState   == PermState.GRANTED &&
                          writeState   == PermState.GRANTED &&
                          storageState == PermState.GRANTED

    // ── Color aliases ──────────────────────────────────────────────────────
    val primaryContainer   = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val errContainer       = MaterialTheme.colorScheme.errorContainer
    val onErrContainer     = MaterialTheme.colorScheme.onErrorContainer
    val secContainer       = MaterialTheme.colorScheme.secondaryContainer
    val onSecContainer     = MaterialTheme.colorScheme.onSecondaryContainer
    val terContainer       = MaterialTheme.colorScheme.tertiaryContainer
    val onTerContainer     = MaterialTheme.colorScheme.onTertiaryContainer

    val pages = listOf(
        SetupPage(Icons.Outlined.Rocket, primaryContainer, onPrimaryContainer,
            s.setupWelcomeTitle, s.setupWelcomeDesc),
        SetupPage(Icons.Outlined.AdminPanelSettings, errContainer, onErrContainer,
            s.setupRootTitle, s.setupRootDesc, "ROOT", s.setupRootCta),
        SetupPage(Icons.Outlined.Notifications, secContainer, onSecContainer,
            s.setupNotifTitle, s.setupNotifDesc, "NOTIFICATION", s.setupNotifCta),
        SetupPage(Icons.Outlined.Tune, terContainer, onTerContainer,
            s.setupWriteTitle, s.setupWriteDesc, "WRITE_SETTINGS", s.setupWriteCta),
        SetupPage(Icons.Outlined.FolderOpen, secContainer, onSecContainer,
            s.setupStorageTitle, s.setupStorageDesc, "STORAGE", s.setupStorageCta),
        SetupPage(
            icon    = if (allPermsGranted) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
            iconBg  = if (allPermsGranted) Color(0xFF1B5E20).copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer,
            iconTint= if (allPermsGranted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onErrorContainer,
            title   = if (allPermsGranted) s.setupDoneTitle else s.setupIncompleteTitle,
            desc    = if (allPermsGranted) s.setupDoneDesc  else s.setupIncompleteDesc
        ),
    )

    val pagerState  = rememberPagerState { pages.size }
    val currentPage = pagerState.currentPage
    val page        = pages[currentPage]
    val isLast      = currentPage == pages.size - 1

    // Auto-check state untuk permission NON-root saat page dibuka
    // ROOT sengaja TIDAK di-check di sini — hanya saat klik tombol
    LaunchedEffect(currentPage) {
        when (page.permissionType) {
            "NOTIFICATION" -> {
                val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                else true
                if (granted) notifState = PermState.GRANTED
            }
            "WRITE_SETTINGS" -> {
                if (android.provider.Settings.System.canWrite(ctx)) writeState = PermState.GRANTED
            }
            "STORAGE" -> {
                val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) true
                else ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                if (ok) storageState = PermState.GRANTED
            }
            // ROOT: tidak ada auto-check — user harus klik tombol
        }
    }

    val canProceed = when (page.permissionType) {
        "ROOT"           -> rootState    == PermState.GRANTED
        "NOTIFICATION"   -> notifState   == PermState.GRANTED
        "WRITE_SETTINGS" -> writeState   == PermState.GRANTED
        "STORAGE"        -> storageState == PermState.GRANTED
        else             -> if (isLast) allPermsGranted else true
    }

    fun nextPage() { scope.launch { pagerState.animateScrollToPage(currentPage + 1) } }
    fun prevPage() { scope.launch { pagerState.animateScrollToPage(currentPage - 1) } }

    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            HorizontalPager(
                state      = pagerState,
                pageSize   = PageSize.Fill,
                beyondViewportPageCount = 0,
                modifier   = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(0.dp)),
                userScrollEnabled = true,
            ) { idx ->
                val pg = pages[idx]
                val permState = when (pg.permissionType) {
                    "ROOT"           -> rootState
                    "NOTIFICATION"   -> notifState
                    "WRITE_SETTINGS" -> writeState
                    "STORAGE"        -> storageState
                    else             -> PermState.IDLE
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp)
                ) {
                    Spacer(Modifier.height(4.dp))

                    Box(
                        modifier = Modifier.size(108.dp).clip(RoundedCornerShape(32.dp)).background(pg.iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(pg.icon, null, tint = pg.iconTint, modifier = Modifier.size(54.dp))
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(pg.title, style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(pg.desc, style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
                    }

                    if (pg.permissionType == null && idx == pages.size - 1 && !allPermsGranted) {
                        MissingPermsSummary(
                            rootMissing    = rootState    != PermState.GRANTED,
                            notifMissing   = notifState   != PermState.GRANTED,
                            writeMissing   = writeState   != PermState.GRANTED,
                            storageMissing = storageState != PermState.GRANTED,
                            strings        = s,
                            onGoToPage     = { targetIdx ->
                                scope.launch { pagerState.animateScrollToPage(targetIdx) }
                            }
                        )
                    }

                    if (pg.permissionType == null && idx == pages.size - 1 && allPermsGranted) {
                        Surface(
                            shape    = RoundedCornerShape(50),
                            color    = Color(0xFF4CAF50).copy(alpha = 0.15f),
                        ) {
                            Row(
                                modifier              = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.CheckCircle, null,
                                    tint     = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text       = s.setupAllPermsGranted,
                                    fontSize   = 13.sp,
                                    color      = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }

                    if (pg.permissionType != null) {
                        PermissionBlock(
                            permType = pg.permissionType,
                            ctaLabel = pg.ctaLabel ?: s.setupBtnRetry,
                            state    = permState,
                            strings  = s,
                            onAction = {
                                when (pg.permissionType) {
                                    // ROOT: panggil RootManager.requestRoot() — satu-satunya tempat
                                    // yang memunculkan dialog SU manager.
                                    "ROOT" -> scope.launch {
                                        rootState = PermState.CHECKING
                                        val granted = RootManager.requestRoot()
                                        rootState = if (granted) PermState.GRANTED else PermState.DENIED
                                    }
                                    "NOTIFICATION" -> {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        else notifState = PermState.GRANTED
                                    }
                                    "WRITE_SETTINGS" -> {
                                        if (android.provider.Settings.System.canWrite(ctx)) {
                                            writeState = PermState.GRANTED
                                        } else {
                                            writeSettingsLauncher.launch(Intent(
                                                android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                                android.net.Uri.parse("package:${ctx.packageName}")
                                            ))
                                        }
                                    }
                                    "STORAGE" -> {
                                        storageState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            PermState.GRANTED
                                        } else {
                                            if (ContextCompat.checkSelfPermission(ctx,
                                                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                                                PermState.GRANTED else PermState.DENIED
                                        }
                                    }
                                }
                            },
                            onRetry = {
                                // Reset ke IDLE — user bisa klik lagi
                                when (pg.permissionType) {
                                    "ROOT"           -> { RootManager.clearCache(); rootState = PermState.IDLE }
                                    "WRITE_SETTINGS" -> writeState   = PermState.IDLE
                                    "NOTIFICATION"   -> notifState   = PermState.IDLE
                                    "STORAGE"        -> storageState = PermState.IDLE
                                }
                            }
                        )
                    }
                }
            }

            // ── Bottom controls ────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 28.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pages.forEachIndexed { i, _ ->
                        val sel = i == currentPage
                        val w by animateDpAsState(if (sel) 24.dp else 8.dp, label = "dot")
                        Box(modifier = Modifier.height(8.dp).width(w).clip(CircleShape).background(
                            if (sel) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        ))
                    }
                }

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick  = { if (isLast) onDone() else nextPage() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(16.dp),
                    enabled  = canProceed
                ) {
                    Text(if (isLast) s.setupBtnStart else s.setupBtnNext,
                        fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }

                AnimatedVisibility(visible = !canProceed && (page.permissionType != null || isLast)) {
                    Text(s.setupRootRequired, color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.alpha(0.9f))
                }

                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    if (currentPage > 0) {
                        TextButton(onClick = { prevPage() }) {
                            Text(s.setupBtnBack, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Spacer(Modifier.width(80.dp))
                    }
                    Spacer(Modifier.width(80.dp))
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun MissingPermsSummary(
    rootMissing: Boolean, notifMissing: Boolean,
    writeMissing: Boolean, storageMissing: Boolean,
    strings: AppStrings, onGoToPage: (Int) -> Unit
) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Warning, null,
                    tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                Text(strings.setupRootRequired,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            if (rootMissing)    MissingPermRow(strings.setupRootTitle,    1, onGoToPage)
            if (notifMissing)   MissingPermRow(strings.setupNotifTitle,   2, onGoToPage)
            if (writeMissing)   MissingPermRow(strings.setupWriteTitle,   3, onGoToPage)
            if (storageMissing) MissingPermRow(strings.setupStorageTitle, 4, onGoToPage)
        }
    }
}

@Composable
private fun MissingPermRow(label: String, pageIdx: Int, onGoToPage: (Int) -> Unit) {
    TextButton(onClick = { onGoToPage(pageIdx) },
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)) {
        Icon(Icons.Outlined.ChevronRight, null,
            tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text("$label", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
    }
}

@Composable
private fun PermissionBlock(
    permType: String, ctaLabel: String, state: PermState,
    strings: AppStrings, onAction: () -> Unit, onRetry: () -> Unit
) {
    AnimatedContent(targetState = state, label = "perm_$permType") { s ->
        when (s) {
            PermState.IDLE -> FilledTonalButton(onClick = onAction,
                modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(when (permType) {
                    "ROOT"           -> Icons.Outlined.AdminPanelSettings
                    "NOTIFICATION"   -> Icons.Outlined.Notifications
                    "WRITE_SETTINGS" -> Icons.Outlined.Tune
                    else             -> Icons.Outlined.FolderOpen
                }, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(ctaLabel, fontWeight = FontWeight.Medium)
            }

            PermState.CHECKING -> Row(modifier = Modifier.fillMaxWidth().height(50.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                Spacer(Modifier.width(12.dp))
                Text(strings.setupRootChecking, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            PermState.GRANTED -> Surface(shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp))
                    Text(when (permType) {
                        "ROOT"           -> strings.setupRootGranted
                        "NOTIFICATION"   -> strings.setupNotifGranted
                        "WRITE_SETTINGS" -> strings.setupWriteGranted
                        else             -> strings.setupStorageGranted
                    }, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Medium)
                }
            }

            PermState.DENIED -> Surface(shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text(when (permType) {
                            "ROOT"           -> strings.setupRootDenied
                            "NOTIFICATION"   -> strings.setupNotifDenied
                            "WRITE_SETTINGS" -> strings.setupWriteDenied
                            else             -> strings.setupStorageDenied
                        }, color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        val sub = when (permType) {
                            "ROOT"           -> strings.setupRootDeniedSub
                            "NOTIFICATION"   -> strings.setupWriteDeniedSub
                            "WRITE_SETTINGS" -> strings.setupWriteDeniedSub
                            "STORAGE"        -> strings.setupWriteDeniedSub
                            else             -> null
                        }
                        if (sub != null) {
                            Text(sub, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                                fontSize = 11.sp)
                            Spacer(Modifier.height(6.dp))
                            OutlinedButton(onClick = onRetry, modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                                Text(strings.setupBtnRetry, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}



