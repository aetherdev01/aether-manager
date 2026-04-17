package dev.aether.manager.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aether.manager.R
import dev.aether.manager.data.MainViewModel
import dev.aether.manager.i18n.LanguageDropdown
import dev.aether.manager.i18n.LocalStrings
import dev.aether.manager.ui.home.TabSectionTitle
import dev.aether.manager.util.BackupManager

@Composable
fun AboutScreen(vm: MainViewModel) {
    val s           = LocalStrings.current
    val ctx         = LocalContext.current
    val scrollState = rememberScrollState()

    val backupList    by vm.backupList.collectAsState()
    val working       by vm.backupWorking.collectAsState()
    var showReset     by remember { mutableStateOf(false) }
    var restoreTarget by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.loadBackups() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Section: Language ─────────────────────────────────
        TabSectionTitle(
            icon  = Icons.Outlined.Language,
            title = "Language / Bahasa / Язык / 语言"
        )
        Surface(
            shape  = RoundedCornerShape(20.dp),
            color  = MaterialTheme.colorScheme.surfaceContainerLow,
            border = androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Display Language",
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Changes UI language instantly",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LanguageDropdown()
            }
        }

        // ── Section: Backup & Reset ───────────────────────────
        TabSectionTitle(
            icon  = Icons.Outlined.Archive,
            title = "Backup & Reset"
        )

        // Progress indicator
        AnimatedVisibility(working) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color    = MaterialTheme.colorScheme.primary
            )
        }

        // Action buttons
        Row(
            modifier            = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick  = { vm.createBackup() },
                enabled  = !working,
                modifier = Modifier.weight(1f).height(48.dp),
                shape    = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Outlined.Save, null, Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Backup", fontWeight = FontWeight.Medium)
            }
            Button(
                onClick  = { showReset = true },
                enabled  = !working,
                modifier = Modifier.weight(1f).height(48.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor   = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Outlined.RestartAlt, null, Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Reset Default", fontWeight = FontWeight.SemiBold)
            }
        }

        // Backup list
        if (backupList.isEmpty()) {
            Surface(
                shape    = RoundedCornerShape(16.dp),
                color    = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier             = Modifier.padding(16.dp),
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.FolderOff, null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Belum ada backup tersimpan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Surface(
                shape    = RoundedCornerShape(16.dp),
                color    = MaterialTheme.colorScheme.surfaceContainerLow,
                border   = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    backupList.forEachIndexed { index, entry ->
                        AboutBackupItem(
                            entry     = entry,
                            working   = working,
                            onRestore = { restoreTarget = entry.filename },
                            onDelete  = { vm.deleteBackup(entry.filename) }
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

        // ── Section: Developer ────────────────────────────────
        TabSectionTitle(
            icon  = Icons.Outlined.Person,
            title = s.aboutSectionDev
        )
        DevProfileCard()

        // ── Section: Komunitas ────────────────────────────────
        TabSectionTitle(
            icon  = Icons.Outlined.Language,
            title = s.aboutSectionLinks
        )
        AboutSection {
            LinkRow(
                icon     = Icons.Outlined.Code,
                label    = s.aboutGithub,
                subtitle = "github.com/aetherdev01",
                onClick  = { ctx.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/aetherdev01"))) }
            )
            AboutDivider()
            LinkRow(
                icon     = Icons.Outlined.Send,
                label    = s.aboutTelegram,
                subtitle = "@get01projects",
                onClick  = { ctx.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://t.me/get01projects"))) }
            )
            AboutDivider()
            LinkRow(
                icon     = Icons.Outlined.Favorite,
                label    = s.aboutSaweriaLabel,
                subtitle = s.aboutSaweria,
                onClick  = { ctx.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://saweria.co/AetherDev"))) }
            )
        }
    } // end Column

    // ── Confirm reset ─────────────────────────────────────────────────────
    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            icon  = { Icon(Icons.Outlined.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Reset ke Default?") },
            text  = {
                Text(
                    "Semua tweak dinonaktifkan dan nilai sistem dikembalikan ke default Android. " +
                    "File backup yang ada tidak terhapus."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showReset = false; vm.resetToDefaults() },
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text("Batal") }
            }
        )
    }

    // ── Confirm restore ───────────────────────────────────────────────────
    restoreTarget?.let { fname ->
        AlertDialog(
            onDismissRequest = { restoreTarget = null },
            icon  = { Icon(Icons.Outlined.Restore, null) },
            title = { Text("Restore Backup?") },
            text  = { Text("Setting aktif diganti dengan backup ini dan langsung diterapkan ke sistem.") },
            confirmButton = {
                TextButton(onClick = { restoreTarget = null; vm.restoreBackup(fname) }) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreTarget = null }) { Text("Batal") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BACKUP ITEM (inline di About)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AboutBackupItem(
    entry    : BackupManager.BackupEntry,
    working  : Boolean,
    onRestore: () -> Unit,
    onDelete : () -> Unit,
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
                "Profile: ${entry.profile}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onRestore, enabled = !working) {
            Icon(Icons.Outlined.Restore, "Restore", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onDelete, enabled = !working) {
            Icon(Icons.Outlined.Delete, "Hapus", tint = MaterialTheme.colorScheme.error)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DEV PROFILE CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DevProfileCard() {
    val s       = LocalStrings.current
    val primary = MaterialTheme.colorScheme.primary

    Surface(
        shape    = RoundedCornerShape(20.dp),
        color    = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape)
                        .background(primary.copy(alpha = 0.15f))
                )
                Box(
                    modifier         = Modifier.size(56.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter            = painterResource(id = R.drawable.profile_avatar),
                        contentDescription = "AetherDev",
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop
                    )
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Name + verified badge
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        "AetherDev",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Surface(shape = CircleShape, color = primary) {
                        Box(Modifier.padding(3.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Check, null,
                                tint     = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    }
                }
                Text(
                    "@AetherDev22",
                    style      = MaterialTheme.typography.bodySmall,
                    color      = primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    s.aboutDevDesc,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION WRAPPER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AboutSection(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape  = RoundedCornerShape(20.dp),
        color  = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(content = content)
    }
}

@Composable
private fun AboutDivider() = HorizontalDivider(
    modifier  = Modifier.padding(start = 56.dp, end = 16.dp),
    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    thickness = 0.5.dp
)

// ─────────────────────────────────────────────────────────────────────────────
// ABOUT ROW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AboutRow(
    key: String, value: String,
    icon: ImageVector, iconTint: Color
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier         = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(17.dp))
        }
        Text(
            key,
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(76.dp)
        )
        Text(
            value,
            style      = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurface,
            modifier   = Modifier.weight(1f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LINK ROW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LinkRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier         = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, null,
                    tint     = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    label,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Outlined.OpenInNew, null,
                tint     = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
