package dev.aether.manager.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions

/**
 * Manages Unity Ads Interstitial Ad (SDK v4+).
 * Preload via IUnityAdsLoadListener, show when ready.
 */
object InterstitialAdManager {

    private const val TAG       = "UnityInterstitial"
    private const val PLACEMENT = "Interstitial_Android"

    private var isLoaded  = false
    private var isLoading = false

    private val loadListener = object : IUnityAdsLoadListener {
        override fun onUnityAdsAdLoaded(placementId: String) {
            isLoaded  = true
            isLoading = false
            Log.d(TAG, "Loaded ✓ placement=$placementId")
        }
        override fun onUnityAdsFailedToLoad(
            placementId: String,
            error: UnityAds.UnityAdsLoadError,
            message: String
        ) {
            isLoaded  = false
            isLoading = false
            Log.w(TAG, "Load failed: $error – $message")
        }
    }

    fun preload(context: Context) {
        if (isLoaded || isLoading) return
        isLoading = true
        UnityAds.load(PLACEMENT, loadListener)
        Log.d(TAG, "Loading placement=$PLACEMENT")
    }

    fun showIfReady(
        activity: Activity,
        onDone: (() -> Unit)? = null
    ) {
        if (!isLoaded) {
            Log.d(TAG, "Skip: not loaded yet")
            preload(activity)
            onDone?.invoke()
            return
        }

        isLoaded = false

        UnityAds.show(activity, PLACEMENT, UnityAdsShowOptions(), object : IUnityAdsShowListener {
            override fun onUnityAdsShowStart(placementId: String) {
                Log.d(TAG, "onShowStart: $placementId")
            }
            override fun onUnityAdsShowClick(placementId: String) {
                Log.d(TAG, "onShowClick: $placementId")
            }
            override fun onUnityAdsShowComplete(
                placementId: String,
                state: UnityAds.UnityAdsShowCompletionState
            ) {
                Log.d(TAG, "onShowComplete: $placementId state=$state")
                onDone?.invoke()
                preload(activity)
            }
            override fun onUnityAdsShowFailure(
                placementId: String,
                error: UnityAds.UnityAdsShowError,
                message: String
            ) {
                Log.w(TAG, "onShowFailure: $placementId error=$error msg=$message")
                onDone?.invoke()
                preload(activity)
            }
        })
    }
}
