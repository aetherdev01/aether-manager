package dev.aether.manager.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.data.MonitorState
import dev.aether.manager.data.UiState
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.ui.components.*
import dev.aether.manager.util.DeviceInfo
import dev.aether.manager.util.SocType

@Composable
fun HomeScreen(vm: MainViewModel) {
    val s            = LocalStrings.current
    val deviceState  by vm.deviceInfo.collectAsState()
    val monitorState by vm.monitorState.collectAsState()
    val scroll       = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedContent(
            targetState   = deviceState,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(150)) },
            label         = "hero"
        ) { state ->
            when (state) {
                is UiState.Loading -> HeroSkeleton()
                is UiState.Success -> HeroCard(state.data)
                is UiState.Error   -> HeroError(state.msg) { vm.refresh() }
            }
        }

        AnimatedVisibility(
            visible = (deviceState as? UiState.Success)?.data?.bootCount?.let { it >= 2 } == true,
            enter   = fadeIn(tween(300)) + expandVertically(),
            exit    = fadeOut(tween(200)) + shrinkVertically()
        ) {
            val info = (deviceState as? UiState.Success)?.data
            if (info != null) BootloopBanner(info, vm)
        }

        AnimatedVisibility(
            visible = deviceState is UiState.Success,
            enter   = fadeIn(tween(400, 100)) + slideInVertically { 32 }
        ) {
            MonitorSection(monitorState) { vm.refreshMonitor() }
        }


    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HERO CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(info: DeviceInfo) {
    val primary          = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onContainer      = MaterialTheme.colorScheme.onPrimaryContainer
    val s                = LocalStrings.current

    Surface(
        shape    = RoundedCornerShape(20.dp),
        color    = primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(s.homeSystemStatus,
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp,
                    color = onContainer.copy(alpha = 0.5f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    SocBadge(info.soc)
                    Surface(shape = CircleShape, color = primary) {
                        Text(info.rootType,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(info.model,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = onContainer,
                    modifier = Modifier.weight(1f))
                ProfilePill(info.profile)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip(s.homeLabelSoc, info.socRaw.ifBlank { info.soc.label }.take(16),
                        Icons.Outlined.Memory, modifier = Modifier.weight(1f))
                    InfoChip(s.homeLabelOs, "Android ${info.android}",
                        Icons.Outlined.PhoneAndroid, modifier = Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip(s.homeLabelKernel, info.kernel.substringBefore("-").take(14),
                        Icons.Outlined.Code, modifier = Modifier.weight(1f))
                    InfoChip(s.homeSelinux, info.selinux.ifBlank { "Unknown" },
                        Icons.Outlined.Shield,
                        highlight = info.selinux.equals("Permissive", true),
                        modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MONITOR SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonitorSection(state: MonitorState, onRefresh: () -> Unit) {
    val s = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(7.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary))
                Text(s.homeMonitor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
            }
            IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // 3 arc gauges: CPU · GPU · Battery
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ArcGaugeCard(s.homeLabelCpu, state.cpuUsage, state.cpuFreq.ifBlank { "— MHz" },
                MaterialTheme.colorScheme.primary, Icons.Outlined.Memory, Modifier.weight(1f))
            ArcGaugeCard(s.homeLabelGpu, state.gpuUsage, state.gpuFreq.ifBlank { "— MHz" },
                MaterialTheme.colorScheme.tertiary, Icons.Outlined.GridView, Modifier.weight(1f))
            val batColor = when {
                state.batLevel <= 15 -> MaterialTheme.colorScheme.error
                state.batLevel <= 30 -> MaterialTheme.colorScheme.tertiary
                else                 -> MaterialTheme.colorScheme.secondary
            }
            val batIcon = when {
                state.batLevel <= 15 -> Icons.Outlined.BatteryAlert
                state.batLevel <= 50 -> Icons.Outlined.Battery3Bar
                else                 -> Icons.Outlined.BatteryFull
            }
            ArcGaugeCard(s.homeLabelBattery, state.batLevel,
                if (state.batTemp > 0f) "%.0f°C".format(state.batTemp) else "—",
                batColor, batIcon, Modifier.weight(1f), invertColor = true)
        }

        // Detail rows
        Surface(shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))) {
            Column {
                RamRow(state.ramUsedMb, state.ramTotalMb)
                MonDivider()
                StorageRow(state.storageUsedGb, state.storageTotalGb)
                if (state.swapTotalMb > 0L) {
                    MonDivider()
                    SwapRow(state.swapUsedMb, state.swapTotalMb)
                }
                MonDivider()
                TempRow(state.cpuTemp, state.batTemp)
                MonDivider()
                GovernorUptimeRow(state.cpuGovernor, state.uptime)
            }
        }
    }
}



// ─────────────────────────────────────────────────────────────────────────────
// CHIPS / PILLS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InfoChip(label: String, value: String, icon: ImageVector,
    modifier: Modifier = Modifier, highlight: Boolean = false) {
    val tint = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), modifier = modifier) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(13.dp))
            Column {
                Text(label, fontSize = 9.sp, letterSpacing = 0.4.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 11.sp)
                Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = if (highlight) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
private fun ProfilePill(profile: String) {
    val (bg, fg, icon) = when (profile) {
        "performance" -> Triple(MaterialTheme.colorScheme.errorContainer,    MaterialTheme.colorScheme.error,    Icons.Outlined.FlashOn)
        "gaming"      -> Triple(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.tertiary, Icons.Outlined.SportsEsports)
        "battery"     -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.secondary, Icons.Outlined.BatteryFull)
        else          -> Triple(MaterialTheme.colorScheme.primaryContainer,  MaterialTheme.colorScheme.primary,  Icons.Outlined.Balance)
    }
    Surface(shape = RoundedCornerShape(50), color = bg) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(12.dp))
            Text(profile.replaceFirstChar { it.uppercaseChar() },
                color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SocBadge(soc: SocType) {
    val (bg, fg) = when (soc) {
        SocType.SNAPDRAGON -> MaterialTheme.colorScheme.primaryContainer   to MaterialTheme.colorScheme.primary
        SocType.MEDIATEK   -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        SocType.EXYNOS     -> MaterialTheme.colorScheme.tertiaryContainer  to MaterialTheme.colorScheme.onTertiaryContainer
        SocType.KIRIN      -> MaterialTheme.colorScheme.errorContainer     to MaterialTheme.colorScheme.onErrorContainer
        SocType.OTHER      -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurface
    }
    Surface(shape = CircleShape, color = bg) {
        Text(soc.label, modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            color = fg, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ARC GAUGE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ArcGaugeCard(
    label: String, value: Int, subText: String,
    color: Color, icon: ImageVector, modifier: Modifier = Modifier,
    invertColor: Boolean = false
) {
    val animVal by animateFloatAsState(value.coerceIn(0, 100) / 100f,
        tween(800, easing = FastOutSlowInEasing), label = "arc")
    val arcColor = if (invertColor) color else when {
        value >= 90 -> MaterialTheme.colorScheme.error
        value >= 70 -> MaterialTheme.colorScheme.tertiary
        else        -> color
    }
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, null, tint = arcColor, modifier = Modifier.size(13.dp))
            }
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(4.dp),
                contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val stroke = 8.dp.toPx(); val pad = stroke / 2f
                    val tl = Offset(pad, pad)
                    val sz = androidx.compose.ui.geometry.Size(size.width - pad * 2, size.height - pad * 2)
                    drawArc(track, 135f, 270f, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
                    if (animVal > 0f)
                        drawArc(arcColor, 135f, 270f * animVal, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${value}%", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(subText, fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MONITOR ROWS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MonDivider() = HorizontalDivider(
    modifier  = Modifier.padding(start = 56.dp, end = 16.dp),
    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    thickness = 0.5.dp
)

@Composable
private fun BarRow(icon: ImageVector, label: String, leftText: String, pct: Float,
    pctLabel: String, color: Color) {
    val anim by animateFloatAsState(pct, tween(800, easing = FastOutSlowInEasing), label = "bar")
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp))
            .background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(leftText, style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(pctLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                Box(modifier = Modifier.fillMaxWidth(anim).fillMaxHeight().clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(color.copy(0.7f), color))))
            }
        }
    }
}

@Composable
private fun RamRow(usedMb: Long, totalMb: Long) {
    val s   = LocalStrings.current
    val pct = if (totalMb > 0) (usedMb.toFloat() / totalMb).coerceIn(0f, 1f) else 0f
    val col = if (pct > 0.85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    BarRow(Icons.Outlined.Dns, s.homeLabelRam,
        "${fmtMb(usedMb)} / ${fmtMb(totalMb)}", pct, "${(pct * 100).toInt()}%", col)
}

@Composable
private fun StorageRow(usedGb: Float, totalGb: Float) {
    val s   = LocalStrings.current
    val pct = if (totalGb > 0f) (usedGb / totalGb).coerceIn(0f, 1f) else 0f
    val col = if (pct > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
    BarRow(Icons.Outlined.Storage, s.homeLabelStorage,
        "%.1f / %.1f GB".format(usedGb, totalGb), pct, "${(pct * 100).toInt()}%", col)
}

@Composable
private fun SwapRow(usedMb: Long, totalMb: Long) {
    val s   = LocalStrings.current
    val pct = if (totalMb > 0) (usedMb.toFloat() / totalMb).coerceIn(0f, 1f) else 0f
    BarRow(Icons.Outlined.SwapVert, s.homeLabelSwap,
        "${fmtMb(usedMb)} / ${fmtMb(totalMb)}", pct, "${(pct * 100).toInt()}%",
        MaterialTheme.colorScheme.tertiary)
}

@Composable
private fun TempRow(cpuTemp: Float, batTemp: Float) {
    val s   = LocalStrings.current
    val hot = cpuTemp > 60f
    val col = if (hot) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp))
            .background(col.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Thermostat, null, tint = col, modifier = Modifier.size(16.dp))
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(s.homeTempCpu, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (cpuTemp > 0f) "%.0f°C".format(cpuTemp) else "—",
                    fontWeight = FontWeight.Bold, fontSize = 14.sp, color = col)
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp),
                horizontalAlignment = Alignment.End) {
                Text(s.homeTempBat, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (batTemp > 0f) "%.0f°C".format(batTemp) else "—",
                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun GovernorUptimeRow(governor: String, uptime: String) {
    val s   = LocalStrings.current
    val col = MaterialTheme.colorScheme.secondary
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp))
            .background(col.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Timer, null, tint = col, modifier = Modifier.size(16.dp))
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(s.homeLabelGovernor, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(governor.ifBlank { "—" },
                    fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface)
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp),
                horizontalAlignment = Alignment.End) {
                Text(s.homeLabelUptime, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(uptime.ifBlank { "—" },
                    fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SKELETON & ERROR & BOOTLOOP
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroSkeleton() {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                SkeletonBox(width = 80.dp, height = 10.dp)
                SkeletonBox(width = 70.dp, height = 20.dp, cornerRadius = 10.dp)
            }
            SkeletonBox(width = 180.dp, height = 20.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) { SkeletonBox(Modifier.weight(1f), height = 40.dp, cornerRadius = 12.dp) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) { SkeletonBox(Modifier.weight(1f), height = 40.dp, cornerRadius = 12.dp) }
            }
        }
    }
}

@Composable
private fun HeroError(msg: String, onRetry: () -> Unit) {
    val s = LocalStrings.current
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(40.dp))
            Text(msg, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer)
            Button(onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(s.homeRetry)
            }
        }
    }
}

@Composable
private fun BootloopBanner(info: DeviceInfo, vm: MainViewModel) {
    val s = LocalStrings.current
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.errorContainer,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f)) {
                Text(s.homeBootloopTitle, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer)
                Text(s.homeBootloopSub.format(info.bootCount), fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f))
            }
            Switch(checked = info.safeMode, onCheckedChange = { vm.toggleSafeMode(it) },
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.error))
        }
    }
}

private fun fmtMb(mb: Long): String = if (mb >= 1024) "%.1f GB".format(mb / 1024f) else "$mb MB"
