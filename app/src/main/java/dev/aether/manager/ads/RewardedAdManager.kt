package dev.aether.manager.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions

/**
 * Manages Unity Ads Rewarded Ad (SDK v4+).
 */
object RewardedAdManager {

    private const val TAG     = "UnityRewarded"
    private val     PLACEMENT = AdManager.REWARDED_PLACEMENT

    @Volatile private var isLoaded  = false
    @Volatile private var isLoading = false

    private val loadListener = object : IUnityAdsLoadListener {
        override fun onUnityAdsAdLoaded(placementId: String) {
            isLoaded  = true
            isLoading = false
            Log.d(TAG, "Ad loaded ✓ placement=$placementId")
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

    /** Pre-load rewarded ad. Aman dipanggil berkali-kali. */
    fun preload(context: Context) {
        if (!UnityAds.isInitialized) {
            Log.w(TAG, "Skipping preload — Unity not initialized yet")
            return
        }
        if (isLoaded || isLoading) return
        isLoading = true
        Log.d(TAG, "Preloading placement=$PLACEMENT")
        UnityAds.load(PLACEMENT, loadListener)
    }

    /**
     * Tampilkan rewarded ad jika sudah siap.
     *
     * @param activity    Foreground Activity
     * @param onRewarded  Dipanggil saat user menyelesaikan iklan (COMPLETED)
     * @param onDone      Dipanggil setelah ad selesai/gagal
     */
    fun showIfReady(
        activity: Activity,
        onRewarded: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null
    ) {
        if (!UnityAds.isInitialized) {
            Log.w(TAG, "showIfReady skipped — Unity not initialized")
            onDone?.invoke()
            return
        }

        if (!isLoaded) {
            Log.d(TAG, "Rewarded: not ready, triggering preload")
            preload(activity)
            onDone?.invoke()
            return
        }

        isLoaded = false

        Log.d(TAG, "Showing rewarded ad…")
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
                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                    onRewarded?.invoke()
                }
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
