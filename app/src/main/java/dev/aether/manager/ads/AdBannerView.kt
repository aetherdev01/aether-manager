package dev.aether.manager.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Banner AdMob — adaptive width, BANNER height.
 *
 * Penggunaan:
 *   AdBannerView()                          // di mana saja dalam Composable
 *   AdBannerView(modifier = Modifier.fillMaxWidth())
 */
@Composable
fun AdBannerView(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdManager.bannerId
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            // Reload jika view di-recompose (misal orientasi berubah)
            adView.loadAd(AdRequest.Builder().build())
        }
    )
}
