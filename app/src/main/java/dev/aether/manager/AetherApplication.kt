package dev.aether.manager

import android.app.Application
import android.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import dev.aether.manager.ads.AdManager
import dev.aether.manager.ads.InterstitialAdManager

class AetherApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Unity Ads once at app start with the correct Game ID.
        // Banner & interstitial composables rely on this being ready.
        UnityAds.initialize(
            this,
            AdManager.GAME_ID,          // 6091240
            AdManager.isTestMode,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    Log.d("UnityAds", "SDK initialized ✓ (gameId=${AdManager.GAME_ID})")
                    // Preload interstitial silently so it's ready when needed.
                    InterstitialAdManager.preload()
                }
                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError?,
                    message: String?
                ) {
                    Log.w("UnityAds", "Init failed: $error – $message")
                }
            }
        )
    }
}
