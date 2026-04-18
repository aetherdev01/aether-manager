package dev.aether.manager.ads

import android.app.Activity
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

/**
 * Unity Ads Banner composable.
 * Menampilkan banner di mana pun composable ini dipasang.
 */
@Composable
fun UnityAdsBanner(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
) {
    val context  = LocalContext.current
    val activity = context as? Activity ?: return

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val container = FrameLayout(ctx)
            val banner = BannerView(
                activity,
                AdManager.BANNER_PLACEMENT,
                UnityBannerSize.getDynamicSize(ctx)
            )
            banner.listener = object : BannerView.IListener {
                override fun onBannerLoaded(bannerAdView: BannerView) {}
                override fun onBannerShown(bannerAdView: BannerView) {}
                override fun onBannerClick(bannerAdView: BannerView) {}
                override fun onBannerFailedToLoad(bannerAdView: BannerView, errorInfo: BannerErrorInfo) {}
                override fun onBannerLeftApplication(bannerView: BannerView) {}
            }
            banner.load()
            container.addView(banner)
            container
        }
    )
}

/**
 * Interstitial ad trigger composable.
 * Trigger interstitial saat key berubah, dengan cooldown & session delay.
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
