package dev.aether.manager.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.aether.manager.ads.InterstitialAdTrigger

/**
 * AdMob interstitial trigger wrapper.
 * Dipasang di layar manapun yang perlu trigger interstitial otomatis.
 */
@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
) {
    InterstitialAdTrigger()
}
