package dev.aether.manager.ads

/**
 * Central Unity Ads configuration.
 * Game ID       : 6091240
 * Ad Units      : Interstitial_Android | Banner_Android | Rewarded_Android
 */
object AdManager {

    // ── Credentials ───────────────────────────────────────────
    const val GAME_ID                = "6091240"
    const val INTERSTITIAL_PLACEMENT = "Interstitial_Android"
    const val BANNER_PLACEMENT       = "Banner_Android"
    const val REWARDED_PLACEMENT     = "Rewarded_Android"

    // ── Anti-spam policy ──────────────────────────────────────
    /** Minimum gap (ms) between two interstitial shows. */
    const val INTERSTITIAL_COOLDOWN_MS = 60_000L    // 1 menit

    /** Minimum session time (ms) before first interstitial. */
    const val FIRST_SHOW_DELAY_MS      = 30_000L    // 30 detik setelah launch

    /** true = test mode (debug build), false = production */
    val isTestMode: Boolean
        get() = dev.aether.manager.BuildConfig.DEBUG
}
