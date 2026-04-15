package dev.aether.manager.ads

import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

/**
 * Unity Ads Banner — auto-init SDK lalu langsung load iklan.
 *
 * Penggunaan:
 *   AdBannerView()
 *   AdBannerView(modifier = Modifier.fillMaxWidth())
 */
@Composable
fun AdBannerView(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
) {
    val context = LocalContext.current

    // BannerView yang di-remember agar tidak re-create tiap recompose
    val bannerView = remember {
        BannerView(context, AdManager.BANNER_PLACEMENT_ID, UnityBannerSize.getDynamicSize(context))
    }

    DisposableEffect(Unit) {
        // Init Unity Ads lalu langsung load banner
        if (!UnityAds.isInitialized()) {
            UnityAds.initialize(
                context,
                AdManager.GAME_ID,
                AdManager.isTestMode,
                object : IUnityAdsInitializationListener {
                    override fun onInitializationComplete() {
                        loadBanner(bannerView)
                    }
                    override fun onInitializationFailed(
                        error: UnityAds.UnityAdsInitializationError?,
                        message: String?
                    ) { /* silent fail */ }
                }
            )
        } else {
            loadBanner(bannerView)
        }

        onDispose {
            bannerView.destroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { _ ->
            FrameLayout(context).also { container ->
                bannerView.parent?.let { (it as? FrameLayout)?.removeView(bannerView) }
                container.addView(bannerView)
            }
        }
    )
}

private fun loadBanner(banner: BannerView) {
    banner.listener = object : BannerView.IListener {
        override fun onBannerLoaded(bannerAdView: BannerView?) { /* loaded */ }
        override fun onBannerClick(bannerAdView: BannerView?) { /* clicked */ }
        override fun onBannerFailedToLoad(bannerAdView: BannerView?, errorInfo: BannerErrorInfo?) { /* silent */ }
        override fun onBannerLeftApplication(bannerAdView: BannerView?) { /* left app */ }
    }
    banner.load()
}
