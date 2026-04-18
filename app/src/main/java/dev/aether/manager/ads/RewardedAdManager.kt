package dev.aether.manager.ads

import android.app.Activity
import android.util.Log
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions

/**
 * Manages Unity Ads Rewarded Ad.
 * Show hanya saat placement sudah ready.
 */
object RewardedAdManager {

    private const val TAG = "UnityRewarded"
    private val PLACEMENT  = AdManager.REWARDED_PLACEMENT

    /**
     * Show rewarded ad jika ready.
     *
     * @param activity    Current foreground Activity
     * @param onRewarded  Dipanggil saat user menyelesaikan iklan (reward granted)
     * @param onDone      Dipanggil setelah ad selesai/gagal
     */
    fun showIfReady(
        activity: Activity,
        onRewarded: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null
    ) {
        if (!UnityAds.isReady(PLACEMENT)) {
            Log.d(TAG, "Skip: placement not ready")
            onDone?.invoke()
            return
        }

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
            }
            override fun onUnityAdsShowFailure(
                placementId: String,
                error: UnityAds.UnityAdsShowError,
                message: String
            ) {
                Log.w(TAG, "onShowFailure: $placementId error=$error msg=$message")
                onDone?.invoke()
            }
        })
    }
}
