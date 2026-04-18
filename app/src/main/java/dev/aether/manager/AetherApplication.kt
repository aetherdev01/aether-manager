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

        UnityAds.initialize(
            this,
            AdManager.GAME_ID,
            AdManager.isTestMode,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    Log.d("UnityAds", "SDK initialized ✓ testMode=${AdManager.isTestMode}")
                    InterstitialAdManager.preload(this@AetherApplication)
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
