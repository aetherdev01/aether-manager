package dev.aether.manager.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions

/**
 * Manages Unity Ads Interstitial (SDK v4+).
 * - Preloads via IUnityAdsLoadListener
 * - Anti-spam cooldown + session delay
 * - Auto-reloads after each show
 */
object InterstitialAdManager {

    private const val TAG      = "UnityInterstitial"
    private val     PLACEMENT  = AdManager.INTERSTITIAL_PLACEMENT

    private val sessionStartMs = System.currentTimeMillis()
    private var lastShownMs    = 0L
    private var isLoaded       = false
    private var isLoading      = false

    // ── Load listener ─────────────────────────────────────────

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

    // ── Public API ────────────────────────────────────────────

    /** Pre-load ad. Safe to call multiple times. */
    fun preload(context: Context) {
        if (isLoaded || isLoading) return
        isLoading = true
        UnityAds.load(PLACEMENT, loadListener)
        Log.d(TAG, "Loading placement=$PLACEMENT")
    }

    /**
     * Show interstitial if loaded and cooldown passed.
     *
     * @param activity  Current foreground Activity
     * @param onDone    Called after ad finishes or is skipped
     */
    fun showIfReady(activity: Activity, onDone: ((dismissed: Boolean) -> Unit)? = null) {
        val now     = System.currentTimeMillis()
        val elapsed = now - sessionStartMs
        val gap     = now - lastShownMs

        when {
            !isLoaded -> {
                Log.d(TAG, "Skip: not loaded yet")
                preload(activity)
                onDone?.invoke(true)
            }
            elapsed < AdManager.FIRST_SHOW_DELAY_MS -> {
                Log.d(TAG, "Skip: too soon after launch (${elapsed / 1000}s)")
                onDone?.invoke(true)
            }
            lastShownMs != 0L && gap < AdManager.INTERSTITIAL_COOLDOWN_MS -> {
                Log.d(TAG, "Skip: cooldown (${gap / 1000}s / ${AdManager.INTERSTITIAL_COOLDOWN_MS / 1000}s)")
                onDone?.invoke(true)
            }
            else -> {
                lastShownMs = now
                isLoaded    = false   // consumed; reload after show

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
                        onDone?.invoke(true)
                        preload(activity)   // reload untuk berikutnya
                    }
                    override fun onUnityAdsShowFailure(
                        placementId: String,
                        error: UnityAds.UnityAdsShowError,
                        message: String
                    ) {
                        Log.w(TAG, "onShowFailure: $placementId error=$error msg=$message")
                        onDone?.invoke(true)
                        preload(activity)
                    }
                })
            }
        }
    }
}
