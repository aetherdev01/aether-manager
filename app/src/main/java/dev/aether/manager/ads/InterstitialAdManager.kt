package dev.aether.manager.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Manages AdMob Interstitial with:
 * - Anti-spam cooldown (INTERSTITIAL_COOLDOWN_MS between shows)
 * - Delayed first show (FIRST_SHOW_DELAY_MS after launch)
 * - Preloads next ad after each show for instant availability
 */
object InterstitialAdManager {

    private const val TAG = "InterstitialAd"

    private val sessionStartMs = System.currentTimeMillis()
    private var lastShownMs    = 0L
    private var interstitialAd: InterstitialAd? = null
    private var isLoading      = false

    // ── Public API ────────────────────────────────────────────

    /** Pre-load ad. Safe to call multiple times. */
    fun preload(context: Context) {
        if (interstitialAd != null || isLoading) return
        isLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            AdManager.INTERSTITIAL_AD_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    Log.d(TAG, "Interstitial loaded ✓")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    Log.w(TAG, "Load failed: ${error.message}")
                }
            }
        )
    }

    /**
     * Show interstitial if ready.
     * Checks: initialized, loaded, session delay, cooldown.
     *
     * @param activity  Current foreground Activity
     * @param onDone    Called after ad finishes or is dismissed
     */
    fun showIfReady(activity: Activity, onDone: ((dismissed: Boolean) -> Unit)? = null) {
        val now     = System.currentTimeMillis()
        val elapsed = now - sessionStartMs
        val gap     = now - lastShownMs

        val ad = interstitialAd

        when {
            ad == null -> {
                Log.d(TAG, "Skip: ad not loaded yet")
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
                lastShownMs    = now
                interstitialAd = null   // mark consumed; will refill after show

                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Interstitial dismissed")
                        onDone?.invoke(true)
                        preload(activity)   // silently preload next
                    }
                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.w(TAG, "Show failed: ${adError.message}")
                        onDone?.invoke(true)
                        preload(activity)
                    }
                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Interstitial showing")
                    }
                }
                ad.show(activity)
            }
        }
    }
}
