package dev.aether.manager.ads

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Interstitial ad trigger composable.
 *
 * Contoh:
 *   InterstitialAdTrigger()               // trigger saat composable muncul
 *   InterstitialAdTrigger(key = tabIndex) // trigger saat tab berubah
 */
@Composable
fun InterstitialAdTrigger(
    key: Any? = Unit,
    onDone: ((dismissed: Boolean) -> Unit)? = null,
) {
    val context  = LocalContext.current
    val activity = context as? Activity ?: return

    LaunchedEffect(key) {
        InterstitialAdManager.showIfReady(activity, onDone)
    }
}
