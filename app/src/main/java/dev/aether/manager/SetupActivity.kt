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
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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

// ── Animated ring orbiting icon ───────────────────────────────────────────────
@Composable
private fun AnimatedIconBox(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    granted: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon_orbit")

    // Rotating ring angle
    val ringAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "ring_angle"
    )
    // Pulse glow
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val grantedScale by animateFloatAsState(
        targetValue = if (granted) 1.08f else 1f,
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium),
        label = "granted_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(140.dp)
    ) {
        // Outer glow blur
        Box(
            modifier = Modifier
                .size(130.dp)
                .blur(24.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(iconTint.copy(alpha = glowAlpha * 0.5f), Color.Transparent)
                    )
                )
        )

        // Orbiting dashed ring via Canvas
        Canvas(modifier = Modifier.size(130.dp)) {
            val cx     = size.width / 2f
            val cy     = size.height / 2f
            val radius = size.minDimension / 2f - 4.dp.toPx()
            val dotCount = 16
            repeat(dotCount) { i ->
                val theta = (ringAngle + i * (360f / dotCount)) * (PI / 180f).toFloat()
                val x = cx + radius * cos(theta)
                val y = cy + radius * sin(theta)
                val alpha = ((i.toFloat() / dotCount) * 0.9f + 0.1f)
                drawCircle(
                    color  = iconTint.copy(alpha = alpha * 0.55f),
                    radius = if (i % 4 == 0) 4.dp.toPx() else 2.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        // Icon box
        Box(
            modifier = Modifier
                .size(104.dp)
                .scale(grantedScale)
                .clip(RoundedCornerShape(32.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            // Success checkmark overlay
            AnimatedContent(
                targetState = granted,
                transitionSpec = {
                    (scaleIn(spring(Spring.DampingRatioLowBouncy)) + fadeIn()) togetherWith
                            (scaleOut() + fadeOut())
                },
                label = "icon_swap"
            ) { isGranted ->
                Icon(
                    imageVector = if (isGranted) Icons.Outlined.CheckCircle else icon,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF4CAF50) else iconTint,
                    modifier = Modifier.size(52.dp)
                )
            }
        }
    }
}

// ── Step progress indicator (top) ────────────────────────────────────────────
@Composable
private fun StepProgressBar(total: Int, current: Int, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { i ->
            val filled  = i <= current
            val isCurr  = i == current
            val progress by animateFloatAsState(
                targetValue = if (filled) 1f else 0f,
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                label = "bar_$i"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            if (isCurr) Brush.horizontalGradient(
                                listOf(primary.copy(alpha = 0.5f), primary)
                            ) else Brush.horizontalGradient(listOf(primary, primary))
                        )
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SetupScreen(onDone: () -> Unit) {
    val ctx   = LocalContext.current
    val scope = rememberCoroutineScope()
    val s     = LocalStrings.current

    var rootState    by remember { mutableStateOf(PermState.IDLE) }
    var notifState   by remember { mutableStateOf(PermState.IDLE) }
    var writeState   by remember { mutableStateOf(PermState.IDLE) }
    var storageState by remember { mutableStateOf(PermState.IDLE) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notifState = if (granted) PermState.GRANTED else PermState.DENIED }

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

    val primaryContainer   = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val errContainer       = MaterialTheme.colorScheme.errorContainer
    val onErrContainer     = MaterialTheme.colorScheme.onErrorContainer
    val secContainer       = MaterialTheme.colorScheme.secondaryContainer
    val onSecContainer     = MaterialTheme.colorScheme.onSecondaryContainer
    val terContainer       = MaterialTheme.colorScheme.tertiaryContainer
    val onTerContainer     = MaterialTheme.colorScheme.onTertiaryContainer

    val pages = listOf(
        SetupPage(Icons.Outlined.Rocket,              primaryContainer, onPrimaryContainer,
            s.setupWelcomeTitle, s.setupWelcomeDesc),
        SetupPage(Icons.Outlined.AdminPanelSettings,  errContainer,     onErrContainer,
            s.setupRootTitle,    s.setupRootDesc,    "ROOT",           s.setupRootCta),
        SetupPage(Icons.Outlined.Notifications,       secContainer,     onSecContainer,
            s.setupNotifTitle,   s.setupNotifDesc,   "NOTIFICATION",   s.setupNotifCta),
        SetupPage(Icons.Outlined.Tune,                terContainer,     onTerContainer,
            s.setupWriteTitle,   s.setupWriteDesc,   "WRITE_SETTINGS", s.setupWriteCta),
        SetupPage(Icons.Outlined.FolderOpen,          secContainer,     onSecContainer,
            s.setupStorageTitle, s.setupStorageDesc, "STORAGE",        s.setupStorageCta),
        SetupPage(
            icon     = if (allPermsGranted) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
            iconBg   = if (allPermsGranted) Color(0xFF1B5E20).copy(alpha = 0.2f) else errContainer,
            iconTint = if (allPermsGranted) Color(0xFF4CAF50) else onErrContainer,
            title    = if (allPermsGranted) s.setupDoneTitle else s.setupIncompleteTitle,
            desc     = if (allPermsGranted) s.setupDoneDesc  else s.setupIncompleteDesc
        ),
    )

    val pagerState  = rememberPagerState { pages.size }
    val currentPage = pagerState.currentPage
    val isLast      = currentPage == pages.size - 1

    LaunchedEffect(currentPage) {
        val pg = pages[currentPage]
        when (pg.permissionType) {
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
        }
    }

    val canProceed = when (pages[currentPage].permissionType) {
        "ROOT"           -> rootState    == PermState.GRANTED
        "NOTIFICATION"   -> notifState   == PermState.GRANTED
        "WRITE_SETTINGS" -> writeState   == PermState.GRANTED
        "STORAGE"        -> storageState == PermState.GRANTED
        else             -> if (isLast) allPermsGranted else true
    }

    fun nextPage() { scope.launch { pagerState.animateScrollToPage(currentPage + 1) } }
    fun prevPage() { scope.launch { pagerState.animateScrollToPage(currentPage - 1) } }

    // Entry animation
    val screenAlpha  = remember { Animatable(0f) }
    val screenSlideY = remember { Animatable(30f) }
    LaunchedEffect(Unit) {
        launch { screenAlpha.animateTo(1f, tween(420)) }
        launch { screenSlideY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .graphicsLayer(
                    alpha        = screenAlpha.value,
                    translationY = screenSlideY.value.dp.value
                )
        ) {
            // Subtle background gradient orb
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 80.dp, y = (-40).dp)
                    .blur(90.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Top: step progress ─────────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 52.dp)
                ) {
                    Text(
                        text = "Setup  ${currentPage + 1} / ${pages.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                    StepProgressBar(total = pages.size, current = currentPage)
                }

                // ── Pager ──────────────────────────────────────────────
                HorizontalPager(
                    state = pagerState,
                    pageSize = PageSize.Fill,
                    beyondViewportPageCount = 0,
                    modifier = Modifier.fillMaxWidth().weight(1f),
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
                    val isGrantedPage = permState == PermState.GRANTED
                    val isLastPage    = idx == pages.size - 1

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp)
                            .padding(top = 20.dp)
                    ) {
                        // Animated icon box
                        AnimatedIconBox(
                            icon     = pg.icon,
                            iconBg   = pg.iconBg,
                            iconTint = pg.iconTint,
                            granted  = isGrantedPage || (isLastPage && allPermsGranted)
                        )

                        // Title + desc
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AnimatedContent(
                                targetState = pg.title,
                                transitionSpec = {
                                    fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                                },
                                label = "title_$idx"
                            ) { title ->
                                Text(
                                    title,
                                    style      = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign  = TextAlign.Center,
                                    color      = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                pg.desc,
                                style     = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp
                            )
                        }

                        // Success all-perms badge
                        AnimatedVisibility(
                            visible = isLastPage && allPermsGranted,
                            enter   = scaleIn(spring(Spring.DampingRatioLowBouncy)) + fadeIn(),
                            exit    = scaleOut() + fadeOut()
                        ) {
                            AllGrantedBadge()
                        }

                        // Missing perms summary
                        AnimatedVisibility(
                            visible = isLastPage && !allPermsGranted,
                            enter   = slideInVertically { it / 2 } + fadeIn(),
                            exit    = slideOutVertically { it / 2 } + fadeOut()
                        ) {
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

                        // Permission block
                        if (pg.permissionType != null) {
                            PermissionBlock(
                                permType = pg.permissionType,
                                ctaLabel = pg.ctaLabel ?: s.setupBtnRetry,
                                state    = permState,
                                strings  = s,
                                onAction = {
                                    when (pg.permissionType) {
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

                // ── Bottom controls ────────────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .padding(horizontal = 28.dp)
                        .padding(bottom = 32.dp)
                ) {
                    // Dot indicators
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        pages.forEachIndexed { i, _ ->
                            val sel = i == currentPage
                            val w by animateDpAsState(if (sel) 28.dp else 7.dp, tween(300), label = "dot_$i")
                            val color by animateColorAsState(
                                if (sel) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHighest,
                                tween(300), label = "dot_color_$i"
                            )
                            Box(
                                modifier = Modifier
                                    .height(7.dp)
                                    .width(w)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    // Next / Start button
                    val btnScale = remember { Animatable(1f) }
                    LaunchedEffect(canProceed) {
                        if (canProceed) {
                            btnScale.animateTo(1.04f, tween(150))
                            btnScale.animateTo(1f, spring(Spring.DampingRatioLowBouncy))
                        }
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                btnScale.animateTo(0.96f, tween(80))
                                btnScale.animateTo(1f, spring(Spring.DampingRatioLowBouncy))
                            }
                            if (isLast) onDone() else nextPage()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .scale(btnScale.value),
                        shape   = RoundedCornerShape(18.dp),
                        enabled = canProceed,
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        )
                    ) {
                        AnimatedContent(
                            targetState = if (isLast) s.setupBtnStart else s.setupBtnNext,
                            transitionSpec = {
                                slideInVertically { -it } + fadeIn() togetherWith
                                        slideOutVertically { it } + fadeOut()
                            },
                            label = "btn_label"
                        ) { label ->
                            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }

                    // Hint text
                    AnimatedVisibility(
                        visible = !canProceed && (pages[currentPage].permissionType != null || isLast)
                    ) {
                        Text(
                            s.setupRootRequired,
                            color     = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            fontSize  = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Back button row
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        AnimatedVisibility(
                            visible = currentPage > 0,
                            enter   = fadeIn() + slideInHorizontally { -it / 2 },
                            exit    = fadeOut() + slideOutHorizontally { -it / 2 }
                        ) {
                            TextButton(onClick = { prevPage() }) {
                                Icon(Icons.Outlined.ChevronLeft, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(s.setupBtnBack, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── All granted badge ─────────────────────────────────────────────────────────
@Composable
private fun AllGrantedBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "badge_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "badge_alpha"
    )
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFF4CAF50).copy(alpha = 0.15f),
        border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = glowAlpha * 0.4f))
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.CheckCircle, null,
                tint     = Color(0xFF4CAF50),
                modifier = Modifier.size(17.dp)
            )
            Text(
                text       = LocalStrings.current.setupAllPermsGranted,
                fontSize   = 13.sp,
                color      = Color(0xFF4CAF50).copy(alpha = glowAlpha),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            )
        }
    }
}

// ── Missing perms summary ─────────────────────────────────────────────────────
@Composable
private fun MissingPermsSummary(
    rootMissing: Boolean, notifMissing: Boolean,
    writeMissing: Boolean, storageMissing: Boolean,
    strings: AppStrings, onGoToPage: (Int) -> Unit
) {
    Surface(
        shape  = RoundedCornerShape(20.dp),
        color  = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Warning, null,
                    tint     = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    strings.setupRootRequired,
                    color      = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                )
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
    TextButton(
        onClick = { onGoToPage(pageIdx) },
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Icon(
            Icons.Outlined.ChevronRight, null,
            tint     = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp)
    }
}

// ── Permission block ──────────────────────────────────────────────────────────
@Composable
private fun PermissionBlock(
    permType: String, ctaLabel: String, state: PermState,
    strings: AppStrings, onAction: () -> Unit, onRetry: () -> Unit
) {
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            (slideInVertically { it / 2 } + fadeIn(tween(300))) togetherWith
                    (slideOutVertically { -it / 2 } + fadeOut(tween(200)))
        },
        label = "perm_$permType"
    ) { s ->
        when (s) {
            PermState.IDLE -> {
                // CTA Button with icon + subtle glow ring
                val btnGlow = remember { Animatable(0.5f) }
                LaunchedEffect(Unit) {
                    while (true) {
                        btnGlow.animateTo(1f, tween(1000))
                        btnGlow.animateTo(0.5f, tween(1000))
                    }
                }
                val primary = MaterialTheme.colorScheme.primary
                Box(contentAlignment = Alignment.Center) {
                    // Glow behind button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .blur(16.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(primary.copy(alpha = btnGlow.value * 0.15f))
                    )
                    FilledTonalButton(
                        onClick   = onAction,
                        modifier  = Modifier.fillMaxWidth().height(54.dp),
                        shape     = RoundedCornerShape(18.dp),
                        elevation = ButtonDefaults.filledTonalButtonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(
                            when (permType) {
                                "ROOT"           -> Icons.Outlined.AdminPanelSettings
                                "NOTIFICATION"   -> Icons.Outlined.Notifications
                                "WRITE_SETTINGS" -> Icons.Outlined.Tune
                                else             -> Icons.Outlined.FolderOpen
                            }, null, modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(ctaLabel, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }

            PermState.CHECKING -> {
                // Animated checking row
                val infiniteTransition = rememberInfiniteTransition(label = "checking")
                val dotAlpha1 by infiniteTransition.animateFloat(0f, 1f,
                    infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "d1")
                val dotAlpha2 by infiniteTransition.animateFloat(0f, 1f,
                    infiniteRepeatable(tween(400, delayMillis = 133), RepeatMode.Reverse), label = "d2")
                val dotAlpha3 by infiniteTransition.animateFloat(0f, 1f,
                    infiniteRepeatable(tween(400, delayMillis = 266), RepeatMode.Reverse), label = "d3")

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color       = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(strings.setupRootChecking,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(4.dp))
                        // Animated dots
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            listOf(dotAlpha1, dotAlpha2, dotAlpha3).forEach { alpha ->
                                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                                    fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            PermState.GRANTED -> {
                // Success surface with animated checkmark
                val scaleAnim = remember { Animatable(0.7f) }
                LaunchedEffect(Unit) {
                    scaleAnim.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium))
                }
                Surface(
                    shape    = RoundedCornerShape(18.dp),
                    color    = Color(0xFF1B5E20).copy(alpha = 0.12f),
                    border   = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().scale(scaleAnim.value)
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.CheckCircle, null,
                            tint     = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            when (permType) {
                                "ROOT"           -> strings.setupRootGranted
                                "NOTIFICATION"   -> strings.setupNotifGranted
                                "WRITE_SETTINGS" -> strings.setupWriteGranted
                                else             -> strings.setupStorageGranted
                            },
                            color      = Color(0xFF4CAF50),
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 15.sp
                        )
                    }
                }
            }

            PermState.DENIED -> {
                Surface(
                    shape    = RoundedCornerShape(18.dp),
                    color    = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Warning, null,
                            tint     = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                when (permType) {
                                    "ROOT"           -> strings.setupRootDenied
                                    "NOTIFICATION"   -> strings.setupNotifDenied
                                    "WRITE_SETTINGS" -> strings.setupWriteDenied
                                    else             -> strings.setupStorageDenied
                                },
                                color      = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 14.sp
                            )
                            val sub = when (permType) {
                                "ROOT"           -> strings.setupRootDeniedSub
                                "NOTIFICATION"   -> strings.setupWriteDeniedSub
                                "WRITE_SETTINGS" -> strings.setupWriteDeniedSub
                                "STORAGE"        -> strings.setupWriteDeniedSub
                                else             -> null
                            }
                            if (sub != null) {
                                Text(
                                    sub,
                                    color    = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                                    fontSize = 11.5.sp, lineHeight = 16.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick         = onRetry,
                                    modifier        = Modifier.height(36.dp),
                                    shape           = RoundedCornerShape(12.dp),
                                    contentPadding  = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    border          = BorderStroke(1.dp, MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.4f))
                                ) {
                                    Text(strings.setupBtnRetry, fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}