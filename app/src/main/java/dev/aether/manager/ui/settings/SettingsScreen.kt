package dev.aether.manager.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import dev.aether.manager.i18n.AppLanguage
import dev.aether.manager.i18n.LocalLanguage
import dev.aether.manager.i18n.LocalSetLanguage
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
    val s           = LocalStrings.current
    val ctx         = LocalContext.current
    val backupList  by vm.backupList.collectAsState()
    val working     by vm.backupWorking.collectAsState()

    var showReset         by remember { mutableStateOf(false) }
    var showResetProfiles by remember { mutableStateOf(false) }
    var showResetMonitor  by remember { mutableStateOf(false) }
    var showClearCache    by remember { mutableStateOf(false) }
    var restoreTarget     by remember { mutableStateOf<String?>(null) }
    val scrollState       = rememberScrollState()

    // ── Collapsible section states ────────────────────────────────────────
    var backupExpanded     by remember { mutableStateOf(false) }
    var appearanceExpanded by remember { mutableStateOf(false) }
    var generalExpanded    by remember { mutableStateOf(false) }
    var advancedExpanded   by remember { mutableStateOf(false) }
    var aboutExpanded      by remember { mutableStateOf(false) }

    // ── Settings state from ViewModel (persisted) ─────────────────────────
    val darkModeOverride by vm.darkModeOverride.collectAsState()
    val darkMode         by vm.darkMode.collectAsState()
    val dynamicColor     by vm.dynamicColor.collectAsState()
    val autoBackup       by vm.autoBackup.collectAsState()
    val applyOnBoot      by vm.applyOnBoot.collectAsState()
    val notifications    by vm.notifications.collectAsState()
    val debugLog         by vm.debugLog.collectAsState()

    // ── Language ──────────────────────────────────────────────────────────
    val currentLanguage = LocalLanguage.current
    val setLanguage     = LocalSetLanguage.current
    var showLangSheet   by remember { mutableStateOf(false) }

    // ── Static info ───────────────────────────────────────────────────────
    val rootMethod = remember { RootManager.detectRootType() }
    val versionName = remember {
        try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "v?" }
        catch (_: Exception) { "v?" }
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

            // ══ Backup & Reset ════════════════════════════════════════════
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
                    // Backup list
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

            // ══ Appearance ════════════════════════════════════════════════
            SettingsSectionCard(
                icon     = Icons.Outlined.Palette,
                title    = s.settingsSectionAppearance,
                expanded = appearanceExpanded,
                onToggle = { appearanceExpanded = !appearanceExpanded }
            ) {
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    // Dark mode — toggle with tri-state (system / on / off)
                    val systemDark = isSystemInDarkTheme()
                    SettingsRowSwitch(
                        icon            = Icons.Outlined.DarkMode,
                        title           = s.settingsDarkMode,
                        subtitle        = if (darkModeOverride) s.settingsDarkModeDesc
                                          else s.settingsDarkModeDesc,
                        checked         = if (darkModeOverride) darkMode else systemDark,
                        onCheckedChange = { vm.setDarkMode(it) }
                    )
                    SettingsDivider()
                    SettingsRowSwitch(
                        icon            = Icons.Outlined.ColorLens,
                        title           = s.settingsDynamicColor,
                        subtitle        = s.settingsDynamicColorDesc,
                        checked         = dynamicColor,
                        onCheckedChange = { vm.setDynamicColor(it) }
                    )
                    SettingsDivider()
                    // Language picker row
                    SettingsRowInfo(
                        icon     = Icons.Outlined.Language,
                        title    = s.settingsLanguage,
                        subtitle = currentLanguage.nativeName,
                        onClick  = { showLangSheet = true }
                    )
                }
            }

            // ══ General ═══════════════════════════════════════════════════
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
                        onCheckedChange = { vm.setAutoBackup(it) }
                    )
                    SettingsDivider()
                    SettingsRowSwitch(
                        icon            = Icons.Outlined.FlashOn,
                        title           = s.settingsApplyOnBoot,
                        subtitle        = s.settingsApplyOnBootDesc,
                        checked         = applyOnBoot,
                        onCheckedChange = { vm.setApplyOnBoot(it) }
                    )
                    SettingsDivider()
                    SettingsRowSwitch(
                        icon            = Icons.Outlined.Notifications,
                        title           = s.settingsNotifications,
                        subtitle        = s.settingsNotificationsDesc,
                        checked         = notifications,
                        onCheckedChange = { vm.setNotifications(it) }
                    )
                }
            }

            // ══ Advanced ══════════════════════════════════════════════════
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
                        onCheckedChange = { vm.setDebugLog(it) }
                    )
                    SettingsDivider()
                    SettingsRowInfo(
                        icon     = Icons.Outlined.CleaningServices,
                        title    = s.settingsClearCache,
                        subtitle = s.settingsClearCacheDesc,
                        onClick  = { showClearCache = true }
                    )
                }
            }

            // ══ About ═════════════════════════════════════════════════════
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
                        onClick  = {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/get01projects/aether-manager"))
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsRowInfo(
                        icon     = Icons.Outlined.Gavel,
                        title    = s.settingsLicense,
                        subtitle = s.settingsLicenseDesc,
                        onClick  = null
                    )
                    SettingsDivider()
                    SettingsRowInfo(
                        icon     = Icons.Outlined.AppSettingsAlt,
                        title    = "App Info",
                        subtitle = "System app settings",
                        onClick  = {
                            ctx.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", ctx.packageName, null)
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────

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

    if (showClearCache) {
        AlertDialog(
            onDismissRequest = { showClearCache = false },
            icon  = { Icon(Icons.Outlined.CleaningServices, null) },
            title = { Text(s.settingsClearCache) },
            text  = { Text(s.settingsClearCacheDesc) },
            confirmButton = {
                TextButton(onClick = { showClearCache = false; vm.clearAppCache() }) {
                    Text(s.settingsResetConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCache = false }) { Text(s.settingsBtnCancel) }
            }
        )
    }

    // ── Language picker bottom sheet ──────────────────────────────────────
    if (showLangSheet) {
        LanguagePickerSheet(
            current  = currentLanguage,
            onSelect = { lang -> setLanguage(lang); showLangSheet = false },
            onDismiss = { showLangSheet = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LANGUAGE PICKER SHEET
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePickerSheet(
    current  : AppLanguage,
    onSelect : (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Pilih Bahasa / Select Language",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.padding(bottom = 8.dp)
            )
            AppLanguage.entries.forEach { lang ->
                val selected = lang == current
                Surface(
                    shape    = RoundedCornerShape(12.dp),
                    color    = if (selected) MaterialTheme.colorScheme.primaryContainer
                               else MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(lang) }
                ) {
                    Row(
                        modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            lang.langIcon,
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color      = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                         else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                lang.nativeName,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color      = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                             else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                lang.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selected) {
                            Icon(
                                Icons.Outlined.CheckCircle, null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
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
                    modifier = Modifier.size(20.dp).rotate(rotation)
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
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title,    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,  color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsRowInfo(
    icon     : ImageVector,
    title    : String,
    subtitle : String,
    onClick  : (() -> Unit)?,
) {
    val mod = if (onClick != null)
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp)
    else
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)

    Row(
        modifier              = mod,
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title,    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,  color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onClick != null) {
            Icon(Icons.Outlined.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
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
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Archive, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(entry.timestamp, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text(profileLabel,    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onRestore, enabled = !working) {
            Icon(Icons.Outlined.Restore, "Restore", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onDelete, enabled = !working) {
            Icon(Icons.Outlined.Delete, deleteLabel, tint = MaterialTheme.colorScheme.error)
        }
    }
}
