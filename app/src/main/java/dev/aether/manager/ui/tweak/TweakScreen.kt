package dev.aether.manager.ui.tweak

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.data.UiState
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.ui.components.*
import dev.aether.manager.ui.home.TabSectionTitle

@Composable
fun TweakScreen(vm: MainViewModel) {
    val s           = LocalStrings.current
    val tweaks      by vm.tweaks.collectAsState()
    val deviceState by vm.deviceInfo.collectAsState()
    val applying    by vm.applyingTweak.collectAsState()
    val scrollState = rememberScrollState()
    val context     = LocalContext.current
    val activity    = context as? android.app.Activity

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(visible = applying,
                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
            }

            // ── Performance Profile ───────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TabSectionTitle(icon = Icons.Outlined.Tune, title = s.tweakPerformanceProfile)
                val current = (deviceState as? UiState.Success)?.data?.profile ?: "balance"
                ProfileGrid(current = current, onSelect = { vm.setProfile(it) })
            }


            // ── CPU & Kernel ──────────────────────────────────
            TweakSection(s.tweakSectionCpu) {
                TweakRow(Icons.Outlined.DateRange, s.tweakSchedBoost, s.tweakSchedBoostDesc, tweaks.schedboost) { vm.setTweak("schedboost", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.TrendingUp, s.tweakCpuBoost, s.tweakCpuBoostDesc, tweaks.cpuBoost) { vm.setTweak("cpu_boost", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.GridView, s.tweakGpuThrottle, s.tweakGpuThrottleDesc, tweaks.gpuThrottleOff) { vm.setTweak("gpu_throttle_off", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.Memory, s.tweakCpusetOpt, s.tweakCpusetOptDesc, tweaks.cpusetOpt) { vm.setTweak("cpuset_opt", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.Speed, s.tweakMtkBoost, s.tweakMtkBoostDesc, tweaks.mtkBoost) { vm.setTweak("obb_noop", it) }
            }

            // ── Memory ────────────────────────────────────────
            TweakSection(s.tweakSectionMemory) {
                TweakRow(Icons.Outlined.Article, s.tweakLmk, s.tweakLmkDesc, tweaks.lmkAggressive) { vm.setTweak("lmk_aggressive", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.Memory, s.tweakZram, s.tweakZramDesc, tweaks.zram) { vm.setTweak("zram", it) }
                ItemDivider()
                ZramSizeRow(tweaks.zramSize) { vm.setTweakStr("zram_size", it) }
                ItemDivider()
                ZramAlgoRow(tweaks.zramAlgo) { vm.setTweakStr("zram_algo", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.Info, s.tweakVmDirty, s.tweakVmDirtyDesc, tweaks.vmDirtyOpt) { vm.setTweak("vm_dirty_opt", it) }
            }

            // ── I/O ───────────────────────────────────────────
            TweakSection(s.tweakSectionIo) {
                IoSchedulerRow(tweaks.ioScheduler) { vm.setTweakStr("io_scheduler", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.Add, s.tweakIoLatency, s.tweakIoLatencyDesc, tweaks.ioLatencyOpt) { vm.setTweak("io_latency_opt", it) }
            }

            // ── Network ───────────────────────────────────────
            TweakSection(s.tweakSectionNetwork) {
                TweakRow(Icons.Outlined.Language, s.tweakTcpBbr, s.tweakTcpBbrDesc, tweaks.tcpBbr) { vm.setTweak("tcp_bbr", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.Lock, s.tweakDoh, s.tweakDohDesc, tweaks.doh) { vm.setTweak("doh", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.SwapHoriz, s.tweakNetBuffer, s.tweakNetBufferDesc, tweaks.netBuffer) { vm.setTweak("net_buffer", it) }
            }

            // ── Battery & Daily ───────────────────────────────
            TweakSection(s.tweakSectionBattery) {
                TweakRow(Icons.Outlined.NightsStay, s.tweakDoze, s.tweakDozeDesc, tweaks.doze) { vm.setTweak("doze", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.Delete, s.tweakClearCache, s.tweakClearCacheDesc, tweaks.clearCache) { vm.setTweak("clear_cache", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.Animation, s.tweakFastAnim, s.tweakFastAnimDesc, tweaks.fastAnim) { vm.setTweak("fast_anim", it) }
                ItemDivider()
                TweakRow(Icons.Outlined.Shuffle, s.tweakEntropy, s.tweakEntropyDesc, tweaks.entropyBoost) { vm.setTweak("entropy_boost", it) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION WRAPPER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TweakSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TabSectionTitle(icon = Icons.Outlined.Settings, title = title)
        Surface(
            shape          = RoundedCornerShape(20.dp),
            color          = MaterialTheme.colorScheme.surfaceContainerLow,
            border         = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            ),
            tonalElevation = 0.dp
        ) {
            Column(content = content)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOGGLE ROW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TweakRow(
    icon: ImageVector, title: String, subtitle: String,
    checked: Boolean, onToggle: (Boolean) -> Unit
) {
    CardItemRow(
        icon               = icon,
        title              = title,
        subtitle           = subtitle,
        iconContainerColor = if (checked) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surfaceContainerHighest,
        iconTint           = if (checked) MaterialTheme.colorScheme.onPrimaryContainer
                             else MaterialTheme.colorScheme.onSurfaceVariant,
        trailingContent    = { AetherSwitch(checked = checked, onCheckedChange = onToggle) }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// PROFILE GRID — compact 2x2
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileGrid(current: String, onSelect: (String) -> Unit) {
    val profiles = listOf(
        Triple("balance",     "Balance",      "schedutil")        to Pair(Icons.Outlined.Balance,             MaterialTheme.colorScheme.primary),
        Triple("performance", "Performance",  "performance gov")  to Pair(Icons.Outlined.BatteryChargingFull, MaterialTheme.colorScheme.error),
        Triple("gaming",      "Gaming",       "schedutil+boost")  to Pair(Icons.Outlined.SportsEsports,       MaterialTheme.colorScheme.tertiary),
        Triple("battery",     "Battery Saver","powersave gov")    to Pair(Icons.Outlined.BatteryFull,         Color(0xFF2D7D46)),
    )

    Surface(
        shape  = RoundedCornerShape(20.dp),
        color  = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    ) {
        Column {
            profiles.forEachIndexed { idx, (triple, iconColor) ->
                val (key, label, desc) = triple
                val (icon, accentColor) = iconColor
                val isActive = current == key
                Surface(
                    onClick = { onSelect(key) },
                    color   = if (isActive)
                                  accentColor.copy(alpha = 0.08f)
                              else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .run {
                                    if (isActive) this.background(
                                        accentColor.copy(alpha = 0.15f),
                                        RoundedCornerShape(11.dp)
                                    ) else this.background(
                                        MaterialTheme.colorScheme.surfaceContainerHighest,
                                        RoundedCornerShape(11.dp)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, null,
                                tint     = if (isActive) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(label,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color      = if (isActive) accentColor else MaterialTheme.colorScheme.onSurface)
                            Text(desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isActive) {
                            Icon(Icons.Filled.CheckCircle, null,
                                tint = accentColor, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                if (idx < profiles.lastIndex)
                    HorizontalDivider(
                        modifier  = Modifier.padding(start = 68.dp, end = 16.dp),
                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DROPDOWN ROWS
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZramSizeRow(current: String, onSelect: (String) -> Unit) {
    val s = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("536870912" to "512 MB", "1073741824" to "1 GB", "2147483648" to "2 GB", "3221225472" to "3 GB")
    DropdownRow(
        icon    = Icons.Outlined.Memory,
        title   = s.tweakZramSize,
        subtitle = "Ukuran compressed swap",
        value   = options.find { it.first == current }?.second ?: "1 GB",
        expanded = expanded,
        onExpand = { expanded = it }
    ) {
        options.forEach { (v, l) ->
            DropdownMenuItem(text = { Text(l) }, onClick = { onSelect(v); expanded = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZramAlgoRow(current: String, onSelect: (String) -> Unit) {
    val s = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("lz4", "lzo", "zstd", "lz4hc")
    DropdownRow(
        icon    = Icons.Outlined.CompareArrows,
        title   = s.tweakZramAlgo,
        subtitle = "Kompresi — LZ4 paling cepat",
        value   = current.ifBlank { "lz4" },
        expanded = expanded,
        onExpand = { expanded = it }
    ) {
        options.forEach { opt ->
            DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); expanded = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IoSchedulerRow(current: String, onSelect: (String) -> Unit) {
    val s = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("" to "Default", "none" to "none (UFS)", "noop" to "noop (eMMC)",
        "cfq" to "cfq", "deadline" to "deadline", "mq-deadline" to "mq-deadline",
        "bfq" to "bfq", "kyber" to "kyber")
    DropdownRow(
        icon    = Icons.Outlined.Sort,
        title   = s.tweakIoScheduler,
        subtitle = "Algoritma I/O",
        value   = options.find { it.first == current }?.second ?: "Default",
        expanded = expanded,
        onExpand = { expanded = it }
    ) {
        options.forEach { (v, l) ->
            DropdownMenuItem(text = { Text(l) }, onClick = { onSelect(v); expanded = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String,
    expanded: Boolean,
    onExpand: (Boolean) -> Unit,
    menuContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(
                MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpand) {
            OutlinedTextField(
                value         = value,
                onValueChange = {},
                readOnly      = true,
                modifier      = Modifier.menuAnchor().width(105.dp),
                trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                textStyle     = MaterialTheme.typography.bodySmall,
                singleLine    = true
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpand(false) }) {
                menuContent()
            }
        }
    }
}
