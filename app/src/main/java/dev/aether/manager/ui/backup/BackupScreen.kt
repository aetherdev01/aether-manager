package dev.aether.manager.ui.backup

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.util.BackupManager

@Composable
fun BackupScreen(
    vm              : MainViewModel,
    onResetProfiles : () -> Unit,
    onResetMonitor  : () -> Unit,
) {
    val s             = LocalStrings.current
    val backupList    by vm.backupList.collectAsState()
    val working       by vm.backupWorking.collectAsState()
    var showReset     by remember { mutableStateOf(false) }
    var showResetProfiles by remember { mutableStateOf(false) }
    var showResetMonitor  by remember { mutableStateOf(false) }
    var restoreTarget by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.loadBackups() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            s.settingsSectionBackup,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        AnimatedVisibility(working) {
            LinearProgressIndicator(
                Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // ── Backup sekarang ───────────────────────────────────────────────
        OutlinedButton(
            onClick  = { vm.createBackup() },
            enabled  = !working,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Outlined.Save, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(s.settingsBtnBackupNow, fontWeight = FontWeight.Medium)
        }

        // ── Reset ke default ──────────────────────────────────────────────
        Button(
            onClick  = { showReset = true },
            enabled  = !working,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor   = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Icon(Icons.Outlined.RestartAlt, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(s.settingsBtnResetAll, fontWeight = FontWeight.SemiBold)
        }

        // ── Reset App Profile ─────────────────────────────────────────────
        OutlinedButton(
            onClick  = { showResetProfiles = true },
            enabled  = !working,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border   = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Outlined.ManageAccounts, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(s.settingsBtnResetProfiles, fontWeight = FontWeight.Medium)
        }

        // ── Reset Monitor ─────────────────────────────────────────────────
        OutlinedButton(
            onClick  = { showResetMonitor = true },
            enabled  = !working,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.tertiary
            ),
            border   = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.tertiary
            )
        ) {
            Icon(Icons.Outlined.MonitorHeart, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(s.settingsBtnResetMonitor, fontWeight = FontWeight.Medium)
        }

        // ── Daftar backup ─────────────────────────────────────────────────
        if (backupList.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.FolderOff, null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        s.settingsNoBackup,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                s.settingsBackupSaved,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(backupList, key = { it.filename }) { entry ->
                    BackupItem(
                        entry     = entry,
                        working   = working,
                        onRestore = { restoreTarget = entry.filename },
                        onDelete  = { vm.deleteBackup(entry.filename) }
                    )
                }
            }
        }
    }

    // ── Confirm reset ─────────────────────────────────────────────────────
    if (showReset) {
        val s2 = LocalStrings.current
        AlertDialog(
            onDismissRequest = { showReset = false },
            icon  = { Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(s2.settingsResetTitle) },
            text  = { Text(s2.settingsResetDesc) },
            confirmButton = {
                TextButton(
                    onClick = { showReset = false; vm.resetToDefaults() },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(s2.settingsResetConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text(s2.settingsBtnCancel) }
            }
        )
    }

    // ── Confirm restore ───────────────────────────────────────────────────
    restoreTarget?.let { fname ->
        val s2 = LocalStrings.current
        AlertDialog(
            onDismissRequest = { restoreTarget = null },
            icon  = { Icon(Icons.Outlined.Restore, null) },
            title = { Text(s2.settingsRestoreTitle) },
            text  = { Text(s2.settingsRestoreDesc) },
            confirmButton = {
                TextButton(onClick = { restoreTarget = null; vm.restoreBackup(fname) }) {
                    Text(s2.settingsRestoreConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreTarget = null }) { Text(s2.settingsBtnCancel) }
            }
        )
    }

    // ── Confirm reset app profiles ────────────────────────────────────────
    if (showResetProfiles) {
        val s2 = LocalStrings.current
        AlertDialog(
            onDismissRequest = { showResetProfiles = false },
            icon  = { Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(s2.settingsResetProfilesTitle) },
            text  = { Text(s2.settingsResetProfilesDesc) },
            confirmButton = {
                TextButton(
                    onClick = { showResetProfiles = false; onResetProfiles() },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(s2.settingsResetConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showResetProfiles = false }) { Text(s2.settingsBtnCancel) }
            }
        )
    }

    // ── Confirm reset monitor ─────────────────────────────────────────────
    if (showResetMonitor) {
        val s2 = LocalStrings.current
        AlertDialog(
            onDismissRequest = { showResetMonitor = false },
            icon  = { Icon(Icons.Outlined.MonitorHeart, null, tint = MaterialTheme.colorScheme.tertiary) },
            title = { Text(s2.settingsResetMonitorTitle) },
            text  = { Text(s2.settingsResetMonitorDesc) },
            confirmButton = {
                TextButton(
                    onClick = { showResetMonitor = false; onResetMonitor() },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                ) { Text(s2.settingsResetConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showResetMonitor = false }) { Text(s2.settingsBtnCancel) }
            }
        )
    }
}

@Composable
private fun BackupItem(
    entry    : BackupManager.BackupEntry,
    working  : Boolean,
    onRestore: () -> Unit,
    onDelete : () -> Unit,
) {
    val s = LocalStrings.current
    Surface(
        shape    = RoundedCornerShape(12.dp),
        color    = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Archive, null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    entry.timestamp,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    s.settingsBackupProfile.format(entry.profile),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRestore, enabled = !working) {
                Icon(Icons.Outlined.Restore, s.settingsRestoreConfirm, tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete, enabled = !working) {
                Icon(Icons.Outlined.Delete, s.settingsBtnDelete, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
