package dev.aether.manager

import android.app.Application
import com.google.android.gms.ads.MobileAds

class AetherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize AdMob SDK
        MobileAds.initialize(this)
    }
}
