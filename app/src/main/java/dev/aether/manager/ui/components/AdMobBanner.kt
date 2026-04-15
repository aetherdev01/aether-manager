package dev.aether.manager.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.aether.manager.ads.AdBannerView

/**
 * Wrapper Composable untuk Unity Ads Banner.
 * Nama file dipertahankan agar tidak perlu update import di tempat lain.
 */
@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
) {
    AdBannerView(modifier = modifier)
}
