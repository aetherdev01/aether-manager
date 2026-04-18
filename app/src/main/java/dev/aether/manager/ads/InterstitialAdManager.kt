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
 * - preload() dipanggil otomatis setelah Unity init & setelah setiap show selesai.
 * - showIfReady() hanya tampil jika sudah loaded — tidak spam.
 */
object InterstitialAdManager {

    private const val TAG       = "UnityInterstitial"
    private const val PLACEMENT = "Interstitial_Android"

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
            // Retry load setelah gagal (delay handled by Unity SDK)
        }
    }

    /**
     * Preload interstitial ad. Aman dipanggil berkali-kali.
     * Hanya memuat jika belum loaded/loading dan Unity sudah init.
     */
    fun preload(context: Context) {
        if (!UnityAds.isInitialized) {
            Log.w(TAG, "Skipping preload — Unity not initialized yet")
            return
        }
        if (isLoaded || isLoading) {
            Log.d(TAG, "Preload skipped — already loaded=$isLoaded loading=$isLoading")
            return
        }
        isLoading = true
        Log.d(TAG, "Preloading placement=$PLACEMENT")
        UnityAds.load(PLACEMENT, loadListener)
    }

    /**
     * Tampilkan iklan jika sudah siap.
     * Jika belum siap, trigger preload dan lanjutkan tanpa blokir.
     */
    fun showIfReady(
        activity: Activity,
        onDone: (() -> Unit)? = null
    ) {
        if (!UnityAds.isInitialized) {
            Log.w(TAG, "showIfReady skipped — Unity not initialized")
            onDone?.invoke()
            return
        }

        if (!isLoaded) {
            Log.d(TAG, "showIfReady: not ready, triggering preload")
            preload(activity)
            onDone?.invoke()
            return
        }

        // Consumed — set false dulu sebelum show
        isLoaded = false

        Log.d(TAG, "Showing interstitial…")
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
                // Preload untuk iklan berikutnya
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
