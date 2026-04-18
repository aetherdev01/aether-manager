package dev.aether.manager

import android.app.Application
import android.util.Log
import com.topjohnwu.superuser.Shell
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.UnityAds
import dev.aether.manager.ads.AdManager
import dev.aether.manager.ads.InterstitialAdManager

class AetherApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initLibsu()
        initUnityAds()
    }

    private fun initLibsu() {
        // Konfigurasi libsu — harus dipanggil sebelum Shell.getShell() pertama kali
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)  // stderr masuk ke out juga
                .setTimeout(10)                         // timeout 10 detik per command
        )
        Log.d("AetherApp", "libsu Shell builder configured")
    }

    private fun initUnityAds() {
        if (UnityAds.isInitialized) {
            Log.d("AetherApp", "Unity Ads already initialized")
            InterstitialAdManager.preload(this)
            return
        }

        Log.d("AetherApp", "Initializing Unity Ads — gameId=${AdManager.GAME_ID} testMode=${AdManager.isTestMode}")
        UnityAds.initialize(
            this,
            AdManager.GAME_ID,
            AdManager.isTestMode,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    Log.d("AetherApp", "Unity Ads initialized ✓")
                    InterstitialAdManager.preload(this@AetherApplication)
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String
                ) {
                    Log.e("AetherApp", "Unity Ads init failed: $error – $message")
                }
            }
        )
    }
}
