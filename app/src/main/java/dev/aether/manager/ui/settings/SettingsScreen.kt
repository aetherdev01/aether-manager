package dev.aether.manager.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.util.BackupManager
import dev.aether.manager.util.RootManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm              : MainViewModel,
    onBack          : () -> Unit,
    onResetProfiles : () -> Unit,
    onResetMonitor  : () -> Unit,
) {
    val s             = LocalStrings.current
    val backupList    by vm.backupList.collectAsState()
    val working       by vm.backupWorking.collectAsState()
    var showReset         by remember { mutableStateOf(false) }
    var showResetProfiles by remember { mutableStateOf(false) }
    var showResetMonitor  by remember { mutableStateOf(false) }
    var restoreTarget by remember { mutableStateOf<String?>(null) }
    val scrollState   = rememberScrollState()

    // ── Collapsible states ────────────────────────────────────
    var backupExpanded     by remember { mutableStateOf(true) }
    var appearanceExpanded by remember { mutableStateOf(false) }
    var generalExpanded    by remember { mutableStateOf(false) }
    var advancedExpanded   by remember { mutableStateOf(false) }
    var aboutExpanded      by remember { mutableStateOf(false) }

    // ── Toggle prefs (UI-only placeholders) ───────────────────
    var darkMode      by remember { mutableStateOf(false) }
    var dynamicColor  by remember { mutableStateOf(true) }
    var autoBackup    by remember { mutableStateOf(false) }
    var applyOnBoot   by remember { mutableStateOf(true) }
    var notifications by remember { mutableStateOf(true) }
    var debugLog      by remember { mutableStateOf(false) }

    val rootMethod = remember { RootManager.detectRootType() }
    val ctx        = LocalContext.current
    val versionName = remember {
        try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "v?" }
        catch (e: Exception) { "v?" }
    }

    LaunchedEffect(Unit) { vm.loadBackups() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        s.settingsTitle,
                        fontWeight = FontWeight.Medium,
                        fontSize   = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = s.setupBtnBack)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                )
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ══════════════════════════════════════════════════
            // SECTION: Backup & Reset  (collapsible)
            // ══════════════════════════════════════════════════
            SettingsSectionCard(
                icon     = Icons.Outlined.Archive,
                title    = s.settingsSectionBackup,
                expanded = backupExpanded,
                onToggle = { backupExpanded = !backupExpanded }
            ) {
                Column(
                    modifier            = Modifier
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AnimatedVisibility(working) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color    = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick  = { vm.createBackup() },
                            enabled  = !working,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape    = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Outlined.Save, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(s.settingsBtnBackup, fontWeight = FontWeight.Medium)
                        }
                        Button(
                            onClick  = { showReset = true },
                            enabled  = !working,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor   = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(Icons.Outlined.RestartAlt, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(s.settingsBtnResetDefault, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick  = { showResetProfiles = true },
                            enabled  = !working,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border   = androidx.compose.foundation.BorderStroke(
                                1.dp, MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Outlined.ManageAccounts, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(s.settingsBtnResetProfiles, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick  = { showResetMonitor = true },
                            enabled  = !working,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.tertiary
                            ),
                            border   = androidx.compose.foundation.BorderStroke(
                                1.dp, MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Icon(Icons.Outlined.MonitorHeart, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(s.settingsBtnResetMonitor, fontWeight = FontWeight.Medium, maxLines = 1)
                        }
                    }

                    if (backupList.isEmpty()) {
                        Surface(
                            shape    = RoundedCornerShape(12.dp),
                            color    = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier              = Modifier.padding(14.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.FolderOff, null,
                                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    s.settingsNoBackup,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape    = RoundedCornerShape(12.dp),
                            color    = MaterialTheme.colorScheme.surfaceContainerLow,
                            border   = androidx.compose.foundation.BorderStroke(
                                1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                backupList.forEachIndexed { index, entry ->
                                    SettingsBackupItem(
                                        entry        = entry,
                                        working      = working,
                                        profileLabel = s.settingsBackupProfile.format(entry.profile),
                                        deleteLabel  = s.settingsBtnDelete,
                                        onRestore    = { restoreTarget = entry.filename },
                                        onDelete     = { vm.deleteBackup(entry.filename) }
                                    )
                                    if (index < backupList.lastIndex) {
                                        HorizontalDivider(
                                            modifier  = Modifier.padding(start = 56.dp, end = 16.dp),
                                            color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            thickness = 0.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════
            // SECTION: Appearance
            // ══════════════════════════════════════════════════
            SettingsSectionCard(
                icon     = Icons.Outlined.Palette,
                title    = s.settingsSectionAppearance,
                expanded = appearanceExpanded,
                onToggle = { appearanceExpanded = !appearanceExpanded }
            ) {
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    SettingsRowSwitch(
                        icon            = Icons.Outlined.DarkMode,
                        title           = s.settingsDarkMode,
                        subtitle        = s.settingsDarkModeDesc,
                        checked         = darkMode,
                        onCheckedChange = { darkMode = it }
                    )
                    SettingsDivider()
                    SettingsRowSwitch(
                        icon            = Icons.Outlined.ColorLens,
                        title           = s.settingsDynamicColor,
                        subtitle        = s.settingsDynamicColorDesc,
                        checked         = dynamicColor,
                        onCheckedChange = { dynamicColor = it }
                    )
                    SettingsDivider()
                    SettingsRowInfo(
                        icon     = Icons.Outlined.Language,
                        title    = s.settingsLanguage,
                        subtitle = s.settingsLanguageDesc,
                        onClick  = { /* open language picker */ }
                    )
                }
            }

            // ══════════════════════════════════════════════════
            // SECTION: General
            // ══════════════════════════════════════════════════
            SettingsSectionCard(
                icon     = Icons.Outlined.Tune,
                title    = s.settingsSectionGeneral,
                expanded = generalExpanded,
                onToggle = { generalExpanded = !generalExpanded }
            ) {
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    SettingsRowSwitch(
                        icon            = Icons.Outlined.CloudUpload,
                        title           = s.settingsAutoBackup,
                        subtitle        = s.settingsAutoBackupDesc,
                        checked         = autoBackup,
                        onCheckedChange = { autoBackup = it }
                    )
                    SettingsDivider()
                    SettingsRowSwitch(
                        icon            = Icons.Outlined.FlashOn,
                        title           = s.settingsApplyOnBoot,
                        subtitle        = s.settingsApplyOnBootDesc,
                        checked         = applyOnBoot,
                        onCheckedChange = { applyOnBoot = it }
                    )
                    SettingsDivider()
                    SettingsRowSwitch(
                        icon            = Icons.Outlined.Notifications,
                        title           = s.settingsNotifications,
                        subtitle        = s.settingsNotificationsDesc,
                        checked         = notifications,
                        onCheckedChange = { notifications = it }
                    )
                }
            }

            // ══════════════════════════════════════════════════
            // SECTION: Advanced
            // ══════════════════════════════════════════════════
            SettingsSectionCard(
                icon     = Icons.Outlined.Engineering,
                title    = s.settingsSectionAdvanced,
                expanded = advancedExpanded,
                onToggle = { advancedExpanded = !advancedExpanded }
            ) {
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    SettingsRowInfo(
                        icon     = Icons.Outlined.AdminPanelSettings,
                        title    = s.settingsRootMethod,
                        subtitle = rootMethod,
                        onClick  = null
                    )
                    SettingsDivider()
                    SettingsRowSwitch(
                        icon            = Icons.Outlined.BugReport,
                        title           = s.settingsDebugLog,
                        subtitle        = s.settingsDebugLogDesc,
                        checked         = debugLog,
                        onCheckedChange = { debugLog = it }
                    )
                    SettingsDivider()
                    SettingsRowInfo(
                        icon     = Icons.Outlined.CleaningServices,
                        title    = s.settingsClearCache,
                        subtitle = s.settingsClearCacheDesc,
                        onClick  = { /* clear cache */ }
                    )
                }
            }

            // ══════════════════════════════════════════════════
            // SECTION: About
            // ══════════════════════════════════════════════════
            SettingsSectionCard(
                icon     = Icons.Outlined.Info,
                title    = s.settingsSectionAbout,
                expanded = aboutExpanded,
                onToggle = { aboutExpanded = !aboutExpanded }
            ) {
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    SettingsRowInfo(
                        icon     = Icons.Outlined.Tag,
                        title    = s.settingsVersion,
                        subtitle = versionName,
                        onClick  = null
                    )
                    SettingsDivider()
                    SettingsRowInfo(
                        icon     = Icons.Outlined.Code,
                        title    = s.settingsSourceCode,
                        subtitle = s.settingsSourceCodeDesc,
                        onClick  = { /* open GitHub */ }
                    )
                    SettingsDivider()
                    SettingsRowInfo(
                        icon     = Icons.Outlined.Gavel,
                        title    = s.settingsLicense,
                        subtitle = s.settingsLicenseDesc,
                        onClick  = null
                    )
                }
            }
        }
    }

    // ── Dialog: Confirm reset ─────────────────────────────────────────────
    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            icon  = { Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(s.settingsResetTitle) },
            text  = { Text(s.settingsResetDesc) },
            confirmButton = {
                TextButton(
                    onClick = { showReset = false; vm.resetToDefaults() },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(s.settingsResetConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text(s.settingsBtnCancel) }
            }
        )
    }

    // ── Dialog: Confirm restore ───────────────────────────────────────────
    restoreTarget?.let { fname ->
        AlertDialog(
            onDismissRequest = { restoreTarget = null },
            icon  = { Icon(Icons.Outlined.Restore, null) },
            title = { Text(s.settingsRestoreTitle) },
            text  = { Text(s.settingsRestoreDesc) },
            confirmButton = {
                TextButton(onClick = { restoreTarget = null; vm.restoreBackup(fname) }) {
                    Text(s.settingsRestoreConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreTarget = null }) { Text(s.settingsBtnCancel) }
            }
        )
    }

    // ── Dialog: Confirm reset app profiles ────────────────────────────────
    if (showResetProfiles) {
        AlertDialog(
            onDismissRequest = { showResetProfiles = false },
            icon  = { Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(s.settingsResetProfilesTitle) },
            text  = { Text(s.settingsResetProfilesDesc) },
            confirmButton = {
                TextButton(
                    onClick = { showResetProfiles = false; onResetProfiles() },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(s.settingsResetConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showResetProfiles = false }) { Text(s.settingsBtnCancel) }
            }
        )
    }

    // ── Dialog: Confirm reset monitor ─────────────────────────────────────
    if (showResetMonitor) {
        AlertDialog(
            onDismissRequest = { showResetMonitor = false },
            icon  = { Icon(Icons.Outlined.MonitorHeart, null, tint = MaterialTheme.colorScheme.tertiary) },
            title = { Text(s.settingsResetMonitorTitle) },
            text  = { Text(s.settingsResetMonitorDesc) },
            confirmButton = {
                TextButton(
                    onClick = { showResetMonitor = false; onResetMonitor() },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                ) { Text(s.settingsResetConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showResetMonitor = false }) { Text(s.settingsBtnCancel) }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsDivider() = HorizontalDivider(
    modifier  = Modifier.padding(horizontal = 14.dp),
    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    thickness = 0.5.dp
)

// ─────────────────────────────────────────────────────────────────────────────
// COLLAPSIBLE SECTION CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionCard(
    icon     : ImageVector,
    title    : String,
    expanded : Boolean,
    onToggle : () -> Unit,
    content  : @Composable () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label       = "chevron"
    )

    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = MaterialTheme.colorScheme.surfaceContainerLow,
        border   = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon, null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    title,
                    modifier   = Modifier.weight(1f),
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Outlined.KeyboardArrowDown, null,
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotation)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(
                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )
                    content()
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SETTINGS ROW — SWITCH
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsRowSwitch(
    icon            : ImageVector,
    title           : String,
    subtitle        : String,
    checked         : Boolean,
    onCheckedChange : (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon, null,
            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(title,    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,  color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            modifier        = Modifier.height(24.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SETTINGS ROW — INFO / NAVIGATION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsRowInfo(
    icon     : ImageVector,
    title    : String,
    subtitle : String,
    onClick  : (() -> Unit)?,
) {
    val baseModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp)
    val rowModifier = if (onClick != null)
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp)
    else baseModifier

    Row(
        modifier              = rowModifier,
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon, null,
            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(title,    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,  color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onClick != null) {
            Icon(
                Icons.Outlined.ChevronRight, null,
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BACKUP ITEM
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsBackupItem(
    entry        : BackupManager.BackupEntry,
    working      : Boolean,
    profileLabel : String,
    deleteLabel  : String,
    onRestore    : () -> Unit,
    onDelete     : () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Archive, null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                entry.timestamp,
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                profileLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onRestore, enabled = !working) {
            Icon(Icons.Outlined.Restore, "Restore", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onDelete, enabled = !working) {
            Icon(Icons.Outlined.Delete, deleteLabel, tint = MaterialTheme.colorScheme.error)
        }
    }
}
