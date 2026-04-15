package dev.aether.manager.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import dev.aether.manager.ads.AdManager

@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    adSize: AdSize = AdSize.BANNER
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(adSize)
                adUnitId = AdManager.bannerId   // ambil dari native layer
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
