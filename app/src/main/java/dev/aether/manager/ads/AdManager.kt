package dev.aether.manager.ads

/**
 * Central AdMob configuration.
 * App ID  : ca-app-pub-5043818314955328~2380701071
 * Ad Unit : ca-app-pub-5043818314955328/2309164833 (Interstitial)
 */
object AdManager {

    // ── Credentials ───────────────────────────────────────────
    const val APP_ID              = "ca-app-pub-5043818314955328~2380701071"
    const val INTERSTITIAL_AD_ID  = "ca-app-pub-5043818314955328/2309164833"

    // ── Anti-spam policy ──────────────────────────────────────
    /** Minimum gap (ms) between two interstitial shows. */
    const val INTERSTITIAL_COOLDOWN_MS = 1L * 60_000L   // 1 min

    /** Minimum session time (ms) before first interstitial. */
    const val FIRST_SHOW_DELAY_MS      = 1L * 60_000L   // 1 min after launch

    /** true = test mode (debug build), false = production */
    val isTestMode: Boolean
        get() = dev.aether.manager.BuildConfig.DEBUG
}
