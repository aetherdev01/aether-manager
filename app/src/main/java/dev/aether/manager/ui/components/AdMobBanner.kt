package dev.aether.manager.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.aether.manager.ads.UnityAdsBanner

/**
 * Unity Ads Banner wrapper composable.
 * Dipasang di layar mana pun yang butuh banner iklan.
 */
@Composable
fun AdBanner(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
) {
    UnityAdsBanner(modifier = modifier)
}
