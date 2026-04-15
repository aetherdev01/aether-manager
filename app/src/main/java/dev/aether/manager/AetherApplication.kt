package dev.aether.manager

import android.app.Application
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import dev.aether.manager.ads.AdManager

class AetherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Unity Ads SDK saat app start
        UnityAds.initialize(
            this,
            AdManager.GAME_ID,
            AdManager.isTestMode,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() { /* ready */ }
                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError?,
                    message: String?
                ) { /* silent fail */ }
            }
        )
    }
}
