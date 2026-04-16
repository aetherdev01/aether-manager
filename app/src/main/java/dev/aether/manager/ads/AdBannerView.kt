package dev.aether.manager.ads

import android.app.Activity
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.unity3d.ads.UnityAds
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

/**
 * Unity Ads banner composable.
 * Relies on AetherApplication to have initialized the SDK already —
 * so no double-init here. Just creates the BannerView and loads it.
 */
@Composable
fun AdBannerView(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
) {
    val context  = LocalContext.current
    val activity = context as? Activity ?: return

    val bannerView = remember {
        BannerView(activity, AdManager.BANNER_PLACEMENT_ID, UnityBannerSize.getDynamicSize(context))
    }

    DisposableEffect(Unit) {
        // SDK is already initialized by AetherApplication; just load the banner.
        if (UnityAds.isInitialized) {
            loadBanner(bannerView)
        }
        // If for some reason SDK isn't ready yet, banner will stay empty — no crash.
        onDispose {
            bannerView.destroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory  = { _ ->
            FrameLayout(context).also { container ->
                (bannerView.parent as? FrameLayout)?.removeView(bannerView)
                container.addView(bannerView)
            }
        }
    )
}

private fun loadBanner(banner: BannerView) {
    banner.listener = object : BannerView.IListener {
        override fun onBannerLoaded(bannerAdView: BannerView?)         { /* ok */ }
        override fun onBannerShown(bannerAdView: BannerView?)          { /* ok */ }
        override fun onBannerClick(bannerAdView: BannerView?)          { /* ok */ }
        override fun onBannerLeftApplication(bannerAdView: BannerView?) { /* ok */ }
        override fun onBannerFailedToLoad(
            bannerAdView: BannerView?,
            errorInfo: BannerErrorInfo?
        ) { /* silent fail — banner just won't show */ }
    }
    banner.load()
}
