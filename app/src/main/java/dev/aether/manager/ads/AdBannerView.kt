package dev.aether.manager.ads

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Interstitial ad trigger composable.
 *
 * Pakai di mana saja yang perlu trigger interstitial secara otomatis,
 * misalnya saat composable pertama kali masuk ke komposisi (LaunchedEffect).
 *
 * Contoh penggunaan:
 *   InterstitialAdTrigger()              // trigger saat composable muncul
 *   InterstitialAdTrigger(key = tabIndex) // trigger saat tab berubah
 *
 * Tidak ada Banner placement terdaftar di Unity dashboard,
 * sehingga komponen ini murni interstitial.
 */
@Composable
fun InterstitialAdTrigger(
    key: Any? = Unit,
    onDone: ((skipped: Boolean) -> Unit)? = null,
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    LaunchedEffect(key) {
        InterstitialAdManager.showIfReady(activity, onDone)
    }
}
