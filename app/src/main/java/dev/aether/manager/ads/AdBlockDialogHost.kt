package dev.aether.manager.ads

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Host composable — taruh sekali di AetherApp(), di atas semua konten.
 *
 * Usage di MainActivity.kt:
 *
 *   // Di dalam AetherApp() sebelum return:
 *   AdBlockDialogHost()
 */
@Composable
fun AdBlockDialogHost(
    vm: AdBlockViewModel = viewModel()
) {
    val showDialog by vm.showDialog.collectAsState()
    val result     by vm.detectionResult.collectAsState()
    val context    = LocalContext.current

    if (showDialog && result != null) {
        AdBlockDetectedDialog(
            result    = result!!,
            onDisable = {
                // Buka VPN settings agar user bisa nonaktifkan sendiri
                // (kita tidak bisa memaksa — ini etis & sesuai Play Policy)
                vm.onUserAcknowledged()
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_VPN_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                } catch (_: Exception) {
                    // Fallback ke wireless settings jika VPN settings tidak tersedia
                    context.startActivity(
                        Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            },
            onDismiss = {
                vm.onUserDismissed()
            }
        )
    }
}
