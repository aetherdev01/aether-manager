package dev.aether.manager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.aether.manager.i18n.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RebootBottomSheet(
    onDismiss: () -> Unit,
    onReboot: () -> Unit,
    onRebootRecovery: () -> Unit,
    onReloadUI: () -> Unit
) {
    val s = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(s.logRebootOptions,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp))

            RebootOption(Icons.Outlined.RestartAlt, s.logRebootSystem, s.logRebootSystemDesc) { onReboot(); onDismiss() }
            RebootOption(Icons.Outlined.Build, s.logRebootRecovery, s.logRebootRecoveryDesc) { onRebootRecovery(); onDismiss() }
            RebootOption(Icons.Outlined.Refresh, s.logReloadUi, s.logReloadUiDesc) { onReloadUI(); onDismiss() }

            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Text(s.logBtnCancel)
            }
        }
    }
}

@Composable
private fun RebootOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, desc: String,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
