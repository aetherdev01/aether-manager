package dev.aether.manager

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import dev.aether.manager.ads.InterstitialAdManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AetherApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize AdMob SDK on background thread, then preload interstitial.
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@AetherApplication) { initStatus ->
                Log.d("AdMob", "SDK initialized ✓ status=$initStatus")
                // Preload interstitial after SDK ready
                InterstitialAdManager.preload(this@AetherApplication)
            }
        }
    }
}
