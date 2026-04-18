package dev.aether.manager.ads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Dialog persuasif untuk memberi tahu pengguna bahwa ad-blocker/VPN terdeteksi.
 *
 * Sesuai Google Play policy:
 *  - Dialog DAPAT di-dismiss (ada tombol "Tetap Blokir" / "Continue Anyway")
 *  - Tidak memaksa atau mengunci pengguna
 *  - Hanya menjelaskan dampak monetisasi dan meminta dukungan
 */
@Composable
fun AdBlockDetectedDialog(
    result: AdBlockDetector.DetectionResult,
    onDisable: () -> Unit,    // user mau nonaktifkan blocker
    onDismiss: () -> Unit,    // user tetap ingin lanjut dengan blocker
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress   = false,
            dismissOnClickOutside = false
        )
    ) {
        AnimatedVisibility(
            visible = visible,
            enter   = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f)
        ) {
            Card(
                shape  = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Icon ──────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Block,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.error,
                            modifier           = Modifier.size(32.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Title ─────────────────────────────────────────────
                    Text(
                        text       = "Ad Blocker Terdeteksi",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign  = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    // ── Detection detail chips ────────────────────────────
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (result.vpnActive) DetectionChip("VPN Aktif")
                        if (result.adguardDns) DetectionChip("AdGuard DNS")
                    }

                    Spacer(Modifier.height(14.dp))

                    // ── Body ──────────────────────────────────────────────
                    Text(
                        text = buildString {
                            append("Aether Manager gratis dan tetap dikembangkan berkat iklan.\n\n")
                            append("Kami mendeteksi bahwa ")
                            if (result.vpnActive && result.adguardDns)
                                append("VPN dan AdGuard DNS kamu sedang aktif")
                            else if (result.vpnActive)
                                append("VPN kamu sedang aktif")
                            else
                                append("AdGuard DNS kamu sedang aktif")
                            append(", yang memblokir iklan kami.\n\n")
                            append("Pertimbangkan untuk menonaktifkannya saat menggunakan app ini agar kami dapat terus berkembang. 🙏")
                        },
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(24.dp))

                    // ── Primary CTA ───────────────────────────────────────
                    Button(
                        onClick  = onDisable,
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.FavoriteBorder, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Dukung Kami — Nonaktifkan Blocker", fontWeight = FontWeight.Medium)
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── Dismiss (user tetap bisa lanjut) ──────────────────
                    TextButton(
                        onClick  = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Tetap Lanjutkan",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetectionChip(label: String) {
    Surface(
        shape  = RoundedCornerShape(20.dp),
        color  = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
        modifier = Modifier
    ) {
        Text(
            text     = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            color    = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Medium
        )
    }
}
