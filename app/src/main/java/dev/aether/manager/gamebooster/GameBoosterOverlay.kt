package dev.aether.manager.gamebooster

import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider

// ── Color tokens ──────────────────────────────────────────────────────────────
private val PanelBg      = Color(0xF0101018)
private val PanelBorder  = Color(0xFF2A2A3A)
private val AccentPurple = Color(0xFF7C5CBF)
private val AccentGreen  = Color(0xFF3A9E6A)
private val AccentOrange = Color(0xFFE07C3A)
private val AccentRed    = Color(0xFFE05050)

@Composable
fun GameBoosterOverlay(
    windowManager: WindowManager,
    layoutParams:  WindowManager.LayoutParams,
    composeView:   ComposeView,
    onStop:        () -> Unit,
) {
    val context = LocalContext.current
    val vm = remember {
        ViewModelProvider(
            composeView.findViewTreeViewModelStoreOwner()!!,
            GameBoosterViewModel.Factory(context)
        )[GameBoosterViewModel::class.java]
    }

    val monitor by vm.monitor.collectAsState()
    val tweaks  by vm.tweaks.collectAsState()
    val dnd     by vm.dnd.collectAsState()

    var expanded  by remember { mutableStateOf(false) }
    var activeTab by remember { mutableIntStateOf(0) }
    var dragAcc   by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .wrapContentWidth()
            .pointerInput(expanded) {
                detectHorizontalDragGestures(
                    onDragStart  = { dragAcc = 0f },
                    onDragEnd    = {
                        if (!expanded && dragAcc < -60f) expanded = true
                        if (expanded  && dragAcc >  60f) expanded = false
                        dragAcc = 0f
                    },
                    onDragCancel = { dragAcc = 0f },
                    onHorizontalDrag = { _, delta -> dragAcc += delta }
                )
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            // ── Sidebar panel ──────────────────────────────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter   = slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),
                exit    = slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(180)),
            ) {
                Column(
                    modifier = Modifier
                        .width(220.dp)
                        .fillMaxHeight()
                        .background(
                            color = PanelBg,
                            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = PanelBorder,
                            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                        )
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                ) {
                    // ── Header ─────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Bolt, null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Game Booster", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE0E0F0))
                            Text("Aether Manager",  fontSize = 9.sp,  color = Color(0xFF666680))
                        }
                        IconButton(onClick = onStop, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Close, null, tint = Color(0xFF666680), modifier = Modifier.size(14.dp))
                        }
                    }

                    // ── Tab row ────────────────────────────────────────────
                    val tabs = listOf("Monitor", "Tweak", "DND")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawLine(PanelBorder, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                            }
                    ) {
                        tabs.forEachIndexed { i, label ->
                            val sel = activeTab == i
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { activeTab = i }
                                    .drawBehind {
                                        if (sel) drawLine(
                                            AccentPurple,
                                            Offset(0f, size.height),
                                            Offset(size.width, size.height),
                                            2.dp.toPx()
                                        )
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text       = label,
                                    fontSize   = 10.sp,
                                    color      = if (sel) AccentPurple else Color(0xFF666680),
                                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // ── Content ────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp)
                    ) {
                        when (activeTab) {
                            0 -> MonitorTab(monitor)
                            1 -> TweakTab(tweaks, vm)
                            2 -> DndTab(dnd, vm)
                        }
                    }
                }
            }

            // ── Trigger pill ───────────────────────────────────────────────
            val arrowRotation by animateFloatAsState(
                targetValue   = if (expanded) 0f else 180f,
                animationSpec = tween(280),
                label         = "arrow_rot"
            )
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                    .background(AccentPurple)
                    .clickable { expanded = !expanded },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Filled.ChevronLeft,
                    contentDescription = if (expanded) "Tutup" else "Buka Game Booster",
                    tint               = Color.White,
                    modifier           = Modifier
                        .size(14.dp)
                        .graphicsLayer { rotationY = arrowRotation }
                )
            }
        }
    }
}

// ── Monitor Tab ───────────────────────────────────────────────────────────────

@Composable
private fun MonitorTab(m: BoosterMonitor) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

        // FPS + mini chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A1A2E))
                .border(1.dp, PanelBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text       = m.fps.toString(),
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (m.fps >= 55) AccentGreen else AccentOrange
                )
                Text("FPS", fontSize = 9.sp, color = Color(0xFF555570), letterSpacing = 0.5.sp)
            }
            MiniBarChart(fps = m.fps)
        }

        // CPU + RAM
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MetricCard(Modifier.weight(1f), "${m.cpu}%",                        "CPU",  m.cpu / 100f,                                              0.75f)
            MetricCard(Modifier.weight(1f), "${"%.1f".format(m.ram)} GB", "RAM",  if (m.ramTotal > 0) m.ram / m.ramTotal else 0f, 0.75f)
        }
        // GPU + Temp
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MetricCard(Modifier.weight(1f), "${m.gpu}%",                             "GPU",  m.gpu / 100f,                                   0.80f)
            MetricCard(Modifier.weight(1f), "${"%.0f".format(m.temp)}°C", "Suhu", (m.temp - 30f).coerceIn(0f, 30f) / 30f, 0.70f)
        }

        // Network
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A1A2E))
                .border(1.dp, PanelBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("↓ ${"%.0f".format(m.netDown)} MB/s", fontSize = 11.sp, color = AccentPurple)
                Text("↑ ${"%.0f".format(m.netUp)} MB/s",   fontSize = 11.sp, color = AccentGreen)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("PING", fontSize = 9.sp, color = Color(0xFF555570))
                Text("${m.ping}ms", fontSize = 12.sp, color = AccentGreen, fontWeight = FontWeight.SemiBold)
            }
        }

        // Battery
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A1A2E))
                .border(1.dp, PanelBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("BATERAI", fontSize = 9.sp, color = Color(0xFF555570), letterSpacing = 0.5.sp)
            val batColor = when {
                m.battery > 50 -> AccentGreen
                m.battery > 20 -> AccentOrange
                else           -> AccentRed
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF2A2A3A))
            ) {
                val animFill by animateFloatAsState(
                    targetValue   = m.battery / 100f,
                    animationSpec = tween(400),
                    label         = "bat"
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animFill)
                        .background(batColor)
                )
            }
            Text("${m.battery}%", fontSize = 11.sp, color = batColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier,
    value:    String,
    label:    String,
    fillFrac: Float,
    warnOver: Float,
) {
    val barColor = if (fillFrac >= warnOver) AccentOrange else AccentGreen
    val animFill by animateFloatAsState(fillFrac.coerceIn(0f, 1f), tween(400), label = "metric_$label")
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1A1A2E))
            .border(1.dp, PanelBorder, RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentPurple)
        Text(label, fontSize = 9.sp,  color = Color(0xFF555570), letterSpacing = 0.5.sp)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF2A2A3A))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animFill)
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun MiniBarChart(fps: Int) {
    val history = remember { mutableStateListOf<Int>() }
    LaunchedEffect(fps) {
        history.add(fps)
        if (history.size > 10) history.removeAt(0)
    }
    Row(
        verticalAlignment     = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier              = Modifier.height(24.dp)
    ) {
        history.forEach { f ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(((f / 60f) * 22f).coerceIn(4f, 24f).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (f >= 55) AccentGreen else AccentOrange)
            )
        }
    }
}

// ── Tweak Tab ────────────────────────────────────────────────────────────────

@Composable
private fun TweakTab(tweaks: BoosterTweaks, vm: GameBoosterViewModel) {
    data class TweakDef(val key: String, val name: String, val desc: String, val value: Boolean)
    val items = listOf(
        TweakDef("performance", "Performance Mode",    "Max governor + thermal",    tweaks.performanceMode),
        TweakDef("ram",         "RAM Optimizer",       "Kill background apps",      tweaks.ramOptimizer),
        TweakDef("gpu",         "Force GPU Rendering", "Paksa GPU untuk semua UI",  tweaks.forceGpu),
        TweakDef("network",     "Network Boost",       "Prioritas paket game",      tweaks.networkBoost),
        TweakDef("touch",       "Touch Optimizer",     "Kurangi touch latency",     tweaks.touchOptimizer),
        TweakDef("haptics",     "Haptics Off",         "Matikan getaran game",      tweaks.hapticsOff),
        TweakDef("screen",      "Screen Stabilizer",   "Lock brightness & refresh", tweaks.screenStabilizer),
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A1A2E))
                    .border(1.dp, PanelBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, fontSize = 11.sp, color = Color(0xFFC0C0D8), fontWeight = FontWeight.Medium)
                    Text(item.desc, fontSize = 9.sp,  color = Color(0xFF555570))
                }
                Switch(
                    checked         = item.value,
                    onCheckedChange = { vm.setTweak(item.key, it) },
                    modifier        = Modifier.height(20.dp),
                    colors          = SwitchDefaults.colors(
                        checkedTrackColor   = AccentPurple,
                        checkedThumbColor   = Color.White,
                        uncheckedTrackColor = Color(0xFF2A2A3A),
                        uncheckedThumbColor = Color(0xFF666680),
                    )
                )
            }
        }
    }
}

// ── DND Tab ──────────────────────────────────────────────────────────────────

@Composable
private fun DndTab(dnd: BoosterDnd, vm: GameBoosterViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Hero toggle
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A1A2E))
                .border(1.dp, PanelBorder, RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(if (dnd.enabled) "🔕" else "🔔", fontSize = 24.sp)
            Text(
                text       = if (dnd.enabled) "DND Aktif" else "DND Nonaktif",
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color(0xFFC0C0D8)
            )
            Switch(
                checked         = dnd.enabled,
                onCheckedChange = { vm.setDnd(it) },
                colors          = SwitchDefaults.colors(
                    checkedTrackColor   = AccentPurple,
                    checkedThumbColor   = Color.White,
                    uncheckedTrackColor = Color(0xFF2A2A3A),
                    uncheckedThumbColor = Color(0xFF666680),
                )
            )
        }

        // Mode options
        data class DndOpt(val mode: DndMode, val icon: String, val name: String, val sub: String)
        listOf(
            DndOpt(DndMode.ALL,            "🔕", "Blokir semua",      "Notif & panggilan diblokir"),
            DndOpt(DndMode.EMERGENCY_ONLY, "📵", "Darurat only",      "Hanya panggilan penting"),
            DndOpt(DndMode.SILENT,         "💬", "Silent notifikasi", "Mute suara, banner tetap"),
        ).forEach { opt ->
            val sel = dnd.mode == opt.mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (sel) Color(0xFF1E1830) else Color(0xFF1A1A2E))
                    .border(
                        width = 1.dp,
                        color = if (sel) AccentPurple else PanelBorder,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { vm.setDndMode(opt.mode) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (sel) AccentPurple.copy(alpha = 0.2f) else Color(0xFF2A2A3A)),
                    contentAlignment = Alignment.Center
                ) { Text(opt.icon, fontSize = 11.sp) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(opt.name, fontSize = 11.sp, color = Color(0xFFC0C0D8), fontWeight = FontWeight.Medium)
                    Text(opt.sub,  fontSize = 9.sp,  color = Color(0xFF555570))
                }
                if (sel) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AccentPurple)
                    )
                }
            }
        }
    }
}
