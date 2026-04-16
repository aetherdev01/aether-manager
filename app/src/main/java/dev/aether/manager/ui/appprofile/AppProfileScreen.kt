package dev.aether.manager.ui.appprofile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import dev.aether.manager.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ─── Screen root ─────────────────────────────────────────────────────────────

@Composable
fun AppProfileScreen(vm: AppProfileViewModel) {
    val state by vm.state.collectAsState()
    val editing by vm.editingProfile.collectAsState()
    val snack by vm.snack.collectAsState()
    val snackState = remember { SnackbarHostState() }

    LaunchedEffect(snack) {
        if (snack != null) {
            snackState.showSnackbar(snack!!, duration = SnackbarDuration.Short)
            vm.clearSnack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(180)) },
                label = "apps_state"
            ) { s ->
                when (s) {
                    is AppsUiState.Loading -> LoadingContent()
                    is AppsUiState.Error   -> ErrorContent(s.msg) { vm.load() }
                    is AppsUiState.Ready   -> ReadyContent(s, vm)
                }
            }
        }
    }

    val editTarget = editing
    if (editTarget != null) {
        val apps = (state as? AppsUiState.Ready)?.apps ?: emptyList()
        val appInfo = apps.find { it.packageName == editTarget.packageName }
        AppProfileEditor(
            profile   = editTarget,
            appLabel  = appInfo?.label ?: editTarget.packageName,
            appIcon   = appInfo?.icon,
            saving    = vm.savingPkg.collectAsState().value == editTarget.packageName,
            onDismiss = { vm.closeEditor() },
            onSave    = { vm.saveProfile(it) },
        )
    }
}

// ─── State screens ────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("Memuat aplikasi…", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ErrorContent(msg: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
            Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilledTonalButton(onClick = onRetry) { Text("Coba Lagi") }
        }
    }
}

// ─── Main content ─────────────────────────────────────────────────────────────

@Composable
private fun ReadyContent(state: AppsUiState.Ready, vm: AppProfileViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    var filterEnabled by remember { mutableStateOf(false) }

    val filtered = remember(state.apps, state.profiles, searchQuery, filterEnabled) {
        state.apps.filter { app ->
            val matchSearch = app.label.contains(searchQuery, ignoreCase = true) ||
                              app.packageName.contains(searchQuery, ignoreCase = true)
            val matchFilter = if (filterEnabled) state.profiles[app.packageName]?.enabled == true else true
            matchSearch && matchFilter
        }
    }

    val activeCount = remember(state.profiles) {
        state.profiles.values.count { it.enabled }
    }

    Column(Modifier.fillMaxSize()) {
        AppProfileHeader(
            activeCount     = activeCount,
            monitorRunning  = state.monitorRunning,
            onToggleMonitor = { vm.toggleMonitor(it) },
        )
        SearchFilterBar(
            query          = searchQuery,
            onQueryChange  = { searchQuery = it },
            filterEnabled  = filterEnabled,
            onToggleFilter = { filterEnabled = !filterEnabled },
        )
        if (filtered.isEmpty()) {
            EmptyListHint(searchQuery.isNotEmpty())
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    val profile = state.profiles[app.packageName]
                    AppListItem(
                        app      = app,
                        profile  = profile,
                        onClick  = { vm.openEditor(app) },
                        onDelete = if (profile != null) {{ vm.deleteProfile(app.packageName) }} else null,
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun AppProfileHeader(
    activeCount: Int,
    monitorRunning: Boolean,
    onToggleMonitor: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Row 1: Title kiri, Monitor toggle kanan — sejajar vertical center
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "App Profiles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            // Monitor toggle pill
            val monitorBg by animateColorAsState(
                if (monitorRunning) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                label = "monitor_bg"
            )
            val monitorFg by animateColorAsState(
                if (monitorRunning) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "monitor_fg"
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = monitorBg,
                border = if (!monitorRunning) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) else null,
            ) {
                Row(
                    Modifier.padding(start = 10.dp, end = 6.dp, top = 3.dp, bottom = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        if (monitorRunning) Icons.Filled.FiberManualRecord else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = monitorFg,
                        modifier = Modifier.size(8.dp),
                    )
                    Text(
                        if (monitorRunning) "Monitor ON" else "Monitor OFF",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = monitorFg,
                    )
                    Switch(
                        checked = monitorRunning,
                        onCheckedChange = onToggleMonitor,
                        modifier = Modifier
                            .height(24.dp)
                            .scale(0.65f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                        )
                    )
                }
            }
        }

        // Row 2: Status dot + teks aktif — di bawah title, tidak berdesakan dengan toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            val dotColor = if (activeCount > 0) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.outline
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                if (activeCount > 0) "$activeCount profile aktif" else "Tidak ada profile aktif",
                style = MaterialTheme.typography.labelSmall,
                color = if (activeCount > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── Search / Filter bar ──────────────────────────────────────────────────────

@Composable
private fun SearchFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    filterEnabled: Boolean,
    onToggleFilter: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder  = { Text("Cari aplikasi…", style = MaterialTheme.typography.bodySmall) },
            leadingIcon  = { Icon(Icons.Outlined.Search, null, modifier = Modifier.size(18.dp)) },
            trailingIcon = if (query.isNotEmpty()) {{
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Filled.Clear, null, modifier = Modifier.size(16.dp))
                }
            }} else null,
            singleLine   = true,
            modifier     = Modifier.weight(1f),
            shape        = RoundedCornerShape(14.dp),
            textStyle    = MaterialTheme.typography.bodySmall,
        )
        FilterChip(
            selected    = filterEnabled,
            onClick     = onToggleFilter,
            label       = { Text("Aktif", style = MaterialTheme.typography.labelSmall) },
            leadingIcon = if (filterEnabled) {{
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(14.dp))
            }} else null,
            shape = RoundedCornerShape(12.dp),
        )
    }
}

@Composable
private fun EmptyListHint(isSearch: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                if (isSearch) Icons.Outlined.SearchOff else Icons.Outlined.AppsOutage,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Text(
                if (isSearch) "Tidak ada hasil" else "Belum ada app profile",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── App List Item ────────────────────────────────────────────────────────────

@Composable
private fun AppListItem(
    app: AppInfo,
    profile: AppProfile?,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val hasProfile = profile != null
    val isEnabled  = profile?.enabled == true
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Load icon bitmap asynchronously to avoid janky scroll
    val iconBitmap by produceState<Bitmap?>(initialValue = null, key1 = app.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { app.icon?.let { drawableToBitmap(it) } }.getOrNull()
        }
    }

    Surface(
        onClick  = onClick,
        shape    = RoundedCornerShape(14.dp),
        color    = if (isEnabled)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surfaceContainer,
        border   = if (isEnabled) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // App icon
            AppIconView(
                bitmap     = iconBitmap,
                label      = app.label,
                isEnabled  = isEnabled,
                size       = 44.dp,
                cornerSize = 12.dp,
            )

            // App label + pkg + badges
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    app.label,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Text(
                    app.packageName,
                    style   = MaterialTheme.typography.labelSmall,
                    color   = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (hasProfile && isEnabled) {
                    val gov = profile!!.cpuGovernor
                    val rr  = profile.refreshRate
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (gov != "default") ProfileBadge(gov.replaceFirstChar { it.uppercase() }, Icons.Filled.Memory)
                        if (rr  != "default") ProfileBadge("$rr Hz", Icons.Filled.DisplaySettings)
                    }
                }
            }

            // Trailing
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (isEnabled) {
                    Icon(Icons.Filled.CheckCircle, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                if (onDelete != null) {
                    IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Outlined.DeleteOutline, null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                    }
                }
                Icon(Icons.Filled.ChevronRight, null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon  = { Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Hapus Profile?") },
            text  = { Text("Profile \"${app.label}\" akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete?.invoke() }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") }
            }
        )
    }
}

// ─── Shared icon view ─────────────────────────────────────────────────────────

@Composable
private fun AppIconView(
    bitmap: Bitmap?,
    label: String,
    isEnabled: Boolean,
    size: Dp,
    cornerSize: Dp,
) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(cornerSize)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                painter = BitmapPainter(bitmap.asImageBitmap()),
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    if (isEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondaryContainer
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label.take(1).uppercase(),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = if (isEnabled) MaterialTheme.colorScheme.onPrimary
                                 else MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

// ─── Profile badge ────────────────────────────────────────────────────────────

@Composable
private fun ProfileBadge(text: String, icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    ) {
        Row(
            Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary)
            Text(text, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
        }
    }
}

// ─── Bottom Sheet Editor ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppProfileEditor(
    profile: AppProfile,
    appLabel: String,
    appIcon: android.graphics.drawable.Drawable?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (AppProfile) -> Unit,
) {
    var draft by remember(profile) { mutableStateOf(profile) }

    // Load icon async
    val iconBitmap by produceState<Bitmap?>(initialValue = null, key1 = profile.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { appIcon?.let { drawableToBitmap(it) } }.getOrNull()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState    = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle    = { BottomSheetDefaults.DragHandle() },
        shape         = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App header
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppIconView(
                    bitmap     = iconBitmap,
                    label      = appLabel,
                    isEnabled  = draft.enabled,
                    size       = 52.dp,
                    cornerSize = 14.dp,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(appLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(draft.packageName, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                // Enable toggle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Switch(
                        checked  = draft.enabled,
                        onCheckedChange = { draft = draft.copy(enabled = it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                        )
                    )
                    Text(
                        if (draft.enabled) "Aktif" else "Nonaktif",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (draft.enabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider()

            // CPU Governor
            EditorSectionHeader(Icons.Filled.Memory, "CPU Governor")
            GovernorSelector(
                selected = draft.cpuGovernor,
                onSelect = { draft = draft.copy(cpuGovernor = it) },
                enabled  = draft.enabled,
            )

            // Refresh Rate
            EditorSectionHeader(Icons.Filled.DisplaySettings, "Refresh Rate")
            RefreshRateSelector(
                selected = draft.refreshRate,
                onSelect = { draft = draft.copy(refreshRate = it) },
                enabled  = draft.enabled,
            )

            // Extra Tweaks
            EditorSectionHeader(Icons.Filled.Tune, "Tweaks Tambahan")
            ExtraTweaksPanel(
                tweaks   = draft.extraTweaks,
                enabled  = draft.enabled,
                onChange = { draft = draft.copy(extraTweaks = it) },
            )

            Spacer(Modifier.height(4.dp))

            // Save button
            Button(
                onClick  = { onSave(draft) },
                enabled  = !saving,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(14.dp),
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Simpan Profile", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ─── Editor sub-components ────────────────────────────────────────────────────

@Composable
private fun EditorSectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun GovernorSelector(selected: String, onSelect: (String) -> Unit, enabled: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("default", "performance", "powersave").forEach { gov ->
                GovernorChip(
                    label    = CpuGovernors.labels[gov] ?: gov,
                    icon     = govIcon(gov),
                    selected = selected == gov,
                    enabled  = enabled,
                    modifier = Modifier.weight(1f),
                    onClick  = { onSelect(gov) },
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ondemand", "conservative").forEach { gov ->
                GovernorChip(
                    label    = CpuGovernors.labels[gov] ?: gov,
                    icon     = govIcon(gov),
                    selected = selected == gov,
                    enabled  = enabled,
                    modifier = Modifier.weight(1f),
                    onClick  = { onSelect(gov) },
                )
            }
        }
        AnimatedContent(selected, label = "gov_desc") { gov ->
            val desc = govDescription(gov)
            if (desc.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Info, null,
                            modifier = Modifier.size(13.dp).padding(top = 1.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(desc, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun GovernorChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary
        else if (!enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "gov_chip_bg"
    )
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "gov_chip_fg"
    )
    Surface(
        onClick  = { if (enabled) onClick() },
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        color    = bg,
        border   = if (!selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)) else null,
        enabled  = enabled,
    ) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = fg)
            Text(label, style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RefreshRateSelector(selected: String, onSelect: (String) -> Unit, enabled: Boolean) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RefreshRates.all.forEach { rate ->
            val isSelected = selected == rate
            val bg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.secondary
                else if (!enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceVariant,
                label = "rr_chip_$rate"
            )
            val fg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.onSecondary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "rr_fg_$rate"
            )
            Surface(
                onClick  = { if (enabled) onSelect(rate) },
                shape    = RoundedCornerShape(12.dp),
                color    = bg,
                enabled  = enabled,
                border   = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)) else null,
            ) {
                Column(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp), tint = fg)
                    Text(RefreshRates.labels[rate] ?: rate, style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = fg)
                }
            }
        }
    }
}

@Composable
private fun ExtraTweaksPanel(
    tweaks: AppExtraTweaks,
    enabled: Boolean,
    onChange: (AppExtraTweaks) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
    ) {
        Column(Modifier.fillMaxWidth()) {
            TweakToggleRow(Icons.Outlined.BatterySaver, "Disable Doze",
                "Cegah Doze mode saat app aktif",
                tweaks.disableDoze, enabled) { onChange(tweaks.copy(disableDoze = it)) }
            HorizontalDivider(Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp)
            TweakToggleRow(Icons.Outlined.Speed, "Lock CPU Min Freq",
                "Kunci frekuensi minimum CPU agar tidak drop",
                tweaks.lockCpuMin, enabled) { onChange(tweaks.copy(lockCpuMin = it)) }
            HorizontalDivider(Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp)
            TweakToggleRow(Icons.Outlined.CleaningServices, "Kill Background Apps",
                "Matikan semua background app saat dibuka",
                tweaks.killBackground, enabled) { onChange(tweaks.copy(killBackground = it)) }
            HorizontalDivider(Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp)
            TweakToggleRow(Icons.Outlined.Videocam, "GPU Boost",
                "Set GPU governor ke performance",
                tweaks.gpuBoost, enabled) { onChange(tweaks.copy(gpuBoost = it)) }
            HorizontalDivider(Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp)
            TweakToggleRow(Icons.Outlined.Storage, "I/O Latency Opt",
                "Kurangi read-ahead I/O untuk latency lebih rendah",
                tweaks.ioLatency, enabled, isLast = true) { onChange(tweaks.copy(ioLatency = it)) }
        }
    }
}

@Composable
private fun TweakToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    isLast: Boolean = false,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                .background(
                    if (checked && enabled) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon, null,
                modifier = Modifier.size(17.dp),
                tint = if (checked && enabled) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked         = checked,
            onCheckedChange = { if (enabled) onChange(it) },
            enabled         = enabled,
            modifier        = Modifier.scale(0.75f),
            colors          = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            )
        )
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) return drawable.bitmap
    val w = drawable.intrinsicWidth.coerceIn(1, 512)
    val h = drawable.intrinsicHeight.coerceIn(1, 512)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

private fun govIcon(gov: String): ImageVector = when (gov) {
    "performance"  -> Icons.Filled.FlashOn
    "powersave"    -> Icons.Filled.BatterySaver
    "ondemand"     -> Icons.Filled.AutoMode
    "conservative" -> Icons.Filled.TrendingDown
    "schedutil"    -> Icons.Filled.Schedule
    "interactive"  -> Icons.Filled.TouchApp
    else           -> Icons.Filled.Tune
}

private fun govDescription(gov: String): String = when (gov) {
    "default"      -> "Gunakan governor default sistem. Tidak ada perubahan yang diterapkan."
    "performance"  -> "CPU berjalan di frekuensi maksimum terus-menerus. Performa tertinggi, konsumsi baterai besar."
    "powersave"    -> "CPU berjalan di frekuensi minimum. Hemat baterai, performa rendah."
    "ondemand"     -> "CPU naik cepat saat load tinggi, turun saat idle. Balance antara performa dan baterai."
    "conservative" -> "CPU naik/turun perlahan mengikuti load. Lebih hemat dari ondemand, lebih lambat merespons."
    "schedutil"    -> "Berdasarkan scheduler kernel, responsif dan efisien. Direkomendasikan untuk kernel modern."
    "interactive"  -> "Dioptimasi untuk interaksi user, cepat naik saat ada input layar."
    else           -> ""
}
