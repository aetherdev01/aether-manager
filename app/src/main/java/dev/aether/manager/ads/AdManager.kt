package dev.aether.manager.ads

object AdManager {

    const val GAME_ID            = "6091240"
    const val REWARDED_PLACEMENT = "Rewarded_Android"

    val isTestMode: Boolean
        get() = dev.aether.manager.BuildConfig.DEBUG
}
