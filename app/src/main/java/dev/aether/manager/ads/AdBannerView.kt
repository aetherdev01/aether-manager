package dev.aether.manager.ads

import android.app.Activity
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.unity3d.ads.UnityAds
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import kotlinx.coroutines.delay

/**
 * Unity Ads banner composable.
 * Waits for SDK initialization before loading — handles the async init race.
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

    var sdkReady by remember { mutableStateOf(UnityAds.isInitialized()) }

    // Poll until SDK is ready (handles the async init race condition)
    LaunchedEffect(sdkReady) {
        if (!sdkReady) {
            repeat(20) { // max ~10 seconds
                delay(500L)
                if (UnityAds.isInitialized()) {
                    sdkReady = true
                    return@LaunchedEffect
                }
            }
        }
    }

    DisposableEffect(sdkReady) {
        if (sdkReady) {
            loadBanner(bannerView)
        }
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
        override fun onBannerLoaded(bannerAdView: BannerView?)          { /* ok */ }
        override fun onBannerShown(bannerAdView: BannerView?)           { /* ok */ }
        override fun onBannerClick(bannerAdView: BannerView?)           { /* ok */ }
        override fun onBannerLeftApplication(bannerAdView: BannerView?) { /* ok */ }
        override fun onBannerFailedToLoad(
            bannerAdView: BannerView?,
            errorInfo: BannerErrorInfo?
        ) { /* silent fail */ }
    }
    banner.load()
}

