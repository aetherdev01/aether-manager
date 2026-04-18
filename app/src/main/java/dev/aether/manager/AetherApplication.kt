package dev.aether.manager

import android.app.Application
import android.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import dev.aether.manager.ads.AdManager
import dev.aether.manager.ads.InterstitialAdManager
import dev.aether.manager.ads.RewardedAdManager

class AetherApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Unity Ads SDK, lalu preload semua placement
        UnityAds.initialize(
            this,
            AdManager.GAME_ID,
            AdManager.isTestMode,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    Log.d("UnityAds", "SDK initialized ✓ testMode=${AdManager.isTestMode}")
                    // Preload interstitial & rewarded agar siap ditampilkan
                    InterstitialAdManager.preload(this@AetherApplication)
                    RewardedAdManager.preload(this@AetherApplication)
                }
                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String
                ) {
                    Log.w("UnityAds", "Init failed: $error – $message")
                }
            }
        )
    }
}
