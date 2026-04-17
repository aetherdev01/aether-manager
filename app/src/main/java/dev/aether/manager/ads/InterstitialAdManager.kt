package dev.aether.manager.ads

import android.app.Activity
import android.util.Log
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds

/**
 * Manages Unity Ads Interstitial with:
 * - Anti-spam cooldown (INTERSTITIAL_COOLDOWN_MS between shows)
 * - Delayed first show (FIRST_SHOW_DELAY_MS after launch)
 * - Preloads next ad after each show for instant availability
 * - Fully skippable (Unity Interstitial is skip-capable by default)
 */
object InterstitialAdManager {

    private const val TAG = "InterstitialAd"

    private val sessionStartMs  = System.currentTimeMillis()
    private var lastShownMs     = 0L
    private var isLoaded        = false
    private var isLoading       = false

    // ── Public API ────────────────────────────────────────────

    /** Pre-load ad so it's ready when needed. Safe to call multiple times. */
    fun preload() {
        if (isLoaded || isLoading) return
        isLoading = true
        UnityAds.load(
            AdManager.INTERSTITIAL_PLACEMENT_ID,
            object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String?) {
                    isLoaded  = true
                    isLoading = false
                    Log.d(TAG, "Interstitial loaded ✓")
                }
                override fun onUnityAdsFailedToLoad(
                    placementId: String?,
                    error: UnityAds.UnityAdsLoadError?,
                    message: String?
                ) {
                    isLoaded  = false
                    isLoading = false
                    Log.w(TAG, "Load failed: $error – $message")
                }
            }
        )
    }

    /**
     * Show interstitial if:
     *  - SDK initialized
     *  - Ad is loaded
     *  - Session >= FIRST_SHOW_DELAY_MS
     *  - Last show >= INTERSTITIAL_COOLDOWN_MS ago
     *
     * @param activity  Current foreground Activity
     * @param onDone    Called after ad completes or is skipped (success = not skipped early)
     */
    fun showIfReady(activity: Activity, onDone: ((skipped: Boolean) -> Unit)? = null) {
        val now     = System.currentTimeMillis()
        val elapsed = now - sessionStartMs
        val gap     = now - lastShownMs

        when {
            !UnityAds.isInitialized -> {
                Log.d(TAG, "Skip: SDK not initialized")
                onDone?.invoke(true)
            }
            !isLoaded -> {
                Log.d(TAG, "Skip: ad not loaded yet")
                preload()          // try to load for next time
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
                isLoaded    = false     // mark consumed; preload will refill
                UnityAds.show(
                    activity,
                    AdManager.INTERSTITIAL_PLACEMENT_ID,
                    object : IUnityAdsShowListener {
                        override fun onUnityAdsShowStart(placementId: String?) {
                            Log.d(TAG, "Interstitial started")
                        }
                        override fun onUnityAdsShowClick(placementId: String?) {}
                        override fun onUnityAdsShowComplete(
                            placementId: String?,
                            state: UnityAds.UnityAdsShowCompletionState?
                        ) {
                            val skipped = state == UnityAds.UnityAdsShowCompletionState.SKIPPED
                            Log.d(TAG, "Interstitial done, skipped=$skipped")
                            onDone?.invoke(skipped)
                            preload()  // preload next ad silently
                        }
                        override fun onUnityAdsShowFailure(
                            placementId: String?,
                            error: UnityAds.UnityAdsShowError?,
                            message: String?
                        ) {
                            Log.w(TAG, "Show failed: $error – $message")
                            onDone?.invoke(true)
                            isLoaded = false
                            preload()
                        }
                    }
                )
            }
        }
    }
}
