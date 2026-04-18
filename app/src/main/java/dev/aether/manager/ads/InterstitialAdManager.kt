package dev.aether.manager.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions

/**
 * Manages Unity Ads Interstitial with:
 * - Anti-spam cooldown (INTERSTITIAL_COOLDOWN_MS between shows)
 * - Delayed first show (FIRST_SHOW_DELAY_MS after launch)
 * - Checks Unity Ads ready state before showing
 */
object InterstitialAdManager {

    private const val TAG = "UnityInterstitial"
    private val PLACEMENT  = AdManager.INTERSTITIAL_PLACEMENT

    private val sessionStartMs = System.currentTimeMillis()
    private var lastShownMs    = 0L

    // ── Public API ────────────────────────────────────────────

    /**
     * Show interstitial if Unity Ads is ready.
     * Checks: initialized, loaded, session delay, cooldown.
     *
     * @param activity  Current foreground Activity
     * @param onDone    Called after ad finishes or is skipped
     */
    fun showIfReady(activity: Activity, onDone: ((dismissed: Boolean) -> Unit)? = null) {
        val now     = System.currentTimeMillis()
        val elapsed = now - sessionStartMs
        val gap     = now - lastShownMs

        when {
            !UnityAds.isReady(PLACEMENT) -> {
                Log.d(TAG, "Skip: placement not ready yet")
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
                Log.d(TAG, "Showing interstitial ▶")
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
                    }
                    override fun onUnityAdsShowFailure(
                        placementId: String,
                        error: UnityAds.UnityAdsShowError,
                        message: String
                    ) {
                        Log.w(TAG, "onShowFailure: $placementId error=$error msg=$message")
                        onDone?.invoke(true)
                    }
                })
            }
        }
    }
}
