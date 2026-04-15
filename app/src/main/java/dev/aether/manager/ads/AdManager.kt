package dev.aether.manager.ads

/**
 * Central place for Unity Ads configuration.
 * Game ID & placement IDs Unity Ads.
 */
object AdManager {

    // Unity Ads credentials
    const val GAME_ID = "6091240"
    const val BANNER_PLACEMENT_ID = "Banner_Android"

    // true = production, false = test mode (debug build)
    val isTestMode: Boolean
        get() = dev.aether.manager.BuildConfig.DEBUG
}
